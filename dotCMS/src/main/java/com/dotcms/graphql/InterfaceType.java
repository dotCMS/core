package com.dotcms.graphql;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.graphql.datafetcher.FolderFieldDataFetcher;
import com.dotcms.graphql.datafetcher.LanguageDataFetcher;
import com.dotcms.graphql.datafetcher.SiteFieldDataFetcher;
import com.dotcms.graphql.datafetcher.TitleImageFieldDataFetcher;
import com.dotcms.graphql.datafetcher.UserDataFetcher;
import com.dotcms.graphql.resolver.ContentResolver;
import com.dotmarketing.util.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLInterfaceType;

import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.BASE_TYPE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CONTENT_TYPE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.IDENTIFIER;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.INODE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.LIVE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.MOD_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.TITLE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.URL_MAP;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKING;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_DESCRIPTION_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_FILE_NAME_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_METADATA_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_SHOW_ON_MENU_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_SORT_ORDER_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_TITLE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FormContentType.FORM_EMAIL_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FormContentType.FORM_RETURN_PAGE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FormContentType.FORM_TITLE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR;
import static com.dotcms.contenttype.model.type.KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_CACHE_TTL_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_FRIENDLY_NAME_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_HTTP_REQUIRED_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_PAGE_METADATA_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_REDIRECT_URL_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_SEO_DESCRIPTION_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_SEO_KEYWORDS_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_SHOW_ON_MENU_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_SORT_ORDER_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_TEMPLATE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_URL_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PersonaContentType.PERSONA_DESCRIPTION_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PersonaContentType.PERSONA_KEY_TAG_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PersonaContentType.PERSONA_NAME_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PersonaContentType.PERSONA_OTHER_TAGS_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PersonaContentType.PERSONA_PHOTO_FIELD_VAR;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.ACTION_FIELD_VAR;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.FORWARD_TO_FIELD_VAR;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.ORDER_FIELD_VAR;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.URI_FIELD_VAR;
import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_CODE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_PRE_EXECUTE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_TITLE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_USAGE_FIELD_VAR;
import static com.dotcms.graphql.util.TypeUtil.TypeFetcher;
import static com.dotcms.graphql.util.TypeUtil.createInterfaceType;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.ARCHIVED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.FOLDER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HOST_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LOCKED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.MOD_USER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.OWNER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITLE_IMAGE_KEY;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLList.list;

public enum InterfaceType {
    CONTENTLET,
    CONTENT,
    FILEASSET,
    HTMLPAGE,
    PERSONA,
    WIDGET,
    VANITY_URL,
    KEY_VALUE,
    FORM;

    private static Map<String, GraphQLInterfaceType> interfaceTypes = new HashMap<>();

