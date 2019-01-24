package com.dotcms.graphql;

import java.util.Collections;

import graphql.servlet.GraphQLConfiguration;
import graphql.servlet.GraphQLHttpServlet;
import graphql.servlet.GraphQLQueryInvoker;

public class DotGraphQLHttpServlet extends GraphQLHttpServlet {

    @Override
    protected GraphQLConfiguration getConfiguration() {
        return GraphQLConfiguration
            .with(new DotGraphQLSchemaProvider())
            .with(Collections.singletonList(new DotGraphQLServletListener()))
            .with(new DotGraphQLContextBuilder())
            .build();
    }

}
