package com.dotcms.graphql.business;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;

import com.dotcms.graphql.ContentFields;
import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.datafetcher.MapFieldPropertiesDataFetcher;
import com.dotcms.graphql.datafetcher.UserDataFetcher;
import com.dotcms.graphql.datafetcher.page.TemplateDataFetcher;
import com.dotcms.graphql.util.TypeUtil;
import com.dotcms.graphql.util.TypeUtil.TypeFetcher;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
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
        pageFields.put("template", new TypeFetcher(
                GraphQLTypeReference.typeRef("Template"), new TemplateDataFetcher()));
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

        final Map<String, TypeFetcher> templateFields = new HashMap<>();
        templateFields.put("iDate", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("type", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("owner", new TypeFetcher(GraphQLTypeReference.typeRef("User"), new UserDataFetcher()));
        templateFields.put("inode", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("identifier", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("source", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("title", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("friendlyName", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("modDate", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("modUser", new TypeFetcher(GraphQLTypeReference.typeRef("User"), new UserDataFetcher()));
        templateFields.put("sortOrder", new TypeFetcher(GraphQLLong, new MapFieldPropertiesDataFetcher()));
        templateFields.put("showOnMenu", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("image", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("drawed", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("drawedBody", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("theme", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("anonymous", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("template", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("versionId", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("versionType", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("deleted", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("working", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("permissionId", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("name", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("live", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("archived", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("locked", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("permissionType", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("categoryId", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("new", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));
        templateFields.put("idate", new TypeFetcher(GraphQLString, new MapFieldPropertiesDataFetcher()));
        templateFields.put("canEdit", new TypeFetcher(GraphQLBoolean, new MapFieldPropertiesDataFetcher()));

        typeMap.put("Template", TypeUtil.createObjectType("Template", templateFields));

        return typeMap.values();
    }

    Map<String, GraphQLOutputType> getTypesMap() {
        return typeMap;
    }
}
