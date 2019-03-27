package com.dotcms.graphql;

import java.util.Collections;

import graphql.servlet.GraphQLConfiguration;
import graphql.servlet.GraphQLInvocationInputFactory;
import graphql.servlet.GraphQLObjectMapper;
import graphql.servlet.GraphQLQueryInvoker;

public class DotGraphQLHttpServlet extends AbstractGraphQLHttpServlet {

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

}
