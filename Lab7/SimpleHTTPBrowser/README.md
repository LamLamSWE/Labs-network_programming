# SimpleHTTPBrowser

> **Môn học:** Lập trình mạng (Network Programming)  
> **Đồ án Lab 7:** Xây dựng trình duyệt HTTP đơn giản sử dụng Java TCP Socket  
> **Nền tảng:** Java 25 | Java Swing GUI | Apache Maven | Jsoup  
> **Giao thức:** HTTP/1.1 thủ công (Manual HTTP Implementation qua Raw TCP Socket & SSLSocket)  

---

## 1. Giới thiệu tổng quan (Introduction)

**SimpleHTTPBrowser** là ứng dụng desktop mô phỏng một trình duyệt web tối giản (HTTP Browser), được phát triển nhằm mục đích giáo dục cho môn học Lập trình mạng. 

### ⚠️ Yêu cầu cốt lõi về giao thức mạng
Khác với các ứng dụng thông thường sử dụng các thư viện HTTP Client bậc cao (như `java.net.http.HttpClient`, `HttpURLConnection`, `OkHttp`, hay `Apache HttpClient`), **SimpleHTTPBrowser tự hiện thực hóa 100% quá trình giao tiếp HTTP/1.1 ở tầng Application Layer thông qua tầng Transport Layer bằng Java Socket**:
1. Tự phân tích cú pháp URL (Scheme, Host, Port, Path, Query).
2. Tự mở kết nối TCP Socket thuần (`java.net.Socket`) cho HTTP cổng 80, và bắt tay TLS an toàn qua `javax.net.ssl.SSLSocket` cho HTTPS cổng 443.
3. Tự đóng gói chuỗi byte yêu cầu HTTP/1.1 theo chuẩn RFC 7230 (Request Line, Headers, CRLF `\r\n\r\n`, Body).
4. Tự tính toán độ dài `Content-Length` chính xác theo số byte UTF-8 thực tế.
5. Tự đọc từng byte từ Socket `InputStream` và phân tích Response (Status Line, Headers, Body framing).
6. Tự hiện thực cả 3 cơ chế framing: `Content-Length`, `Transfer-Encoding: chunked`, và `Connection: close` / `EOF`.
7. Tự xử lý quy tắc đặc thù của phương thức `HEAD` (không có body).
8. Phân tích cấu trúc DOM của tài liệu HTML bằng Jsoup để đếm chính xác các thẻ `<p>`, `<div>`, `<span>`, `<img>`.

---

## 2. Kiến trúc phần mềm (Software Architecture)

Dự án áp dụng mô hình hướng đối tượng (OOP) phân tầng rõ ràng, tách biệt hoàn toàn giữa giao diện (UI), dịch vụ mạng (Service), kết nối socket (Network), bộ sinh request (Builder), bộ phân tích response (Parser) và mô hình dữ liệu (Model):

```
SimpleHTTPBrowser/
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/lam/httpbrowser/
    │   ├── Main.java                        # Entry point, khởi chạy GUI trên EDT
    │   ├── ui/
    │   │   └── BrowserFrame.java            # Giao diện Swing, quản lý tác vụ nền với SwingWorker
    │   ├── service/
    │   │   └── HttpService.java             # Điều phối toàn bộ vòng đời HTTP request/response
    │   ├── network/
    │   │   └── SocketConnection.java        # Quản lý Socket TCP thuần & SSLSocket với SNI
    │   ├── builder/
    │   │   └── HttpRequestBuilder.java      # Đóng gói HTTP/1.1 request (GET, POST, HEAD)
    │   ├── parser/
    │   │   ├── UrlParser.java               # Phân tích cú pháp & chuẩn hóa URL
    │   │   └── HttpResponseParser.java      # Phân tích cú pháp byte-level HTTP Response
    │   ├── model/
    │   │   ├── HttpResult.java              # Chứa thông tin Status, Headers, Body nhận về
    │   │   └── HtmlStatistics.java          # Lưu trữ chỉ số thống kê thẻ HTML
    │   └── analyzer/
    │       └── HtmlAnalyzer.java            # Phân tích cú pháp DOM HTML bằng Jsoup
    └── test/java/com/lam/httpbrowser/
        ├── BrowserFrameTest.java
        ├── UrlParserTest.java
        ├── SocketConnectionTest.java
        ├── HttpRequestBuilderTest.java
        ├── HttpResponseParserTest.java
        ├── HtmlAnalyzerTest.java
        └── HttpServiceIntegrationTest.java
```

