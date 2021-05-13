package com.dotcms.graphql.business;

import static com.dotcms.util.CollectionsUtils.map;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;

import com.dotcms.graphql.util.TypeUtil;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * This TypesProvider provides a new type called "QueryMetadata" which includes
 * the "totalCount" and "fieldName" for each requested field of the GraphQL Content Delivery API
 */

public enum QueryMetadataTypeProvider implements GraphQLTypesProvider {

    INSTANCE;

    final Map<String, GraphQLOutputType> pageFields = map(
            "totalCount", GraphQLLong,
            "fieldName", GraphQLString);

    GraphQLObjectType countType = TypeUtil.createObjectType("QueryMetadata", pageFields, null);

    @Override
    public Collection<? extends GraphQLType> getTypes() {
        return Collections.singletonList(countType);
    }
}
