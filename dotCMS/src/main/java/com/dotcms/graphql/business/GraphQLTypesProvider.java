package com.dotcms.graphql.business;

import com.dotmarketing.exception.DotDataException;
import graphql.schema.GraphQLType;
import java.util.Collection;

public interface GraphQLTypesProvider {
    Collection<GraphQLType> getTypes() throws DotDataException;
}
