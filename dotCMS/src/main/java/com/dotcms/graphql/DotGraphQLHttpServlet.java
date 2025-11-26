package com.dotcms.graphql;

import com.dotcms.rest.api.CorsFilter;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import graphql.kickstart.servlet.AbstractGraphQLHttpServlet;
import graphql.kickstart.servlet.GraphQLConfiguration;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

public class DotGraphQLHttpServlet extends AbstractGraphQLHttpServlet {

    private static final String CORS_DEFAULT=CorsFilter.CORS_PREFIX + "." + CorsFilter.CORS_DEFAULT;

    private static final String CORS_GRAPHQL = CorsFilter.CORS_PREFIX + ".graphql";

    /**
     * Wrapper to force the request to be a POST
     */
    static class PostRequestWrapper extends HttpServletRequestWrapper {
        public PostRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getMethod() {
            return "POST";
        }
    }


    @Override
    protected GraphQLConfiguration getConfiguration() {
        return GraphQLConfiguration
                .with(new DotGraphQLSchemaProvider())
                .with(List.of(new DotGraphQLServletListener()))
                .with(new DotGraphQLContextBuilder())
                .build();
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {

        if(request.getParameter("qid") == null) {
            Logger.warn(DotGraphQLHttpServlet.class, "No query id (qid) provided in graphql GET .  This can result in invalid cached data by both browsers and CDNs.  Please provide a distinguishing query id (qid) parameter to execute a graphql query via GET, e.g. /api/v1/graphql?qid=123abc");
            Try.run(()->response.sendError(500, "No query id (qid) provided.  This can result in invalid cached data by both browsers and CDNs.  Please provide a distinguishing query id (qid) parameter to execute a graphql query via GET, e.g. /api/v1/graphql?qid=123abc"));
            return;
        }

        HttpServletRequest wrapper = new PostRequestWrapper(request);

        handleRequest(wrapper, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) {
        handleRequest(request, response);
    }

    @Override
    protected void doOptions(final HttpServletRequest request, final HttpServletResponse response) {
        corsHeaders.get().forEach(response::setHeader);

    }

    /**
     * We're moving the request processing from the superclass down here to be able to apply the CORS headers and use our own logger
     * @param request
     * @param response
     */
    protected void handleRequest(HttpServletRequest request, HttpServletResponse response) {
        corsHeaders.get().forEach(response::setHeader);
        try {
            getConfiguration().getHttpRequestHandler().handle(request, response);
        } catch (Exception t) {
            // There's a problem with our slf4j logger in our GraphQL Impl
            // so we're using the dotCMS logger for now
            Logger.error("Error executing GraphQL request!", t);
        }
    }

    /**
     * header list is computed but once. It first reads all values set as default, e.g.
     * api.cors.default.access-control-allow-headers that start with api.cors.default. It then overrides
     * those with the specific ones for graphql, api.cors.graphql.access-control-allow-headers
     *
     */
    protected final Lazy<HashMap<String, String>> corsHeaders = Lazy.of(() -> {

        final HashMap<String, String> headers = new HashMap<>();
        // load defaults
        Config.subsetContainsAsList(CORS_DEFAULT).stream().filter(k -> k.startsWith(CORS_DEFAULT)).forEach(k -> {
            final String prop = Config.getStringProperty(k, null);
            if (UtilMethods.isSet(prop)) {
                headers.put(k.replace(CORS_DEFAULT + ".", "").toLowerCase(), prop);
            }

        });
        // then override with graph
        Config.subsetContainsAsList(CORS_GRAPHQL).stream().filter(k -> k.startsWith(CORS_GRAPHQL)).forEach(k -> {
            final String prop = Config.getStringProperty(k, null);
            if (UtilMethods.isSet(prop)) {
                headers.put(k.replace(CORS_GRAPHQL + ".", "").toLowerCase(), prop);
            } else {
                headers.remove(k.toLowerCase());
            }

        });

        return headers;


    });






}
