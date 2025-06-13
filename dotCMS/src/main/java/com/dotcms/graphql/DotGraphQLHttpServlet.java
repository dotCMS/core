package com.dotcms.graphql;

import javax.servlet.annotation.WebServlet;
import graphql.kickstart.servlet.AbstractGraphQLHttpServlet;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.schema.GraphQLSchema;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import graphql.execution.instrumentation.Instrumentation;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import java.util.HashMap;
import java.util.Map;

@WebServlet(
    name = "DotGraphQLHttpServlet",
    urlPatterns = {"/api/v1/graphql"}
)
public class DotGraphQLHttpServlet extends AbstractGraphQLHttpServlet {

    protected final Lazy<HashMap<String, String>> corsHeaders = Lazy.of(() -> {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("access-control-allow-origin", "*");
        headers.put("access-control-allow-credentials", "true");
        headers.put("access-control-allow-headers", "*");
        headers.put("access-control-allow-methods", "GET,PUT,POST,DELETE,HEAD,OPTIONS,PATCH");
        headers.put("access-control-expose-headers", "Content-Type,Cache-Control");
        return headers;
    });

    @Override
    protected GraphQLConfiguration getConfiguration() {
        return GraphQLConfiguration.with(new DotGraphQLSchemaProvider())
            .with(new DotGraphQLContextBuilder())
            .build();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            corsHeaders.get().forEach(response::setHeader);
            super.doGet(request, response);
        } catch (Exception e) {
            Logger.error(this, "Error handling GraphQL GET request", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            corsHeaders.get().forEach(response::setHeader);
            super.doPost(request, response);
        } catch (Exception e) {
            Logger.error(this, "Error handling GraphQL POST request", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        try {
            corsHeaders.get().forEach(response::setHeader);
            super.doOptions(request, response);
        } catch (Exception e) {
            Logger.error(this, "Error handling GraphQL OPTIONS request", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
