package client;

import common.ChatMessage;
import common.FileOffer;
import common.Message;
import common.Protocol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ChatClientGUI extends JFrame {

    private static final String DEFAULT_SERVER_HOST = "10.60.74.99";
    private static final int DEFAULT_SERVER_PORT = 9000;

    private ChatClient client;

    private JTextField tfServerHost = new JTextField(DEFAULT_SERVER_HOST, 10);
    private JTextField tfServerPort = new JTextField(String.valueOf(DEFAULT_SERVER_PORT), 5);
    private JTextField tfUsername   = new JTextField(10);
    private JPasswordField pfPassword = new JPasswordField(10);
    private JButton btnRegister = new JButton("Register");
    private JButton btnLogin    = new JButton("Login");
    private JLabel   lblStatus  = new JLabel("Disconnected");

    private JList<String> listUsers;
    private DefaultListModel<String> userListModel;
    private JTextArea taChat;
    private JTextField tfInput;
    private JButton btnSend;
    private JButton btnSendFile;
    private JButton btnRefresh;
    private JButton btnLogout;

    private final java.util.Map<String, String> chatBuffers = new java.util.HashMap<>();
    private String currentPeer = null;

    private final java.util.Map<String, String> offerFromMap = new java.util.HashMap<>();

    public ChatClientGUI() {
        setTitle("Chat Client - Client/Server");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setContentPane(buildLoginPanel());

        javax.swing.Timer t = new javax.swing.Timer(3000, e -> {
            if (client != null && client.isConnected()) {
                try {
                    Message resp = client.requestUserList();
                    if (resp != null && resp.getType() == Protocol.USER_LIST) {
                        String[] users = (String[]) resp.getPayload();
                        updateUserList(users);
                    }
                } catch (Exception ignored) {}
            }
        });
        t.start();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (client != null) client.logout();
            }
        });
    }

    private JPanel buildLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        p.add(new JLabel("Server:"), c);
        c.gridx = 1;
        JPanel pServer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pServer.add(tfServerHost);
        pServer.add(new JLabel(":"));
        pServer.add(tfServerPort);
        p.add(pServer, c);

        c.gridx = 0; c.gridy = 1;
        p.add(new JLabel("Username:"), c);
        c.gridx = 1; p.add(tfUsername, c);

        c.gridx = 0; c.gridy = 2;
        p.add(new JLabel("Password:"), c);
        c.gridx = 1; p.add(pfPassword, c);

        c.gridx = 1; c.gridy = 3;
        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pBtns.add(btnRegister);
        pBtns.add(btnLogin);
        p.add(pBtns, c);

        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        lblStatus.setForeground(Color.RED);
        p.add(lblStatus, c);

        c.gridx = 0; c.gridy = 5; c.gridwidth = 2;
        p.add(new JLabel("<html><i>Server A: " + DEFAULT_SERVER_HOST
                + ":" + DEFAULT_SERVER_PORT
                + " | Demo: anh/123456, lam/123456</i></html>"), c);

        btnRegister.addActionListener(e -> doRegister());
        btnLogin.addActionListener(e -> doLogin());

        return p;
    }

    private void doRegister() {
        try {
            if (client == null) {
                client = new ChatClient(tfServerHost.getText().trim(),
                        Integer.parseInt(tfServerPort.getText().trim()),
                        this::onInboundMessage, this::onFileEvent);
                client.connect();
            }
            String u = tfUsername.getText().trim();
            String p = new String(pfPassword.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                lblStatus.setText("Enter username and password"); return;
            }
            Message resp = client.register(u, p);
            byte t = resp.getType();
            if (t == Protocol.OK) {
                lblStatus.setForeground(new Color(0, 128, 0));
                lblStatus.setText("Register successful, please login");
            } else {
                lblStatus.setForeground(Color.RED);
                lblStatus.setText("Register error: " + resp.getPayload());
            }
        } catch (Exception ex) {
            lblStatus.setForeground(Color.RED);
            lblStatus.setText("Error: " + ex.getMessage());
        }
    }

    private void doLogin() {
        try {
            String host = tfServerHost.getText().trim();
            int    port = Integer.parseInt(tfServerPort.getText().trim());
            String u    = tfUsername.getText().trim();
            String p    = new String(pfPassword.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                lblStatus.setText("Enter username and password"); return;
            }
            if (client == null) {
                client = new ChatClient(host, port, this::onInboundMessage, this::onFileEvent);
                client.connect();
            }
            Message resp = client.login(u, p);
            byte t = resp.getType();
            if (t == Protocol.OK) {
                client.setUsername(u);
                lblStatus.setText("Login OK");
                showMainUI();
            } else {
                lblStatus.setForeground(Color.RED);
                lblStatus.setText("Login error: " + resp.getPayload());
                client.disconnect();
                client = null;
            }
        } catch (Exception ex) {
            lblStatus.setForeground(Color.RED);
            lblStatus.setText("Error: " + ex.getMessage());
            if (client != null) { client.disconnect(); client = null; }
        }
    }

    private void showMainUI() {
        setTitle("Chat Client - " + client.getUsername());
        setContentPane(buildMainPanel());
        revalidate();
        repaint();
    }

    private JPanel buildMainPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        userListModel = new DefaultListModel<>();
        listUsers = new JList<>(userListModel);
        listUsers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listUsers.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        listUsers.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String sel = listUsers.getSelectedValue();
                if (sel != null && !sel.equals(currentPeer)) {
                    switchPeer(sel);
                }
            }
        });
        JScrollPane spUsers = new JScrollPane(listUsers);
        spUsers.setBorder(new TitledBorder("Users online"));
        spUsers.setPreferredSize(new Dimension(180, 0));

        btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> refreshUsers());

        btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> {
            if (client != null) {
                client.logout();
                client = null;
            }
            setContentPane(buildLoginPanel());
            setTitle("Chat Client - Client/Server");
            revalidate();
            repaint();
        });

        JPanel leftPanel = new JPanel(new BorderLayout(4, 4));
        leftPanel.add(spUsers, BorderLayout.CENTER);
        JPanel leftBtns = new JPanel(new GridLayout(2, 1, 4, 4));
        leftBtns.add(btnRefresh);
        leftBtns.add(btnLogout);
        leftPanel.add(leftBtns, BorderLayout.SOUTH);

        taChat = new JTextArea();
        taChat.setEditable(false);
        taChat.setLineWrap(true);
        taChat.setWrapStyleWord(true);
        taChat.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane spChat = new JScrollPane(taChat);
        spChat.setBorder(new TitledBorder("Conversation"));

        tfInput = new JTextField();
        tfInput.addActionListener(e -> doSendChat());

        btnSend = new JButton("Send");
        btnSend.addActionListener(e -> doSendChat());

        btnSendFile = new JButton("Send File...");
        btnSendFile.addActionListener(e -> doSendFile());

        JPanel inputPanel = new JPanel(new BorderLayout(4, 4));
        inputPanel.add(tfInput, BorderLayout.CENTER);

        JPanel rightBtns = new JPanel(new GridLayout(1, 2, 4, 4));
        rightBtns.add(btnSend);
        rightBtns.add(btnSendFile);
        inputPanel.add(rightBtns, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(4, 4));
        center.add(spChat, BorderLayout.CENTER);
        center.add(inputPanel, BorderLayout.SOUTH);

        root.add(leftPanel, BorderLayout.WEST);
        root.add(center,   BorderLayout.CENTER);
        return root;
    }

    private void switchPeer(String newPeer) {
        if (currentPeer != null) {
            chatBuffers.put(currentPeer, taChat.getText());
        }
        currentPeer = newPeer;
        taChat.setText(chatBuffers.getOrDefault(newPeer, ""));
        appendChatLine("[System]", "Switched to conversation with " + newPeer);
    }

    private void updateUserList(String[] users) {
        java.util.List<String> newList = new java.util.ArrayList<>();
        if (users != null) {
            for (String u : users) {
                if (!u.equals(client.getUsername())) newList.add(u);
            }
        }
        java.util.Collections.sort(newList);

        String selected = listUsers != null ? listUsers.getSelectedValue() : null;
        userListModel.clear();
        for (String u : newList) userListModel.addElement(u);
        if (selected != null && newList.contains(selected)) {
            listUsers.setSelectedValue(selected, true);
        }
    }

    private void refreshUsers() {
        if (client == null) return;
        try {
            Message resp = client.requestUserList();
            if (resp.getType() == Protocol.USER_LIST) {
                updateUserList((String[]) resp.getPayload());
            }
        } catch (Exception ex) {
            appendChatLine("[Error]", "Refresh error: " + ex.getMessage());
        }
    }

    private void doSendChat() {
        if (client == null || currentPeer == null) {
            JOptionPane.showMessageDialog(this, "Select a user to chat first.");
            return;
        }
        String text = tfInput.getText().trim();
        if (text.isEmpty()) return;
        try {
            client.sendChat(client.getUsername(), currentPeer, text);
            appendChatLine("Me -> " + currentPeer, text);
            tfInput.setText("");
        } catch (Exception ex) {
            appendChatLine("[Error]", "Send error: " + ex.getMessage());
        }
    }

    private void doSendFile() {
        if (client == null || currentPeer == null) {
            JOptionPane.showMessageDialog(this, "Select a user to chat first.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            String transferId = UUID.randomUUID().toString();
            client.sendFile(client.getUsername(), currentPeer, f, transferId);
            appendChatLine("[File]",
                    "Sent file request '" + f.getName() + "' to " + currentPeer
                            + " (id=" + transferId.substring(0, 8) + "...)");
        } catch (Exception ex) {
            appendChatLine("[Error]", "Send file error: " + ex.getMessage());
        }
    }

    private void onInboundMessage(Message msg) {
        SwingUtilities.invokeLater(() -> {
            byte t = msg.getType();
            switch (t) {
                case Protocol.CHAT_RECV: {
                    String from = msg.getFrom();
                    ChatMessage cm = (ChatMessage) msg.getPayload();
                    if (from.equals(currentPeer)) {
                        appendChatLine(from, cm.getText());
                    } else {
                        String old = chatBuffers.getOrDefault(from, "");
                        chatBuffers.put(from, old + formatLine(from, cm.getText()));
                        appendChatLine("[System]",
                                "New message from " + from + " (click to view)");
                    }
                    break;
                }
                case Protocol.SYSTEM_NOTIFY: {
                    String text = (String) msg.getPayload();
                    appendChatLine("[System]", text);
                    break;
                }
                case Protocol.FILE_OFFER_INCOMING: {
                    FileOffer offer = (FileOffer) msg.getPayload();
                    offerFromMap.put(offer.transferId, msg.getFrom());
                    handleIncomingFileOffer(offer, msg.getFrom());
                    break;
                }
            }
        });
    }

    private void handleIncomingFileOffer(FileOffer offer, String fromUser) {
        String me = client.getUsername();
        appendChatLine("[File]", "File request from "
                + fromUser + ": "
                + offer.fileName + " (" + offer.fileSize + " bytes)");

        int opt = JOptionPane.showConfirmDialog(this,
                fromUser + " wants to send file '" + offer.fileName
                        + "' (" + offer.fileSize + " bytes). Accept?",
                "Incoming File",
                JOptionPane.YES_NO_OPTION);
        if (opt != JOptionPane.YES_OPTION) {
            try {
                client.rejectFile(me, fromUser, offer.transferId, "user rejected");
                client.markIncomingRejected(offer.transferId);
                offerFromMap.remove(offer.transferId);
            } catch (Exception ignored) {}
        } else {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(offer.fileName));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                try { client.rejectFile(me, fromUser, offer.transferId, "user cancelled"); }
                catch (Exception ignored) {}
                client.markIncomingRejected(offer.transferId);
                offerFromMap.remove(offer.transferId);
                return;
            }
            File saveTo = fc.getSelectedFile();
            try {
                client.markIncomingAccepted(offer.transferId, saveTo);
                client.acceptFile(me, fromUser, offer.transferId);
                appendChatLine("[File]",
                        "Accepted, receiving file to " + saveTo.getAbsolutePath());
            } catch (Exception ex) {
                appendChatLine("[Error]", "Accept error: " + ex.getMessage());
            }
        }
    }

    private void onFileEvent(ChatClient.FileTransferEvent ev) {
        SwingUtilities.invokeLater(() -> {
            switch (ev.type) {
                case PROGRESS:
                    break;
                case COMPLETED:
                    appendChatLine("[File]", "File received: " + ev.path);
                    break;
                case REJECTED:
                    appendChatLine("[File]", "File rejected: " + ev.info);
                    break;
                case FAILED:
                    appendChatLine("[File]", "File transfer failed: " + ev.info);
                    break;
            }
        });
    }

    private void appendChatLine(String who, String text) {
        taChat.append(formatLine(who, text));
        taChat.setCaretPosition(taChat.getDocument().getLength());
    }

    private String formatLine(String who, String text) {
        String ts = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        return "[" + ts + "] " + who + ": " + text + "\n";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClientGUI().setVisible(true));
    }
}
