import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class MailServer extends JFrame {
    private static final int PORT = 5000;
    private static final Path BASE_DIR = Paths.get("server_data");
    private static final int PASSWORD_ITERATIONS = 120_000;
    private static final int PASSWORD_KEY_LENGTH = 256;
    private static final String PASSWORD_FILE = "password.txt";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String WELCOME =
            "Thank you for using this service. we hope that you will feel comfortable with our mail service.";

    private ServerSocket serverSocket;
    private ExecutorService pool;
    private volatile boolean running = false;
    private final AtomicInteger clientCount = new AtomicInteger(0);
    private final AtomicLong nextClientId = new AtomicLong(0);
    private final Map<Long, ClientInfo> connectedClients = new ConcurrentHashMap<>();

    private JLabel statusLabel;
    private JLabel clientCountLabel;
    private JLabel accountCountLabel;
    private JLabel fileCountLabel;
    private JTextArea logArea;
    private DefaultTableModel accountModel;
    private JTable accountTable;
    private DefaultTableModel clientModel;
    private JTable clientTable;
    private JButton startButton;
    private JButton stopButton;

    private final Color BG = new Color(245, 247, 251);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(79, 70, 229);
    private final Color PRIMARY_DARK = new Color(55, 48, 163);
    private final Color GREEN = new Color(5, 150, 105);
    private final Color RED = new Color(220, 38, 38);
    private final Color TEXT = new Color(17, 24, 39);
    private final Color MUTED = new Color(107, 114, 128);
    private final Color BORDER = new Color(229, 231, 235);

    public MailServer() {
        setTitle("Mail Server - Server Management");
        setSize(1000, 760);
        setMinimumSize(new Dimension(800, 640));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            Files.createDirectories(BASE_DIR);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }

        buildUI();
        refreshAccounts();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServer();
            }
        });
    }

    private void buildUI() {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(createHeader(), BorderLayout.NORTH);
        add(createMain(), BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD);
        header.setPreferredSize(new Dimension(0, 64));
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 14));
        left.setOpaque(false);

        JLabel icon = new JLabel("✉");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 25));
        icon.setForeground(PRIMARY);
        icon.setBorder(new EmptyBorder(0, 20, 0, 10));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Mail Server");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT);
        titleBox.add(title);

        left.add(icon);
        left.add(titleBox);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 13));
        right.setOpaque(false);

        statusLabel = new JLabel("●  STOPPED");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusLabel.setForeground(RED);

        startButton = primaryButton("START SERVER");
        startButton.addActionListener(e -> startServer());

        stopButton = dangerButton("■  STOP");
        stopButton.addActionListener(e -> stopServer());
        stopButton.setEnabled(false);

        right.add(statusLabel);
        right.add(startButton);
        right.add(stopButton);
        right.setBorder(new EmptyBorder(0, 0, 0, 16));

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private JPanel createMain() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);
        stats.add(statCard("CLIENTS ONLINE", "0", PRIMARY));
        stats.add(statCard("ACCOUNTS", "0", GREEN));
        stats.add(statCard("TOTAL FILES", "0", new Color(217, 119, 6)));

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        JPanel accountsCard = cardPanel();
        accountsCard.setLayout(new BorderLayout());

        JPanel accountsHeader = new JPanel(new BorderLayout());
        accountsHeader.setOpaque(false);
        accountsHeader.setBorder(new EmptyBorder(14, 16, 10, 16));

        JLabel accountTitle = sectionTitle("Accounts trên Server");
        JLabel accountHint = new JLabel("Các thư mục account được tạo bởi Client");
        accountHint.setForeground(MUTED);
        accountHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JPanel ah = new JPanel();
        ah.setOpaque(false);
        ah.setLayout(new BoxLayout(ah, BoxLayout.Y_AXIS));
        ah.add(accountTitle);
        ah.add(accountHint);

        JButton refresh = normalButton("↻  Làm mới");
        refresh.addActionListener(e -> refreshAccounts());

        accountsHeader.add(ah, BorderLayout.WEST);
        accountsHeader.add(refresh, BorderLayout.EAST);

        accountModel = new DefaultTableModel(new Object[]{"Account", "Số file", "Thư mục Server"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        accountTable = new JTable(accountModel);
        accountTable.setRowHeight(32);
        accountTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        accountTable.setForeground(TEXT);
        accountTable.setGridColor(BORDER);
        accountTable.setSelectionBackground(new Color(238, 242, 255));
        accountTable.setSelectionForeground(TEXT);
        accountTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        accountTable.getTableHeader().setBackground(new Color(249, 250, 251));

        JScrollPane tableScroll = new JScrollPane(accountTable);
        tableScroll.setBorder(new EmptyBorder(0, 16, 14, 16));

        accountsCard.add(accountsHeader, BorderLayout.NORTH);
        accountsCard.add(tableScroll, BorderLayout.CENTER);

        JPanel clientsCard = cardPanel();
        clientsCard.setPreferredSize(new Dimension(0, 132));
        clientsCard.setLayout(new BorderLayout());

        JPanel clientsHeader = new JPanel(new BorderLayout());
        clientsHeader.setOpaque(false);
        clientsHeader.setBorder(new EmptyBorder(10, 16, 7, 16));
        JLabel clientsTitle = sectionTitle("Client đang kết nối");
        JLabel clientsHint = new JLabel("Username và trạng thái xác thực; mật khẩu không được hiển thị.");
        clientsHint.setForeground(MUTED);
        clientsHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JPanel clientsTitleBox = new JPanel();
        clientsTitleBox.setOpaque(false);
        clientsTitleBox.setLayout(new BoxLayout(clientsTitleBox, BoxLayout.Y_AXIS));
        clientsTitleBox.add(clientsTitle);
        clientsTitleBox.add(clientsHint);
        clientsHeader.add(clientsTitleBox, BorderLayout.WEST);

        clientModel = new DefaultTableModel(new Object[]{"Username", "Client IP", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        clientTable = new JTable(clientModel);
        clientTable.setRowHeight(24);
        clientTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clientTable.setForeground(TEXT);
        clientTable.setGridColor(BORDER);
        clientTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 10));
        clientTable.getTableHeader().setBackground(new Color(249, 250, 251));
        JScrollPane clientsScroll = new JScrollPane(clientTable);
        clientsScroll.setBorder(new EmptyBorder(0, 16, 10, 16));
        clientsCard.add(clientsHeader, BorderLayout.NORTH);
        clientsCard.add(clientsScroll, BorderLayout.CENTER);

        JPanel logCard = cardPanel();
        logCard.setPreferredSize(new Dimension(0, 150));
        logCard.setLayout(new BorderLayout());

        JPanel logHeader = new JPanel(new BorderLayout());
        logHeader.setOpaque(false);
        logHeader.setBorder(new EmptyBorder(10, 16, 8, 16));

        JLabel logTitle = sectionTitle("Server Log");
        JButton clear = normalButton("Xóa log");
        clear.addActionListener(e -> logArea.setText(""));
        logHeader.add(logTitle, BorderLayout.WEST);
        logHeader.add(clear, BorderLayout.EAST);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setForeground(new Color(229, 231, 235));
        logArea.setBackground(new Color(17, 24, 39));
        logArea.setBorder(new EmptyBorder(10, 12, 10, 12));

        logCard.add(logHeader, BorderLayout.NORTH);
        logCard.add(new JScrollPane(logArea), BorderLayout.CENTER);

        center.add(clientsCard, BorderLayout.NORTH);
        center.add(accountsCard, BorderLayout.CENTER);
        center.add(logCard, BorderLayout.SOUTH);

        root.add(stats, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);

        return root;
    }

    private JPanel statCard(String title, String value, Color color) {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout());

        JLabel t = new JLabel(title);
        t.setForeground(MUTED);
        t.setFont(new Font("Segoe UI", Font.BOLD, 9));
        t.setBorder(new EmptyBorder(15, 18, 2, 18));

        JLabel v = new JLabel(value);
        v.setForeground(color);
        v.setFont(new Font("Segoe UI", Font.BOLD, 21));
        v.setBorder(new EmptyBorder(0, 18, 13, 18));

        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);

        if (title.equals("CLIENTS ONLINE")) clientCountLabel = v;
        if (title.equals("ACCOUNTS")) accountCountLabel = v;
        if (title.equals("TOTAL FILES")) fileCountLabel = v;

        return card;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 16));
        l.setForeground(TEXT);
        return l;
    }

    private JPanel cardPanel() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setBorder(new CompoundBorder(
                new LineBorder(BORDER),
                new EmptyBorder(0, 0, 0, 0)));
        return p;
    }

    private JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(PRIMARY_DARK);
        b.setForeground(Color.yellow);
        b.setFont(new Font("Segoe UI", Font.BOLD, 10));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        return b;
    }

    private JButton dangerButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(254, 242, 242));
        b.setForeground(RED);
        b.setFont(new Font("Segoe UI", Font.BOLD, 10));
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(
                new LineBorder(new Color(254, 202, 202)),
                new EmptyBorder(8, 13, 8, 13)));
        return b;
    }

    private JButton normalButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(249, 250, 251));
        b.setForeground(TEXT);
        b.setFont(new Font("Segoe UI", Font.BOLD, 10));
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(
                new LineBorder(BORDER),
                new EmptyBorder(8, 13, 8, 13)));
        return b;
    }

    private void startServer() {
        if (running) return;

        try {
            serverSocket = new ServerSocket(PORT);
            pool = Executors.newCachedThreadPool();
            running = true;

            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            statusLabel.setText("●  RUNNING");
            statusLabel.setForeground(GREEN);

            log("SERVER STARTED — Listening on port " + PORT);
            log("Data directory: " + BASE_DIR.toAbsolutePath());
            refreshAccounts();

            Thread acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        long clientId = nextClientId.incrementAndGet();
                        connectedClients.put(clientId, new ClientInfo(
                                clientId, socket.getInetAddress().getHostAddress()));
                        refreshClients();
                        int count = clientCount.incrementAndGet();
                        updateClientCount(count);

                        log("[+] Client connected: " +
                                socket.getInetAddress().getHostAddress());
                        pool.execute(new ClientHandler(socket, clientId));
                    } catch (IOException e) {
                        if (running) log("[!] Accept error: " + e.getMessage());
                    }
                }
            }, "MailServer-Acceptor");

            acceptThread.setDaemon(true);
            acceptThread.start();

        } catch (IOException e) {
            log("[ERROR] Cannot start server: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Không thể mở port " + PORT + "\n\n" + e.getMessage(),
                    "Server Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        if (!running) return;

        running = false;

        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}

        if (pool != null) pool.shutdownNow();

        connectedClients.clear();
        refreshClients();
        clientCount.set(0);
        updateClientCount(0);

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusLabel.setText("●  STOPPED");
        statusLabel.setForeground(RED);

        log("SERVER STOPPED");
    }

    private void updateClientCount(int count) {
        SwingUtilities.invokeLater(() -> {
            if (clientCountLabel != null) clientCountLabel.setText(String.valueOf(count));
        });
    }

    private void refreshClients() {
        SwingUtilities.invokeLater(() -> {
            if (clientModel == null) return;
            clientModel.setRowCount(0);
            connectedClients.values().stream()
                    .sorted(Comparator.comparingLong(client -> client.id))
                    .forEach(client -> clientModel.addRow(new Object[]{
                            client.username == null ? "—" : client.username,
                            client.address,
                            client.username == null ? "Đang chờ đăng nhập" : "Đã xác thực"
                    }));
        });
    }

    private void refreshAccounts() {
        SwingUtilities.invokeLater(() -> {
            accountModel.setRowCount(0);

            int accounts = 0;
            int files = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(BASE_DIR)) {
                for (Path dir : stream) {
                    if (!Files.isDirectory(dir)) continue;

                    accounts++;
                    int count = 0;

                    try (DirectoryStream<Path> fs = Files.newDirectoryStream(dir)) {
                        for (Path f : fs) {
                            if (Files.isRegularFile(f) &&
                                    !PASSWORD_FILE.equals(f.getFileName().toString())) count++;
                        }
                    }

                    files += count;

                    accountModel.addRow(new Object[]{
                            dir.getFileName().toString(),
                            count,
                            dir.toAbsolutePath().toString()
                    });
                }
            } catch (IOException e) {
                log("[ERROR] Refresh accounts: " + e.getMessage());
            }

            if (accountCountLabel != null) accountCountLabel.setText(String.valueOf(accounts));
            if (fileCountLabel != null) fileCountLabel.setText(String.valueOf(files));
        });
    }

    private void log(String message) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                logArea.append("[" + time + "] " + message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    private static String safeName(String name) {
        return name == null ? "" : name.trim().replaceAll("[^A-Za-z0-9_.@-]", "_");
    }

    private static Path accountDir(String username) {
        return BASE_DIR.resolve(safeName(username));
    }

    private static String createAccount(String username, char[] password) {
        username = username == null ? "" : username.trim();
        if (username.isEmpty()) return "ERROR|Username không được rỗng.";
        if (password.length == 0) return "ERROR|Mật khẩu không được rỗng.";

        try {
            Path dir = accountDir(username);
            Path passwordFile = dir.resolve(PASSWORD_FILE);
            if (Files.exists(dir)) {
                if (Files.exists(passwordFile)) return "ERROR|Account đã tồn tại.";

                writePassword(passwordFile, password);
                return "OK|Đã thiết lập mật khẩu cho account cũ.";
            }

            Files.createDirectories(dir);
            Files.writeString(dir.resolve("new_email.txt"),
                    WELCOME, StandardCharsets.UTF_8);
            writePassword(passwordFile, password);
            return "OK|Tạo account thành công.";
        } catch (IOException | GeneralSecurityException e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private static void writePassword(Path passwordFile, char[] password)
            throws IOException, GeneralSecurityException {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = hashPassword(password, salt);
        try {
            String credential = Base64.getEncoder().encodeToString(salt) + ":" +
                    Base64.getEncoder().encodeToString(hash);
            Files.writeString(passwordFile, credential, StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(hash, (byte) 0);
        }
    }

    private static boolean verifyPassword(String username, char[] password)
            throws IOException, GeneralSecurityException {
        Path passwordFile = accountDir(username).resolve(PASSWORD_FILE);
        if (!Files.isRegularFile(passwordFile)) return false;

        String[] credential = Files.readString(passwordFile, StandardCharsets.UTF_8).split(":", -1);
        if (credential.length != 2) return false;

        byte[] salt = Base64.getDecoder().decode(credential[0]);
        byte[] expectedHash = Base64.getDecoder().decode(credential[1]);
        byte[] actualHash = hashPassword(password, salt);
        try {
            return MessageDigest.isEqual(expectedHash, actualHash);
        } finally {
            Arrays.fill(expectedHash, (byte) 0);
            Arrays.fill(actualHash, (byte) 0);
        }
    }

    private static byte[] hashPassword(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PASSWORD_ITERATIONS, PASSWORD_KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static char[] decodePassword(String encodedPassword) {
        byte[] passwordBytes = Base64.getDecoder().decode(encodedPassword);
        try {
            return new String(passwordBytes, StandardCharsets.UTF_8).toCharArray();
        } finally {
            Arrays.fill(passwordBytes, (byte) 0);
        }
    }

    private static List<String> listFiles(String username) {
        List<String> result = new ArrayList<>();
        Path dir = accountDir(username);

        if (!Files.isDirectory(dir)) return result;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p) &&
                        !PASSWORD_FILE.equals(p.getFileName().toString()))
                    result.add(p.getFileName().toString());
            }
        } catch (IOException ignored) {}

        Collections.sort(result);
        return result;
    }

    private static String sendMail(String from, String to, String subject, String body) {
        Path dir = accountDir(to);

        if (!Files.isDirectory(dir))
            return "ERROR|Account người nhận không tồn tại.";

        String time = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String fileName = "email_" + time + ".txt";

        String content =
                "From: " + from + "\n" +
                "To: " + to + "\n" +
                "Subject: " + subject + "\n" +
                "Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) +
                "\n\n" + body;

        try {
            Files.writeString(dir.resolve(fileName), content, StandardCharsets.UTF_8);
            return "OK|" + fileName;
        } catch (IOException e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private static String readMail(String username, String filename) {
        Path dir = accountDir(username);
        Path file = dir.resolve(Paths.get(filename).getFileName().toString());

        if (PASSWORD_FILE.equals(file.getFileName().toString()) ||
                !Files.isRegularFile(file))
            return "ERROR|Không tìm thấy file.";

        try {
            return "CONTENT|" +
                    Files.readString(file, StandardCharsets.UTF_8)
                            .replace("\n", "\\n");
        } catch (IOException e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private static String encodeList(List<String> files) {
        return String.join("\t", files);
    }

    private static class ClientInfo {
        private final long id;
        private final String address;
        private volatile String username;

        private ClientInfo(long id, String address) {
            this.id = id;
            this.address = address;
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final long clientId;

        ClientHandler(Socket socket, long clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            String address = socket.getInetAddress().getHostAddress();

            try (
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter out = new PrintWriter(
                            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
            ) {
                String authenticatedUsername = null;
                String request;

                while ((request = in.readLine()) != null) {
                    String[] parts = request.split("\\|", -1);
                    String command = parts[0];

                    switch (command) {
                        case "CREATE":
                            String newUser = parts.length > 1 ? parts[1] : "";
                            char[] newPassword;
                            try {
                                newPassword = decodePassword(parts.length > 2 ? parts[2] : "");
                            } catch (IllegalArgumentException e) {
                                out.println("ERROR|Mật khẩu không hợp lệ.");
                                break;
                            }
                            String createResult;
                            try {
                                createResult = createAccount(newUser, newPassword);
                            } finally {
                                Arrays.fill(newPassword, '\0');
                            }
                            out.println(createResult);
                            log("[ACCOUNT] " + newUser + " -> " + createResult);
                            refreshAccounts();
                            break;

                        case "LOGIN":
                            String username = parts.length > 1 ? parts[1] : "";
                            authenticatedUsername = null;
                            ClientInfo client = connectedClients.get(clientId);
                            if (client != null) {
                                client.username = null;
                                refreshClients();
                            }
                            char[] loginPassword;
                            try {
                                loginPassword = decodePassword(parts.length > 2 ? parts[2] : "");
                            } catch (IllegalArgumentException e) {
                                out.println("ERROR|Mật khẩu không hợp lệ.");
                                break;
                            }
                            try {
                                if (!Files.isDirectory(accountDir(username))) {
                                    out.println("ERROR|Account không tồn tại.");
                                    log("[LOGIN FAILED] " + username + " from " + address);
                                } else if (!Files.isRegularFile(
                                        accountDir(username).resolve(PASSWORD_FILE))) {
                                    out.println("ERROR|Account chưa có mật khẩu. Hãy dùng Tạo account mới để thiết lập.");
                                } else if (!verifyPassword(username, loginPassword)) {
                                    out.println("ERROR|Sai mật khẩu.");
                                    log("[LOGIN FAILED] " + username + " from " + address);
                                } else {
                                    authenticatedUsername = username;
                                    if (client != null) {
                                        client.username = username;
                                        refreshClients();
                                    }
                                    List<String> files = listFiles(username);
                                    out.println("OK|" + encodeList(files));
                                    log("[LOGIN] " + username + " from " + address +
                                            " — " + files.size() + " file(s)");
                                }
                            } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
                                out.println("ERROR|Không thể xác thực mật khẩu.");
                                log("[LOGIN ERROR] " + username + " from " + address +
                                        " — " + e.getMessage());
                            } finally {
                                Arrays.fill(loginPassword, '\0');
                            }
                            break;

                        case "LIST":
                            username = parts.length > 1 ? parts[1] : "";
                            if (authenticatedUsername == null ||
                                    !authenticatedUsername.equals(username)) {
                                out.println("ERROR|Vui lòng đăng nhập lại.");
                            } else {
                                List<String> files = listFiles(username);
                                out.println("OK|" + encodeList(files));
                            }
                            break;

                        case "READ":
                            username = parts.length > 1 ? parts[1] : "";
                            String filename = parts.length > 2 ? parts[2] : "";
                            if (authenticatedUsername == null ||
                                    !authenticatedUsername.equals(username)) {
                                out.println("ERROR|Vui lòng đăng nhập lại.");
                            } else {
                                out.println(readMail(username, filename));
                                log("[READ] " + username + " -> " + filename);
                            }
                            break;

                        case "SEND":
                            String from = parts.length > 1 ? parts[1] : "";
                            String to = parts.length > 2 ? parts[2] : "";
                            String subject = parts.length > 3 ? parts[3] : "";
                            String body = parts.length > 4
                                    ? parts[4].replace("\\n", "\n")
                                    : "";
                            if (authenticatedUsername == null ||
                                    !authenticatedUsername.equals(from)) {
                                out.println("ERROR|Vui lòng đăng nhập lại.");
                            } else {
                                String result = sendMail(from, to, subject, body);
                                out.println(result);
                                log("[MAIL] " + from + " -> " + to +
                                        " | " + (result.startsWith("OK|") ? "SAVED" : result));
                                refreshAccounts();
                            }
                            break;

                        case "QUIT":
                            out.println("OK|Bye");
                            return;
                        default:
                            out.println("ERROR|Command không hợp lệ.");
                    }
                }
            } catch (IOException e) {
                log("[DISCONNECT] " + address + " — " + e.getMessage());
            } finally {
                connectedClients.remove(clientId);
                refreshClients();
                int count = clientCount.decrementAndGet();
                updateClientCount(Math.max(count, 0));
                log("[-] Client disconnected: " + address);

                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            MailServer server = new MailServer();
            server.setVisible(true);
            server.log("Server application ready. Press START SERVER.");
        });
    }
}
