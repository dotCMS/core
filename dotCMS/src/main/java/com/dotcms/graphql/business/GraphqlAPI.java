package com.dotcms.graphql.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.exception.DotDataException;

import graphql.schema.GraphQLObjectType;
import java.util.Collection;
import java.util.Map;

import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;

public interface GraphqlAPI {

    String TYPES_AND_FIELDS_VALID_NAME_REGEX = "[_A-Za-z][_0-9A-Za-z]*";

    GraphQLSchema getSchema() throws DotDataException;

    void invalidateSchema();

    void printSchema();
}
