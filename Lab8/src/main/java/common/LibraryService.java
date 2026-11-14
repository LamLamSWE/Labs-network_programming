package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote Interface định nghĩa các phương thức dịch vụ thư viện từ xa.
 * Kế thừa từ java.rmi.Remote và tất cả các phương thức đều ném ra RemoteException.
 */
public interface LibraryService extends Remote {

    /**
     * Lấy toàn bộ danh sách sách hiện có trong thư viện.
     *
     * @return danh sách các đối tượng Book
     * @throws RemoteException nếu có lỗi giao tiếp RMI
     */
    List<Book> getBooks() throws RemoteException;

    /**
     * Tìm kiếm sách theo tiêu đề hoặc tác giả.
     *
     * @param keyword từ khóa tìm kiếm
     * @return danh sách sách khớp với từ khóa
     * @throws RemoteException nếu có lỗi giao tiếp RMI
     */
    List<Book> searchBooks(String keyword) throws RemoteException;

    /**
     * Mượn sách theo ID sách và tên người dùng.
     *
     * @param username tên người mượn
     * @param bookId   ID cuốn sách cần mượn
     * @return true nếu mượn thành công, false nếu sách không tồn tại hoặc đã bị mượn
     * @throws RemoteException nếu có lỗi giao tiếp RMI
     */
    boolean borrowBook(String username, int bookId) throws RemoteException;

    /**
     * Trả sách theo ID sách và tên người dùng.
     *
     * @param username tên người trả
     * @param bookId   ID cuốn sách cần trả
     * @return true nếu trả thành công, false nếu sách chưa được mượn hoặc không phải do người dùng này mượn
     * @throws RemoteException nếu có lỗi giao tiếp RMI
     */
    boolean returnBook(String username, int bookId) throws RemoteException;

    /**
     * Lấy danh sách các cuốn sách mà người dùng cụ thể đang mượn.
     *
     * @param username tên người dùng
     * @return danh sách sách mà người dùng đang mượn
     * @throws RemoteException nếu có lỗi giao tiếp RMI
     */
    List<Book> getBorrowedBooks(String username) throws RemoteException;
}

