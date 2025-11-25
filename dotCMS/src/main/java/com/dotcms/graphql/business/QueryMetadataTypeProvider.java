package com.dotcms.graphql.business;

import com.dotcms.graphql.util.TypeUtil;
import com.dotmarketing.util.Logger;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static graphql.scalars.ExtendedScalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;

/**
 * This TypesProvider provides a new type called "QueryMetadata" which includes
 * the "totalCount" and "fieldName" for each requested field of the GraphQL Content Delivery API
 */

public enum QueryMetadataTypeProvider implements GraphQLTypesProvider {

    INSTANCE;

    final Map<String, GraphQLOutputType> pageFields = Map.of(
            "totalCount", GraphQLLong,
            "fieldName", GraphQLString);

    GraphQLObjectType countType = TypeUtil.createObjectType("QueryMetadata", pageFields, null);

    @Override
    public Collection<? extends GraphQLType> getTypes() {

        Logger.debug(this, ()->"Creating Query Metadata types");
        return List.of(countType);
    }
}
