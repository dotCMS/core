package com.dotcms.graphql;

import com.dotcms.graphql.datafetcher.ContentMapDataFetcher;
import com.dotcms.graphql.datafetcher.FolderFieldDataFetcher;
import com.dotcms.graphql.datafetcher.LanguageDataFetcher;
import com.dotcms.graphql.datafetcher.SiteFieldDataFetcher;
import com.dotcms.graphql.datafetcher.TitleImageFieldDataFetcher;
import com.dotcms.graphql.datafetcher.UserDataFetcher;
import com.dotcms.graphql.util.TypeUtil.TypeFetcher;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.PropertyDataFetcher;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.BASE_TYPE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CONTENT_TYPE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CREATION_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.IDENTIFIER;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.INODE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.LIVE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.MOD_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.TITLE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.URL_MAP;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKING;
import static com.dotcms.graphql.CustomFieldType.BINARY;
import static com.dotcms.graphql.CustomFieldType.FOLDER;
import static com.dotcms.graphql.CustomFieldType.LANGUAGE;
import static com.dotcms.graphql.CustomFieldType.SITE;
import static com.dotcms.graphql.CustomFieldType.USER;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.ARCHIVED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.FOLDER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HOST_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LOCKED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.MOD_USER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.OWNER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.PUBLISH_DATE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.PUBLISH_USER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITLE_IMAGE_KEY;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;

/**
 * Utility class that defines and returns the available fields for the {@link InterfaceType#CONTENTLET}
 */
public final class ContentFields {

    private ContentFields() {}

    public static Map<String, TypeFetcher> getContentFields() {
        final Map<String, TypeFetcher> contentFields = new HashMap<>();
        contentFields.put(CREATION_DATE, new TypeFetcher(GraphQLString));
        contentFields.put(MOD_DATE, new TypeFetcher(GraphQLString));
        contentFields.put(TITLE, new TypeFetcher(GraphQLString));
        contentFields.put(TITLE_IMAGE_KEY, new TypeFetcher(GraphQLTypeReference.typeRef(BINARY.getTypeName()),
                new TitleImageFieldDataFetcher()));
        contentFields.put(CONTENT_TYPE, new TypeFetcher(GraphQLString));
        contentFields.put(BASE_TYPE, new TypeFetcher(GraphQLString));
        contentFields.put(LIVE, new TypeFetcher(GraphQLBoolean));
        contentFields.put(WORKING, new TypeFetcher(GraphQLBoolean));
        contentFields.put(ARCHIVED_KEY, new TypeFetcher(GraphQLBoolean));
        contentFields.put(LOCKED_KEY, new TypeFetcher(GraphQLBoolean));
        contentFields.put("conLanguage", new TypeFetcher(GraphQLTypeReference.typeRef(LANGUAGE.getTypeName()),
                new LanguageDataFetcher()));
        contentFields.put(IDENTIFIER, new TypeFetcher(GraphQLID));
        contentFields.put(INODE, new TypeFetcher(GraphQLID));
        contentFields.put(HOST_KEY, new TypeFetcher(GraphQLTypeReference.typeRef(SITE.getTypeName()),
                new SiteFieldDataFetcher()));
        contentFields.put(FOLDER_KEY, new TypeFetcher(GraphQLTypeReference.typeRef(FOLDER.getTypeName()),
                new FolderFieldDataFetcher()));
        contentFields.put(URL_MAP, new TypeFetcher(GraphQLString, PropertyDataFetcher
                .fetching((Function<Contentlet, String>) (contentlet) ->
                        UtilMethods.isSet(contentlet.getStringProperty("urlMap"))
                                ? contentlet.getStringProperty("urlMap")
                                : contentlet.getStringProperty("URL_MAP_FOR_CONTENT"))));
        contentFields.put(OWNER_KEY, new TypeFetcher(GraphQLTypeReference.typeRef(USER.getTypeName()),
                new UserDataFetcher()));
        contentFields.put(MOD_USER_KEY, new TypeFetcher(GraphQLTypeReference.typeRef(USER.getTypeName()),
                new UserDataFetcher()));

        contentFields.put("_map", new TypeFetcher(ExtendedScalars.Json, new ContentMapDataFetcher(),
                GraphQLArgument.newArgument().name("key").type(GraphQLString).build(),
                GraphQLArgument.newArgument().name("depth").type(GraphQLInt).defaultValueProgrammatic(0).build(),
                GraphQLArgument.newArgument().name("render").type(GraphQLBoolean).defaultValueProgrammatic(null).build()));

        contentFields.put(PUBLISH_DATE_KEY, new TypeFetcher(GraphQLString, PropertyDataFetcher
                .fetching((Function<Contentlet, String>) contentlet ->
                        UtilMethods.isSet(contentlet.getStringProperty("publishDate"))
                                ? contentlet.getStringProperty("publishDate")
                                : "")));
        contentFields.put(PUBLISH_USER_KEY, new TypeFetcher(GraphQLTypeReference.typeRef(USER.getTypeName()),
                new UserDataFetcher()));
        return contentFields;
    }

}
