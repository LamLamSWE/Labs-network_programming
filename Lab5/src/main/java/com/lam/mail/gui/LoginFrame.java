package com.lam.mail.gui;

import com.lam.mail.MailClient;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField serverIpField;
    private final JTextField serverPortField;
    private final JTextField usernameField;
    private final JButton loginButton;
    private final JButton createAccountButton;

    public LoginFrame() {
        super("UDP Mail Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 320);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout(10, 10));

        // Title
        JLabel titleLabel = new JLabel("UDP MAIL CLIENT", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        add(titleLabel, BorderLayout.NORTH);

        // Center form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Server IP
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.35;
        JLabel ipLabel = new JLabel("Server IP:");
        ipLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(ipLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.65;
        serverIpField = new JTextField("127.0.0.1", 15);
        serverIpField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(serverIpField, gbc);

        // Server Port
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.35;
        JLabel portLabel = new JLabel("Server Port:");
        portLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(portLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.65;
        serverPortField = new JTextField(String.valueOf(MailClient.DEFAULT_PORT), 15);
        serverPortField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(serverPortField, gbc);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.35;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.65;
        usernameField = new JTextField(15);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(usernameField, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Bottom button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        loginButton = new JButton("Login");
        createAccountButton = new JButton("Create Account");

        loginButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        createAccountButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        buttonPanel.add(loginButton);
        buttonPanel.add(createAccountButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Actions
        loginButton.addActionListener(e -> performLogin());
        createAccountButton.addActionListener(e -> performOpenCreateAccount());
        getRootPane().setDefaultButton(loginButton);
    }

    private MailClient validateAndCreateClient() {
        String ip = serverIpField.getText().trim();
        String portStr = serverPortField.getText().trim();

        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Server IP cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            serverIpField.requestFocus();
            return null;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Server Port must be an integer between 1 and 65535.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            serverPortField.requestFocus();
            return null;
        }

        return new MailClient(ip, port);
    }

    private void performLogin() {
        MailClient client = validateAndCreateClient();
        if (client == null) return;

        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            usernameField.requestFocus();
            return;
        }

        setFormEnabled(false);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return client.login(username);
            }

            @Override
            protected void done() {
                setFormEnabled(true);
                try {
                    String response = get();
                    if (response != null && response.startsWith("FILES")) {
                        MailboxFrame mailboxFrame = new MailboxFrame(client, username, response, LoginFrame.this);
                        mailboxFrame.setVisible(true);
                        setVisible(false);
                    } else if (response != null && response.startsWith("ERROR|")) {
                        String errMsg = response.substring("ERROR|".length());
                        JOptionPane.showMessageDialog(LoginFrame.this, errMsg, "Login Failed", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(LoginFrame.this, "Unexpected response: " + response, "Warning", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(LoginFrame.this, "Could not connect to server: " + ex.getCause().getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void performOpenCreateAccount() {
        MailClient client = validateAndCreateClient();
        if (client == null) return;

        CreateAccountDialog dialog = new CreateAccountDialog(this, client);
        dialog.setVisible(true);
    }

    public void resetForRelogin() {
        usernameField.requestFocus();
        usernameField.selectAll();
    }

    private void setFormEnabled(boolean enabled) {
        serverIpField.setEnabled(enabled);
        serverPortField.setEnabled(enabled);
        usernameField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        createAccountButton.setEnabled(enabled);
        setCursor(enabled ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}

