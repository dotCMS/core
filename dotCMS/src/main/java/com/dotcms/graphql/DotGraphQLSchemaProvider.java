package com.dotcms.graphql;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.HandshakeRequest;

import graphql.schema.GraphQLSchema;
import graphql.servlet.GraphQLSchemaProvider;

public class DotGraphQLSchemaProvider implements GraphQLSchemaProvider {
    @Override
    public GraphQLSchema getSchema(HttpServletRequest request) {
        return getSchema();
    }

    @Override
    public GraphQLSchema getSchema(HandshakeRequest request) {
        return getSchema();
    }

    @Override
    public GraphQLSchema getSchema() {
        try {
            return APILocator.getGraphqlAPI().getSchema();
        } catch(DotDataException e) {
            Logger.error(this, "Error with Schema retrieval/generation", e);
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public GraphQLSchema getReadOnlySchema(HttpServletRequest request) {
        return null;
    }

}
