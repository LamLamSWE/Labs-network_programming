package com.lam.mail.gui;

import com.lam.mail.MailClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MailboxFrame extends JFrame {
    private final MailClient client;
    private final String username;
    private final LoginFrame loginFrame;
    private final DefaultListModel<String> listModel;
    private final JList<String> fileList;
    private final JButton openMailButton;
    private final JButton refreshButton;
    private final JButton sendEmailButton;
    private final JButton logoutButton;

    public MailboxFrame(MailClient client, String username, String initialFilesResponse, LoginFrame loginFrame) {
        super("UDP Mail Client - Mailbox (" + username + ")");
        this.client = client;
        this.username = username;
        this.loginFrame = loginFrame;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 440);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Top bar
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(12, 15, 5, 15));

        JLabel userLabel = new JLabel("Logged in as: " + username);
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        topPanel.add(userLabel, BorderLayout.WEST);

        JPanel topButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        openMailButton = new JButton("Open Mail");
        refreshButton = new JButton("Refresh");
        sendEmailButton = new JButton("Send Email");

        openMailButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sendEmailButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        topButtonPanel.add(openMailButton);
        topButtonPanel.add(refreshButton);
        topButtonPanel.add(sendEmailButton);
        topPanel.add(topButtonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center: Mailbox list
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        JLabel mailboxLabel = new JLabel("MAILBOX");
        mailboxLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        mailboxLabel.setForeground(Color.DARK_GRAY);
        centerPanel.add(mailboxLabel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(fileList);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        JLabel tipLabel = new JLabel("Tip: Double-click an email or click 'Open Mail' to view text.");
        tipLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        tipLabel.setForeground(Color.GRAY);
        centerPanel.add(tipLabel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom bar
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Populate initial files
        updateFileList(initialFilesResponse);

        // Listeners
        openMailButton.addActionListener(e -> openSelectedEmail());

        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedEmail();
                }
            }
        });

        fileList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    openSelectedEmail();
                }
            }
        });

        refreshButton.addActionListener(e -> performRefresh());
        sendEmailButton.addActionListener(e -> {
            SendEmailDialog dialog = new SendEmailDialog(this, client, username);
            dialog.setVisible(true);
        });
        logoutButton.addActionListener(e -> performLogout());
    }

    public void updateFileList(String response) {
        listModel.clear();
        if (response == null || response.equals("FILES") || response.equals("FILES|")) {
            listModel.addElement("(No emails in mailbox)");
            return;
        }

        if (response.startsWith("FILES|")) {
            String[] files = response.substring("FILES|".length()).split("\\|", -1);
            for (String file : files) {
                if (!file.trim().isEmpty()) {
                    listModel.addElement(file);
                }
            }
        }
    }

    public void openSelectedEmail() {
        String selectedFile = fileList.getSelectedValue();
        if (selectedFile == null || selectedFile.equals("(No emails in mailbox)")) {
            JOptionPane.showMessageDialog(this, "Please select an email to open.", "No Email Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        setButtonsEnabled(false);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return client.readEmail(username, selectedFile);
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                try {
                    String response = get();
                    if (response != null && response.startsWith("EMAIL_CONTENT|")) {
                        String[] parts = response.split("\\|", 3);
                        String content = (parts.length >= 3) ? parts[2] : "";
                        ViewEmailDialog dialog = new ViewEmailDialog(MailboxFrame.this, username, selectedFile, content);
                        dialog.setVisible(true);
                    } else if (response != null && response.startsWith("ERROR|")) {
                        JOptionPane.showMessageDialog(MailboxFrame.this, response.substring("ERROR|".length()), "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(MailboxFrame.this, "Unexpected response: " + response, "Warning", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MailboxFrame.this, "Network error: " + ex.getCause().getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    public void performRefresh() {
        setButtonsEnabled(false);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return client.getMailbox(username);
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                try {
                    String response = get();
                    if (response != null && response.startsWith("FILES")) {
                        updateFileList(response);
                    } else if (response != null && response.startsWith("ERROR|")) {
                        JOptionPane.showMessageDialog(MailboxFrame.this, response.substring("ERROR|".length()), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MailboxFrame.this, "Network error: " + ex.getCause().getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void performLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            if (loginFrame != null) {
                loginFrame.resetForRelogin();
                loginFrame.setVisible(true);
            }
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        openMailButton.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        sendEmailButton.setEnabled(enabled);
        logoutButton.setEnabled(enabled);
        setCursor(enabled ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
}
