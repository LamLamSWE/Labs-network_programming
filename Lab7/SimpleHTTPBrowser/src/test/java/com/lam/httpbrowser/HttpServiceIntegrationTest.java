package com.lam.httpbrowser;

import com.lam.httpbrowser.model.HttpResult;
import com.lam.httpbrowser.service.HttpService;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class HttpServiceIntegrationTest {

    private final HttpService httpService = new HttpService();

    @Test
    public void testGetOverLocalTcpSocket() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();

            Thread serverThread = new Thread(() -> {
                try (Socket client = server.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII));
                     OutputStream out = client.getOutputStream()) {

                    // Read request line and headers
                    String requestLine = reader.readLine();
                    assertTrue(requestLine.startsWith("GET /home HTTP/1.1"));

                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        // consume headers
                    }

                    // Respond
                    String responseBody = "<html><body><p>Hello from local server</p></body></html>";
                    byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
                    String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/html; charset=UTF-8\r\n" +
                            "Content-Length: " + bodyBytes.length + "\r\n" +
                            "Connection: close\r\n\r\n";

                    out.write(responseHeaders.getBytes(StandardCharsets.US_ASCII));
                    out.write(bodyBytes);
                    out.flush();
                } catch (Exception ignored) {}
            });
            serverThread.start();

            HttpResult result = httpService.executeRequest("http://127.0.0.1:" + port + "/home", "GET", null);

            assertEquals(200, result.getStatusCode());
            assertEquals("OK", result.getStatusMessage());
            assertTrue(result.isHtml());
            assertTrue(result.getBody().contains("Hello from local server"));

            serverThread.join(2000);
        }
    }

    @Test
    public void testPostOverLocalTcpSocket() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();

            Thread serverThread = new Thread(() -> {
                try (Socket client = server.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                     OutputStream out = client.getOutputStream()) {

                    String requestLine = reader.readLine();
                    assertTrue(requestLine.startsWith("POST /submit HTTP/1.1"));

                    int contentLength = 0;
                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        if (line.toLowerCase().startsWith("content-length:")) {
                            contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                        }
                    }

                    char[] bodyChars = new char[contentLength];
                    int totalRead = 0;
                    while (totalRead < contentLength) {
                        int r = reader.read(bodyChars, totalRead, contentLength - totalRead);
                        if (r == -1) break;
                        totalRead += r;
                    }
                    String receivedBody = new String(bodyChars);
                    assertEquals("name=Lam&age=20", receivedBody);

                    String response = "HTTP/1.1 201 Created\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n\r\n";
                    out.write(response.getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                } catch (Exception ignored) {}
            });
            serverThread.start();

            HttpResult result = httpService.executeRequest("http://127.0.0.1:" + port + "/submit", "POST", "name=Lam&age=20");

            assertEquals(201, result.getStatusCode());
            assertEquals("Created", result.getStatusMessage());

            serverThread.join(2000);
        }
    }

    @Test
    public void testHeadOverLocalTcpSocket() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();

            Thread serverThread = new Thread(() -> {
                try (Socket client = server.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII));
                     OutputStream out = client.getOutputStream()) {

                    String requestLine = reader.readLine();
                    assertTrue(requestLine.startsWith("HEAD /file.pdf HTTP/1.1"));

                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {}

                    String response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/pdf\r\n" +
                            "Content-Length: 1048576\r\n" +
                            "Connection: close\r\n\r\n";
                    out.write(response.getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                } catch (Exception ignored) {}
            });
            serverThread.start();

            HttpResult result = httpService.executeRequest("http://127.0.0.1:" + port + "/file.pdf", "HEAD", null);

            assertEquals(200, result.getStatusCode());
            assertEquals("1048576", result.getFirstHeader("Content-Length"));
            assertEquals(0, result.getBodyBytes().length);
            assertEquals("", result.getBody());

            serverThread.join(2000);
        }
    }

    @Test
    public void testConnectionRefusedErrorHandling() {
        // Try connecting to a port where no server is listening
        Exception exception = assertThrows(Exception.class, () ->
                httpService.executeRequest("http://127.0.0.1:54321/test", "GET", null)
        );
        assertTrue(exception.getMessage().contains("Connection refused") ||
                   exception.getMessage().contains("refused"));
    }

    @Test
    public void testDnsResolutionFailureHandling() {
        Exception exception = assertThrows(Exception.class, () ->
                httpService.executeRequest("http://domain-that-definitely-does-not-exist-9988776655.xyz/", "GET", null)
        );
        assertTrue(exception.getMessage().contains("DNS resolution failed") ||
                   exception.getMessage().contains("check the domain"));
    }
}

