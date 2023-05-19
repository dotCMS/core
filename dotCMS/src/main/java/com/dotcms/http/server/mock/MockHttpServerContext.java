package com.dotcms.http.server.mock;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.io.IOUtils;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

/**
 * Represent a url to mock into a {@link MockHttpServer}, you can set the URL to response and also
 * what is going to  be the URL.
 *
 * Examples:
 *
 *  To listing to http://127.0.0.1:5000/testing and response with a http code of 200 and a response
 *  body of "It is working"
 *
 * <code>
 * final String cubeServerIp = "127.0.0.1";
 * final int cubeJsServerPort = 5000;
 *
 * final MockHttpServer mockhttpServer = new MockHttpServer(cubeServerIp, cubeJsServerPort);
 *
 * final MockHttpServerContext mockHttpServerContext = new  MockHttpServerContext.Builder()
 *      .uri("/testing")
 *      .responseStatus(HttpURLConnection.HTTP_OK)
 *      .responseBody("It is working")
 *      .build();
 *
 *      mockhttpServer.addContext(mockHttpServerContext);
 *      mockhttpServer.start();
 * </code>
 *
 * If you want to check a condition to every request you can do:
 *
 * <code>
 * final String cubeServerIp = "127.0.0.1";
 * final int cubeJsServerPort = 5000;
 *
 * final MockHttpServer mockhttpServer = new MockHttpServer(cubeServerIp, cubeJsServerPort);
 *
 * final MockHttpServerContext mockHttpServerContext = new  MockHttpServerContext.Builder()
 *      .uri("/testing")
 *      .responseStatus(HttpURLConnection.HTTP_OK)
 *      .responseBody("It is working")
 *      .requestCondition("Cube JS Query is not right",
 *           context -> context.getRequestParameter("mode")
 *              .orElse(StringPool.BLANK)
 *              .equals("test")
 *      .build();
 *
 *      mockhttpServer.addContext(mockHttpServerContext);
 *      mockhttpServer.start();
 * </code>
 *
 * In this case we are checking that the URl include a "mode" query parameters with a "test" value.
 */
public class MockHttpServerContext {

    private String uri;
    private int status;
    private String body;
    private List<Condition> conditions;
    private boolean mustBeCalled = false;
    private MockHttpServerContext(final String uri,
            final int status,
            final String body,
            final List<Condition> conditions,
            final boolean mustBeCalled){
        this.uri = uri;
        this.status = status;
        this.body = body;
        this.conditions = conditions;
        this.mustBeCalled = mustBeCalled;
    }

    public boolean isMustBeCalled() {
        return mustBeCalled;
    }

    public String getUri() {
        return uri;
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }

    public List<Condition> getRequestConditions() {
        return conditions;
    }

    public static class Builder {
        private String uri;
        private int status;
        private String body;
        private List<Condition> requestConditions = new ArrayList<>();
        private boolean mustBeCalled = false;

        public Builder uri(final String uri) {
            this.uri = uri;
            return this;
        }

        public MockHttpServerContext build(){
            return new MockHttpServerContext(uri, status, body, requestConditions, mustBeCalled);
        }

        public Builder requestCondition(final String message, final Function<RequestContext, Boolean> handler) {
            this.requestConditions.add(new Condition(handler, message));
            return this;
        }

        public Builder requestCondition(final Function<RequestContext, String> messageFunc,
                final Function<RequestContext, Boolean> handler) {
            this.requestConditions.add(new Condition(handler, messageFunc));
            return this;
        }

        public Builder responseStatus(final int status) {
            this.status = status;
            return this;
        }

        public Builder responseBody(String body) {
            this.body = body;
            return this;
        }

        /**
         * Make required that this context get at least one hit during the test
         * @return
         */
        public Builder mustBeCalled() {
            this.mustBeCalled = true;
            return this;
        }
    }

    public static class RequestContext {
        final HttpExchange httpExchange;
        final Map<String, String> requestParameters = new HashMap<>();

        public RequestContext(HttpExchange httpExchange) {
            this.httpExchange = httpExchange;

            final String query = httpExchange.getRequestURI().getQuery();

            if (UtilMethods.isSet(query)) {
                final String[] querySplited = query.split(StringPool.AMPERSAND);

                for (String queryParameter : querySplited) {
                    final String[] parameter = queryParameter.split(StringPool.EQUAL);
                    this.requestParameters.put(parameter[0], parameter[1]);
                }
            }
        }

        public String getRequestBody() {
            final InputStream requestBody = this.httpExchange.getRequestBody();

            try {
                return IOUtils.toString(requestBody, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public Optional<String> getRequestParameter(final String name) {
            return this.requestParameters.get(name) != null ? Optional.of(this.requestParameters.get(name)) :
                    Optional.empty();
        }

        public Optional<Map<String, Object>> getRequestParameterAsMap(final String name) {
            try {
                return this.requestParameters.get(name) != null ?
                        Optional.ofNullable(JsonUtil.getJsonFromString(this.requestParameters.get(name))) :
                        Optional.empty();
            } catch (IOException e) {
                throw new DotRuntimeException(e);
            }
        }

        public Headers getHeaders() {
            return this.httpExchange.getRequestHeaders();
        }
    }

    public static class Condition {
        private Function<RequestContext, Boolean> validation;
        private  Function<RequestContext, String>  message;

        public Condition(final Function<RequestContext, Boolean> validation, final String message) {
            this(validation, (context) -> message);
        }

        public Condition(final Function<RequestContext, Boolean> validation,
                final Function<RequestContext, String> message) {
            this.validation = validation;
            this.message = message;
        }


        public Function<RequestContext, Boolean> getValidation() {
            return validation;
        }

        public String getMessage(final RequestContext context) {
            return message.apply(context);
        }
    }

    @Override
    public String toString() {
        return "MockHttpServerContext{" +
                "uri='" + uri + '\'' +
                ", status=" + status +
                ", body='" + body + '\'' +
                ", conditions=" + conditions +
                ", mustBeCalled=" + mustBeCalled +
                '}';
    }
}
