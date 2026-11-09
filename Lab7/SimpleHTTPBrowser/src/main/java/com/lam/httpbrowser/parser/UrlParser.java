package com.lam.httpbrowser.parser;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Parses and validates HTTP/HTTPS URLs.
 * Extracts scheme, hostname, port, path, and query parameters.
 */
public class UrlParser {

    private final String rawUrl;
    private final String scheme;
    private final String host;
    private final int port;
    private final String path;
    private final String query;

    /**
     * Constructs a parsed URL from a raw input string.
     *
     * @param input the URL string entered by the user
     * @throws IllegalArgumentException if the URL is invalid or uses an unsupported scheme
     */
    public UrlParser(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty.");
        }

        this.rawUrl = input.trim();
        String normalized = rawUrl;

        // Auto-prepend http:// if no scheme is provided (i.e. no "://")
        if (!normalized.contains("://")) {
            normalized = "http://" + normalized;
        }

        try {
            URI uri = new URI(normalized);

            String parsedScheme = uri.getScheme();
            if (parsedScheme == null) {
                throw new IllegalArgumentException("Missing URL scheme.");
            }
            this.scheme = parsedScheme.toLowerCase();

            if (!"http".equals(this.scheme) && !"https".equals(this.scheme)) {
                throw new IllegalArgumentException("Unsupported scheme: " + this.scheme + ". Only HTTP and HTTPS are supported.");
            }

            String parsedHost = uri.getHost();
            if (parsedHost == null || parsedHost.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid or missing hostname in URL: " + input);
            }
            this.host = parsedHost;

            // Port handling: use explicit port if provided, otherwise default to 80 (HTTP) or 443 (HTTPS)
            int parsedPort = uri.getPort();
            if (parsedPort != -1) {
                if (parsedPort <= 0 || parsedPort > 65535) {
                    throw new IllegalArgumentException("Port number out of range (1-65535): " + parsedPort);
                }
                this.port = parsedPort;
            } else {
                this.port = "https".equals(this.scheme) ? 443 : 80;
            }

            // Path handling: defaults to "/" if empty
            String parsedPath = uri.getRawPath();
            if (parsedPath == null || parsedPath.isEmpty()) {
                this.path = "/";
            } else {
                this.path = parsedPath;
            }

            // Query string handling
            this.query = uri.getRawQuery();

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URL syntax: " + e.getMessage(), e);
        }
    }

    public String getRawUrl() {
        return rawUrl;
    }

    public String getScheme() {
        return scheme;
    }

    public boolean isHttps() {
        return "https".equalsIgnoreCase(scheme);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query;
    }

    /**
     * Returns the full request target for the HTTP request line (e.g. "/index.html?id=1").
     */
    public String getRequestTarget() {
        if (query != null && !query.isEmpty()) {
            return path + "?" + query;
        }
        return path;
    }

    @Override
    public String toString() {
        return scheme + "://" + host + (isDefaultPort() ? "" : ":" + port) + getRequestTarget();
    }

    private boolean isDefaultPort() {
        return ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
    }
}