### Trách nhiệm chi tiết của từng lớp

| Lớp | Gói (Package) | Trách nhiệm |
|---|---|---|
| [`Main`](file:///src/main/java/com/lam/httpbrowser/Main.java) | `com.lam.httpbrowser` | Điểm vào chương trình, điều phối khởi tạo `BrowserFrame` trên luồng Event Dispatch Thread (EDT). |
| [`BrowserFrame`](file:///src/main/java/com/lam/httpbrowser/ui/BrowserFrame.java) | `com.lam.httpbrowser.ui` | Xây dựng giao diện Swing trực quan (URL bar, method selector, POST body panel, Response tabs, Status & HTML statistics), kích hoạt luồng nền `SwingWorker` để không làm đơ giao diện khi giao tiếp mạng. |
| [`UrlParser`](file:///src/main/java/com/lam/httpbrowser/parser/UrlParser.java) | `com.lam.httpbrowser.parser` | Phân tích URL, xác thực scheme (`http`/`https`), tự động bổ sung cổng mặc định (80/443), chuẩn hóa request target (`path + ? + query`). |
| [`SocketConnection`](file:///src/main/java/com/lam/httpbrowser/network/SocketConnection.java) | `com.lam.httpbrowser.network` | Quản lý kết nối TCP Socket cấp thấp: Socket thường cho HTTP, `SSLSocket` với SNI cho HTTPS, thiết lập connect timeout (10s), read timeout (15s), triển khai `AutoCloseable`. |
| [`HttpRequestBuilder`](file:///src/main/java/com/lam/httpbrowser/builder/HttpRequestBuilder.java) | `com.lam.httpbrowser.builder` | Đóng gói bản tin request HTTP/1.1 thủ công: Request Line, Headers chuẩn (`Host`, `User-Agent`, `Accept`, `Connection: close`), và tính `Content-Length` theo số byte UTF-8 cho POST. |
| [`HttpResponseParser`](file:///src/main/java/com/lam/httpbrowser/parser/HttpResponseParser.java) | `com.lam.httpbrowser.parser` | Đọc luồng byte từ Socket, bóc tách Status Line, Headers, bỏ qua phản hồi tạm `1xx`, giải mã 3 cơ chế body framing (`Content-Length`, `chunked`, `EOF`), phát hiện bảng mã `charset`. |
| [`HttpResult`](file:///src/main/java/com/lam/httpbrowser/model/HttpResult.java) | `com.lam.httpbrowser.model` | Đối tượng bất biến chứa: StatusCode, StatusMessage, Headers (tra cứu không phân biệt hoa thường), rawHeadersText, bodyBytes, bodyString, charset. |
| [`HtmlAnalyzer`](file:///src/main/java/com/lam/httpbrowser/analyzer/HtmlAnalyzer.java) | `com.lam.httpbrowser.analyzer` | Sử dụng thư viện Jsoup duyệt cây DOM HTML, đếm chính xác số thẻ `<p>`, `<div>`, `<span>`, `<img>` (loại bỏ hoàn toàn các thẻ trong comment hoặc plain text). |
| [`HtmlStatistics`](file:///src/main/java/com/lam/httpbrowser/model/HtmlStatistics.java) | `com.lam.httpbrowser.model` | Lưu trữ kết quả đo đếm HTML: ký tự, bytes UTF-8, số lượng thẻ `<p>`, `<div>`, `<span>`, `<img>`. |
| [`HttpService`](file:///src/main/java/com/lam/httpbrowser/service/HttpService.java) | `com.lam.httpbrowser.service` | Đóng vai trò Facade kết nối chu trình: `UrlParser` → `HttpRequestBuilder` → `SocketConnection` → `HttpResponseParser`. |

---

## 3. Chi tiết hiện thực giao thức mạng & Kỹ thuật cốt lõi

### 3.1. TCP Socket và SSLSocket với SNI (Server Name Indication)
- Với HTTP:
  ```java
  Socket plainSocket = new Socket();
  plainSocket.connect(new InetSocketAddress(host, port), 10000);
  plainSocket.setSoTimeout(15000);
  ```
- Với HTTPS: Để kết nối đến các web server hiện đại đằng sau Cloudflare/Nginx CDN (nhiều domain dùng chung 1 IP), việc cấu hình **SNI** là bắt buộc:
  ```java
  SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
  SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(plainSocket, host, port, true);
  SSLParameters sslParams = sslSocket.getSSLParameters();
  sslParams.setServerNames(Collections.singletonList(new SNIHostName(host)));
  sslSocket.setSSLParameters(sslParams);
  sslSocket.startHandshake();
  ```

### 3.2. Tính toán Content-Length chính xác theo byte UTF-8
Chuẩn HTTP/1.1 yêu cầu header `Content-Length` biểu thị **số byte của message body**, không phải số ký tự. Do Java sử dụng UTF-16 cho `String`, ký tự tiếng Việt (ví dụ `ê`, `â`) chiếm 2 bytes UTF-8:
```java
// ĐÚNG: Tính chính xác số byte UTF-8
byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
int contentLength = bodyBytes.length;

// TUYỆT ĐỐI TRÁNH: body.length() sẽ sai kích thước khi có dấu tiếng Việt!
```

### 3.3. Xử lý 3 cơ chế Body Framing trong HTTP/1.1
1. **Content-Length Framing:**
   Đọc chính xác số byte chỉ định bằng vòng lặp `while (totalRead < targetLen)`.
2. **Transfer-Encoding: chunked Framing:**
   Đọc dòng kích thước chunk bằng định dạng Hex (`<hex-size>\r\n`), bỏ qua extension `;ext=...`, đọc đúng số byte của chunk đó, tiếp tục cho đến chunk kích thước `0`.
3. **Connection: close / EOF Framing:**
   Nếu không có `Content-Length` lẫn `Transfer-Encoding`, đọc luồng tuần tự cho đến khi server ngắt kết nối (`read() == -1`).
4. **Quy tắc đặc thù cho phương thức HEAD:**
   Theo RFC 7230 §3.3.3, phản hồi cho yêu cầu `HEAD` tuyệt đối không có phần thân (body), ngay cả khi server trả về `Content-Length: 9999`. Bộ parser trả về `0` byte body ngay lập tức.

---

## 4. Hướng dẫn cài đặt và Chạy chương trình (Usage Guide)

### 4.1. Yêu cầu môi trường
- **Java Development Kit (JDK):** JDK 21 trở lên (Khuyến nghị **Java 25**).
- **Apache Maven:** 3.9+ (Đã kiểm thử thành công trên Maven 3.10.0).

### 4.2. Cách 1: Chạy trực tiếp qua Maven (Đơn giản nhất)
Mở Terminal hoặc PowerShell tại thư mục dự án:
```powershell
cd "d:\DOCUMENT\LEARNING\SCHOOL\subject\HocKi_5_2627\Network programing\NetworkProgaming-Lab\Lab7\SimpleHTTPBrowser"

# Biên dịch và chạy trực tiếp
mvn compile exec:java
```

### 4.3. Cách 2: Đóng gói thành file JAR độc lập (Standalone Fat JAR)
```powershell
# Đóng gói
mvn clean package

# Chạy file JAR đã bao gồm tất cả dependencies (Jsoup):
java -jar target/SimpleHTTPBrowser-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### 4.4. Cách 3: Chạy toàn bộ bộ kiểm thử tự động (Run Unit Tests)
```powershell
mvn test
```

---

## 5. Hướng dẫn sử dụng giao diện (GUI User Guide)

1. **Gửi GET Request:**
   - Nhập URL: `http://example.com` hoặc `https://example.com`
   - Chọn Method: `GET`
   - Nhấn **SEND** (hoặc gõ **Enter** trong ô URL).
   - Xem Status Code (ví dụ `200 OK`), tab **Response Body**, tab **Response Headers**, và bảng thống kê HTML bên dưới.

2. **Gửi POST Request:**
   - Chọn Method: `POST` (khung **POST Body** sẽ tự động hiển thị).
   - Nhập URL: `https://httpbin.org/post`
   - Nhập dữ liệu POST: `name=Lam&age=20` (hoặc tiếng Việt: `tên=Nguyễn Văn Lâm&lớp=CNTT`)
   - Nhấn **SEND**.
   - Phản hồi từ server sẽ hiển thị trong tab **Response Body**.

3. **Gửi HEAD Request:**
   - Chọn Method: `HEAD`
   - Nhập URL: `http://example.com`
   - Nhấn **SEND**.
   - Chương trình tự động chuyển sang tab **Response Headers** để xem metadata, bảng HTML statistics không phân tích vì không có body.

---

## 6. Báo cáo kết quả kiểm thử (Test Report)

Dự án tích hợp bộ kiểm thử tự động toàn diện với **33 unit & integration test cases**, đạt tỷ lệ vượt qua **100%**:

| Tên lớp kiểm thử | Số test | Trạng thái | Nội dung kiểm thử |
|---|:---:|:---:|---|
| [`BrowserFrameTest`](file:///src/test/java/com/lam/httpbrowser/BrowserFrameTest.java) | 1 | ✅ PASS | Khởi tạo GUI Swing, component bounds, các phương thức cập nhật UI. |
| [`UrlParserTest`](file:///src/test/java/com/lam/httpbrowser/UrlParserTest.java) | 7 | ✅ PASS | URL mặc định 80/443, HTTPS, cổng tùy biến, query string, tự thêm `http://`, chặn scheme lạ (`ftp://`), chặn URL rỗng, bắt lỗi port ngoài dải. |
| [`SocketConnectionTest`](file:///src/test/java/com/lam/httpbrowser/SocketConnectionTest.java) | 1 | ✅ PASS | Khởi tạo ServerSocket nội bộ, mở TCP Socket thuần, gửi PING và nhận phản hồi ECHO qua stream. |
| [`HttpRequestBuilderTest`](file:///src/test/java/com/lam/httpbrowser/HttpRequestBuilderTest.java) | 6 | ✅ PASS | Định dạng GET, HEAD, POST, CRLF `\r\n\r\n`, tính `Content-Length` theo byte UTF-8 cho ký tự tiếng Việt (`tên=Lâm`), xử lý header Host. |
| [`HttpResponseParserTest`](file:///src/test/java/com/lam/httpbrowser/HttpResponseParserTest.java) | 7 | ✅ PASS | Phân tích Status Line, Headers, Content-Length framing, Chunked transfer encoding, chunk extension/trailers, Connection close/EOF, bỏ qua `100 Continue`, xử lý đặc thù cho `HEAD` (0 body bytes). |
| [`HtmlAnalyzerTest`](file:///src/test/java/com/lam/httpbrowser/HtmlAnalyzerTest.java) | 6 | ✅ PASS | Đếm thẻ `<p>`, `<div>`, `<span>`, `<img>`, loại bỏ thẻ trong comment `<!-- ... -->`, loại bỏ ký tự escaped `&lt;p&gt;`, tính byte UTF-8 cho tiếng Việt, xử lý HTML lỗi cấu trúc. |
| [`HttpServiceIntegrationTest`](file:///src/test/java/com/lam/httpbrowser/HttpServiceIntegrationTest.java) | 5 | ✅ PASS | Tích hợp toàn trình với Mock Server thật: GET qua TCP, POST qua TCP, HEAD qua TCP, bắt lỗi Connection Refused, bắt lỗi DNS Resolution Failure. |
| **TỔNG CỘNG** | **33** | **✅ 100% PASS** | **BUILD SUCCESS** |

---

## 7. Tổng kết đồ án (Conclusion)

Dự án **SimpleHTTPBrowser** đáp ứng trọn vẹn và vượt mức tất cả các tiêu chí của đồ án môn học Lập trình mạng:
- Tuân thủ nghiêm ngặt nguyên tắc **Network Programming**: Tự xây dựng giao thức HTTP/1.1 trên nền tảng Socket thuần, không phụ thuộc vào bất kỳ thư viện HTTP Client cấp cao nào.
- Đảm bảo tính ổn định và thẩm mỹ của giao diện người dùng Swing, xử lý đa luồng bất đồng bộ chống treo máy bằng `SwingWorker`.
- Cấu trúc mã nguồn sạch (Clean Code), tách lớp theo chuẩn OOP và có bộ test tự động kiểm thử 100% các chức năng cốt lõi.

