package com.dotcms.graphql.business;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;

import com.dotcms.graphql.ContentFields;
import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.util.TypeUtil;
import com.dotcms.graphql.util.TypeUtil.TypeFetcher;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class that provides all the {@link GraphQLType}s needed for the Page API
 */

public enum PageAPIGraphQLTypesProvider implements GraphQLTypesProvider {

    INSTANCE;

    Map<String, GraphQLOutputType> typeMap = new HashMap<>();

    @Override
    public Collection<? extends GraphQLType> getTypes() {

        final Map<String, TypeFetcher> pageFields = new HashMap<>(ContentFields.getContentFields());
        pageFields.put("__icon__", new TypeFetcher(GraphQLString));
        pageFields.put("cachettl", new TypeFetcher(GraphQLString));
        pageFields.put("canEdit", new TypeFetcher(GraphQLBoolean));
        pageFields.put("canLock", new TypeFetcher(GraphQLBoolean));
        pageFields.put("canRead", new TypeFetcher(GraphQLBoolean));
        pageFields.put("deleted", new TypeFetcher(GraphQLBoolean));
        pageFields.put("description", new TypeFetcher(GraphQLString));
        pageFields.put("extension", new TypeFetcher(GraphQLString));
        pageFields.put("friendlyName", new TypeFetcher(GraphQLString));
        pageFields.put("hasLiveVersion", new TypeFetcher(GraphQLBoolean));
        pageFields.put("hasTitleImage", new TypeFetcher(GraphQLBoolean));
        pageFields.put("httpsRequired", new TypeFetcher(GraphQLBoolean));
        pageFields.put("image", new TypeFetcher(GraphQLString));
        pageFields.put("imageContentAsset", new TypeFetcher(GraphQLString));
        pageFields.put("imageVersion", new TypeFetcher(GraphQLString));
        pageFields.put("isContentlet", new TypeFetcher(GraphQLBoolean));
        pageFields.put("liveInode", new TypeFetcher(GraphQLString));
        pageFields.put("mimeType", new TypeFetcher(GraphQLString));
        pageFields.put("name", new TypeFetcher(GraphQLString));
        pageFields.put("pageURI", new TypeFetcher(GraphQLString));
        pageFields.put("pageUrl", new TypeFetcher(GraphQLString));
        pageFields.put("shortyLive", new TypeFetcher(GraphQLString));
        pageFields.put("path", new TypeFetcher(GraphQLString));
        pageFields.put("publishDate", new TypeFetcher(GraphQLString));
        pageFields.put("seoTitle", new TypeFetcher(GraphQLString));
        pageFields.put("seodescription", new TypeFetcher(GraphQLString));
        pageFields.put("shortDescription", new TypeFetcher(GraphQLString));
        pageFields.put("shortyWorking", new TypeFetcher(GraphQLString));
        pageFields.put("sortOrder", new TypeFetcher(GraphQLLong));
        pageFields.put("stInode", new TypeFetcher(GraphQLString));
        pageFields.put("statusIcons", new TypeFetcher(GraphQLString));
        pageFields.put("tags", new TypeFetcher(GraphQLString));
        pageFields.put("template", new TypeFetcher(GraphQLString));
        pageFields.put("templateIdentifier", new TypeFetcher(GraphQLString));
        pageFields.put("type", new TypeFetcher(GraphQLString));
        pageFields.put("url", new TypeFetcher(GraphQLString));
        pageFields.put("workingInode", new TypeFetcher(GraphQLString));
        pageFields.put("wfExpireDate", new TypeFetcher(GraphQLString));
        pageFields.put("wfExpireTime", new TypeFetcher(GraphQLString));
        pageFields.put("wfNeverExpire", new TypeFetcher(GraphQLString));
        pageFields.put("wfPublishDate", new TypeFetcher(GraphQLString));
        pageFields.put("wfPublishTime", new TypeFetcher(GraphQLString));

        typeMap.put("Page", TypeUtil.createObjectType("Page", pageFields));

        return typeMap.values();
    }

    Map<String, GraphQLOutputType> getTypesMap() {
        return typeMap;
    }
}
