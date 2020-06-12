package com.dotcms.graphql.business;

import static graphql.Scalars.GraphQLBoolean;

import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.datafetcher.page.PagePropertiesDataFetcher;
import com.dotcms.graphql.util.TypeUtil;
import com.dotcms.graphql.util.TypeUtil.TypeFetcher;
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

        final Map<String, TypeFetcher> pageFields = new HashMap<>(InterfaceType.getContentFields());
        pageFields.put("cachettl", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("canEdit", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("canLock", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("canRead", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("deleted", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("friendlyName", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("hasTitleImage", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("httpsRequired", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("liveInode", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("pageURI", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("pageUrl", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("shortyLive", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("shortyWorking", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("sortOrder", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("templateIdentifier", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("url", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("workingInode", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("wfExpireDate", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("wfExpireTime", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("wfNeverExpire", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("wfPublishDate", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));
        pageFields.put("wfPublishTime", new TypeFetcher(GraphQLBoolean, new PagePropertiesDataFetcher()));

        typeMap.put("Page", TypeUtil.createObjectType("Page", pageFields));

        return typeMap.values();
    }

    Map<String, GraphQLOutputType> getTypesMap() {
        return typeMap;
    }
}
