# LAB 5: XÂY DỰNG MAIL SERVER BẰNG UDP

> **Lưu ý quan trọng về UDP:**  
> UDP không đảm bảo packet được truyền thành công, không đảm bảo thứ tự và không có cơ chế connection như TCP. Chương trình này chỉ mô phỏng Mail Server đơn giản phục vụ mục đích học tập trong môn học Lập trình mạng (Network Programming).

---

## 1. Giới thiệu bài toán

Bài lab yêu cầu xây dựng một hệ thống **Mail Server** và **Mail Client** đơn giản hoạt động dựa trên giao thức **UDP**:
- Server quản lý danh sách hòm thư (mailbox) của người dùng bằng cấu trúc thư mục trên ổ đĩa.
- Mỗi khi client tạo tài khoản mới, server tự động tạo thư mục tương ứng cho tài khoản và một email chào mừng `new_email.txt`.
- Mỗi khi client gửi email, server lưu nội dung email vào thư mục mailbox của người nhận thành file riêng biệt (`email_001.txt`, `email_002.txt`,...).
- Khi client đăng nhập vào một tài khoản, server đọc và trả về danh sách tất cả các file có trong hòm thư của người đó.

---

## 2. Công nghệ sử dụng

- **Ngôn ngữ:** Java (tương thích Java 17+).
- **Mạng (Networking):** `DatagramSocket`, `DatagramPacket`, `InetAddress` (thuần UDP, không dùng TCP/HTTP).
- **Lưu trữ:** Java File I/O / NIO (`java.nio.file.Files`, `java.nio.file.Path`).
- **Build Tool:** Apache Maven.

---

## 3. Kiến trúc Client / Server

```text
       +--------------------+
       |   UDP MailClient   |
       +---------+----------+
                 |
                 | UDP DatagramPacket (Port 5000)
                 v
       +--------------------+
       |   UDP MailServer   |
       +---------+----------+
                 |
                 v
             accounts/
                 ├── lam/
                 │   ├── new_email.txt
                 │   ├── email_001.txt
                 │   └── email_002.txt
                 └── nam/
                     ├── new_email.txt
                     └── email_001.txt
```

---

## 4. UDP được sử dụng như thế nào

- **Connectionless:** Client và Server không thiết lập kết nối trước khi gửi dữ liệu như TCP (`Socket`/`ServerSocket`).
- **Server:** Khởi tạo `DatagramSocket(5000)`, liên tục lắng nghe `socket.receive(packet)` để nhận các gói tin UDP từ bất kỳ client nào, xử lý nội dung chuỗi (UTF-8), sau đó gửi phản hồi ngược lại `clientAddress:clientPort` thông qua `socket.send(responsePacket)`.
- **Client:** Khởi tạo `DatagramSocket()`, đóng gói request thành `DatagramPacket` gửi tới địa chỉ `serverHost:serverPort`. Client thiết lập `setSoTimeout(3000)` (3 giây) để tránh bị treo vô hạn nếu server không phản hồi do mất mát gói tin mạng đặc trưng của UDP.

---

## 5. Giao thức giao tiếp (Protocol)

Hệ thống sử dụng protocol dạng văn bản (text-based) ngắn gọn phân tách bởi dấu gạch đứng `|`:

### Client -> Server

| Lệnh | Định dạng | Ý nghĩa |
| :--- | :--- | :--- |
| **PING** | `PING` | Kiểm tra kết nối UDP tới server |
| **CREATE_ACCOUNT** | `CREATE_ACCOUNT\|<username>` | Yêu cầu tạo tài khoản mới |
| **LOGIN** | `LOGIN\|<username>` | Yêu cầu đăng nhập và lấy danh sách email |
| **SEND_EMAIL** | `SEND_EMAIL\|<sender>\|<receiver>\|<message>` | Gửi nội dung thư tới người nhận |
| **READ_EMAIL** | `READ_EMAIL\|<username>\|<filename>` | Đọc nội dung file email trong hòm thư |

### Server -> Client

| Phản hồi | Định dạng | Ý nghĩa |
| :--- | :--- | :--- |
| **PONG** | `PONG` | Phản hồi kiểm tra kết nối |
| **SUCCESS** | `SUCCESS\|<thông báo>` | Thực hiện thao tác thành công |
| **ERROR** | `ERROR\|<thông báo lỗi>` | Có lỗi xảy ra (tài khoản đã tồn tại, không tìm thấy người nhận, tin nhắn rỗng,...) |
| **FILES** | `FILES\|<file1>\|<file2>\|...` | Danh sách các file email trong hòm thư |
| **EMAIL_CONTENT** | `EMAIL_CONTENT\|<filename>\|<content>` | Trả về toàn bộ nội dung text của file email |

---

## 6. Cấu trúc thư mục lưu trữ Mailbox

Thư mục `accounts/` được tạo tự động tại thư mục làm việc của MailServer:

```text
accounts/
├── lam/
│   ├── new_email.txt
│   ├── email_001.txt
│   └── email_002.txt
└── nam/
    ├── new_email.txt
    └── email_001.txt
```

