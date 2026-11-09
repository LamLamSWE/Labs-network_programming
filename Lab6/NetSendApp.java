import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class NetSendApp extends JFrame {

    private static final int PORT = 5000;
    private static final int BUFFER_SIZE = 4096;
    private static final String BROADCAST_ADDRESS = "255.255.255.255";

    private final DatagramSocket socket;
    private final JTextField commandField = new JTextField();
    private final JTextArea conversation = new JTextArea();
    private final DefaultListModel<String> contactModel = new DefaultListModel<>();
    private final JList<String> contacts = new JList<>(contactModel);
    private final JLabel status = new JLabel("Đang lắng nghe UDP cổng " + PORT);

    private NetSendApp(DatagramSocket socket) {
        super("NET SEND - Trao đổi tin nhắn");
        this.socket = socket;
        buildInterface();
        startReceiving();
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(PORT);
                new NetSendApp(socket).setVisible(true);
            } catch (SocketException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Không thể mở cổng UDP " + PORT + ": " + e.getMessage(),
                        "Lỗi khởi động",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    private void buildInterface() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(700, 430));
        setSize(850, 520);
        setLocationRelativeTo(null);

        JPanel commandPanel = new JPanel(new BorderLayout(8, 0));
        commandPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
        commandPanel.add(new JLabel("Lệnh:"), BorderLayout.WEST);
        commandField.setToolTipText("Ví dụ: net send 192.168.1.10 Xin chào");
        commandPanel.add(commandField, BorderLayout.CENTER);
        JButton sendButton = new JButton("Gửi");
        commandPanel.add(sendButton, BorderLayout.EAST);

        JLabel help = new JLabel(
                "Cú pháp: net send {IP | group | *} message  "
                        + "(group: các IP cách nhau bằng dấu phẩy)"
        );
        help.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));

        conversation.setEditable(false);
        conversation.setLineWrap(true);
        conversation.setWrapStyleWord(true);
        JScrollPane conversationScroll = new JScrollPane(conversation);
        conversationScroll.setBorder(BorderFactory.createTitledBorder("Tin nhắn"));

        contacts.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JScrollPane contactsScroll = new JScrollPane(contacts);
        contactsScroll.setPreferredSize(new Dimension(190, 0));
        contactsScroll.setBorder(BorderFactory.createTitledBorder("Receivers / liên hệ"));

        JButton addContactButton = new JButton("Thêm IP");
        JPanel contactPanel = new JPanel(new BorderLayout(0, 6));
        contactPanel.add(contactsScroll, BorderLayout.CENTER);
        JPanel contactButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        contactButtonPanel.add(addContactButton);
        contactPanel.add(contactButtonPanel, BorderLayout.SOUTH);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 0));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));
        contentPanel.add(conversationScroll, BorderLayout.CENTER);
        contentPanel.add(contactPanel, BorderLayout.EAST);

        status.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));
        setLayout(new BorderLayout());
        add(commandPanel, BorderLayout.NORTH);
        add(help, BorderLayout.BEFORE_FIRST_LINE);
        add(contentPanel, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        sendButton.addActionListener(event -> sendCommand());
        commandField.addActionListener(event -> sendCommand());
        addContactButton.addActionListener(event -> addContact());
        contacts.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    String selected = contacts.getSelectedValue();
                    if (selected != null) {
                        commandField.setText("net send " + selected + " ");
                        commandField.requestFocusInWindow();
                        commandField.setCaretPosition(commandField.getText().length());
                    }
                }
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent event) {
                socket.close();
            }
        });
    }

    private void sendCommand() {
        String command = commandField.getText().trim();
        if (!command.regionMatches(true, 0, "net send ", 0, 9)) {
            showError("Lệnh phải có dạng: net send {IP | group | *} message");
            return;
        }

        String arguments = command.substring(9).trim();
        int separator = firstWhitespace(arguments);
        if (separator < 0 || separator == arguments.length() - 1) {
            showError("Vui lòng nhập đầy đủ đối tượng nhận và nội dung tin nhắn.");
            return;
        }

        String target = arguments.substring(0, separator);
        String message = arguments.substring(separator).trim();
        if (target.isEmpty() || message.isEmpty()) {
            showError("Đối tượng nhận và nội dung tin nhắn không được để trống.");
            return;
        }

        Set<String> destinations = parseDestinations(target);
        boolean broadcast = destinations.remove("*");
        if (destinations.isEmpty() && !broadcast) {
            showError("Vui lòng nhập IP, danh sách IP hoặc * làm đối tượng nhận.");
            return;
        }

        try {
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            if (broadcast) {
                destinations.add(BROADCAST_ADDRESS);
                socket.setBroadcast(true);
            }

            Set<String> sentTo = new LinkedHashSet<>();
            for (String destination : destinations) {
                InetAddress address = InetAddress.getByName(destination);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, PORT);
                socket.send(packet);
                sentTo.add(address.getHostAddress());
                if (!BROADCAST_ADDRESS.equals(address.getHostAddress())) {
                    addContact(address.getHostAddress());
                }
            }

            appendMessage("Bạn → " + String.join(", ", sentTo) + ": " + message);
            status.setText("Đã gửi tin nhắn đến " + sentTo.size() + " địa chỉ.");
            commandField.setText("");
        } catch (IOException | IllegalArgumentException e) {
            showError("Không thể gửi tin nhắn: " + e.getMessage());
        }
    }

    private void startReceiving() {
        Thread receiverThread = new Thread(() -> {
            while (!socket.isClosed()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                    String sender = packet.getAddress().getHostAddress();
                    String message = new String(
                            packet.getData(),
                            packet.getOffset(),
                            packet.getLength(),
                            StandardCharsets.UTF_8
                    );
                    SwingUtilities.invokeLater(() -> {
                        addContact(sender);
                        appendMessage(sender + " → Bạn: " + message);
                        status.setText("Nhận tin nhắn từ " + sender + ".");
                    });
                } catch (SocketException e) {
                    if (!socket.isClosed()) {
                        SwingUtilities.invokeLater(
                                () -> showError("Lỗi nhận tin nhắn: " + e.getMessage())
                        );
                    }
                    return;
                } catch (IOException e) {
                    SwingUtilities.invokeLater(
                            () -> showError("Lỗi nhận tin nhắn: " + e.getMessage())
                    );
                }
            }
        }, "net-send-receiver");
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void addContact() {
        String input = JOptionPane.showInputDialog(
                this,
                "Nhập IP hoặc hostname của receiver:",
                "Thêm receiver",
                JOptionPane.PLAIN_MESSAGE
        );
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        try {
            addContact(InetAddress.getByName(input.trim()).getHostAddress());
            status.setText("Đã thêm receiver " + input.trim() + ".");
        } catch (IOException e) {
            showError("IP hoặc hostname không hợp lệ: " + e.getMessage());
        }
    }

    private void addContact(String address) {
        if (!contactModel.contains(address)) {
            contactModel.addElement(address);
        }
    }

    private void appendMessage(String message) {
        conversation.append(message + System.lineSeparator());
        conversation.setCaretPosition(conversation.getDocument().getLength());
    }

    private void showError(String message) {
        status.setText(message);
        JOptionPane.showMessageDialog(
                this, message, "Không thể thực hiện", JOptionPane.ERROR_MESSAGE
        );
    }

    private static int firstWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static Set<String> parseDestinations(String target) {
        Set<String> destinations = new LinkedHashSet<>();
        for (String item : target.split(",")) {
            String destination = item.trim();
            if (!destination.isEmpty()) {
                destinations.add(destination);
            }
        }
        return destinations;
    }
}
