package com.lam.httpbrowser;

import com.lam.httpbrowser.model.HttpResult;
import com.lam.httpbrowser.parser.HttpResponseParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class HttpResponseParserTest {

    private final HttpResponseParser parser = new HttpResponseParser();

    @Test
    public void testContentLengthFraming() throws Exception {
        String raw = "HTTP/1.1 200 OK\r\n" +
                "Server: Apache\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: 15\r\n" +
                "\r\n" +
                "<h1>Hello!</h1>";

        InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));
        HttpResult result = parser.parse(in, "GET");

        assertEquals("HTTP/1.1", result.getHttpVersion());
        assertEquals(200, result.getStatusCode());
        assertEquals("OK", result.getStatusMessage());
        assertEquals("Apache", result.getFirstHeader("Server"));
        assertEquals("<h1>Hello!</h1>", result.getBody());
        assertEquals(15, result.getBodyBytes().length);
        assertTrue(result.isHtml());
    }

    @Test
    public void testChunkedTransferEncoding() throws Exception {
        String raw = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                "\r\n" +
                "7\r\n" +
                "Mozilla\r\n" +
                "9\r\n" +
                "Developer\r\n" +
                "7\r\n" +
                "Network\r\n" +
                "0\r\n" +
                "\r\n";

        InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));
        HttpResult result = parser.parse(in, "GET");

        assertEquals(200, result.getStatusCode());
        assertEquals("MozillaDeveloperNetwork", result.getBody());
        assertFalse(result.isHtml());
    }

    @Test
    public void testChunkedWithExtensionsAndTrailers() throws Exception {
        String raw = "HTTP/1.1 200 OK\r\n" +
                "Transfer-Encoding: chunked\r\n" +
                "\r\n" +
                "4;ext=test\r\n" +
                "Wiki\r\n" +
                "5\r\n" +
                "pedia\r\n" +
                "0\r\n" +
                "Expires: Wed, 21 Oct 2026 07:28:00 GMT\r\n" +
                "\r\n";

        InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));
        HttpResult result = parser.parse(in, "POST");

        assertEquals(200, result.getStatusCode());
        assertEquals("Wikipedia", result.getBody());
    }

    @Test
    public void testConnectionCloseEofFraming() throws Exception {
        String raw = "HTTP/1.1 200 OK\r\n" +
                "Connection: close\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                "Data until socket closes";

        InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));
        HttpResult result = parser.parse(in, "GET");

        assertEquals(200, result.getStatusCode());
        assertEquals("Data until socket closes", result.getBody());
    }

    @Test
    public void testHeadResponseHasNoBodyRegardlessOfContentLength() throws Exception {
        String raw = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: 9999\r\n" +
                "\r\n";

        InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));
        HttpResult result = parser.parse(in, "HEAD");

        assertEquals(200, result.getStatusCode());
        assertEquals("9999", result.getFirstHeader("Content-Length"));
        assertEquals(0, result.getBodyBytes().length);
        assertEquals("", result.getBody());
    }

    @Test
    public void testInterim100ContinueSkipping() throws Exception {
        String raw = "HTTP/1.1 100 Continue\r\n" +
                "\r\n" +
                "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 4\r\n" +
                "\r\n" +
                "Done";

        InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));
        HttpResult result = parser.parse(in, "POST");

        assertEquals(200, result.getStatusCode());
        assertEquals("Done", result.getBody());
    }

    @Test
    public void testHttpErrorStatusCodes() throws Exception {
        String raw = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: 9\r\n" +
                "\r\n" +
                "Not Found";

        InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));
        HttpResult result = parser.parse(in, "GET");

        assertEquals(404, result.getStatusCode());
        assertEquals("Not Found", result.getStatusMessage());
        assertEquals("Not Found", result.getBody());
    }
}

