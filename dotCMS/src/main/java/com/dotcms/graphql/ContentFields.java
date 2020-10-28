package com.dotcms.graphql;

import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.BASE_TYPE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CONTENT_TYPE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.IDENTIFIER;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.INODE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.LIVE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.MOD_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.TITLE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.URL_MAP;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKING;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.ARCHIVED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.FOLDER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HOST_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LOCKED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.MOD_USER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.OWNER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITLE_IMAGE_KEY;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLString;

import com.dotcms.graphql.datafetcher.ContentMapDataFetcher;
import com.dotcms.graphql.datafetcher.FolderFieldDataFetcher;
import com.dotcms.graphql.datafetcher.LanguageDataFetcher;
import com.dotcms.graphql.datafetcher.SiteFieldDataFetcher;
import com.dotcms.graphql.datafetcher.TitleImageFieldDataFetcher;
import com.dotcms.graphql.datafetcher.UserDataFetcher;
import com.dotcms.graphql.util.TypeUtil.TypeFetcher;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.PropertyDataFetcher;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class that defines and returns the available fields for the {@link InterfaceType#CONTENTLET}
 */
public final class ContentFields {

    private ContentFields() {}

    public static Map<String, TypeFetcher> getContentFields() {
        final Map<String, TypeFetcher> contentFields = new HashMap<>();
        contentFields.put(MOD_DATE, new TypeFetcher(GraphQLString));
        contentFields.put(TITLE, new TypeFetcher(GraphQLString));
        contentFields.put(TITLE_IMAGE_KEY, new TypeFetcher(GraphQLTypeReference.typeRef("Binary"),
                new TitleImageFieldDataFetcher()));
        contentFields.put(CONTENT_TYPE, new TypeFetcher(GraphQLString));
        contentFields.put(BASE_TYPE, new TypeFetcher(GraphQLString));
        contentFields.put(LIVE, new TypeFetcher(GraphQLBoolean));
        contentFields.put(WORKING, new TypeFetcher(GraphQLBoolean));
        contentFields.put(ARCHIVED_KEY, new TypeFetcher(GraphQLBoolean));
        contentFields.put(LOCKED_KEY, new TypeFetcher(GraphQLBoolean));
        contentFields.put("conLanguage", new TypeFetcher(GraphQLTypeReference.typeRef("Language"),
                new LanguageDataFetcher()));
        contentFields.put(IDENTIFIER, new TypeFetcher(GraphQLID));
        contentFields.put(INODE, new TypeFetcher(GraphQLID));
        contentFields.put(HOST_KEY, new TypeFetcher(GraphQLTypeReference.typeRef("Site"),
                new SiteFieldDataFetcher()));
        contentFields.put(FOLDER_KEY, new TypeFetcher(GraphQLTypeReference.typeRef("Folder"),
                new FolderFieldDataFetcher()));
        contentFields.put(URL_MAP, new TypeFetcher(GraphQLString, PropertyDataFetcher
                .fetching((Function<Contentlet, String>) (contentlet) ->
                        UtilMethods.isSet(contentlet.getStringProperty("urlMap"))
                                ? contentlet.getStringProperty("urlMap")
                                : contentlet.getStringProperty("URL_MAP_FOR_CONTENT"))));
        contentFields.put(OWNER_KEY, new TypeFetcher(GraphQLTypeReference.typeRef("User"),
                new UserDataFetcher()));
        contentFields.put(MOD_USER_KEY, new TypeFetcher(GraphQLTypeReference.typeRef("User"),
                new UserDataFetcher()));
        contentFields.put("map", new TypeFetcher(GraphQLString, new ContentMapDataFetcher(),
                GraphQLArgument.newArgument().name("key").type(GraphQLString).build()));
        return contentFields;
    }

}
