package com.lam.httpbrowser.network;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;

/**
 * Manages low-level TCP socket connections.
 * Supports standard TCP Socket for HTTP and SSLSocket for HTTPS.
 * Implements AutoCloseable for safe resource management.
 */
public class SocketConnection implements AutoCloseable {

    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000; // 10 seconds
    private static final int DEFAULT_READ_TIMEOUT_MS = 15_000;    // 15 seconds

    private final String host;
    private final int port;
    private final boolean isHttps;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    private Socket socket;
    private BufferedInputStream inputStream;
    private OutputStream outputStream;

    public SocketConnection(String host, int port, boolean isHttps) {
        this(host, port, isHttps, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
    }

    public SocketConnection(String host, int port, boolean isHttps, int connectTimeoutMs, int readTimeoutMs) {
        this.host = host;
        this.port = port;
        this.isHttps = isHttps;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * Connects to the remote server over TCP.
     * Uses plain Socket for HTTP, and wraps with SSLSocket for HTTPS.
     *
     * @throws IOException if a network error occurs during connection or TLS handshake
     */
    public void connect() throws IOException {
        // Step 1: Establish raw TCP connection with timeout
        Socket plainSocket = new Socket();
        plainSocket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
        plainSocket.setSoTimeout(readTimeoutMs);

        if (isHttps) {
            // Step 2: For HTTPS, wrap TCP socket in SSLSocket
            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(plainSocket, host, port, true);

            // Configure Server Name Indication (SNI) for virtual hosting support
            SSLParameters sslParams = sslSocket.getSSLParameters();
            try {
                sslParams.setServerNames(Collections.singletonList(new SNIHostName(host)));
                sslSocket.setSSLParameters(sslParams);
            } catch (IllegalArgumentException ignored) {
                // Ignore if host is a raw IP address where SNI is not applicable
            }

            // Perform TLS handshake
            sslSocket.startHandshake();
            this.socket = sslSocket;
        } else {
            this.socket = plainSocket;
        }

        // Initialize streams
        this.inputStream = new BufferedInputStream(socket.getInputStream());
        this.outputStream = socket.getOutputStream();
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ignored) {}
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ignored) {}
        }
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}

