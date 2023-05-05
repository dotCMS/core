package com.dotcms.http.server.mock;

import com.dotcms.http.server.mock.MockHttpServerContext.Condition;
import com.dotcms.http.server.mock.MockHttpServerContext.RequestContext;
import com.dotmarketing.util.UtilMethods;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import graphql.AssertException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

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
    private List<MockHttpServerContext> contextsUsed = new ArrayList<>();

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

            final Map<String, List<MockHttpServerContext>> contextMap = orderContextByUri();

            for (Entry<String, List<MockHttpServerContext>> entry : contextMap.entrySet()) {
                final List<MockHttpServerContext> contexts = entry.getValue();

                httpServer.createContext(entry.getKey(), exchange -> {
                    this.uris.add(exchange.getRequestURI());

                    for (final MockHttpServerContext context : contexts) {
                        if (validate(context, exchange)) {
                            this.contextsUsed.add(context);
                            sendSuccessResponse(exchange, context);
                            return;
                        }
                    }

                    final RequestContext requestContext = new RequestContext(exchange);

                    this.errors.add(getErrorMessage(contexts, requestContext));

                    sendFailResponse(exchange);
                });
            }

            httpServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getErrorMessage(List<MockHttpServerContext> contexts,
            RequestContext requestContext) {
        return "New Request:" + contexts.stream()
                    .flatMap(context -> context.getRequestConditions().stream())
                    .map(requestCondition -> requestCondition.getMessage(requestContext))
                    .collect(Collectors.joining("\n"));
    }

    private static void sendFailResponse(HttpExchange exchange) {
        try {
            exchange.sendResponseHeaders(500, 0);
            exchange.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendSuccessResponse(HttpExchange exchange, MockHttpServerContext context) {
        try {
            exchange.sendResponseHeaders(context.getStatus(),
                    context.getBody().length());

            if (UtilMethods.isSet(context.getBody())) {
                writeBody(context, exchange);
            }

            exchange.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private Map<String, List<MockHttpServerContext>> orderContextByUri() {
        final Map<String, List<MockHttpServerContext>> contextMap = new HashMap<>();

        for (MockHttpServerContext mockHttpServerContext : mockHttpServerContexts) {
            if (!contextMap.containsKey(mockHttpServerContext.getUri())) {
                contextMap.put(mockHttpServerContext.getUri(), new ArrayList<>());
            }

            contextMap.get(mockHttpServerContext.getUri()).add(mockHttpServerContext);
        }
        return contextMap;
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
            if (!contextsUsed.stream().anyMatch(context -> context.equals(mockHttpServerContext))) {
                throw new AssertException("Must be called context: " + mockHttpServerContext);
            }

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
