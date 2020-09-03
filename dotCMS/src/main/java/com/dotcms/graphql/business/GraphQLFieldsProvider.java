package com.dotcms.graphql.business;

import com.dotmarketing.exception.DotDataException;
import graphql.schema.GraphQLFieldDefinition;
import java.util.Collection;

/**
 * Interface for providers of {@link GraphQLFieldDefinition}s.
 *
 * In case there's a need to provide {@link GraphQLFieldDefinition}s to GraphQL, this interface needs to
 * be implemented
 */
public interface GraphQLFieldsProvider {

    /**
     * Returns a collection of {@link GraphQLFieldDefinition}s to be included in the GraphQL Schema
     * @return a collection of {@link GraphQLFieldDefinition}s
     * @throws DotDataException in case of invalid data
     */
    Collection<GraphQLFieldDefinition> getFields() throws DotDataException;
}
