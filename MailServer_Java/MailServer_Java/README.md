# JAVA MAIL SERVER – CLIENT / SERVER

## Yêu cầu
- JDK 17+ khuyến nghị.
- Không cần thư viện ngoài.
- Giao diện dùng Java Swing.
- Kết nối dùng TCP Socket.

## Cấu trúc
```
MailServer_Java/
├── src/
│   ├── MailServer.java
│   └── MailClient.java
└── server_data/
```

## 1. Compile
Mở Terminal tại thư mục `src`:

Windows:
```bash
javac -encoding UTF-8 MailServer.java MailClient.java
```

Linux/macOS:
```bash
javac -encoding UTF-8 MailServer.java MailClient.java
```

## 2. Chạy Server
Tại thư mục `src`:
```bash
java MailServer
```

Server chạy port 5000.

## 3. Chạy Client
Mở Terminal khác tại `src`:
```bash
java MailClient
```

Nếu Server và Client cùng máy:
```
Server IP: 127.0.0.1
```

Nếu khác máy trong LAN, nhập IPv4 của máy Server, ví dụ:
```
192.168.1.10
```

## 4. Demo
Tạo account `alice`, sau đó `bob`.

Server tạo:
```
server_data/
├── alice/
│   └── new_email.txt
└── bob/
    └── new_email.txt
```

Alice gửi email cho Bob.

Server tạo:
```
server_data/bob/email_YYYYMMDD_HHMMSS_SSS.txt
```

Bob đăng nhập, Client nhận danh sách:
```
new_email.txt
email_YYYYMMDD_HHMMSS_SSS.txt
```

Double-click file để đọc email.

## 5. Đúng yêu cầu đề bài
1. Client tạo account cùng mật khẩu -> Server tạo thư mục tương ứng và lưu mật khẩu dạng băm PBKDF2.
2. Server tạo `new_email.txt` với nội dung mặc định.
3. Client gửi email -> Server xác định account nhận và lưu email thành file.
4. Client kiểm tra username/mật khẩu không được để trống rồi gửi yêu cầu đăng nhập; Server xác thực mật khẩu và chỉ gửi tên file nếu thông tin đúng.
5. Server hỗ trợ nhiều Client đồng thời bằng `ExecutorService`.

Mật khẩu được lưu trên Server dưới dạng PBKDF2 hash với salt riêng cho từng account. Với account cũ chưa có mật khẩu, chọn **Tạo account mới** và nhập lại username cùng mật khẩu để thiết lập; email hiện có vẫn được giữ lại.
Giao diện Server có bảng **Client đang kết nối** hiển thị IP, username và trạng thái xác thực; mật khẩu không được hiển thị.

## Lưu ý
Đây là Mail Server mô phỏng cho bài tập Client–Server, không phải SMTP/IMAP mail server thực tế.
Kết nối TCP hiện chưa dùng TLS; không sử dụng mật khẩu thật hoặc tái sử dụng từ dịch vụ khác.
