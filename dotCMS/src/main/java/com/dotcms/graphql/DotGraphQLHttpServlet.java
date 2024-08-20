package com.dotcms.graphql;

import com.dotcms.rest.api.CorsFilter;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import graphql.kickstart.servlet.AbstractGraphQLHttpServlet;
import graphql.kickstart.servlet.GraphQLConfiguration;
import io.vavr.Function0;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DotGraphQLHttpServlet extends AbstractGraphQLHttpServlet {

    private static final String CORS_DEFAULT=CorsFilter.CORS_PREFIX + "." + CorsFilter.CORS_DEFAULT;

    private static final String CORS_GRAPHQL = CorsFilter.CORS_PREFIX + ".graphql";

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
        handleRequest(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) {
        handleRequest(request, response);
    }

    @Override
    protected void doOptions(final HttpServletRequest request, final HttpServletResponse response) {
        handleRequest(request, response);
    }

    /**
     * We're moving the request processing from the superclass down here to be able to apply the CORS headers and use our own logger
     * @param request
     * @param response
     */
    protected void handleRequest(HttpServletRequest request, HttpServletResponse response) {
        corsHeaders.apply().forEach(response::setHeader);
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
    protected final Function0<HashMap<String, String>> corsHeaders = Function0.of(() -> {

        final HashMap<String, String> headers = new HashMap<>();
        // load defaults
        Config.subset(CORS_DEFAULT).forEachRemaining(k -> {
            final String prop = Config.getStringProperty(CORS_DEFAULT + "." + k, null);
            if (UtilMethods.isSet(prop)) {
                headers.put(k.toLowerCase(), prop);
            }

        });
        // then override with graph
        Config.subset(CORS_GRAPHQL).forEachRemaining(k -> {
            final String prop = Config.getStringProperty(CORS_GRAPHQL + "." + k, null);
            if (UtilMethods.isSet(prop)) {
                headers.put(k.toLowerCase(), prop);
            } else {
                headers.remove(k.toLowerCase());
            }

        });



        return headers;


    }).memoized();






}