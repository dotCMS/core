package com.dotcms.graphql.business;

import static com.dotcms.util.CollectionsUtils.map;
import static graphql.Scalars.GraphQLString;

import com.dotcms.graphql.util.TypeUtil;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public enum QueryMetadataTypeProvider implements GraphQLTypesProvider {

    INSTANCE;

    final Map<String, GraphQLOutputType> pageFields = map(
            "totalCount", GraphQLString,
            "fieldName", GraphQLString);

    GraphQLObjectType countType = TypeUtil.createObjectType("QueryMetadata", pageFields, null);

    @Override
    public Collection<? extends GraphQLType> getTypes() {
        return Collections.singletonList(countType);
    }
}
