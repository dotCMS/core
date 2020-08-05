package com.dotcms.graphql.business;

import com.dotmarketing.exception.DotDataException;
import graphql.schema.GraphQLType;
import java.util.Collection;

/**
 * Interface for providers of {@link GraphQLType}s.
 *
 * In case there's a need to provide {@link GraphQLType}s to GraphQL, this interface needs to
 * be implemented
 */
public interface GraphQLTypesProvider {

    /**
     * Returns a collection of {@link GraphQLType}s to be included in the GraphQL Schema
     * @return a collection of {@link GraphQLType}s
     * @throws DotDataException in case of invalid data
     */
    Collection<? extends GraphQLType> getTypes() throws DotDataException;
}
