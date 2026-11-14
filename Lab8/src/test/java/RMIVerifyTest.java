import common.Book;
import common.LibraryService;
import server.LibraryServiceImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

/**
 * Kiểm thử tự động toàn bộ luồng RMI:
 * - Khởi tạo RMI Registry
 * - Đăng ký Remote Service
 * - Client lookup và gọi các phương thức từ xa
 * - Mượn sách, kiểm tra mượn trùng, trả sách, kiểm tra người mượn
 */
public class RMIVerifyTest {
    public static void main(String[] args) {
        try {
            System.out.println(">>> Starting Server for test...");
            Registry registry = LocateRegistry.createRegistry(11099);
            LibraryServiceImpl serviceImpl = new LibraryServiceImpl();
            registry.rebind("LibraryServiceTest", serviceImpl);

            System.out.println(">>> Testing Client Lookup...");
            Registry clientRegistry = LocateRegistry.getRegistry("localhost", 11099);
            LibraryService clientService = (LibraryService) clientRegistry.lookup("LibraryServiceTest");

            System.out.println(">>> 1. Testing getBooks()...");
            List<Book> books = clientService.getBooks();
            if (books.size() != 8) {
                throw new AssertionError("Expected 8 books, got " + books.size());
            }
            System.out.println("Total books: " + books.size());

            System.out.println(">>> 2. Testing searchBooks('Clean')...");
            List<Book> searchResult = clientService.searchBooks("Clean");
            if (searchResult.size() != 1 || !searchResult.get(0).getTitle().equals("Clean Code")) {
                throw new AssertionError("Search failed!");
            }
            System.out.println("Search found: " + searchResult.get(0));

            System.out.println(">>> 3. Testing borrowBook('Lam', 1)...");
            boolean borrowOk = clientService.borrowBook("Lam", 1);
            if (!borrowOk) {
                throw new AssertionError("Borrow should succeed!");
            }
            System.out.println("Borrow status: " + borrowOk);

            System.out.println(">>> 4. Testing duplicate borrowBook('Bob', 1)...");
            boolean duplicateBorrow = clientService.borrowBook("Bob", 1);
            if (duplicateBorrow) {
                throw new AssertionError("Borrow should fail because already borrowed!");
            }
            System.out.println("Duplicate borrow status (expected false): " + duplicateBorrow);

            System.out.println(">>> 5. Testing getBorrowedBooks('Lam')...");
            List<Book> lamBooks = clientService.getBorrowedBooks("Lam");
            if (lamBooks.size() != 1 || lamBooks.get(0).getId() != 1) {
                throw new AssertionError("Lam should have 1 borrowed book!");
            }
            System.out.println("Lam's books: " + lamBooks);

            System.out.println(">>> 6. Testing returnBook by wrong user ('Bob', 1)...");
            boolean wrongReturn = clientService.returnBook("Bob", 1);
            if (wrongReturn) {
                throw new AssertionError("Return by wrong user should fail!");
            }
            System.out.println("Wrong return status (expected false): " + wrongReturn);

            System.out.println(">>> 7. Testing returnBook by correct user ('Lam', 1)...");
            boolean returnOk = clientService.returnBook("Lam", 1);
            if (!returnOk) {
                throw new AssertionError("Return should succeed!");
            }
            System.out.println("Return status: " + returnOk);

            System.out.println(">>> 8. Testing getBorrowedBooks('Lam') after return...");
            lamBooks = clientService.getBorrowedBooks("Lam");
            if (!lamBooks.isEmpty()) {
                throw new AssertionError("Lam should have 0 borrowed books after return!");
            }
            System.out.println("Lam's books count: " + lamBooks.size());

            System.out.println("==========================================");
            System.out.println(">>> ALL RMI VERIFICATION TESTS PASSED! <<<");
            System.out.println("==========================================");
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
}

