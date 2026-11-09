package com.lam.mail.gui;

import com.lam.mail.MailClient;

import javax.swing.*;
import java.awt.*;

public class SendEmailDialog extends JDialog {
    private final MailClient client;
    private final String sender;
    private final JTextField toField;
    private final JTextArea messageArea;
    private final JButton sendButton;
    private final JButton cancelButton;

    public SendEmailDialog(Frame owner, MailClient client, String sender) {
        super(owner, "Send Email", true);
        this.client = client;
        this.sender = sender;

        setLayout(new BorderLayout(10, 10));
        setSize(450, 350);
        setLocationRelativeTo(owner);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // From field (read-only)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        JLabel fromLabel = new JLabel("From:");
        fromLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(fromLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.8;
        JTextField fromField = new JTextField(sender);
        fromField.setEditable(false);
        fromField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(fromField, gbc);

        // To field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.2;
        JLabel toLabel = new JLabel("To:");
        toLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(toLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.8;
        toField = new JTextField(15);
        toField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(toField, gbc);

        // Message field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel msgLabel = new JLabel("Message:");
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(msgLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.8;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        messageArea = new JTextArea(6, 25);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(messageArea);
        formPanel.add(scrollPane, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        sendButton = new JButton("Send");
        cancelButton = new JButton("Cancel");

        sendButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        buttonPanel.add(sendButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Listeners
        sendButton.addActionListener(e -> performSendEmail());
        cancelButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(sendButton);
    }

    private void performSendEmail() {
        String receiver = toField.getText().trim();
        String message = messageArea.getText().trim();

        if (receiver.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Receiver username cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            toField.requestFocus();
            return;
        }

        if (message.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Message cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            messageArea.requestFocus();
            return;
        }

        setFormEnabled(false);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return client.sendEmail(sender, receiver, message);
            }

            @Override
            protected void done() {
                setFormEnabled(true);
                try {
                    String response = get();
                    if (response != null && response.startsWith("SUCCESS|")) {
                        String msg = response.substring("SUCCESS|".length());
                        JOptionPane.showMessageDialog(SendEmailDialog.this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else if (response != null && response.startsWith("ERROR|")) {
                        String errMsg = response.substring("ERROR|".length());
                        JOptionPane.showMessageDialog(SendEmailDialog.this, errMsg, "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(SendEmailDialog.this, "Unexpected response: " + response, "Warning", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SendEmailDialog.this, "Network error: " + ex.getCause().getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void setFormEnabled(boolean enabled) {
        toField.setEnabled(enabled);
        messageArea.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
        setCursor(enabled ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
}

