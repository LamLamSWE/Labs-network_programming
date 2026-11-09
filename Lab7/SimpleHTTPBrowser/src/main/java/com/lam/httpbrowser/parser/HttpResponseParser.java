package com.lam.httpbrowser.parser;

import com.lam.httpbrowser.model.HttpResult;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manually parses HTTP/1.1 response bytes from an InputStream.
 *
 * Implements:
 * 1. Status line parsing (HTTP/1.1 200 OK)
 * 2. Response header parsing and case-insensitive aggregation
 * 3. Interim 1xx response skipping (e.g., 100 Continue)
 * 4. HEAD request handling (no body extracted regardless of Content-Length)
 * 5. Three response body framing mechanisms:
 *    - Content-Length
 *    - Transfer-Encoding: chunked
 *    - Connection: close / EOF reading
 * 6. Character set extraction from Content-Type (fallback to UTF-8)
 */
public class HttpResponseParser {

    private static final Pattern CHARSET_PATTERN =
            Pattern.compile("charset\\s*=\\s*['\"]?([a-zA-Z0-9_\\-]+)['\"]?", Pattern.CASE_INSENSITIVE);

    /**
     * Parses an HTTP response from the given InputStream.
     *
     * @param in the stream to read response bytes from
     * @param requestMethod the HTTP method used in the request ("GET", "POST", "HEAD")
     * @return HttpResult encapsulating the parsed response
     * @throws IOException if a network or protocol parsing error occurs
     */
    public HttpResult parse(InputStream in, String requestMethod) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("InputStream cannot be null.");
        }
        boolean isHead = "HEAD".equalsIgnoreCase(requestMethod);

        while (true) {
            // 1. Read status line (skipping any leading empty lines)
            String statusLine;
            do {
                statusLine = readLine(in);
                if (statusLine == null) {
                    throw new EOFException("Connection closed by server before receiving status line.");
                }
            } while (statusLine.trim().isEmpty());

            String[] statusParts = statusLine.split(" ", 3);
            if (statusParts.length < 2) {
                throw new IOException("Malformed HTTP status line: " + statusLine);
            }

            String httpVersion = statusParts[0];
            int statusCode;
            try {
                statusCode = Integer.parseInt(statusParts[1]);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid HTTP status code in: " + statusLine, e);
            }
            String statusMessage = statusParts.length >= 3 ? statusParts[2] : "";

            // 2. Read headers until empty line
            Map<String, List<String>> headers = new LinkedHashMap<>();
            StringBuilder rawHeaders = new StringBuilder();
            rawHeaders.append(statusLine).append("\r\n");

            String line;
            while ((line = readLine(in)) != null && !line.isEmpty()) {
                rawHeaders.append(line).append("\r\n");
                int colonIdx = line.indexOf(':');
                if (colonIdx > 0) {
                    String name = line.substring(0, colonIdx).trim();
                    String value = line.substring(colonIdx + 1).trim();
                    headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
                }
            }

            // 3. Handle interim 1xx status codes (e.g. 100 Continue)
            if (statusCode >= 100 && statusCode < 200) {
                // Discard 1xx interim response and read the real/final response
                continue;
            }

            // 4. Determine Charset from Content-Type header
            Charset charset = detectCharset(headers);

            // 5. Read response body according to framing rules
            byte[] bodyBytes;
            if (isHead || statusCode == 204 || statusCode == 304) {
                // HEAD responses and 204/304 have no body (RFC 7230 §3.3.3 point 1)
                bodyBytes = new byte[0];
            } else if (isChunked(headers)) {
                // Chunked transfer encoding
                bodyBytes = readChunkedBody(in);
            } else if (getContentLength(headers) >= 0) {
                // Content-Length framing
                long contentLength = getContentLength(headers);
                bodyBytes = readFixedLengthBody(in, contentLength);
            } else {
                // Read until EOF (Connection: close framing)
                bodyBytes = readUntilEof(in);
            }

            return new HttpResult(httpVersion, statusCode, statusMessage, headers,
                    rawHeaders.toString(), bodyBytes, charset);
        }
    }

    /**
     * Reads a single line (terminated by CRLF or LF) from the InputStream as an ASCII/ISO-8859-1 string.
     */
    private String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        int prev = -1;
        int curr;

        while ((curr = in.read()) != -1) {
            if (curr == '\n') {
                // Strip preceding '\r' if present
                byte[] bytes = lineBuffer.toByteArray();
                int len = bytes.length;
                if (len > 0 && bytes[len - 1] == '\r') {
                    return new String(bytes, 0, len - 1, StandardCharsets.ISO_8859_1);
                }
                return new String(bytes, 0, len, StandardCharsets.ISO_8859_1);
            }
            lineBuffer.write(curr);
            prev = curr;
        }

        if (lineBuffer.size() == 0 && prev == -1) {
            return null; // EOF reached with no content
        }
        return new String(lineBuffer.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    /**
     * Reads a chunked response body.
     */
    private byte[] readChunkedBody(InputStream in) throws IOException {
        ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();

        while (true) {
            String sizeLine = readLine(in);
            if (sizeLine == null) {
                throw new EOFException("Premature EOF while reading chunk size.");
            }
            sizeLine = sizeLine.trim();
            if (sizeLine.isEmpty()) {
                continue;
            }

            // Chunk extensions can follow a semicolon, e.g. "1a;ext=val" -> split by ';'
            int semicolonIdx = sizeLine.indexOf(';');
            if (semicolonIdx != -1) {
                sizeLine = sizeLine.substring(0, semicolonIdx).trim();
            }

            int chunkSize;
            try {
                chunkSize = Integer.parseInt(sizeLine, 16);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid chunk size hex: '" + sizeLine + "'", e);
            }

            if (chunkSize == 0) {
                // Final chunk (0-size). Read any trailing headers until empty line
                String trailer;
                while ((trailer = readLine(in)) != null && !trailer.trim().isEmpty()) {
                    // discard trailer headers
                }
                break;
            }

            // Read exactly chunkSize bytes
            byte[] chunkData = new byte[chunkSize];
            int totalRead = 0;
            while (totalRead < chunkSize) {
                int read = in.read(chunkData, totalRead, chunkSize - totalRead);
                if (read == -1) {
                    throw new EOFException("Premature EOF inside chunk data. Expected " + chunkSize +
                            " bytes, got " + totalRead);
                }
                totalRead += read;
            }
            bodyBuffer.write(chunkData);

            // Read the CRLF following chunk data
            String trailingCrlf = readLine(in);
            if (trailingCrlf != null && !trailingCrlf.isEmpty()) {
                // Not empty, but consumed trailing line
            }
        }

        return bodyBuffer.toByteArray();
    }

    /**
     * Reads a fixed-length body specified by Content-Length.
     */
    private byte[] readFixedLengthBody(InputStream in, long contentLength) throws IOException {
        if (contentLength <= 0) {
            return new byte[0];
        }
        if (contentLength > Integer.MAX_VALUE - 8) {
            throw new IOException("Content-Length exceeds maximum supported buffer size: " + contentLength);
        }

        int targetLen = (int) contentLength;
        byte[] body = new byte[targetLen];
        int totalRead = 0;

        while (totalRead < targetLen) {
            int read = in.read(body, totalRead, targetLen - totalRead);
            if (read == -1) {
                // Server closed early: return what was received rather than failing completely
                byte[] partial = new byte[totalRead];
                System.arraycopy(body, 0, partial, 0, totalRead);
                return partial;
            }
            totalRead += read;
        }

        return body;
    }

    /**
     * Reads all bytes until EOF (server closes socket connection).
     */
    private byte[] readUntilEof(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[4096];
        int read;
        while ((read = in.read(temp)) != -1) {
            buffer.write(temp, 0, read);
        }
        return buffer.toByteArray();
    }

    /**
     * Checks if the Transfer-Encoding header contains "chunked".
     */
    private boolean isChunked(Map<String, List<String>> headers) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if ("Transfer-Encoding".equalsIgnoreCase(entry.getKey())) {
                for (String val : entry.getValue()) {
                    if (val.toLowerCase(Locale.ROOT).contains("chunked")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Extracts Content-Length from headers, or returns -1 if absent or invalid.
     */
    private long getContentLength(Map<String, List<String>> headers) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if ("Content-Length".equalsIgnoreCase(entry.getKey())) {
                for (String val : entry.getValue()) {
                    try {
                        return Long.parseLong(val.trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return -1;
    }

    /**
     * Detects charset from Content-Type header. Defaults to UTF-8.
     */
    private Charset detectCharset(Map<String, List<String>> headers) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if ("Content-Type".equalsIgnoreCase(entry.getKey())) {
                for (String val : entry.getValue()) {
                    Matcher matcher = CHARSET_PATTERN.matcher(val);
                    if (matcher.find()) {
                        String charsetName = matcher.group(1).trim();
                        try {
                            return Charset.forName(charsetName);
                        } catch (Exception ignored) {
                            // If invalid/unsupported, fall back to UTF-8
                        }
                    }
                }
            }
        }
        return StandardCharsets.UTF_8;
    }
}

