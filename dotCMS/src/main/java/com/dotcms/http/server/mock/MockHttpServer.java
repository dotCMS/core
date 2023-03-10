package com.dotcms.http.server.mock;

import com.dotcms.http.server.mock.MockHttpServerContext.Condition;
import com.dotcms.http.server.mock.MockHttpServerContext.RequestContext;
import com.dotcms.repackage.twitter4j.internal.http.HttpResponseCode;
import com.dotmarketing.util.UtilMethods;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import graphql.AssertException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mock for a HttpServer
 */
public class MockHttpServer {

    private String ip;
    private int port;

    private List<MockHttpServerContext> mockHttpServerContexts = new ArrayList<>();
    private  HttpServer httpServer;
    private List<String> errors = new ArrayList<>();
    private List<URI> uris = new ArrayList<>();

    public MockHttpServer(String ip, int port) {
        this.ip =ip;
        this.port = port;
    }

    /**
     * Add a {@link MockHttpServerContext} to the MockHttpServer
     * @param mockHttpServerContext
     */
    public void addContext(final MockHttpServerContext mockHttpServerContext) {
        this.mockHttpServerContexts.add(mockHttpServerContext);
    }

    /**
     * Start the Mock Http Server
     */
    public void start() {

        try {
            httpServer = HttpServer.create(new InetSocketAddress(ip, port), 0);

            for (MockHttpServerContext mockHttpServerContext : mockHttpServerContexts) {
                httpServer.createContext(mockHttpServerContext.getUri(), exchange -> {
                    this.uris.add(exchange.getRequestURI());

                    if (!validate(mockHttpServerContext, exchange)){
                        exchange.sendResponseHeaders(HttpResponseCode.INTERNAL_SERVER_ERROR, 0);
                    } else {

                        exchange.sendResponseHeaders(mockHttpServerContext.getStatus(),
                                mockHttpServerContext.getBody().length());

                        if (UtilMethods.isSet(mockHttpServerContext.getBody())) {
                            writeBody(mockHttpServerContext, exchange);
                        }
                    }

                    exchange.close();
                });
            }

            httpServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stop the Mock Http Server
     */
    public void stop(){
        httpServer.stop(0);
    }
    private static void writeBody(final MockHttpServerContext mockHttpServerContext,
            final HttpExchange exchange) throws IOException {
        final OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(mockHttpServerContext.getBody().getBytes());
        responseBody.close();
    }

    private boolean validate(MockHttpServerContext mockHttpServerContext,
            HttpExchange exchange) {
        final List<Condition> requestConditions =
                mockHttpServerContext.getRequestConditions();

        final RequestContext requestContext = new RequestContext(exchange);

        for (Condition requestCondition : requestConditions) {
            if (!requestCondition.getValidation().apply(requestContext)) {
                this.errors.add(requestCondition.getMessage());
                return false;
            }
        }

        return true;
    }

    /**
     * Validate if
     */
    public void validate() {
        if (!errors.isEmpty()) {
            throw new AssertionError(errors.stream().collect(Collectors.joining("\n")) );
        }

        final List<MockHttpServerContext> mustBeCalledContext = this.mockHttpServerContexts.stream()
                .filter(context -> context.isMustBeCalled())
                .collect(Collectors.toList());

        for (final MockHttpServerContext mockHttpServerContext : mustBeCalledContext) {
            if (!uris.stream().anyMatch(uri -> uri.getPath().equals(mockHttpServerContext.getUri()))) {
                throw new AssertException(mockHttpServerContext.getUri() + " Must be called");
            }
        }
    }


    /**
     * Check if the path was hit, if it is not then throw an {@link AssertionError}
     * @param path
     */
    public void mustNeverCalled(final String path) {
        if (uris.stream().anyMatch(uri -> uri.getPath().equals(path))) {
            throw new AssertException(path + " Must never called");
        }
    }
}
