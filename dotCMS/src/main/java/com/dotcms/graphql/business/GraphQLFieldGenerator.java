package com.dotcms.graphql.business;

import com.dotcms.contenttype.model.field.Field;
import graphql.schema.GraphQLFieldDefinition;

/**
 * Interface for any generator of a GraphQLFieldDefinition
 */
public interface GraphQLFieldGenerator {

    /**
     * Generated a {@link GraphQLFieldDefinition} for the provided {@link Field}
     * @param field field whose {@link GraphQLFieldDefinition} is requested
     * @return the GraphQL field definition
     */
    GraphQLFieldDefinition generateField(final Field field);
}
