# BÁO CÁO BÀI TẬP LAB 8: MINI LIBRARY MANAGEMENT SỬ DỤNG JAVA RMI

> **Môn học:** Lập trình mạng (Network Programming)  
> **Chủ đề:** Xây dựng ứng dụng quản lý mượn/trả sách thư viện theo mô hình Client - Server bằng Java RMI (Remote Method Invocation) kết hợp giao diện Java Swing.

---

## 1. GIỚI THIỆU BÀI TOÁN

Trong các hệ thống phân tán, **Remote Method Invocation (RMI)** là cơ chế cho phép một đối tượng chạy trên một máy ảo Java (JVM) có thể triệu gọi trực tiếp các phương thức của một đối tượng đang nằm trên một JVM khác (có thể trên cùng một máy hoặc khác máy qua mạng).

Bài toán được đặt ra trong bài Lab này là: **Xây dựng ứng dụng quản lý mượn/trả sách thư viện đơn giản (Mini Library)**:
- **Server:** Chịu trách nhiệm quản lý danh mục sách, trạng thái khả dụng (*Available / Borrowed*), danh sách người đang mượn sách, và xử lý các thao tác mượn/trả đảm bảo an toàn đa luồng (*Thread-safe*).
- **Client:** Cung cấp giao diện đồ họa **Java Swing** cho người dùng nhập Username, tìm kiếm sách, thực hiện mượn sách, trả sách, xem danh sách sách cá nhân đang mượn và cập nhật dữ liệu từ xa theo thời gian thực qua RMI.

---

## 2. MỤC TIÊU DỰ ÁN

1. Nắm vững kiến trúc và nguyên lý hoạt động của **Java RMI** trong mô hình Client–Server.
2. Hiểu rõ quy trình:
   - Định nghĩa **Remote Interface** kế thừa `java.rmi.Remote`.
   - Cài đặt **Remote Implementation** kế thừa `UnicastRemoteObject`.
   - Khởi tạo và quản lý **RMI Registry** trên cổng mặc định `1099`.
   - Đăng ký đối tượng (`rebind`) và tìm kiếm đối tượng (`lookup`).
   - Truyền tải đối tượng dữ liệu qua mạng với `java.io.Serializable`.
3. Xử lý bài toán tương tranh (**Concurrency / Race Condition**) khi nhiều Client cùng lúc mượn một cuốn sách.
4. Xây dựng giao diện **Swing GUI** thân thiện, bắt lỗi đầy đủ bằng `JOptionPane`.

---

## 3. CÔNG NGHỆ BẮT BUỘC SỬ DỤNG

- **Ngôn ngữ:** Java (JDK 17 trở lên).
- **Cơ chế mạng:** Java RMI (`java.rmi.*`).
- **Giao diện:** Java Swing (`javax.swing.*`, `java.awt.*`).
- **Quản lý dự án:** Maven (cấu hình đơn giản, không đa module phức tạp).
- **Không sử dụng:** Spring, Spring Boot, Socket thủ công, REST API hay Database (lưu trữ In-Memory).

---

## 4. KIẾN TRÚC CLIENT - SERVER & RMI

```text
                           +--------------------------+
                           |       RMI REGISTRY       |
                           |       (Port: 1099)       |
                           +--------------------------+
                             ▲                      │
              1. rebind()    │                      │ 2. lookup()
              "LibraryService"                      │ "LibraryService"
                             │                      ▼
                   ┌─────────────────┐     ┌──────────────────┐
                   │   RMI SERVER    │     │    RMI CLIENT    │
                   │ (JVM Server)    │     │   (JVM Client)   │
                   ├─────────────────┤     ├──────────────────┤
                   │LibraryServiceImpl     │   LibraryService │
                   │  (Remote Object)│     │      (Stub)      │
                   └────────┬────────┘     └────────┬─────────┘
                            │                       │
                            │   3. Remote Invocation│
                            │◄──────────────────────┘
                            │   (TCP/JRMP Network)
                            │
                            │   4. Return Book Data
                            └──────────────────────►
```

### Nguyên lý hoạt động:
1. **Server** khởi chạy `RMIServer`:
   - Tạo `RMI Registry` tại cổng `1099`.
   - Tạo đối tượng `LibraryServiceImpl` (kế thừa `UnicastRemoteObject`).
   - Đăng ký đối tượng này vào Registry với tên định danh `"LibraryService"`.
