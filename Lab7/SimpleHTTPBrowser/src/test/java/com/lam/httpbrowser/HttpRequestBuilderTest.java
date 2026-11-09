package com.lam.httpbrowser;

import com.lam.httpbrowser.builder.HttpRequestBuilder;
import com.lam.httpbrowser.parser.UrlParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class HttpRequestBuilderTest {

    @Test
    public void testGetRequestFormatting() throws Exception {
        UrlParser url = new UrlParser("http://example.com/");
        HttpRequestBuilder builder = new HttpRequestBuilder("GET", url);

        String request = builder.buildString();
        assertTrue(request.startsWith("GET / HTTP/1.1\r\n"));
        assertTrue(request.contains("Host: example.com\r\n"));
        assertTrue(request.contains("User-Agent: " + HttpRequestBuilder.DEFAULT_USER_AGENT + "\r\n"));
        assertTrue(request.contains("Connection: close\r\n"));
        assertTrue(request.endsWith("\r\n\r\n"));
    }

    @Test
    public void testHeadRequestFormatting() throws Exception {
        UrlParser url = new UrlParser("http://example.com/index.html");
        HttpRequestBuilder builder = new HttpRequestBuilder("HEAD", url);

        String request = builder.buildString();
        assertTrue(request.startsWith("HEAD /index.html HTTP/1.1\r\n"));
        assertTrue(request.contains("Host: example.com\r\n"));
        assertTrue(request.contains("Connection: close\r\n"));
        assertTrue(request.endsWith("\r\n\r\n"));
        assertFalse(request.contains("Content-Length"));
    }

    @Test
    public void testPostRequestStandardBody() throws Exception {
        UrlParser url = new UrlParser("https://httpbin.org/post");
        String payload = "name=Lam&age=20";
        HttpRequestBuilder builder = new HttpRequestBuilder("POST", url).setBody(payload);

        String request = builder.buildString();
        assertTrue(request.startsWith("POST /post HTTP/1.1\r\n"));
        assertTrue(request.contains("Host: httpbin.org\r\n"));
        assertTrue(request.contains("Content-Type: application/x-www-form-urlencoded\r\n"));
        assertTrue(request.contains("Content-Length: " + payload.getBytes(StandardCharsets.UTF_8).length + "\r\n"));
        assertTrue(request.contains("Connection: close\r\n"));
        assertTrue(request.endsWith("\r\n\r\n" + payload));
    }

    @Test
    public void testPostContentLengthWithMultibyteUtf8Characters() throws Exception {
        UrlParser url = new UrlParser("http://example.com/form");
        // "tên=Lâm" has 7 characters, but 9 UTF-8 bytes ('ê' is 2 bytes, 'â' is 2 bytes)
        String vietnamesePayload = "tên=Lâm";
        byte[] utf8Bytes = vietnamesePayload.getBytes(StandardCharsets.UTF_8);

        assertEquals(7, vietnamesePayload.length());
        assertEquals(9, utf8Bytes.length);

        HttpRequestBuilder builder = new HttpRequestBuilder("POST", url).setBody(vietnamesePayload);
        String request = builder.buildString();

        // Must calculate Content-Length using UTF-8 byte array length, NOT string character length!
        assertTrue(request.contains("Content-Length: 9\r\n"));
        assertFalse(request.contains("Content-Length: 7\r\n"));
    }

    @Test
    public void testHostHeaderWithCustomPort() throws Exception {
        UrlParser url = new UrlParser("http://localhost:8080/api/users");
        HttpRequestBuilder builder = new HttpRequestBuilder("GET", url);

        String request = builder.buildString();
        assertTrue(request.contains("Host: localhost:8080\r\n"));
    }

    @Test
    public void testHostHeaderWithDefaultPortsOmitsPort() throws Exception {
        UrlParser httpUrl = new UrlParser("http://example.com:80/home");
        HttpRequestBuilder httpBuilder = new HttpRequestBuilder("GET", httpUrl);
        assertTrue(httpBuilder.buildString().contains("Host: example.com\r\n"));

        UrlParser httpsUrl = new UrlParser("https://example.com:443/home");
        HttpRequestBuilder httpsBuilder = new HttpRequestBuilder("GET", httpsUrl);
        assertTrue(httpsBuilder.buildString().contains("Host: example.com\r\n"));
    }
}

