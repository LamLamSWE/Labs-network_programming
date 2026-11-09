package com.lam.mail.gui;

import javax.swing.*;
import java.awt.*;

public class ViewEmailDialog extends JDialog {

    public ViewEmailDialog(Frame owner, String username, String filename, String content) {
        super(owner, "Email: " + filename, true);
        setSize(500, 380);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // Header panel
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 15, 5, 15));

        JLabel fileLabel = new JLabel("File: " + filename);
        fileLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel accountLabel = new JLabel("Mailbox: " + username);
        accountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        accountLabel.setForeground(Color.DARK_GRAY);

        headerPanel.add(fileLabel);
        headerPanel.add(accountLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Content panel
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        JLabel contentLabel = new JLabel("Content:");
        contentLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        centerPanel.add(contentLabel, BorderLayout.NORTH);

        JTextArea contentArea = new JTextArea(content);
        contentArea.setEditable(false);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentArea.setCaretPosition(0);
        contentArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(contentArea);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(closeButton);
    }
}

