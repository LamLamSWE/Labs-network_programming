package com.lam.mail.gui;

import com.lam.mail.MailClient;

import javax.swing.*;
import java.awt.*;

public class CreateAccountDialog extends JDialog {
    private final MailClient client;
    private final JTextField usernameField;
    private final JButton createButton;
    private final JButton cancelButton;

    public CreateAccountDialog(Frame owner, MailClient client) {
        super(owner, "Create Account", true);
        this.client = client;

        setLayout(new BorderLayout(10, 10));
        setSize(360, 180);
        setLocationRelativeTo(owner);
        setResizable(false);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        usernameField = new JTextField(15);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(usernameField, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        createButton = new JButton("Create");
        cancelButton = new JButton("Cancel");

        createButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Actions
        createButton.addActionListener(e -> performCreateAccount());
        cancelButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(createButton);
    }

    private void performCreateAccount() {
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
                return client.createAccount(username);
            }

            @Override
            protected void done() {
                setFormEnabled(true);
                try {
                    String response = get();
                    if (response != null && response.startsWith("SUCCESS|")) {
                        String msg = response.substring("SUCCESS|".length());
                        JOptionPane.showMessageDialog(CreateAccountDialog.this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else if (response != null && response.startsWith("ERROR|")) {
                        String errMsg = response.substring("ERROR|".length());
                        JOptionPane.showMessageDialog(CreateAccountDialog.this, errMsg, "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(CreateAccountDialog.this, "Unexpected response: " + response, "Warning", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CreateAccountDialog.this, "Network error: " + ex.getCause().getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void setFormEnabled(boolean enabled) {
        usernameField.setEnabled(enabled);
        createButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
        setCursor(enabled ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
}

