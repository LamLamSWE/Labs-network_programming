import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class MailClient extends JFrame {
    private static final int PORT = 5000;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private final Color BG = new Color(245, 247, 251);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(79, 70, 222);
    private final Color TEXT = new Color(17, 24, 39);
    private final Color MUTED = new Color(107, 114, 128);
    private final Color BORDER = new Color(229, 231, 235);

    private JTable mailTable;
    private DefaultTableModel tableModel;
    private JLabel countLabel;
    private JTextField hostField;
    private JTextField usernameField;
    private JPasswordField passwordField;

    public MailClient() {
        setTitle("Mail Client - Java");
        setSize(940, 620);
        setMinimumSize(new Dimension(760, 540));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        showLogin();
    }

    private void showLogin() {
        getContentPane().removeAll();
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG);

        JPanel card = cardPanel();
        card.setPreferredSize(new Dimension(390, 480));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("MAIL SERVER");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 12));
        brand.setForeground(PRIMARY);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setBorder(new EmptyBorder(28, 30, 14, 30));

        JLabel loginTitle = new JLabel("Đăng nhập");
        loginTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        loginTitle.setForeground(TEXT);
        loginTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginTitle.setBorder(new EmptyBorder(0, 30, 4, 30));

        JLabel hint = new JLabel("Đăng nhập hoặc tạo tài khoản email");
        hint.setForeground(MUTED);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setBorder(new EmptyBorder(0, 30, 18, 30));

        hostField = styledTextField("127.0.0.1");
        usernameField = styledTextField("");
        passwordField = styledPasswordField();

        card.add(brand);
        card.add(loginTitle);
        card.add(hint);
        card.add(fieldLabel("Server IP"));
        card.add(withMargin(hostField, 30, 4, 30, 12));
        card.add(fieldLabel("Username / Email"));
        card.add(withMargin(usernameField, 30, 4, 30, 12));
        card.add(fieldLabel("Mật khẩu"));
        card.add(withMargin(passwordField, 30, 4, 30, 18));

        JButton login = primaryButton("Đăng nhập");
        login.addActionListener(e -> login());
        card.add(withMargin(login, 30, 0, 30, 8));

        JButton create = normalButton("Tạo account mới");
        create.addActionListener(e -> createAccount());
        card.add(withMargin(create, 30, 0, 30, 8));

        JLabel port = new JLabel("Port 5000 · TCP");
        port.setForeground(MUTED);
        port.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        port.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(Box.createVerticalGlue());
        card.add(port);
        card.add(Box.createVerticalStrut(18));

        center.add(card);
        add(center, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setBorder(new EmptyBorder(2, 30, 2, 30));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel withMargin(JComponent c, int l, int t, int r, int b) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(t, l, b, r));
        p.add(c);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JTextField styledTextField(String text) {
        JTextField f = new JTextField(text);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(new CompoundBorder(
                new LineBorder(BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return f;
    }

    private JPasswordField styledPasswordField() {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(new CompoundBorder(
                new LineBorder(BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return f;
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
        b.setBackground(PRIMARY);
        b.setForeground(Color.GREEN);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 15, 10, 15));
        return b;
    }

    private JButton normalButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(249, 250, 251));
        b.setForeground(TEXT);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(
                new LineBorder(BORDER),
                new EmptyBorder(9, 14, 9, 14)));
        return b;
    }

    private boolean connect() {
        try {
            socket = new Socket(hostField.getText().trim(), PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Không thể kết nối Server.\n\n" + e.getMessage() +"\n\nHãy chạy MailServer trước.",
                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private String request(String message) {
        try {
            out.println(message);
            return in.readLine();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void createAccount() {
        String user = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        if (!validateCredentials(user, password)) {
            Arrays.fill(password, '\0');
            return;
        }

        if (!connect()) {
            Arrays.fill(password, '\0');
            return;
        }

        String response = request("CREATE|" + user + "|" + encodePassword(password));
        Arrays.fill(password, '\0');
        closeConnection();

        if (response != null && response.startsWith("OK|")) {
            JOptionPane.showMessageDialog(this,
                    response.substring(3) + "\nThông tin account đã được lưu trên Server.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } else if (response != null) {
            showError(response);
        }
    }

    private void login() {
        String user = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        if (!validateCredentials(user, password)) {
            Arrays.fill(password, '\0');
            return;
        }

        if (!connect()) {
            Arrays.fill(password, '\0');
            return;
        }

        String response = request("LOGIN|" + user + "|" + encodePassword(password));
        Arrays.fill(password, '\0');

        if (response != null && response.startsWith("OK|")) {
            username = user;
            String[] files = response.length() > 3 && !response.substring(3).isEmpty() ? response.substring(3).split("\\t"): new String[0];
            showMailbox(files);
        } else {
            showError(response);
            closeConnection();
        }
    }

    private boolean validateCredentials(String user, char[] password) {
        if (user.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập username.");
            return false;
        }
        if (password.length == 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mật khẩu.");
            return false;
        }
        return true;
    }

    private String encodePassword(char[] password) {
        byte[] passwordBytes = new String(password).getBytes(StandardCharsets.UTF_8);
        try {
            return Base64.getEncoder().encodeToString(passwordBytes);
        } finally {
            Arrays.fill(passwordBytes, (byte) 0);
        }
    }

    private void showMailbox(String[] files) {
        getContentPane().removeAll();
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        header.setPreferredSize(new Dimension(0, 58));

        JLabel brand = new JLabel("MAIL SERVER");
        brand.setForeground(PRIMARY);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 15));
        brand.setBorder(new EmptyBorder(0, 22, 0, 16));

        JLabel logged = new JLabel("Đang đăng nhập: " + username);
        logged.setForeground(MUTED);
        logged.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton logout = normalButton("Đăng xuất");
        logout.addActionListener(e -> logout());

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 14));
        leftHeader.setOpaque(false);
        leftHeader.add(brand);
        leftHeader.add(logged);

        header.add(leftHeader, BorderLayout.WEST);
        header.add(logout, BorderLayout.EAST);
        logout.setBorder(new EmptyBorder(8, 14, 8, 14));

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(BG);
        body.setBorder(new EmptyBorder(18, 22, 20, 22));

        JPanel main = cardPanel();
        main.setLayout(new BorderLayout());

        JLabel title = new JLabel("Hộp thư đến");
        title.setForeground(TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setBorder(new EmptyBorder(17, 20, 12, 12));

        countLabel = new JLabel(files.length + " thư");
        countLabel.setForeground(MUTED);
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton refresh = normalButton("Làm mới");
        refresh.addActionListener(e -> refreshFiles());

        JButton compose = primaryButton("Soạn thư");
        compose.addActionListener(e -> composeEmail());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        actions.setOpaque(false);
        actions.add(countLabel);
        actions.add(refresh);
        actions.add(compose);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(0, 0, 4, 14));
        titlePanel.add(title, BorderLayout.WEST);
        titlePanel.add(actions, BorderLayout.EAST);
        main.add(titlePanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"Tên file"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        mailTable = new JTable(tableModel);
        mailTable.setRowHeight(38);
        mailTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        mailTable.setForeground(TEXT);
        mailTable.setShowGrid(false);
        mailTable.setIntercellSpacing(new Dimension(0, 0));
        mailTable.setSelectionBackground(new Color(238, 242, 255));
        mailTable.setSelectionForeground(TEXT);
        mailTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        mailTable.getTableHeader().setBackground(new Color(249, 250, 251));
        mailTable.getTableHeader().setForeground(MUTED);
        mailTable.getTableHeader().setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        mailTable.setFillsViewportHeight(true);

        for (String f : files) tableModel.addRow(new Object[]{f});

        mailTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) openSelected();
            }
        });

        JScrollPane scroll = new JScrollPane(mailTable);
        scroll.setBorder(new EmptyBorder(0, 14, 14, 14));
        main.add(scroll, BorderLayout.CENTER);

        body.add(main, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private void refreshFiles() {
        String response = request("LIST|" + username);
        if (response == null || !response.startsWith("OK|")) return;

        String data = response.substring(3);
        String[] files = data.isEmpty() ? new String[0] : data.split("\\t");

        tableModel.setRowCount(0);
        for (String f : files) tableModel.addRow(new Object[]{f});
        countLabel.setText(files.length + " thư");
    }

    private void openSelected() {
        int row = mailTable.getSelectedRow();
        if (row < 0) return;

        String filename = tableModel.getValueAt(row, 0).toString();
        String response = request("READ|" + username + "|" + filename);

        if (response != null && response.startsWith("CONTENT|")) {
            String content = response.substring(8).replace("\\n", "\n");
            showReader(filename, content);
        } else if (response != null) {
            showError(response);
        }
    }

    private void showReader(String filename, String content) {
        JDialog dialog = new JDialog(this, filename, true);
        dialog.setSize(760, 560);
        dialog.setLocationRelativeTo(this);

        JPanel panel = cardPanel();
        panel.setLayout(new BorderLayout());

        JLabel title = new JLabel(filename);
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(TEXT);
        title.setBorder(new EmptyBorder(20, 20, 12, 20));

        JTextArea area = new JTextArea(content);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        area.setForeground(TEXT);
        area.setBackground(new Color(250, 250, 252));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setBorder(new EmptyBorder(15, 15, 15, 15));

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void composeEmail() {
        JDialog dialog = new JDialog(this, "Soạn email", true);
        dialog.setSize(720, 620);
        dialog.setLocationRelativeTo(this);

        JPanel panel = cardPanel();
        panel.setLayout(new BorderLayout());

        JLabel title = new JLabel("Soạn email mới");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT);
        title.setBorder(new EmptyBorder(22, 25, 10, 25));
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JTextField from = styledTextField(username);
        JTextField to = styledTextField("");
        JTextField subject = styledTextField("");
        JTextArea body = new JTextArea(10, 30);
        body.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setBorder(new EmptyBorder(8, 8, 8, 8));

        form.add(labelAndField("Người gửi", from));
        form.add(labelAndField("Người nhận", to));
        form.add(labelAndField("Tiêu đề", subject));

        JLabel bodyLabel = new JLabel("Nội dung");
        bodyLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        bodyLabel.setForeground(TEXT);
        bodyLabel.setBorder(new EmptyBorder(8, 25, 4, 25));
        bodyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(bodyLabel);

        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(new EmptyBorder(0, 25, 0, 25));
        bodyScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(bodyScroll);

        panel.add(form, BorderLayout.CENTER);

        JButton send = primaryButton("GỬI EMAIL");
        send.addActionListener(e -> {
            String text = body.getText().trim();
            if (to.getText().trim().isEmpty() || text.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Vui lòng nhập người nhận và nội dung.");
                return;
            }

            String encodedBody = text.replace("\n", "\\n").replace("|", "/");
            String response = request(
                    "SEND|" + username + "|" +
                    to.getText().trim() + "|" +
                    subject.getText().trim().replace("|", "/") + "|" +
                    encodedBody);

            if (response != null && response.startsWith("OK|")) {
                JOptionPane.showMessageDialog(dialog,
                        "Email đã được Server lưu vào account người nhận.",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } else if (response != null) {
                showError(response);
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(10, 20, 15, 20));
        bottom.add(send);
        panel.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private JPanel labelAndField(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(5, 25, 5, 25));

        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(TEXT);
        l.setBorder(new EmptyBorder(0, 0, 4, 0));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(l, BorderLayout.NORTH);
        wrapper.add(field, BorderLayout.CENTER);
        p.add(wrapper);
        return p;
    }

    private void logout() {
        try {
            if (out != null) out.println("QUIT");
        } catch (Exception ignored) {}
        closeConnection();
        username = null;
        showLogin();
    }

    private void closeConnection() {
        try 
        { if (socket != null) socket.close(); 
        } catch (Exception ignored) {
            System.out.println("Error closing socket: " + ignored.getMessage());
        }
        socket = null;
        in = null;
        out = null;
    }

    private void showError(String response) {
        String msg = response;
        if (response != null && response.contains("|"))
            msg = response.substring(response.indexOf('|') + 1);

        JOptionPane.showMessageDialog(this, msg,
                "Thông báo", JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            new MailClient().setVisible(true);
        });
    }
}