2. **Client** khởi chạy `RMIClient`:
   - Hiển thị `LoginFrame` yêu cầu người dùng nhập `Username`.
   - Kết nối tới `RMI Registry` tại `localhost:1099` và gọi phương thức `lookup("LibraryService")`.
   - RMI trả về cho Client một đối tượng tham chiếu từ xa (**Stub**).
3. **Gọi hàm từ xa (Remote Method Call):**
   - Khi người dùng bấm Mượn/Trả sách, Client gọi trực tiếp hàm trên đối tượng `LibraryService` như hàm nội bộ.
   - Dưới tầng giao vận, RMI tuần tự hóa tham số (`Serialization`), gửi qua mạng, Server thực thi trên `LibraryServiceImpl`, cập nhật danh sách sách và trả kết quả về cho Client.

---

## 5. CẤU TRÚC THƯ MỤC DỰ ÁN

```text
Lab8/
├── pom.xml                                    # File cấu hình Maven (Java 17+, không dependency ngoài)
├── README.md                                  # Tài liệu hướng dẫn và lý thuyết bảo vệ bài Lab
└── src/
    ├── main/
    │   └── java/
    │       ├── common/                        # Thành phần dùng chung giữa Client và Server
    │       │   ├── Book.java                  # Model Book (implements Serializable)
    │       │   └── LibraryService.java        # Remote Interface (extends Remote)
    │       │
    │       ├── server/                        # Phía Server
    │       │   ├── LibraryServiceImpl.java    # Cài đặt Remote Object (Thread-safe)
    │       │   └── RMIServer.java             # Khởi tạo RMI Registry & đăng ký dịch vụ
    │       │
    │       └── client/                        # Phía Client
    │           ├── RMIClient.java             # Entry point khởi chạy Client
    │           ├── LoginFrame.java            # Màn hình đăng nhập & kết nối RMI
    │           └── MainFrame.java             # Giao diện chính (Xem, Tìm, Mượn, Trả, Sách của tôi)
    │
    └── test/
        └── java/
            └── RMIVerifyTest.java             # Kiểm thử tự động logic RMI & tương tranh
```

---

## 6. CHI TIẾT CÁC CLASS & REMOTE INTERFACE

### 6.1. `common.Book` (Model)
- Thuộc tính: `id` (int), `title` (String), `author` (String), `available` (boolean), `borrowedBy` (String).
- **Bắt buộc `implements Serializable`**: Đối tượng `Book` được truyền qua mạng giữa Server và Client dưới dạng dòng byte nhị phân. Nếu không có `Serializable`, Java RMI sẽ ném ngoại lệ `java.io.NotSerializableException`.

### 6.2. `common.LibraryService` (Remote Interface)
```java
public interface LibraryService extends Remote {
    List<Book> getBooks() throws RemoteException;
    List<Book> searchBooks(String keyword) throws RemoteException;
    boolean borrowBook(String username, int bookId) throws RemoteException;
    boolean returnBook(String username, int bookId) throws RemoteException;
    List<Book> getBorrowedBooks(String username) throws RemoteException;
}
```
- Phải kế thừa `java.rmi.Remote`.
- Mọi phương thức đều phải khai báo `throws RemoteException` vì giao tiếp mạng có thể phát sinh lỗi (đứt cáp, mất kết nối, Server sập,...).

### 6.3. `server.LibraryServiceImpl` (Remote Implementation)
- Kế thừa `UnicastRemoteObject` và triển khai `LibraryService`.
- Khởi tạo sẵn 8 cuốn sách kinh điển về CNTT (Clean Code, Effective Java, Introduction to Algorithms,...).
- Các phương thức `borrowBook()`, `returnBook()`, `getBooks()` đều sử dụng từ khóa **`synchronized`** để đảm bảo tính an toàn tương tranh (**Thread-Safe**). Khi 2 Client cùng bấm mượn 1 cuốn sách tại cùng 1 tích tắc, chỉ 1 Client mượn được, Client còn lại sẽ nhận được kết quả `false`.

### 6.4. `server.RMIServer` (Server Bootstrapper)
- Khởi tạo `LocateRegistry.createRegistry(1099)`.
- Tạo đối tượng `LibraryServiceImpl`.
- Đăng ký vào Registry: `registry.rebind("LibraryService", libraryService)`.

