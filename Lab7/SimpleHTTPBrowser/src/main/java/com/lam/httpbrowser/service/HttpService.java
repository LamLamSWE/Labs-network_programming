package com.lam.httpbrowser.service;

import com.lam.httpbrowser.builder.HttpRequestBuilder;
import com.lam.httpbrowser.model.HttpResult;
import com.lam.httpbrowser.network.SocketConnection;
import com.lam.httpbrowser.parser.HttpResponseParser;
import com.lam.httpbrowser.parser.UrlParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Coordinates the full HTTP request and response lifecycle:
 * 1. URL parsing and validation
 * 2. TCP / SSLSocket connection establishment
 * 3. HTTP request construction and transmission
 * 4. HTTP response reception and parsing
 */
public class HttpService {

    private final HttpResponseParser responseParser;

    public HttpService() {
        this.responseParser = new HttpResponseParser();
    }

    /**
     * Executes an HTTP request over a low-level TCP Socket / SSLSocket.
     *
     * @param rawUrl the URL entered by the user
     * @param method the HTTP method ("GET", "POST", "HEAD")
     * @param postBody the body data for POST requests (ignored for GET/HEAD)
     * @return HttpResult containing status, headers, and body
     * @throws IOException on network or protocol failure
     */
    public HttpResult executeRequest(String rawUrl, String method, String postBody) throws IOException {
        // Step 1: Parse and validate URL
        UrlParser urlParser = new UrlParser(rawUrl);

        // Step 2: Build HTTP request bytes
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder(method, urlParser);
        if ("POST".equalsIgnoreCase(method) && postBody != null) {
            requestBuilder.setBody(postBody);
        }
        byte[] requestBytes = requestBuilder.buildBytes();

        // Step 3: Establish TCP Socket (or SSLSocket for HTTPS)
        try (SocketConnection connection = new SocketConnection(
                urlParser.getHost(),
                urlParser.getPort(),
                urlParser.isHttps())) {

            connection.connect();

            // Step 4: Transmit request bytes through Socket OutputStream
            OutputStream out = connection.getOutputStream();
            out.write(requestBytes);
            out.flush();

            // Step 5: Receive and parse response bytes from Socket InputStream
            return responseParser.parse(connection.getInputStream(), method);

        } catch (UnknownHostException e) {
            throw new IOException("DNS resolution failed for host: '" + urlParser.getHost() + "'. Please check the domain name.", e);
        } catch (ConnectException e) {
            throw new IOException("Connection refused by " + urlParser.getHost() + ":" + urlParser.getPort() + ". The server may be down or the port is blocked.", e);
        } catch (SocketTimeoutException e) {
            throw new IOException("Connection or read timed out while communicating with " + urlParser.getHost() + ":" + urlParser.getPort() + ".", e);
        }
    }
}

