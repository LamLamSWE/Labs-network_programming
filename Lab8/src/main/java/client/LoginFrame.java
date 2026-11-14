package client;

import common.LibraryService;
import server.RMIServer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Giao diện đăng nhập đơn giản:
 * - Nhập tên người dùng (Username)
 * - Tùy chọn Host và Port của RMI Server
 * - Kết nối RMI Registry, tra cứu (lookup) Remote Object
 * - Mở MainFrame khi đăng nhập thành công
 */
public class LoginFrame extends JFrame {
    private final JTextField txtHost;
    private final JTextField txtPort;
    private final JTextField txtUsername;
    private final JButton btnLogin;

    public LoginFrame() {
        setTitle("Mini Library - Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 280);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main panel with padding
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Header Title
        JLabel lblTitle = new JLabel("MINI LIBRARY RMI", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(33, 82, 168));
        panel.add(lblTitle, BorderLayout.NORTH);

        // Form fields in GridBagLayout
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Host
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("Server Host:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtHost = new JTextField("localhost");
        formPanel.add(txtHost, gbc);

        // Port
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        formPanel.add(new JLabel("Server Port:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtPort = new JTextField(String.valueOf(RMIServer.PORT));
        formPanel.add(txtPort, gbc);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        JLabel lblUser = new JLabel("Tên người dùng:");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblUser, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtUsername = new JTextField(15);
        formPanel.add(txtUsername, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Bottom Button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnLogin = new JButton("ĐĂNG NHẬP");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogin.setBackground(new Color(33, 82, 168));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setPreferredSize(new Dimension(150, 36));
        buttonPanel.add(btnLogin);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(panel);

        // Events
        ActionListener loginAction = e -> handleLogin();
        btnLogin.addActionListener(loginAction);
        txtUsername.addActionListener(loginAction);
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String host = txtHost.getText().trim();
        String portStr = txtPort.getText().trim();

        // 1. Kiểm tra validation username
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập tên người dùng (Username)!",
                    "Cảnh báo",
                    JOptionPane.WARNING_MESSAGE);
            txtUsername.requestFocus();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Cổng (Port) phải là một số hợp lệ!",
                    "Lỗi cấu hình",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Kết nối RMI Registry và Lookup Remote Object
        try {
            btnLogin.setEnabled(false);
            btnLogin.setText("Đang kết nối...");

            Registry registry = LocateRegistry.getRegistry(host, port);
            LibraryService service = (LibraryService) registry.lookup(RMIServer.SERVICE_NAME);

            // Kiểm tra ping dịch vụ thành công
            service.getBooks();

            // Chuyển sang MainFrame
            MainFrame mainFrame = new MainFrame(service, username);
            mainFrame.setVisible(true);
            this.dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Không thể kết nối đến RMI Server tại " + host + ":" + port + "!\n" +
                    "Nguyên nhân: " + ex.getMessage() + "\n\n" +
                    "Vui lòng kiểm tra:\n" +
                    "1. RMIServer đã được khởi động chưa?\n" +
                    "2. Cổng " + port + " có đang mở không?",
                    "Lỗi kết nối RMI",
                    JOptionPane.ERROR_MESSAGE);
            btnLogin.setEnabled(true);
            btnLogin.setText("ĐĂNG NHẬP");
        }
    }
}