- **new_email.txt:** Nội dung thư chào mừng mặc định khi tạo tài khoản:
  `Thank you for using this service. we hope that you will feel comfortable........`
- **email_XXX.txt:** Được tự động đánh số thứ tự từ `email_001.txt`, `email_002.txt`... dựa trên số file hiện có, tránh ghi đè email cũ.

---

## 7. Hướng dẫn biên dịch và chạy

### 7.1. Biên dịch Project

Tại thư mục `Lab5`, chạy lệnh Maven:

```bash
mvn clean compile
```

### 7.2. Chạy Server

Khởi động Mail Server lắng nghe tại UDP port 5000:

```bash
java -cp target/classes com.lam.mail.MailServer
```

Console hiển thị:
```text
[SERVER] UDP Mail Server is running on port 5000...
[SERVER] Mailbox directory: .../Lab5/accounts
```

### 7.3. Chạy Client (Console)

Mở một cửa sổ dòng lệnh khác và chạy Mail Client ở chế độ Console:

```bash
# Chạy với địa chỉ mặc định localhost:5000
java -cp target/classes com.lam.mail.MailClient

# Hoặc truyền tham số Host và Port:
java -cp target/classes com.lam.mail.MailClient localhost 5000
```

Giao diện tương tác Console:
```text
=================================
       UDP MAIL CLIENT
=================================
1. Create account
2. Login
3. Send email
4. Exit
=================================
Choose:
```

### 7.4. Chạy Mail Client với Giao diện đồ họa (Java Swing GUI)

Chạy Client với giao diện đồ họa trực quan (Swing GUI):

```bash
java -cp target/classes com.lam.mail.gui.MailClientGui
```

Hoặc:
```bash
java -cp target/classes com.lam.mail.gui.LoginFrame
```

Các màn hình chính trên GUI:
- **LoginFrame:** Cấu hình Server IP, Server Port, nhập Username, nút `Login` và `Create Account`.
- **CreateAccountDialog:** Hộp thoại tạo tài khoản mới nhanh chóng.
- **MailboxFrame:** Hiển thị `Logged in as: <username>`, danh sách file email trong hòm thư (`JList`), các nút `Open Mail`, `Refresh`, `Send Email`, `Logout`.
- **ViewEmailDialog:** Cửa sổ xem chi tiết nội dung email (hiển thị khi nhấn đúp vào email hoặc chọn email và bấm nút `Open Mail`).
- **SendEmailDialog:** Hộp thoại gửi email với trường `From` (tự động điền), `To` (người nhận), và `Message` (nội dung thư nhiều dòng).

---

## 8. Hướng dẫn Test các trường hợp theo đề bài

### Test 1 — Tạo tài khoản mới (`CREATE_ACCOUNT`)
- Chọn `1`, nhập username: `lam`
- Kết quả mong đợi: `[SUCCESS] Account created successfully`
- Kiểm tra trên server: file `accounts/lam/new_email.txt` được tạo với nội dung chào mừng.

### Test 2 — Tạo tài khoản đã tồn tại
- Chọn `1`, nhập lại username: `lam`
- Kết quả mong đợi: `[ERROR] Account already exists`

### Test 3 — Gửi email giữa 2 tài khoản
- Tạo thêm tài khoản `nam` (Chọn `1` -> `nam`).
- Chọn `3` (Send email):
  - Sender: `lam`
  - Receiver: `nam`
  - Message: `Hello Nam!`
- Kết quả mong đợi: `[SUCCESS] Email sent successfully`
- Kiểm tra trên server: file `accounts/nam/email_001.txt` được tạo với nội dung `Hello Nam!`.

### Test 4 — Gửi email tới người nhận không tồn tại
- Chọn `3` (Send email):
  - Sender: `lam`
  - Receiver: `abc`
  - Message: `Hello`
- Kết quả mong đợi: `[ERROR] Receiver account does not exist`

### Test 5 — Đăng nhập và xem Mailbox
- Chọn `2` (Login), nhập username: `nam`
- Kết quả mong đợi: Hiển thị danh sách file:
  ```text
  ===== MAILBOX (nam) =====
  1. email_001.txt
  2. new_email.txt
  =========================
  ```

### Test 6 — Chạy nhiều Client đồng thời
- Mở 2 cửa sổ terminal Client 1 và Client 2 cùng trỏ tới Server.
- Client 1 đăng nhập tài khoản `lam` và gửi thư cho `nam`.
- Client 2 đăng nhập tài khoản `nam` sẽ thấy ngay email mới xuất hiện trong danh sách hòm thư.

---

## 9. Hạn chế của chương trình

1. **Đặc thù UDP:** Dữ liệu có thể bị thất lạc hoặc đến không đúng thứ tự nếu đường truyền mạng gặp sự cố vì UDP không có cơ chế bắt tay (handshake) và ACK tự động.
2. **Kích thước email:** Kích thước Datagram giới hạn tối đa theo bộ đệm buffer (4KB), phù hợp cho email dạng văn bản ngắn phục vụ bài lab.
3. **Bảo mật:** Không có mật khẩu đăng nhập hay mã hóa gói tin (đúng phạm vi yêu cầu bài tập cơ sở mạng).

