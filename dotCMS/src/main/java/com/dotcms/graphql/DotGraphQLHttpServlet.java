package com.dotcms.graphql;

import com.dotcms.rest.api.CorsFilter;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.kickstart.servlet.AbstractGraphQLHttpServlet;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.input.GraphQLInvocationInputFactory;
import io.vavr.Function0;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DotGraphQLHttpServlet extends AbstractGraphQLHttpServlet {
    
    
    final private static String CORS_DEFAULT=CorsFilter.CORS_PREFIX + "." + CorsFilter.CORS_DEFAULT;
    
    final private static String CORS_GRAPHQL = CorsFilter.CORS_PREFIX + ".graphql";
    @Override
    protected GraphQLQueryInvoker getQueryInvoker() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected GraphQLInvocationInputFactory getInvocationInputFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected GraphQLObjectMapper getGraphQLObjectMapper() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isAsyncServletMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected GraphQLConfiguration getConfiguration() {
        return GraphQLConfiguration
            .with(new DotGraphQLSchemaProvider())
            .with(Collections.singletonList(new DotGraphQLServletListener()))
            .with(new DotGraphQLContextBuilder())
            .build();
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {
        corsHeaders.apply().entrySet().stream().forEach(e -> response.setHeader(e.getKey(), e.getValue()));
        super.doGet(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) {
        corsHeaders.apply().entrySet().stream().forEach(e -> response.setHeader(e.getKey(), e.getValue()));
        super.doPost(request, response);
    }

    @Override
    protected void doOptions(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        corsHeaders.apply().entrySet().stream().forEach(e -> response.setHeader(e.getKey(), e.getValue()));
        super.doOptions(request, response);
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