### 6.5. `client.LoginFrame` & `client.MainFrame` (Swing GUI)
- **LoginFrame:** Nhập Host, Port, Username. Bắt lỗi không để trống Username, bắt lỗi không kết nối được Server.
- **MainFrame:**
  - `JTable` hiển thị: ID, Tên sách, Tác giả, Trạng thái (*Available / Borrowed*), Người mượn (*Borrower*).
  - Tìm kiếm sách tức thì theo tiêu đề hoặc tác giả.
  - Nút **Mượn sách (Borrow)**: Kiểm tra trạng thái khả dụng, báo lỗi nếu sách đã có người mượn.
  - Nút **Trả sách (Return)**: Kiểm tra quyền trả sách (chỉ người đang mượn cuốn sách đó mới được trả).
  - Nút **Sách của tôi (My Books)**: Lọc nhanh danh sách những cuốn sách mà người dùng này đang mượn.
  - Nút **Làm mới (Refresh)**: Đồng bộ danh sách mới nhất từ Server.
  - Nút **Đăng xuất (Logout)**: Trở về màn hình đăng nhập.

---

## 7. HƯỚNG DẪN BIÊN DỊCH VÀ KHỞI CHẠY

### Cách 1: Sử dụng IntelliJ IDEA / Eclipse / VS Code (Khuyên dùng)
1. Mở thư mục `Lab8` bằng **IntelliJ IDEA** (hoặc mở cả repository `NetworkProgaming-Lab`).
2. IntelliJ sẽ tự động nhận diện `Lab8/pom.xml`.
3. **Chạy Server:**
   - Mở file `src/main/java/server/RMIServer.java`.
   - Nhấn chuột phải chọn **Run 'RMIServer.main()'**.
   - Kiểm tra Console hiển thị: `[RMI] Server is ready to receive requests from clients...`.
4. **Chạy Client:**
   - Mở file `src/main/java/client/RMIClient.java`.
   - Nhấn chuột phải chọn **Run 'RMIClient.main()'**.
   - Giao diện đăng nhập sẽ xuất hiện. Nhập tên người dùng (ví dụ: `Alice`) và bấm **ĐĂNG NHẬP**.

### Cách 2: Sử dụng dòng lệnh (Terminal / PowerShell / CMD)
Đứng tại thư mục `Lab8`:

1. **Biên dịch dự án:**
   ```bash
   mvn compile
   ```

2. **Khởi động RMI Server:**
   ```bash
   java -cp target/classes server.RMIServer
   ```
   Hoặc dùng Maven Exec Plugin:
   ```bash
   mvn exec:java -Dexec.mainClass="server.RMIServer"
   ```

3. **Khởi động RMI Client:**
   Mở một cửa sổ Terminal mới:
   ```bash
   java -cp target/classes client.RMIClient
   ```
   Hoặc dùng Maven:
   ```bash
   mvn exec:java -Dexec.mainClass="client.RMIClient"
   ```

---

## 8. HƯỚNG DẪN TEST CHẠY NHIỀU CLIENT CÙNG LÚC (MULTI-CLIENT DEMO)

Để minh họa rõ nét tính năng Client–Server và khả năng xử lý tương tranh của RMI:

1. Đảm bảo `RMIServer` đang chạy.
2. **Khởi chạy Client 1:**
   - Đăng nhập với Username: `Alice`.
3. **Khởi chạy Client 2:**
   - Mở thêm một instance của `RMIClient` (trong IntelliJ: chọn *Edit Configurations* -> tích chọn *Allow multiple instances*).
   - Đăng nhập với Username: `Bob`.
4. **Kịch bản kiểm thử trực tiếp:**
   - **Kịch bản 1 (Mượn sách):**
     - Trên cửa sổ của `Alice`, chọn sách ID 1 (`Clean Code`) và bấm **Mượn sách (Borrow)**.
     - Cửa sổ của Alice hiển thị sách 1 chuyển thành `Borrowed` (Người mượn: `Alice`).
     - Chuyển sang cửa sổ của `Bob`, bấm nút **Làm mới (Refresh)** -> Ngay lập tức Bob thấy sách 1 đã chuyển thành `Borrowed` (Người mượn: `Alice`).
   - **Kịch bản 2 (Chống mượn trùng):**
     - `Bob` cố tình chọn sách ID 1 và bấm **Mượn sách (Borrow)** -> Hệ thống bật thông báo cảnh báo: *Cuốn sách hiện đã có người mượn!*
   - **Kịch bản 3 (Kiểm tra quyền trả sách):**
     - `Bob` cố tình chọn sách ID 1 và bấm **Trả sách (Return)** -> Hệ thống từ chối: *Bạn không thể trả sách này vì người mượn là 'Alice'!*
   - **Kịch bản 4 (Sách của tôi & Trả sách):**
     - Trên cửa sổ của `Alice`, bấm nút **Sách của tôi (My Books)** -> Chỉ hiển thị duy nhất cuốn `Clean Code`.
     - `Alice` bấm **Trả sách (Return)** -> Trả thành công, trạng thái sách chuyển lại thành `Available`.
     - `Bob` bấm **Làm mới** -> Thấy sách đã `Available` và bây giờ `Bob` có thể mượn sách bình thường.

