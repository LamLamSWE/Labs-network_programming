package com.lam.httpbrowser.model;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Encapsulates the parsed HTTP response:
 * - Status code (e.g. 200, 404)
 * - Status message / reason phrase (e.g. "OK", "Not Found")
 * - Response headers (with case-insensitive lookup)
 * - Raw response headers text (as received from server)
 * - Response body (both raw bytes and decoded String)
 */
public class HttpResult {

    private final String httpVersion;
    private final int statusCode;
    private final String statusMessage;
    private final Map<String, List<String>> headers;
    private final String rawHeadersText;
    private final byte[] bodyBytes;
    private final String bodyString;
    private final Charset charset;

    public HttpResult(String httpVersion,
                      int statusCode,
                      String statusMessage,
                      Map<String, List<String>> headers,
                      String rawHeadersText,
                      byte[] bodyBytes,
                      Charset charset) {
        this.httpVersion = httpVersion != null ? httpVersion : "HTTP/1.1";
        this.statusCode = statusCode;
        this.statusMessage = statusMessage != null ? statusMessage : "";
        this.rawHeadersText = rawHeadersText != null ? rawHeadersText : "";
        this.bodyBytes = bodyBytes != null ? bodyBytes : new byte[0];
        this.charset = charset != null ? charset : StandardCharsets.UTF_8;
        this.bodyString = new String(this.bodyBytes, this.charset);

        // Store headers in a case-insensitive map
        Map<String, List<String>> caseInsensitiveMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (headers != null) {
            headers.forEach((k, v) -> caseInsensitiveMap.put(k, new ArrayList<>(v)));
        }
        this.headers = Collections.unmodifiableMap(caseInsensitiveMap);
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Case-insensitive header lookup. Returns the first header value, or null if absent.
     */
    public String getFirstHeader(String headerName) {
        List<String> values = headers.get(headerName);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        return null;
    }

    public String getRawHeadersText() {
        return rawHeadersText;
    }

    public byte[] getBodyBytes() {
        return bodyBytes;
    }

    public String getBody() {
        return bodyString;
    }

    public Charset getCharset() {
        return charset;
    }

    /**
     * Checks if the Content-Type header declares this response as HTML.
     */
    public boolean isHtml() {
        String contentType = getFirstHeader("Content-Type");
        if (contentType == null) {
            return false;
        }
        return contentType.toLowerCase(Locale.ROOT).contains("text/html");
    }

    @Override
    public String toString() {
        return httpVersion + " " + statusCode + " " + statusMessage +
                " (" + bodyBytes.length + " body bytes)";
    }
}

