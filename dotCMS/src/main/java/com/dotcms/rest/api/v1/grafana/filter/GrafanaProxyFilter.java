package com.dotcms.rest.api.v1.grafana.filter;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebFilter("/grafana-proxy/*")
public class GrafanaProxyFilter implements Filter {

    private static final String GRAFANA_URL = "grafana.url";
    private static final String GRAFANA_URL_VALUE = "http://localhost:3000";
    private HttpClient httpClient;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static String getGrafanaUrlValue() {
        return Config.getStringProperty(GRAFANA_URL, GRAFANA_URL_VALUE);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        try {
            // Build target URL
            String path = request.getRequestURI().replace("/grafana-proxy", "");
            String queryString = request.getQueryString();
            String targetUrl = getGrafanaUrlValue() + path;

            if (queryString != null && !queryString.isEmpty()) {
                targetUrl += "?" + queryString;
            }

            // Build HTTP request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(30));

            // Copy headers
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);

                if (!shouldSkipRequestHeader(headerName)) {
                    requestBuilder.header(headerName, headerValue);
                }
            }

            // Handle different HTTP methods
            String method = request.getMethod().toUpperCase();
            switch (method) {
                case "GET":
                    requestBuilder.GET();
                    break;

                case "POST":
                case "PUT":
                    byte[] body = readRequestBody(request);
                    HttpRequest.BodyPublisher publisher = body.length > 0
                            ? HttpRequest.BodyPublishers.ofByteArray(body)
                            : HttpRequest.BodyPublishers.noBody();

                    if ("POST".equals(method)) {
                        requestBuilder.POST(publisher);
                    } else {
                        requestBuilder.PUT(publisher);
                    }
                    break;

                case "DELETE":
                    requestBuilder.DELETE();
                    break;

                default:
                    requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            // Execute request
            HttpResponse<byte[]> httpResponse = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            // Copy response status
            response.setStatus(httpResponse.statusCode());

            // Copy response headers
            Map<String, List<String>> responseHeaders = httpResponse.headers().map();
            for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
                String headerName = entry.getKey();

                if (headerName != null && !shouldSkipResponseHeader(headerName)) {
                    for (String headerValue : entry.getValue()) {
                        response.addHeader(headerName, headerValue);
                    }
                }
            }

            // Modify headers for iframe support
            response.setHeader("X-Frame-Options", "ALLOWALL");

            // Copy response body
            byte[] responseBody = httpResponse.body();
            if (responseBody != null && responseBody.length > 0) {
                response.getOutputStream().write(responseBody);
            }

        } catch (Exception e) {
            Logger.error(this, "Error proxying request to Grafana: " + e.getMessage(), e);
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Proxy error: " + e.getMessage()
            );
        }
    }

    private byte[] readRequestBody(HttpServletRequest request) throws IOException {
        try (InputStream is = request.getInputStream()) {
            return is.readAllBytes(); // Java 9+
        }
    }

    private static final Set<String> SKIP_REQUEST_HEADERS = Set.of(
        "host",
        "content-length",
        "transfer-encoding",
        "connection",
        "upgrade",
        "x-frame-options"
    );

    private boolean shouldSkipRequestHeader(String headerName) {
        return SKIP_REQUEST_HEADERS.contains(headerName.toLowerCase());
    }

    private boolean shouldSkipResponseHeader(String headerName) {
        String lower = headerName.toLowerCase();
        return lower.equals("transfer-encoding") ||
                lower.equals("connection") ||
                lower.equals("x-frame-options") ||     // Remove Grafana's X-Frame-Options
                lower.equals("content-security-policy"); // Remove CSP that might block iframe
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}