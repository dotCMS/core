package com.dotcms.graphql;

import graphql.servlet.GraphQLConfiguration;
import graphql.servlet.GraphQLHttpServlet;

public class DotGraphQLHttpServlet extends GraphQLHttpServlet {

    @Override
    protected GraphQLConfiguration getConfiguration() {
        return GraphQLConfiguration.with(new DotGraphQLSchemaProvider()).build();
    }
}
