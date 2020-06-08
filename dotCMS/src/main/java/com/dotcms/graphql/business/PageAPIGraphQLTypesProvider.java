package com.dotcms.graphql.business;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;

import com.dotcms.graphql.datafetcher.page.PageDataFetcher;
import com.dotcms.graphql.datafetcher.page.PagePropertiesDataFetcher;
import com.dotcms.graphql.util.TypeUtil;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public enum PageAPIGraphQLTypesProvider implements GraphQLTypesProvider {

    INSTANCE;

    Map<String, GraphQLOutputType> typeMap = new HashMap<>();

    @Override
    public Collection<? extends GraphQLType> getTypes() {

        final Map<String, GraphQLOutputType> pageFields = new HashMap<>();
        pageFields.put("archived", GraphQLBoolean);
        pageFields.put("baseType", GraphQLString);
        pageFields.put("pageURI", GraphQLString);
        pageFields.put("pageURL", GraphQLString);

        typeMap.put("Page", TypeUtil.createObjectType("Page", pageFields,
                new PagePropertiesDataFetcher()));

        return typeMap.values();
    }

    Map<String, GraphQLOutputType> getTypesMap() {
        return typeMap;
    }
}
