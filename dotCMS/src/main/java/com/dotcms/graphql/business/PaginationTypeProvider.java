package com.dotcms.graphql.business;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.scalars.ExtendedScalars.GraphQLLong;

import com.dotcms.graphql.util.TypeUtil;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This TypesProvider provides a new type called "Pagination" with pagination data for each
 * requested field of the GraphQL Content Delivery API
 */
public enum PaginationTypeProvider implements GraphQLTypesProvider {

    INSTANCE;

    final Map<String, GraphQLOutputType> paginationFields = Map.of(
            "totalPages", GraphQLInt,
            "totalRecords", GraphQLLong,
            "pageRecords", GraphQLLong,
            "hasNextPage", GraphQLBoolean,
            "hasPreviousPage", GraphQLBoolean,
            "pageSize", GraphQLInt,
            "page", GraphQLInt,
            "offset", GraphQLInt,
            "fieldName", GraphQLString);

    final GraphQLObjectType paginationType = TypeUtil.createObjectType(
            "Pagination", paginationFields, null
    );

    @Override
    public Collection<? extends GraphQLType> getTypes() {
        return List.of(paginationType);
    }
}
