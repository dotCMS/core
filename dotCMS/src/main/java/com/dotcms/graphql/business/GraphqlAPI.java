package com.dotcms.graphql.business;

import com.dotmarketing.exception.DotDataException;

import graphql.schema.GraphQLSchema;

public interface GraphqlAPI {
    GraphQLSchema getSchema() throws DotDataException;
    void invalidateSchema();
}
