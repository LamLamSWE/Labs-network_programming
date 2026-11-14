package client;

import common.Book;
import common.LibraryService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Giao diện chính của ứng dụng Thư viện (MainFrame):
 * - Hiển thị danh sách sách trong bảng JTable
 * - Tìm kiếm sách theo từ khóa
 * - Mượn sách
 * - Trả sách
 * - Xem danh sách sách của tôi (My Books)
 * - Làm mới danh sách sách
 * - Đăng xuất
 */
public class MainFrame extends JFrame {
    private final LibraryService service;
    private final String currentUser;

    private JTextField txtSearch;
    private JButton btnSearch;
    private JButton btnShowAll;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblStatus;

    public MainFrame(LibraryService service, String username) {
        this.service = service;
        this.currentUser = username;

        initUI();
        refreshBookList();
    }

    private void initUI() {
        setTitle("Mini Library RMI - Người dùng: " + currentUser);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 520);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(12, 15, 12, 15));

        // 1. TOP PANEL: Search bar
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBorder(BorderFactory.createTitledBorder("Tìm kiếm sách"));

        JPanel searchForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        searchForm.add(new JLabel("Từ khóa (Tên sách / Tác giả):"));
        txtSearch = new JTextField(25);
        searchForm.add(txtSearch);

        btnSearch = new JButton("Tìm kiếm");
        btnSearch.setBackground(new Color(41, 128, 185));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        searchForm.add(btnSearch);

        btnShowAll = new JButton("Hiện tất cả");
        btnShowAll.setFocusPainted(false);
        searchForm.add(btnShowAll);

        topPanel.add(searchForm, BorderLayout.WEST);

        // Header user info
        JPanel userInfoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JLabel lblUser = new JLabel("Xin chào, " + currentUser + " | ");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setBackground(new Color(231, 76, 60));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        userInfoPanel.add(lblUser);
        userInfoPanel.add(btnLogout);
        topPanel.add(userInfoPanel, BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 2. CENTER PANEL: JTable of Books
        String[] columnNames = {"ID", "Tiêu đề sách (Title)", "Tác giả (Author)", "Trạng thái", "Người mượn"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho phép chỉnh sửa trực tiếp trên bảng
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);

        // Center align for ID, Status, Borrower
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 3. BOTTOM PANEL: Action buttons & Status
        JPanel bottomContainer = new JPanel(new BorderLayout(5, 5));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton btnBorrow = new JButton("Mượn sách (Borrow)");
        btnBorrow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBorrow.setBackground(new Color(46, 204, 113));
        btnBorrow.setForeground(Color.WHITE);
        btnBorrow.setFocusPainted(false);
        btnBorrow.setPreferredSize(new Dimension(170, 36));

        JButton btnReturn = new JButton("Trả sách (Return)");
        btnReturn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnReturn.setBackground(new Color(230, 126, 34));
        btnReturn.setForeground(Color.WHITE);
        btnReturn.setFocusPainted(false);
        btnReturn.setPreferredSize(new Dimension(150, 36));

        JButton btnMyBooks = new JButton("Sách của tôi (My Books)");
        btnMyBooks.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnMyBooks.setBackground(new Color(155, 89, 182));
        btnMyBooks.setForeground(Color.WHITE);
        btnMyBooks.setFocusPainted(false);
        btnMyBooks.setPreferredSize(new Dimension(180, 36));

        JButton btnRefresh = new JButton("Làm mới (Refresh)");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setPreferredSize(new Dimension(150, 36));

        buttonPanel.add(btnBorrow);
        buttonPanel.add(btnReturn);
        buttonPanel.add(btnMyBooks);
        buttonPanel.add(btnRefresh);

        lblStatus = new JLabel("Sẵn sàng.", SwingConstants.LEFT);
        lblStatus.setBorder(new EmptyBorder(5, 5, 5, 5));

        bottomContainer.add(buttonPanel, BorderLayout.CENTER);
        bottomContainer.add(lblStatus, BorderLayout.SOUTH);

        mainPanel.add(bottomContainer, BorderLayout.SOUTH);
        setContentPane(mainPanel);

        // Event Listeners
        btnSearch.addActionListener(e -> handleSearch());
        txtSearch.addActionListener(e -> handleSearch());
        btnShowAll.addActionListener(e -> {
            txtSearch.setText("");
            refreshBookList();
        });

        btnBorrow.addActionListener(e -> handleBorrow());
        btnReturn.addActionListener(e -> handleReturn());
        btnMyBooks.addActionListener(e -> handleMyBooks());
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            refreshBookList();
        });

        btnLogout.addActionListener(e -> handleLogout());
    }

    /**
     * Tải danh sách sách lên bảng JTable
     */
    private void loadBooksToTable(List<Book> books) {
        tableModel.setRowCount(0);
        int availableCount = 0;
        int borrowedCount = 0;

        for (Book book : books) {
            String status = book.isAvailable() ? "Available" : "Borrowed";
            String borrower = (book.getBorrowedBy() != null) ? book.getBorrowedBy() : "-";
            if (book.isAvailable()) {
                availableCount++;
            } else {
                borrowedCount++;
            }

            tableModel.addRow(new Object[]{
                    book.getId(),
                    book.getTitle(),
                    book.getAuthor(),
                    status,
                    borrower
            });
        }

        lblStatus.setText(String.format("Tổng số sách hiển thị: %d | Có sẵn: %d | Đã mượn: %d",
                books.size(), availableCount, borrowedCount));
    }

    /**
     * Làm mới toàn bộ danh sách sách từ Server
     */
    private void refreshBookList() {
        try {
            List<Book> books = service.getBooks();
            loadBooksToTable(books);
        } catch (RemoteException ex) {
            handleRemoteException(ex);
        }
    }

    /**
     * Xử lý tìm kiếm sách
     */
    private void handleSearch() {
        String keyword = txtSearch.getText().trim();
        try {
            List<Book> results = service.searchBooks(keyword);
            loadBooksToTable(results);
            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy cuốn sách nào khớp với từ khóa: '" + keyword + "'",
                        "Kết quả tìm kiếm",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (RemoteException ex) {
            handleRemoteException(ex);
        }
    }

    /**
     * Xử lý mượn sách
     */
    private void handleBorrow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một cuốn sách từ bảng để mượn!",
                    "Chưa chọn sách",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bookId = (int) tableModel.getValueAt(selectedRow, 0);
        String title = (String) tableModel.getValueAt(selectedRow, 1);
        String status = (String) tableModel.getValueAt(selectedRow, 3);

        // Kiểm tra ngay trên giao diện nếu sách đã được mượn
        if ("Borrowed".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this,
                    "Cuốn sách '" + title + "' hiện đã có người mượn!\nVui lòng chọn sách khác.",
                    "Sách đã được mượn",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            boolean success = service.borrowBook(currentUser, bookId);
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Bạn đã mượn thành công cuốn sách:\n\"" + title + "\"!",
                        "Mượn sách thành công",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Mượn sách thất bại! Có thể cuốn sách vừa được một người dùng khác mượn.",
                        "Mượn sách thất bại",
                        JOptionPane.ERROR_MESSAGE);
            }
            refreshBookList();
        } catch (RemoteException ex) {
            handleRemoteException(ex);
        }
    }

    /**
     * Xử lý trả sách
     */
    private void handleReturn() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một cuốn sách từ bảng để trả!",
                    "Chưa chọn sách",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bookId = (int) tableModel.getValueAt(selectedRow, 0);
        String title = (String) tableModel.getValueAt(selectedRow, 1);
        String status = (String) tableModel.getValueAt(selectedRow, 3);
        String borrower = (String) tableModel.getValueAt(selectedRow, 4);

        // Kiểm tra xem sách có đang mượn không
        if ("Available".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this,
                    "Cuốn sách '" + title + "' đang có sẵn tại thư viện, chưa bị mượn nên không thể trả!",
                    "Cảnh báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Kiểm tra xem có đúng người dùng này mượn không
        if (!"-".equals(borrower) && !currentUser.equalsIgnoreCase(borrower)) {
            JOptionPane.showMessageDialog(this,
                    "Bạn không thể trả sách này vì cuốn sách đang do '" + borrower + "' mượn!",
                    "Lỗi quyền trả sách",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            boolean success = service.returnBook(currentUser, bookId);
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Bạn đã trả thành công cuốn sách:\n\"" + title + "\"!",
                        "Trả sách thành công",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Trả sách thất bại! Vui lòng kiểm tra lại trạng thái sách trên server.",
                        "Trả sách thất bại",
                        JOptionPane.ERROR_MESSAGE);
            }
            refreshBookList();
        } catch (RemoteException ex) {
            handleRemoteException(ex);
        }
    }

    /**
     * Xem danh sách sách người dùng hiện tại đang mượn
     */
    private void handleMyBooks() {
        try {
            List<Book> myBooks = service.getBorrowedBooks(currentUser);
            loadBooksToTable(myBooks);
            if (myBooks.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Bạn hiện không mượn bất kỳ cuốn sách nào.",
                        "Sách của tôi",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (RemoteException ex) {
            handleRemoteException(ex);
        }
    }

    /**
     * Đăng xuất và quay lại màn hình Login
     */
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn đăng xuất không?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            this.dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }

    /**
     * Xử lý khi mất kết nối RMI tới Server
     */
    private void handleRemoteException(RemoteException ex) {
        JOptionPane.showMessageDialog(this,
                "Mất kết nối tới RMI Server!\nChi tiết: " + ex.getMessage() +
                "\n\nVui lòng kiểm tra lại RMIServer.",
                "Lỗi kết nối RMI",
                JOptionPane.ERROR_MESSAGE);
    }
}

