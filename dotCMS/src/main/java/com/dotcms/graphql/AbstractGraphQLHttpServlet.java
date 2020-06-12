//package com.dotcms.graphql;
//
//import com.google.common.io.ByteStreams;
//import com.google.common.io.CharStreams;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.BufferedInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.Writer;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.function.BiConsumer;
//import java.util.function.Consumer;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//import javax.servlet.AsyncContext;
//import javax.servlet.Servlet;
//import javax.servlet.ServletConfig;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.Part;
//
//import graphql.ExecutionResult;
//import graphql.introspection.IntrospectionQuery;
//import graphql.schema.GraphQLFieldDefinition;
//import graphql.servlet.GraphQLBatchedInvocationInput;
//import graphql.servlet.GraphQLConfiguration;
//import graphql.servlet.GraphQLInvocationInputFactory;
//import graphql.servlet.GraphQLMBean;
//import graphql.servlet.GraphQLObjectMapper;
//import graphql.servlet.GraphQLQueryInvoker;
//import graphql.servlet.GraphQLServletListener;
//import graphql.servlet.GraphQLSingleInvocationInput;
//import graphql.servlet.internal.GraphQLRequest;
//import graphql.servlet.internal.VariableMapper;
//
///**
// * @author Andrew Potter
// */
//public abstract class AbstractGraphQLHttpServlet extends HttpServlet implements Servlet, GraphQLMBean {
//
//    public static final Logger log = LoggerFactory.getLogger(AbstractGraphQLHttpServlet.class);
//
//    public static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
//    public static final String APPLICATION_GRAPHQL = "application/graphql";
//    public static final int STATUS_OK = 200;
//    public static final int STATUS_BAD_REQUEST = 400;
//
//    private static final GraphQLRequest INTROSPECTION_REQUEST = new GraphQLRequest(IntrospectionQuery.INTROSPECTION_QUERY, new HashMap<>(), null);
//    private static final String[] MULTIPART_KEYS = new String[]{"operations", "graphql", "query"};
//
//    private GraphQLConfiguration configuration;
//
//    /**
//     * @deprecated override {@link #getConfiguration()} instead
//     */
//    @Deprecated
//    protected abstract GraphQLQueryInvoker getQueryInvoker();
//
//    /**
//     * @deprecated override {@link #getConfiguration()} instead
//     */
//    @Deprecated
//    protected abstract GraphQLInvocationInputFactory getInvocationInputFactory();
//
//    /**
//     * @deprecated override {@link #getConfiguration()} instead
//     */
//    @Deprecated
//    protected abstract GraphQLObjectMapper getGraphQLObjectMapper();
//
//    /**
//     * @deprecated override {@link #getConfiguration()} instead
//     */
//    @Deprecated
//    protected abstract boolean isAsyncServletMode();
//
//    protected GraphQLConfiguration getConfiguration() {
//        return GraphQLConfiguration.with(getInvocationInputFactory())
//                .with(getQueryInvoker())
//                .with(getGraphQLObjectMapper())
//                .with(isAsyncServletMode())
//                .with(listeners)
//                .build();
//    }
//
//    /**
//     * @deprecated use {@link #getConfiguration()} instead
//     */
//    @Deprecated
//    private final List<GraphQLServletListener> listeners;
//
//    private HttpRequestHandler getHandler;
//    private HttpRequestHandler postHandler;
//
//    public AbstractGraphQLHttpServlet() {
//        this(null);
//    }
//
//    public AbstractGraphQLHttpServlet(List<GraphQLServletListener> listeners) {
//        this.listeners = listeners != null ? new ArrayList<>(listeners) : new ArrayList<>();
//    }
//
//    @Override
//    public void init(ServletConfig servletConfig) {
//        this.configuration = getConfiguration();
//
//        this.getHandler = (request, response) -> {
//            GraphQLInvocationInputFactory invocationInputFactory = configuration.getInvocationInputFactory();
//            GraphQLObjectMapper graphQLObjectMapper = configuration.getObjectMapper();
//            GraphQLQueryInvoker queryInvoker = configuration.getQueryInvoker();
//
//            String path = request.getPathInfo();
//            if (path == null) {
//                path = request.getServletPath();
//            }
//            if (path.contentEquals("/schema.json")) {
//                query(queryInvoker, graphQLObjectMapper, invocationInputFactory.create(INTROSPECTION_REQUEST, request, response), response);
//            } else {
//                String query = request.getParameter("query");
//                if (query != null) {
//
//                    if (isBatchedQuery(query)) {
//                        queryBatched(queryInvoker, graphQLObjectMapper, invocationInputFactory.createReadOnly(graphQLObjectMapper.readBatchedGraphQLRequest(query), request, response), response);
//                    } else {
//                        final Map<String, Object> variables = new HashMap<>();
//                        if (request.getParameter("variables") != null) {
//                            variables.putAll(graphQLObjectMapper.deserializeVariables(request.getParameter("variables")));
//                        }
//
//                        String operationName = request.getParameter("operationName");
//
//                        query(queryInvoker, graphQLObjectMapper, invocationInputFactory.createReadOnly(new GraphQLRequest(query, variables, operationName), request, response), response);
//                    }
//                } else {
//                    response.setStatus(STATUS_BAD_REQUEST);
//                    log.info("Bad GET request: path was not \"/schema.json\" or no query variable named \"query\" given");
//                }
//            }
//        };
//
//        this.postHandler = (request, response) -> {
//            GraphQLInvocationInputFactory invocationInputFactory = configuration.getInvocationInputFactory();
//            GraphQLObjectMapper graphQLObjectMapper = configuration.getObjectMapper();
//            GraphQLQueryInvoker queryInvoker = configuration.getQueryInvoker();
//
//            if (APPLICATION_GRAPHQL.equals(request.getContentType())) {
//                String query = CharStreams.toString(request.getReader());
//                query(queryInvoker, graphQLObjectMapper, invocationInputFactory.create(new GraphQLRequest(query, null, null)), response);
//            } else if (request.getContentType() != null && request.getContentType().startsWith("multipart/form-data") && !request.getParts().isEmpty()) {
//                final Map<String, List<Part>> fileItems = request.getParts()
//                    .stream()
//                    .collect(Collectors.groupingBy(Part::getName));
//
//                for (String key : MULTIPART_KEYS) {
//                    // Check to see if there is a part under the key we seek
//                    if (!fileItems.containsKey(key)) {
//                        continue;
//                    }
//
//                    final Optional<Part> queryItem = getFileItem(fileItems, key);
//                    if (!queryItem.isPresent()) {
//                        // If there is a part, but we don't see an item, then break and return BAD_REQUEST
//                        break;
//                    }
//
//                    InputStream inputStream = asMarkableInputStream(queryItem.get().getInputStream());
//
//                    final Optional<Map<String, List<String>>> variablesMap =
//                        getFileItem(fileItems, "map").map(graphQLObjectMapper::deserializeMultipartMap);
//
//                    if (isBatchedQuery(inputStream)) {
//                        List<GraphQLRequest> graphQLRequests =
//                            graphQLObjectMapper.readBatchedGraphQLRequest(inputStream);
//                        variablesMap.ifPresent(map -> graphQLRequests.forEach(r -> mapMultipartVariables(r, map, fileItems)));
//                        GraphQLBatchedInvocationInput invocationInput =
//                            invocationInputFactory.create(graphQLRequests, request, response);
//                        invocationInput.getContext().setParts(fileItems);
//                        queryBatched(queryInvoker, graphQLObjectMapper, invocationInput, response);
//                        return;
//                    } else {
//                        GraphQLRequest graphQLRequest;
//                        if ("query".equals(key)) {
//                            graphQLRequest = buildRequestFromQuery(inputStream, graphQLObjectMapper, fileItems);
//                        } else {
//                            graphQLRequest = graphQLObjectMapper.readGraphQLRequest(inputStream);
//                        }
//
//                        variablesMap.ifPresent(m -> mapMultipartVariables(graphQLRequest, m, fileItems));
//                        GraphQLSingleInvocationInput invocationInput =
//                            invocationInputFactory.create(graphQLRequest, request, response);
//                        invocationInput.getContext().setParts(fileItems);
//                        query(queryInvoker, graphQLObjectMapper, invocationInput, response);
//                        return;
//                    }
//                }
//
//                response.setStatus(STATUS_BAD_REQUEST);
//                log.info("Bad POST multipart request: no part named " + Arrays.toString(MULTIPART_KEYS));
//            } else {
//                // this is not a multipart request
//                InputStream inputStream = asMarkableInputStream(request.getInputStream());
//
//                if (isBatchedQuery(inputStream)) {
//                    queryBatched(queryInvoker, graphQLObjectMapper, invocationInputFactory.create(graphQLObjectMapper.readBatchedGraphQLRequest(inputStream), request, response), response);
//                } else {
//                    query(queryInvoker, graphQLObjectMapper, invocationInputFactory.create(graphQLObjectMapper.readGraphQLRequest(inputStream), request, response), response);
//                }
//            }
//        };
//    }
//
//    private static InputStream asMarkableInputStream(InputStream inputStream) {
//        if (!inputStream.markSupported()) {
//            inputStream = new BufferedInputStream(inputStream);
//        }
//        return inputStream;
//    }
//
//    private GraphQLRequest buildRequestFromQuery(InputStream inputStream,
//                                                 GraphQLObjectMapper graphQLObjectMapper,
//                                                 Map<String, List<Part>> fileItems) throws IOException {
//        GraphQLRequest graphQLRequest;
//        String query = new String(ByteStreams.toByteArray(inputStream));
//
//        Map<String, Object> variables = null;
//        final Optional<Part> variablesItem = getFileItem(fileItems, "variables");
//        if (variablesItem.isPresent()) {
//            variables = graphQLObjectMapper.deserializeVariables(new String(ByteStreams.toByteArray(variablesItem.get().getInputStream())));
//        }
//
//        String operationName = null;
//        final Optional<Part> operationNameItem = getFileItem(fileItems, "operationName");
//        if (operationNameItem.isPresent()) {
//            operationName = new String(ByteStreams.toByteArray(operationNameItem.get().getInputStream())).trim();
//        }
//
//        graphQLRequest = new GraphQLRequest(query, variables, operationName);
//        return graphQLRequest;
//    }
//
//    private void mapMultipartVariables(GraphQLRequest request,
//                                       Map<String, List<String>> variablesMap,
//                                       Map<String, List<Part>> fileItems) {
//        Map<String, Object> variables = request.getVariables();
//
//        variablesMap.forEach((partName, objectPaths) -> {
//            Part part = getFileItem(fileItems, partName)
//                    .orElseThrow(() -> new RuntimeException("unable to find part name " +
//                            partName +
//                            " as referenced in the variables map"));
//
//            objectPaths.forEach(objectPath -> VariableMapper.mapVariable(objectPath, variables, part));
//        });
//    }
//
//    public void addListener(GraphQLServletListener servletListener) {
//        configuration.add(servletListener);
//    }
//
//    public void removeListener(GraphQLServletListener servletListener) {
//        configuration.remove(servletListener);
//    }
//
//    @Override
//    public String[] getQueries() {
//        return configuration.getInvocationInputFactory().getSchemaProvider().getSchema().getQueryType().getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).toArray(String[]::new);
//    }
//
//    @Override
//    public String[] getMutations() {
//        return configuration.getInvocationInputFactory().getSchemaProvider().getSchema().getMutationType().getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).toArray(String[]::new);
//    }
//
//    @Override
//    public String executeQuery(String query) {
//        try {
//            return configuration.getObjectMapper().serializeResultAsJson(configuration.getQueryInvoker().query(configuration.getInvocationInputFactory().create(new GraphQLRequest(query, new HashMap<>(), null))));
//        } catch (Exception e) {
//            return e.getMessage();
//        }
//    }
//
//    private void doRequestAsync(HttpServletRequest request, HttpServletResponse response, HttpRequestHandler handler) {
//        if (configuration.isAsyncServletModeEnabled()) {
//            AsyncContext asyncContext = request.startAsync();
//            HttpServletRequest asyncRequest = (HttpServletRequest) asyncContext.getRequest();
//            HttpServletResponse asyncResponse = (HttpServletResponse) asyncContext.getResponse();
//            new Thread(() -> doRequest(asyncRequest, asyncResponse, handler, asyncContext)).start();
//        } else {
//            doRequest(request, response, handler, null);
//        }
//    }
//
//    private void doRequest(HttpServletRequest request, HttpServletResponse response, HttpRequestHandler handler, AsyncContext asyncContext) {
//
//        List<GraphQLServletListener.RequestCallback> requestCallbacks = runListeners(l -> l.onRequest(request, response));
//
//        try {
//            handler.handle(request, response);
//            runCallbacks(requestCallbacks, c -> c.onSuccess(request, response));
//        } catch (Throwable t) {
//            response.setStatus(500);
//            log.error("Error executing GraphQL request!", t);
//            runCallbacks(requestCallbacks, c -> c.onError(request, response, t));
//        } finally {
//            runCallbacks(requestCallbacks, c -> c.onFinally(request, response));
//            if (asyncContext != null) {
//                asyncContext.complete();
//            }
//        }
//    }
//
//    @Override
//    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        doRequestAsync(req, resp, getHandler);
//    }
//
//    @Override
//    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        doRequestAsync(req, resp, postHandler);
//    }
//
//    private Optional<Part> getFileItem(Map<String, List<Part>> fileItems, String name) {
//        return Optional.ofNullable(fileItems.get(name)).filter(list -> !list.isEmpty()).map(list -> list.get(0));
//    }
//
//    private void query(GraphQLQueryInvoker queryInvoker, GraphQLObjectMapper graphQLObjectMapper, GraphQLSingleInvocationInput invocationInput, HttpServletResponse resp) throws IOException {
//        ExecutionResult result = queryInvoker.query(invocationInput);
//
//        resp.setContentType(APPLICATION_JSON_UTF8);
//        resp.setStatus(STATUS_OK);
//        resp.getWriter().write(graphQLObjectMapper.serializeResultAsJson(result));
//    }
//
//    private void queryBatched(GraphQLQueryInvoker queryInvoker, GraphQLObjectMapper graphQLObjectMapper, GraphQLBatchedInvocationInput invocationInput, HttpServletResponse resp) throws Exception {
//        resp.setContentType(APPLICATION_JSON_UTF8);
//        resp.setStatus(STATUS_OK);
//
//        Writer respWriter = resp.getWriter();
//        respWriter.write('[');
//
//        queryInvoker.query(invocationInput, (result, hasNext) -> {
//            respWriter.write(graphQLObjectMapper.serializeResultAsJson(result));
//            if (hasNext) {
//                respWriter.write(',');
//            }
//        });
//
//        respWriter.write(']');
//    }
//
//    private <R> List<R> runListeners(Function<? super GraphQLServletListener, R> action) {
//        return configuration.getListeners().stream()
//                .map(listener -> {
//                    try {
//                        return action.apply(listener);
//                    } catch (Throwable t) {
//                        log.error("Error running listener: {}", listener, t);
//                        return null;
//                    }
//                })
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }
//
//    private <T> void runCallbacks(List<T> callbacks, Consumer<T> action) {
//        callbacks.forEach(callback -> {
//            try {
//                action.accept(callback);
//            } catch (Throwable t) {
//                log.error("Error running callback: {}", callback, t);
//            }
//        });
//    }
//
//    private boolean isBatchedQuery(InputStream inputStream) throws IOException {
//        if (inputStream == null) {
//            return false;
//        }
//
//        ByteArrayOutputStream result = new ByteArrayOutputStream();
//        byte[] buffer = new byte[128];
//        int length;
//
//        inputStream.mark(0);
//        while ((length = inputStream.read(buffer)) != -1) {
//            result.write(buffer, 0, length);
//            String chunk = result.toString();
//            Boolean isArrayStart = isArrayStart(chunk);
//            if (isArrayStart != null) {
//                inputStream.reset();
//                return isArrayStart;
//            }
//        }
//
//        inputStream.reset();
//        return false;
//    }
//
//    private boolean isBatchedQuery(String query) {
//        if (query == null) {
//            return false;
//        }
//
//        Boolean isArrayStart = isArrayStart(query);
//        return isArrayStart != null && isArrayStart;
//    }
//
//    // return true if the first non whitespace character is the beginning of an array
//    private Boolean isArrayStart(String s) {
//        for (int i = 0; i < s.length(); i++) {
//            char ch = s.charAt(i);
//            if (!Character.isWhitespace(ch)) {
//                return ch == '[';
//            }
//        }
//
//        return null;
//    }
//
//    protected interface HttpRequestHandler extends BiConsumer<HttpServletRequest, HttpServletResponse> {
//        @Override
//        default void accept(HttpServletRequest request, HttpServletResponse response) {
//            try {
//                handle(request, response);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        void handle(HttpServletRequest request, HttpServletResponse response) throws Exception;
//    }
//}
