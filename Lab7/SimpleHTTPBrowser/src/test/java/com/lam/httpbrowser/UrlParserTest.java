package com.lam.httpbrowser;

import com.lam.httpbrowser.parser.UrlParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UrlParserTest {

    @Test
    public void testHttpUrlWithDefaultPort() {
        UrlParser parser = new UrlParser("http://example.com");
        assertEquals("http", parser.getScheme());
        assertFalse(parser.isHttps());
        assertEquals("example.com", parser.getHost());
        assertEquals(80, parser.getPort());
        assertEquals("/", parser.getPath());
        assertNull(parser.getQuery());
        assertEquals("/", parser.getRequestTarget());
    }

    @Test
    public void testHttpsUrlWithDefaultPort() {
        UrlParser parser = new UrlParser("https://example.com");
        assertEquals("https", parser.getScheme());
        assertTrue(parser.isHttps());
        assertEquals("example.com", parser.getHost());
        assertEquals(443, parser.getPort());
        assertEquals("/", parser.getPath());
        assertEquals("/", parser.getRequestTarget());
    }

    @Test
    public void testCustomPortAndPathAndQuery() {
        UrlParser parser = new UrlParser("http://localhost:8080/search?q=java&page=1");
        assertEquals("http", parser.getScheme());
        assertEquals("localhost", parser.getHost());
        assertEquals(8080, parser.getPort());
        assertEquals("/search", parser.getPath());
        assertEquals("q=java&page=1", parser.getQuery());
        assertEquals("/search?q=java&page=1", parser.getRequestTarget());
    }

    @Test
    public void testUrlWithoutSchemeDefaultsToHttp() {
        UrlParser parser = new UrlParser("example.org/path");
        assertEquals("http", parser.getScheme());
        assertEquals("example.org", parser.getHost());
        assertEquals(80, parser.getPort());
        assertEquals("/path", parser.getPath());
    }

    @Test
    public void testInvalidSchemeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new UrlParser("ftp://ftp.example.com"));
    }

    @Test
    public void testEmptyUrlThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new UrlParser("   "));
        assertThrows(IllegalArgumentException.class, () -> new UrlParser(null));
    }

    @Test
    public void testInvalidPortThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new UrlParser("http://example.com:99999"));
    }
}

