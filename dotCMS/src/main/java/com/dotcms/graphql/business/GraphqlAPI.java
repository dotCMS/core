package com.dotcms.graphql.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.exception.DotDataException;

import java.util.Map;

import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;

public interface GraphqlAPI {
    GraphQLSchema getSchema() throws DotDataException;
    Map<Class<? extends Field>, GraphQLOutputType> getFieldClassGraphqlTypeMap();
    void invalidateSchema();

}