---

## 9. VÍ DỤ VỀ LUỒNG DỮ LIỆU KHI MƯỢN SÁCH

```text
[Alice - Client GUI]
       │
       │ 1. Alice chọn sách ID 1, nhấn "Mượn sách"
       ▼
[RMI Stub] (Client side)
       │
       │ 2. Marshalling tham số: ("Alice", 1)
       │    Gửi gói tin TCP/JRMP qua mạng
       ▼
[RMI Skeleton / Dispatcher] (Server side)
       │
       │ 3. Unmarshalling tham số
       ▼
[LibraryServiceImpl]
       │
       │ 4. synchronized borrowBook("Alice", 1)
       │    - Kiểm tra book.isAvailable() == true
       │    - Gán available = false, borrowedBy = "Alice"
       │    - In log Server: [SERVER SUCCESS] User 'Alice' borrowed book ID 1
       │    - return true;
       ▼
[RMI Skeleton]
       │
       │ 5. Marshalling kết quả boolean 'true'
       │    Gửi phản hồi về Client qua mạng
       ▼
[RMI Stub]
       │
       │ 6. Unmarshalling kết quả 'true'
       ▼
[Alice - Client GUI]
       │
       │ 7. Hiển thị thông báo "Mượn sách thành công!"
       │    Gọi service.getBooks() để cập nhật lại JTable
       ▼
```

---

## 10. KIẾN THỨC CỐT LÕI ĐỂ BẢO VỆ TRƯỚC GIẢNG VIÊN

| Khái niệm / Câu hỏi | Giải thích ngắn gọn, chính xác |
| :--- | :--- |
| **Java RMI là gì?** | Là cơ chế của Java cho phép một chương trình trên máy khách gọi phương thức của một đối tượng trên máy chủ từ xa giống như đang gọi phương thức nội bộ. |
| **Vai trò của `Remote` Interface?** | Giao diện đánh dấu (marker interface). Bất kỳ phương thức nào có thể gọi từ xa đều phải được khai báo trong interface kế thừa `java.rmi.Remote`. |
| **Tại sao các hàm RMI phải `throws RemoteException`?** | Vì việc giao tiếp diễn ra qua mạng, có thể gặp các lỗi vật lý hoặc giao thức (timeout, rớt mạng, server chết). `RemoteException` là checked exception để bắt buộc lập trình viên phải xử lý lỗi mạng. |
| **Vai trò của `UnicastRemoteObject`?** | Lớp cơ sở cung cấp cơ chế để đối tượng từ xa có thể lắng nghe các kết nối TCP/IP trên một cổng ẩn danh và nhận các cuộc gọi RMI đến. |
| **RMI Registry là gì?** | Là một dịch vụ định danh (naming service) đóng vai trò như cuốn danh bạ điện thoại: Server đăng ký tên dịch vụ (`rebind`), Client tìm kiếm dịch vụ theo tên (`lookup`) để nhận đối tượng Stub. Cổng mặc định là `1099`. |
| **Stub và Skeleton là gì?** | - **Stub:** Đại diện của đối tượng từ xa nằm ở phía Client, chịu trách nhiệm đóng gói tham số (marshalling) và gửi qua mạng.<br>- **Skeleton:** Bộ phận tiếp nhận ở phía Server (từ Java 5 trở đi đã được xử lý tự động bởi RMI runtime qua dynamic proxy/reflection), giải mã tham số và gọi hàm trên đối tượng thực tế. |
| **Tại sao `Book` phải `implements Serializable`?** | Vì các đối tượng truyền qua mạng trong RMI được truyền theo dạng **Pass-by-value** (bản sao). Java cần tuần tự hóa (Serialize) đối tượng thành dòng byte để truyền qua socket. |
| **Cách xử lý tương tranh khi nhiều Client mượn cùng 1 sách?** | Sử dụng từ khóa `synchronized` trên các phương thức `borrowBook()` và `returnBook()` trong `LibraryServiceImpl`. Điều này đảm bảo mỗi thời điểm chỉ có 1 luồng được thay đổi trạng thái của sách, loại bỏ hoàn toàn hiện tượng Race Condition. |

