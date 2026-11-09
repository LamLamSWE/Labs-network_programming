package com.lam.httpbrowser;

import com.lam.httpbrowser.network.SocketConnection;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class SocketConnectionTest {

    @Test
    public void testTcpSocketConnectSendReceive() throws Exception {
        // Start a mock local ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            // Background thread to handle the incoming client connection
            Thread serverThread = new Thread(() -> {
                try (Socket client = serverSocket.accept();
                     InputStream in = client.getInputStream();
                     OutputStream out = client.getOutputStream()) {

                    byte[] buffer = new byte[64];
                    int read = in.read(buffer);
                    String received = new String(buffer, 0, read, StandardCharsets.UTF_8);

                    // Echo back with prefix
                    String response = "ECHO:" + received;
                    out.write(response.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } catch (Exception ignored) {}
            });
            serverThread.start();

            // Client connects using SocketConnection
            try (SocketConnection conn = new SocketConnection("127.0.0.1", port, false)) {
                conn.connect();
                assertTrue(conn.isConnected());

                // Send data
                String message = "PING";
                conn.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
                conn.getOutputStream().flush();

                // Read echo response
                byte[] responseBuffer = new byte[64];
                int bytesRead = conn.getInputStream().read(responseBuffer);
                assertTrue(bytesRead > 0);

                String response = new String(responseBuffer, 0, bytesRead, StandardCharsets.UTF_8);
                assertEquals("ECHO:PING", response);
            }

            serverThread.join(2000);
        }
    }
}

