package com.lam.httpbrowser.builder;

import com.lam.httpbrowser.parser.UrlParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Manually constructs HTTP/1.1 requests (GET, POST, HEAD) formatted according to RFC standards.
 * Ensures proper CRLF line separators and accurate UTF-8 byte length calculation for Content-Length.
 */
public class HttpRequestBuilder {

    public static final String CRLF = "\r\n";
    public static final String DEFAULT_USER_AGENT = "SimpleHTTPBrowser/1.0";
    public static final String DEFAULT_ACCEPT = "text/html, application/xhtml+xml, application/xml;q=0.9, */*;q=0.8";
    public static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final String method;
    private final UrlParser urlParser;
    private String body;
    private String contentType = DEFAULT_CONTENT_TYPE;
    private String userAgent = DEFAULT_USER_AGENT;
    private String accept = DEFAULT_ACCEPT;

    public HttpRequestBuilder(String method, UrlParser urlParser) {
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP method cannot be null or empty.");
        }
        if (urlParser == null) {
            throw new IllegalArgumentException("UrlParser cannot be null.");
        }
        this.method = method.trim().toUpperCase();
        this.urlParser = urlParser;
    }

    public HttpRequestBuilder setBody(String body) {
        this.body = body;
        return this;
    }

    public HttpRequestBuilder setContentType(String contentType) {
        if (contentType != null && !contentType.trim().isEmpty()) {
            this.contentType = contentType.trim();
        }
        return this;
    }

    public HttpRequestBuilder setUserAgent(String userAgent) {
        if (userAgent != null && !userAgent.trim().isEmpty()) {
            this.userAgent = userAgent.trim();
        }
        return this;
    }

    public HttpRequestBuilder setAccept(String accept) {
        if (accept != null && !accept.trim().isEmpty()) {
            this.accept = accept.trim();
        }
        return this;
    }

    /**
     * Constructs the Host header value.
     * Includes the port only if it is non-default (not 80 for HTTP, not 443 for HTTPS).
     */
    private String getHostHeaderValue() {
        int port = urlParser.getPort();
        boolean isHttps = urlParser.isHttps();
        boolean isDefault = (!isHttps && port == 80) || (isHttps && port == 443);
        return isDefault ? urlParser.getHost() : urlParser.getHost() + ":" + port;
    }

    /**
     * Builds the complete HTTP/1.1 request as a byte array ready for transmission over a TCP socket.
     */
    public byte[] buildBytes() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        // 1. Request Line: <METHOD> <Request-Target> HTTP/1.1\r\n
        String requestLine = method + " " + urlParser.getRequestTarget() + " HTTP/1.1" + CRLF;
        buffer.write(requestLine.getBytes(StandardCharsets.US_ASCII));

        // 2. Request Headers
        StringBuilder headers = new StringBuilder();
        headers.append("Host: ").append(getHostHeaderValue()).append(CRLF);
        headers.append("User-Agent: ").append(userAgent).append(CRLF);
        headers.append("Accept: ").append(accept).append(CRLF);
        headers.append("Connection: close").append(CRLF); // Ensure connection closes after response

        // 3. Body and Content-Length handling for POST
        byte[] bodyBytes = null;
        if ("POST".equals(method)) {
            headers.append("Content-Type: ").append(contentType).append(CRLF);
            if (body != null && !body.isEmpty()) {
                // Calculate Content-Length using UTF-8 byte array length (NOT String.length())
                bodyBytes = body.getBytes(StandardCharsets.UTF_8);
                headers.append("Content-Length: ").append(bodyBytes.length).append(CRLF);
            } else {
                headers.append("Content-Length: 0").append(CRLF);
            }
        }

        // 4. Empty line separating headers and body: \r\n
        headers.append(CRLF);
        buffer.write(headers.toString().getBytes(StandardCharsets.US_ASCII));

        // 5. Append request body if present
        if (bodyBytes != null && bodyBytes.length > 0) {
            buffer.write(bodyBytes);
        }

        return buffer.toByteArray();
    }

    /**
     * Returns the request represented as a String (useful for debugging/logging).
     */
    public String buildString() throws IOException {
        return new String(buildBytes(), StandardCharsets.UTF_8);
    }
}

