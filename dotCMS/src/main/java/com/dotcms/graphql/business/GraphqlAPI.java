package com.dotcms.graphql.business;

import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;
import graphql.schema.GraphQLSchema;

public interface GraphqlAPI {

    String TYPES_AND_FIELDS_VALID_NAME_REGEX = "[_A-Za-z][_0-9A-Za-z]*";

    GraphQLSchema getSchema() throws DotDataException;

    GraphQLSchema getSchema(User user) throws DotDataException;

    void invalidateSchema();

    void printSchema();
}
