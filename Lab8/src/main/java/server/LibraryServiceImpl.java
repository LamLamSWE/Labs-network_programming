package server;

import common.Book;
import common.LibraryService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Remote Implementation triển khai interface LibraryService.
 * Kế thừa UnicastRemoteObject để cho phép đối tượng này lắng nghe các yêu cầu gọi hàm từ xa từ Client.
 */
public class LibraryServiceImpl extends UnicastRemoteObject implements LibraryService {
    private static final long serialVersionUID = 1L;

    // Danh sách lưu trữ sách trong bộ nhớ (In-memory storage)
    private final List<Book> books = new ArrayList<>();

    public LibraryServiceImpl() throws RemoteException {
        super();
        initDefaultBooks();
    }

    /**
     * Khởi tạo sẵn danh sách 8 cuốn sách mẫu trong thư viện.
     */
    private void initDefaultBooks() {
        books.add(new Book(1, "Clean Code", "Robert C. Martin"));
        books.add(new Book(2, "Effective Java", "Joshua Bloch"));
        books.add(new Book(3, "Java: The Complete Reference", "Herbert Schildt"));
        books.add(new Book(4, "Head First Java", "Kathy Sierra & Bert Bates"));
        books.add(new Book(5, "Introduction to Algorithms", "Thomas H. Cormen"));
        books.add(new Book(6, "Design Patterns", "Erich Gamma et al."));
        books.add(new Book(7, "Computer Networking: A Top-Down Approach", "James Kurose & Keith Ross"));
        books.add(new Book(8, "Operating System Concepts", "Abraham Silberschatz"));
        System.out.println("[SERVER] Initialized " + books.size() + " default books into library.");
    }

    /**
     * Lấy toàn bộ danh sách sách.
     * Sử dụng synchronized để bảo vệ dữ liệu khi nhiều Client đọc/ghi cùng lúc.
     */
    @Override
    public synchronized List<Book> getBooks() throws RemoteException {
        System.out.println("[SERVER] getBooks() called. Returning " + books.size() + " books.");
        return new ArrayList<>(books);
    }

    /**
     * Tìm kiếm sách theo từ khóa (tiêu đề hoặc tên tác giả).
     */
    @Override
    public synchronized List<Book> searchBooks(String keyword) throws RemoteException {
        System.out.println("[SERVER] searchBooks() called with keyword: '" + keyword + "'");
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(books);
        }
        String lower = keyword.trim().toLowerCase();
        List<Book> result = new ArrayList<>();
        for (Book b : books) {
            if (b.getTitle().toLowerCase().contains(lower) || b.getAuthor().toLowerCase().contains(lower)) {
                result.add(b);
            }
        }
        return result;
    }

    /**
     * Xử lý yêu cầu mượn sách.
     * Kiểm tra sách có tồn tại và đang Available không.
     * Sử dụng synchronized để tránh trường hợp Race Condition khi 2 Client cùng bấm mượn 1 cuốn sách.
     */
    @Override
    public synchronized boolean borrowBook(String username, int bookId) throws RemoteException {
        if (username == null || username.trim().isEmpty()) {
            System.err.println("[SERVER] Reject borrow: Username is empty.");
            return false;
        }

        for (Book book : books) {
            if (book.getId() == bookId) {
                if (book.isAvailable()) {
                    book.setAvailable(false);
                    book.setBorrowedBy(username.trim());
                    System.out.printf("[SERVER SUCCESS] User '%s' borrowed book ID %d: '%s'%n",
                            username, bookId, book.getTitle());
                    return true;
                } else {
                    System.out.printf("[SERVER FAILED] User '%s' failed to borrow book ID %d (Already borrowed by '%s')%n",
                            username, bookId, book.getBorrowedBy());
                    return false;
                }
            }
        }

        System.out.printf("[SERVER FAILED] Book ID %d not found for borrow request by '%s'%n", bookId, username);
        return false;
    }

    /**
     * Xử lý yêu cầu trả sách.
     * Chỉ người đang mượn cuốn sách đó mới được phép trả sách.
     */
    @Override
    public synchronized boolean returnBook(String username, int bookId) throws RemoteException {
        if (username == null || username.trim().isEmpty()) {
            System.err.println("[SERVER] Reject return: Username is empty.");
            return false;
        }

        for (Book book : books) {
            if (book.getId() == bookId) {
                // Kiểm tra xem sách có đang bị mượn không và người trả có đúng là người mượn không
                if (!book.isAvailable() && username.trim().equalsIgnoreCase(book.getBorrowedBy())) {
                    book.setAvailable(true);
                    book.setBorrowedBy(null);
                    System.out.printf("[SERVER SUCCESS] User '%s' returned book ID %d: '%s'%n",
                            username, bookId, book.getTitle());
                    return true;
                } else {
                    System.out.printf("[SERVER FAILED] User '%s' cannot return book ID %d (Current borrower: '%s', Available: %s)%n",
                            username, bookId, book.getBorrowedBy(), book.isAvailable());
                    return false;
                }
            }
        }

        System.out.printf("[SERVER FAILED] Book ID %d not found for return request by '%s'%n", bookId, username);
        return false;
    }

    /**
     * Lấy danh sách các cuốn sách do một người dùng cụ thể đang mượn.
     */
    @Override
    public synchronized List<Book> getBorrowedBooks(String username) throws RemoteException {
        System.out.println("[SERVER] getBorrowedBooks() called for user: '" + username + "'");
        if (username == null || username.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Book> result = new ArrayList<>();
        for (Book b : books) {
            if (!b.isAvailable() && username.trim().equalsIgnoreCase(b.getBorrowedBy())) {
                result.add(b);
            }
        }
        return result;
    }
}

