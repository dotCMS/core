package com.dotcms.graphql.business;

import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;
import graphql.schema.GraphQLSchema;

/**
 * Api to
 */
public interface GraphqlAPI {

    String TYPES_AND_FIELDS_VALID_NAME_REGEX = "[_A-Za-z][_0-9A-Za-z]*";

    GraphQLSchema getSchema() throws DotDataException;

    GraphQLSchema getSchema(User user) throws DotDataException;

    void invalidateSchema();

    /**
     * If GRAPHQL_PRINT_SCHEMA is turn on to true, prints the schema in the file system
     *
     */
    void printSchema();
}
