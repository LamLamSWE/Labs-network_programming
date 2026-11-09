package com.lam.mail;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MailServer {
    public static final int PORT = 5000;
    public static final String DEFAULT_ACCOUNTS_DIR = "accounts";
    public static final String WELCOME_MESSAGE = "Thank you for using this service. we hope that you will feel comfortable........";
    private static final int BUFFER_SIZE = 8192;

    private final DatagramSocket socket;
    private final Path accountsDir;
    private volatile boolean running;

    public MailServer(int port) throws SocketException {
        this(port, Path.of(DEFAULT_ACCOUNTS_DIR));
    }

    public MailServer(int port, Path accountsDir) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.accountsDir = accountsDir;
        this.running = true;
        initAccountsDir();
    }

    private void initAccountsDir() {
        try {
            if (!Files.exists(accountsDir)) {
                Files.createDirectories(accountsDir);
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Failed to create accounts root directory: " + e.getMessage());
        }
    }

    public void start() {
        System.out.println("[SERVER] UDP Mail Server is running on port " + socket.getLocalPort() + "...");
        System.out.println("[SERVER] Mailbox directory: " + accountsDir.toAbsolutePath());
        byte[] buffer = new byte[BUFFER_SIZE];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String request = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                System.out.println("[SERVER] Received from " + clientAddress.getHostAddress() + ":" + clientPort + " -> " + request);

                String response = handleRequest(request);

                byte[] responseData = response.getBytes(StandardCharsets.UTF_8);
                DatagramPacket responsePacket = new DatagramPacket(
                        responseData,
                        responseData.length,
                        clientAddress,
                        clientPort
                );
                socket.send(responsePacket);
                System.out.println("[SERVER] Sent to " + clientAddress.getHostAddress() + ":" + clientPort + " -> " + response);

            } catch (IOException e) {
                if (!running) {
                    System.out.println("[SERVER] Server stopped.");
                    break;
                }
                System.err.println("[SERVER] Error: " + e.getMessage());
            }
        }
    }

    public String handleRequest(String request) {
        if (request == null || request.trim().isEmpty()) {
            return "ERROR|Empty request";
        }

        String[] parts = request.split("\\|", 4);
        String command = parts[0].trim().toUpperCase();

        switch (command) {
            case "PING":
                return "PONG";

            case "CREATE_ACCOUNT":
                if (parts.length < 2 || parts[1].trim().isEmpty()) {
                    return "ERROR|Username cannot be empty";
                }
                return handleCreateAccount(parts[1].trim());

            case "LOGIN":
                if (parts.length < 2 || parts[1].trim().isEmpty()) {
                    return "ERROR|Username cannot be empty";
                }
                return handleLogin(parts[1].trim());

            case "SEND_EMAIL":
                return handleSendEmail(parts);

            case "READ_EMAIL":
                if (parts.length < 2 || parts[1].trim().isEmpty()) {
                    return "ERROR|Username cannot be empty";
                }
                if (parts.length < 3 || parts[2].trim().isEmpty()) {
                    return "ERROR|Filename cannot be empty";
                }
                return handleReadEmail(parts[1].trim(), parts[2].trim());

            default:
                return "ERROR|Invalid command";
        }
    }

    private String handleCreateAccount(String username) {
        if (!isValidUsername(username)) {
            return "ERROR|Invalid username";
        }

        Path userDir = accountsDir.resolve(username);
        if (Files.exists(userDir)) {
            return "ERROR|Account already exists";
        }

        try {
            Files.createDirectories(userDir);
            Path welcomeFile = userDir.resolve("new_email.txt");
            Files.writeString(welcomeFile, WELCOME_MESSAGE, StandardCharsets.UTF_8);
            return "SUCCESS|Account created successfully";
        } catch (IOException e) {
            return "ERROR|Failed to create account: " + e.getMessage();
        }
    }

    private String handleLogin(String username) {
        if (!isValidUsername(username)) {
            return "ERROR|Invalid username";
        }

        Path userDir = accountsDir.resolve(username);
        if (!Files.exists(userDir) || !Files.isDirectory(userDir)) {
            return "ERROR|Account does not exist";
        }

        try (Stream<Path> stream = Files.list(userDir)) {
            List<String> files = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());

            if (files.isEmpty()) {
                return "FILES";
            }
            return "FILES|" + String.join("|", files);
        } catch (IOException e) {
            return "ERROR|Failed to read mailbox: " + e.getMessage();
        }
    }

    private String handleSendEmail(String[] parts) {
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            return "ERROR|Sender cannot be empty";
        }
        if (parts.length < 3 || parts[2].trim().isEmpty()) {
            return "ERROR|Receiver cannot be empty";
        }
        if (parts.length < 4 || parts[3].trim().isEmpty()) {
            return "ERROR|Message cannot be empty";
        }

        String sender = parts[1].trim();
        String receiver = parts[2].trim();
        String message = parts[3];

        if (!isValidUsername(sender)) {
            return "ERROR|Invalid sender username";
        }
        if (!isValidUsername(receiver)) {
            return "ERROR|Invalid receiver username";
        }

        Path senderDir = accountsDir.resolve(sender);
        if (!Files.exists(senderDir) || !Files.isDirectory(senderDir)) {
            return "ERROR|Sender account does not exist";
        }

        Path receiverDir = accountsDir.resolve(receiver);
        if (!Files.exists(receiverDir) || !Files.isDirectory(receiverDir)) {
            return "ERROR|Receiver account does not exist";
        }

        try {
            String fileName = getNextEmailFileName(receiverDir);
            Path emailFile = receiverDir.resolve(fileName);
            Files.writeString(emailFile, message, StandardCharsets.UTF_8);
            return "SUCCESS|Email sent successfully";
        } catch (IOException e) {
            return "ERROR|Failed to send email: " + e.getMessage();
        }
    }

    private String handleReadEmail(String username, String filename) {
        if (!isValidUsername(username)) {
            return "ERROR|Invalid username";
        }
        if (filename == null || filename.trim().isEmpty() || !filename.matches("^[a-zA-Z0-9_.-]+$") || filename.contains("..")) {
            return "ERROR|Invalid filename";
        }

        Path userDir = accountsDir.resolve(username);
        if (!Files.exists(userDir) || !Files.isDirectory(userDir)) {
            return "ERROR|Account does not exist";
        }

        Path emailFile = userDir.resolve(filename).normalize();
        if (!emailFile.toAbsolutePath().startsWith(userDir.toAbsolutePath().normalize())) {
            return "ERROR|Access denied";
        }

        if (!Files.exists(emailFile) || !Files.isRegularFile(emailFile)) {
            return "ERROR|Email file does not exist";
        }

        try {
            String content = Files.readString(emailFile, StandardCharsets.UTF_8);
            return "EMAIL_CONTENT|" + filename + "|" + content;
        } catch (IOException e) {
            return "ERROR|Failed to read email: " + e.getMessage();
        }
    }

    private synchronized String getNextEmailFileName(Path userDir) throws IOException {
        int maxIndex = 0;
        try (Stream<Path> stream = Files.list(userDir)) {
            List<Path> files = stream.filter(Files::isRegularFile).collect(Collectors.toList());
            for (Path file : files) {
                String name = file.getFileName().toString();
                if (name.matches("^email_\\d+\\.txt$")) {
                    String numPart = name.substring("email_".length(), name.length() - ".txt".length());
                    try {
                        int num = Integer.parseInt(numPart);
                        if (num > maxIndex) {
                            maxIndex = num;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        int nextIndex = maxIndex + 1;
        return String.format("email_%03d.txt", nextIndex);
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z0-9_-]+$");
    }

    public void stop() {
        this.running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public static void main(String[] args) {
        try {
            MailServer server = new MailServer(PORT);
            server.start();
        } catch (SocketException e) {
            System.err.println("[SERVER] Could not start server: " + e.getMessage());
        }
    }
}
