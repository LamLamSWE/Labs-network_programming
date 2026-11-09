package com.lam.mail;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class MailClient {
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 5000;
    private static final int BUFFER_SIZE = 8192;
    private static final int TIMEOUT_MS = 3000;

    private final String serverHost;
    private final int serverPort;
    private String currentLoggedInUser = null;

    public MailClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getCurrentLoggedInUser() {
        return currentLoggedInUser;
    }

    public void setCurrentLoggedInUser(String currentLoggedInUser) {
        this.currentLoggedInUser = currentLoggedInUser;
    }

    public String sendAndReceive(String message) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);

            byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName(serverHost);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, serverPort);
            socket.send(sendPacket);

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(receivePacket);

            return new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8).trim();
        }
    }

    // --- Helper methods for GUI & API integration (reusing sendAndReceive) ---

    public String createAccount(String username) throws IOException {
        return sendAndReceive("CREATE_ACCOUNT|" + username);
    }

    public String login(String username) throws IOException {
        String response = sendAndReceive("LOGIN|" + username);
        if (response.startsWith("FILES")) {
            this.currentLoggedInUser = username;
        }
        return response;
    }

    public String sendEmail(String sender, String receiver, String message) throws IOException {
        return sendAndReceive("SEND_EMAIL|" + sender + "|" + receiver + "|" + message);
    }

    public String getMailbox(String username) throws IOException {
        return sendAndReceive("LOGIN|" + username);
    }

    public String readEmail(String username, String filename) throws IOException {
        return sendAndReceive("READ_EMAIL|" + username + "|" + filename);
    }

    // --- Console UI Display and Interactive Methods ---

    public static void displayMailbox(String username, String response) {
        System.out.println();
        System.out.println("=================================");
        System.out.println("       MAILBOX" + (username != null ? " (" + username + ")" : ""));
        System.out.println("=================================");

        if (response.equals("FILES") || response.equals("FILES|")) {
            System.out.println("(Mailbox is empty)");
        } else if (response.startsWith("FILES|")) {
            String[] files = response.substring("FILES|".length()).split("\\|", -1);
            for (int i = 0; i < files.length; i++) {
                System.out.println((i + 1) + ". " + files[i]);
            }
        } else {
            System.out.println("Invalid mailbox response: " + response);
        }
        System.out.println("=================================");
        System.out.println();
    }

    public void startInteractive(Scanner scanner) {
        System.out.println("Connected to Mail Server at " + serverHost + ":" + serverPort);

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Choose: ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String choiceStr = scanner.nextLine().trim();
            if (choiceStr.isEmpty()) {
                continue;
            }

            switch (choiceStr) {
                case "1":
                    handleCreateAccount(scanner);
                    break;
                case "2":
                    handleLogin(scanner);
                    break;
                case "3":
                    handleSendEmail(scanner);
                    break;
                case "4":
                    System.out.println("Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please choose from 1 to 4.\n");
                    break;
            }
        }
    }

    private void printMenu() {
        System.out.println("=================================");
        System.out.println("       UDP MAIL CLIENT");
        if (currentLoggedInUser != null) {
            System.out.println("   (Logged in as: " + currentLoggedInUser + ")");
        }
        System.out.println("=================================");
        System.out.println("1. Create account");
        System.out.println("2. Login");
        System.out.println("3. Send email");
        System.out.println("4. Exit");
        System.out.println("=================================");
    }

    private void handleCreateAccount(Scanner scanner) {
        System.out.print("Enter username: ");
        if (!scanner.hasNextLine()) return;
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("[CLIENT] Username cannot be empty.\n");
            return;
        }

        try {
            String response = createAccount(username);
            if (response.startsWith("SUCCESS|")) {
                System.out.println("[SUCCESS] " + response.substring("SUCCESS|".length()) + "\n");
            } else if (response.startsWith("ERROR|")) {
                System.out.println("[ERROR] " + response.substring("ERROR|".length()) + "\n");
            } else {
                System.out.println("[RESPONSE] " + response + "\n");
            }
        } catch (SocketTimeoutException e) {
            System.err.println("[TIMEOUT] Server did not respond within timeout (3s).\n");
        } catch (IOException e) {
            System.err.println("[ERROR] Network error: " + e.getMessage() + "\n");
        }
    }

    private void handleLogin(Scanner scanner) {
        System.out.print("Enter username: ");
        if (!scanner.hasNextLine()) return;
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("[CLIENT] Username cannot be empty.\n");
            return;
        }

        try {
            String response = login(username);
            if (response.startsWith("FILES")) {
                displayMailbox(username, response);
            } else if (response.startsWith("ERROR|")) {
                System.out.println("[ERROR] " + response.substring("ERROR|".length()) + "\n");
            } else {
                System.out.println("[RESPONSE] " + response + "\n");
            }
        } catch (SocketTimeoutException e) {
            System.err.println("[TIMEOUT] Server did not respond within timeout (3s).\n");
        } catch (IOException e) {
            System.err.println("[ERROR] Network error: " + e.getMessage() + "\n");
        }
    }

    private void handleSendEmail(Scanner scanner) {
        String sender;
        if (currentLoggedInUser != null) {
            System.out.print("Sender [" + currentLoggedInUser + "]: ");
            if (!scanner.hasNextLine()) return;
            String input = scanner.nextLine().trim();
            sender = input.isEmpty() ? currentLoggedInUser : input;
        } else {
            System.out.print("Sender: ");
            if (!scanner.hasNextLine()) return;
            sender = scanner.nextLine().trim();
        }

        if (sender.isEmpty()) {
            System.out.println("[CLIENT] Sender cannot be empty.\n");
            return;
        }

        System.out.print("Receiver: ");
        if (!scanner.hasNextLine()) return;
        String receiver = scanner.nextLine().trim();
        if (receiver.isEmpty()) {
            System.out.println("[CLIENT] Receiver cannot be empty.\n");
            return;
        }

        System.out.print("Message: ");
        if (!scanner.hasNextLine()) return;
        String message = scanner.nextLine();
        if (message.trim().isEmpty()) {
            System.out.println("[CLIENT] Message cannot be empty.\n");
            return;
        }

        try {
            String response = sendEmail(sender, receiver, message);
            if (response.startsWith("SUCCESS|")) {
                System.out.println("[SUCCESS] " + response.substring("SUCCESS|".length()) + "\n");
            } else if (response.startsWith("ERROR|")) {
                System.out.println("[ERROR] " + response.substring("ERROR|".length()) + "\n");
            } else {
                System.out.println("[RESPONSE] " + response + "\n");
            }
        } catch (SocketTimeoutException e) {
            System.err.println("[TIMEOUT] Server did not respond within timeout (3s).\n");
        } catch (IOException e) {
            System.err.println("[ERROR] Network error: " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        // If single argument containing pipe or PING, run in one-shot CLI mode for testing/automation
        if (args.length == 1 && (args[0].contains("|") || args[0].equalsIgnoreCase("PING"))) {
            MailClient client = new MailClient(DEFAULT_HOST, DEFAULT_PORT);
            try {
                System.out.println("[CLIENT] Sending: " + args[0] + " to " + DEFAULT_HOST + ":" + DEFAULT_PORT);
                String response = client.sendAndReceive(args[0]);
                if (response.startsWith("FILES")) {
                    displayMailbox(null, response);
                } else {
                    System.out.println("[CLIENT] Response from server: " + response);
                }
            } catch (IOException e) {
                System.err.println("[CLIENT] Error: " + e.getMessage());
            }
            return;
        }

        // Host / Port configuration
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length >= 2) {
            host = args[0];
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1] + ", using default " + DEFAULT_PORT);
            }
        }

        Scanner scanner = new Scanner(System.in);
        MailClient client = new MailClient(host, port);
        client.startInteractive(scanner);
    }
}