    static {

        final Map<String, TypeFetcher> contentFields = new HashMap<>();
        contentFields.put(MOD_DATE, new TypeFetcher(GraphQLString));
        contentFields.put(TITLE, new TypeFetcher(GraphQLString));
        contentFields.put(TITLE_IMAGE_KEY, new TypeFetcher(CustomFieldType.BINARY.getType(), new TitleImageFieldDataFetcher()));
        contentFields.put(CONTENT_TYPE, new TypeFetcher(GraphQLString));
        contentFields.put(BASE_TYPE, new TypeFetcher(GraphQLString));
        contentFields.put(LIVE, new TypeFetcher(GraphQLBoolean));
        contentFields.put(WORKING, new TypeFetcher(GraphQLBoolean));
        contentFields.put(ARCHIVED_KEY, new TypeFetcher(GraphQLBoolean));
        contentFields.put(LOCKED_KEY, new TypeFetcher(GraphQLBoolean));
        contentFields.put("conLanguage", new TypeFetcher(CustomFieldType.LANGUAGE.getType(), new LanguageDataFetcher()));
        contentFields.put(IDENTIFIER, new TypeFetcher(GraphQLID));
        contentFields.put(INODE, new TypeFetcher(GraphQLID));
        contentFields.put(HOST_KEY, new TypeFetcher(CustomFieldType.SITE.getType(), new SiteFieldDataFetcher()));
        contentFields.put(FOLDER_KEY, new TypeFetcher(CustomFieldType.FOLDER.getType(), new FolderFieldDataFetcher()));
        contentFields.put(URL_MAP, new TypeFetcher(GraphQLString));
        contentFields.put(OWNER_KEY, new TypeFetcher(CustomFieldType.USER.getType(), new UserDataFetcher()));
        contentFields.put(MOD_USER_KEY, new TypeFetcher(CustomFieldType.USER.getType(), new UserDataFetcher()));

        interfaceTypes.put("CONTENTLET", createInterfaceType("Contentlet", contentFields, new ContentResolver()));


        interfaceTypes.put("CONTENT", createInterfaceType("ContentBaseType", contentFields, new ContentResolver()));

        final Map<String, TypeFetcher> fileAssetFields = new HashMap<>(contentFields);
        fileAssetFields.put(FILEASSET_FILE_NAME_FIELD_VAR, new TypeFetcher(GraphQLString));
        fileAssetFields.put(FILEASSET_DESCRIPTION_FIELD_VAR, new TypeFetcher(GraphQLString));
        fileAssetFields.put(FILEASSET_FILEASSET_FIELD_VAR, new TypeFetcher(CustomFieldType.BINARY.getType()));
        fileAssetFields.put(FILEASSET_METADATA_FIELD_VAR, new TypeFetcher(list(CustomFieldType.KEY_VALUE.getType())));
        fileAssetFields.put(FILEASSET_SHOW_ON_MENU_FIELD_VAR, new TypeFetcher(list(GraphQLString)));
        fileAssetFields.put(FILEASSET_SORT_ORDER_FIELD_VAR, new TypeFetcher(GraphQLInt));

        interfaceTypes.put("FILEASSET", createInterfaceType("FileBaseType", fileAssetFields, new ContentResolver()));

        final Map<String, TypeFetcher> pageAssetFields = new HashMap<>(contentFields);
        pageAssetFields.put(PAGE_URL_FIELD_VAR, new TypeFetcher(GraphQLString));
        pageAssetFields.put(PAGE_TEMPLATE_FIELD_VAR, new TypeFetcher(GraphQLString));
        pageAssetFields.put(PAGE_SHOW_ON_MENU_FIELD_VAR, new TypeFetcher(list(GraphQLString)));
        pageAssetFields.put(PAGE_SORT_ORDER_FIELD_VAR, new TypeFetcher(GraphQLInt));
        pageAssetFields.put(PAGE_CACHE_TTL_FIELD_VAR, new TypeFetcher(GraphQLString));
        pageAssetFields.put(PAGE_FRIENDLY_NAME_FIELD_VAR, new TypeFetcher(GraphQLString));
        pageAssetFields.put(PAGE_REDIRECT_URL_FIELD_VAR, new TypeFetcher(GraphQLString));
        pageAssetFields.put(PAGE_HTTP_REQUIRED_FIELD_VAR, new TypeFetcher(list(GraphQLString)));
        pageAssetFields.put(PAGE_SEO_DESCRIPTION_FIELD_VAR, new TypeFetcher(GraphQLString));
        pageAssetFields.put(PAGE_SEO_KEYWORDS_FIELD_VAR, new TypeFetcher(GraphQLString));
        pageAssetFields.put(PAGE_PAGE_METADATA_FIELD_VAR, new TypeFetcher(GraphQLString));

        interfaceTypes.put("HTMLPAGE", createInterfaceType("PageBaseType", pageAssetFields, new ContentResolver()));

        final Map<String, TypeFetcher> personaFields = new HashMap<>(contentFields);
        personaFields.put(PERSONA_NAME_FIELD_VAR, new TypeFetcher(GraphQLString));
        personaFields.put(PERSONA_KEY_TAG_FIELD_VAR, new TypeFetcher(GraphQLString));
        personaFields.put(PERSONA_PHOTO_FIELD_VAR, new TypeFetcher(CustomFieldType.BINARY.getType()));
        personaFields.put(PERSONA_OTHER_TAGS_FIELD_VAR, new TypeFetcher(list(GraphQLString)));
        personaFields.put(PERSONA_DESCRIPTION_FIELD_VAR, new TypeFetcher(GraphQLString));

        interfaceTypes.put("PERSONA", createInterfaceType("PersonaBaseType", personaFields, new ContentResolver()));

        final Map<String, TypeFetcher> widgetFields = new HashMap<>(contentFields);
        widgetFields.put(WIDGET_TITLE_FIELD_VAR, new TypeFetcher(GraphQLString));
        widgetFields.put(WIDGET_CODE_FIELD_VAR, new TypeFetcher(GraphQLString));
        widgetFields.put(WIDGET_USAGE_FIELD_VAR, new TypeFetcher(GraphQLString));
        widgetFields.put(WIDGET_PRE_EXECUTE_FIELD_VAR, new TypeFetcher(GraphQLString));

        interfaceTypes.put("WIDGET", createInterfaceType("WidgetBaseType", widgetFields, new ContentResolver()));

        final Map<String, TypeFetcher> vanityUrlFields = new HashMap<>(contentFields);
        vanityUrlFields.put(URI_FIELD_VAR, new TypeFetcher(GraphQLString));
        vanityUrlFields.put(FORWARD_TO_FIELD_VAR, new TypeFetcher(GraphQLString));
        vanityUrlFields.put(ACTION_FIELD_VAR, new TypeFetcher(GraphQLString));
        vanityUrlFields.put(ORDER_FIELD_VAR, new TypeFetcher(GraphQLInt));

        interfaceTypes.put("VANITY_URL", createInterfaceType("VanityURLBaseType", vanityUrlFields, new ContentResolver()));

        final Map<String, TypeFetcher> keyValueFields = new HashMap<>(contentFields);
        keyValueFields.put(KEY_VALUE_KEY_FIELD_VAR, new TypeFetcher(GraphQLString));
        keyValueFields.put(KEY_VALUE_VALUE_FIELD_VAR, new TypeFetcher(GraphQLString));

        interfaceTypes.put("KEY_VALUE", createInterfaceType("KeyValueBaseType", keyValueFields, new ContentResolver()));

        final Map<String, TypeFetcher> formFields = new HashMap<>(contentFields);
        formFields.put(FORM_TITLE_FIELD_VAR, new TypeFetcher(GraphQLString));
        formFields.put(FORM_EMAIL_FIELD_VAR, new TypeFetcher(GraphQLString));
        formFields.put(FORM_RETURN_PAGE_FIELD_VAR, new TypeFetcher(GraphQLString));

        interfaceTypes.put("FORM", createInterfaceType("FormBaseType", formFields, new ContentResolver()));
    }

    public GraphQLInterfaceType getType() {
        return interfaceTypes.get(this.name());
    }

    public static Set<GraphQLInterfaceType> valuesAsSet() {
        final Set<GraphQLInterfaceType> types = new HashSet<>();

        for(final InterfaceType type : InterfaceType.values()) {
            if(type.getType()!=null) {
                types.add(type.getType());
            }
        }

        return types;
    }

    public static GraphQLInterfaceType getInterfaceForBaseType(final BaseContentType baseContentType) {
        GraphQLInterfaceType type = null;
        try {
            type = InterfaceType.valueOf(baseContentType.name()).getType();
        } catch (IllegalArgumentException e) {
            Logger.debug(InterfaceType.class, "No GraphQL Interface for this base type: " + baseContentType.name());
        }

        return type;
    }

}
