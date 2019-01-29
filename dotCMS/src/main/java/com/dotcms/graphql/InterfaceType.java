package com.dotcms.graphql;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.graphql.resolver.ContentResolver;
import com.dotcms.graphql.util.TypeUtil;
import com.dotmarketing.util.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLOutputType;

import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.BASE_TYPE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CATEGORIES;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CONTENTLET_FOLER;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CONTENTLET_HOST;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CONTENT_TYPE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.DELETED;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.EXPIRE_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.IDENTIFIER;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.INODE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.LANGUAGE_ID;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.LIVE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.LOCKED;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.MOD_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.PARENT_PATH;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.PATH;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.PUBLISH_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.TITLE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.URL_MAP;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKFLOW_ASSIGN;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKFLOW_CREATED_BY;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKFLOW_MOD_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKFLOW_STEP;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKING;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_DESCRIPTION_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_FILE_NAME_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_METADATA_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_SHOW_ON_MENU_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_SITE_OR_FOLDER_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_SORT_ORDER_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_TITLE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FormContentType.FORM_EMAIL_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FormContentType.FORM_HOST_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FormContentType.FORM_RETURN_PAGE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FormContentType.FORM_TITLE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR;
import static com.dotcms.contenttype.model.type.KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_CACHE_TTL_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_FRIENDLY_NAME_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_HOST_FOLDER_FIELD_VAR;
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
import static com.dotcms.contenttype.model.type.PersonaContentType.PERSONA_HOST_FOLDER_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PersonaContentType.PERSONA_KEY_TAG_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PersonaContentType.PERSONA_NAME_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PersonaContentType.PERSONA_OTHER_TAGS_FIELD_VAR;
import static com.dotcms.contenttype.model.type.PersonaContentType.PERSONA_PHOTO_FIELD_VAR;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.ACTION_FIELD_VAR;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.FORWARD_TO_FIELD_VAR;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.ORDER_FIELD_VAR;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.SITE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.URI_FIELD_VAR;
import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_CODE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_PRE_EXECUTE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_TITLE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_USAGE_FIELD_VAR;
import static com.dotcms.graphql.util.TypeUtil.createInterfaceType;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLList.list;

public enum InterfaceType {
    CONTENTLET,
    FILEASSET,
    HTMLPAGE,
    PERSONA,
    WIDGET,
    VANITY_URL,
    KEY_VALUE,
    FORM;

    private static Map<String, GraphQLInterfaceType> interfaceTypes = new HashMap<>();

    static {

        final Map<String, GraphQLOutputType> contentFields = new HashMap<>();
        contentFields.put(MOD_DATE, GraphQLString);
        contentFields.put(TITLE, GraphQLString);
        contentFields.put(CONTENT_TYPE, GraphQLString);
        contentFields.put(BASE_TYPE, GraphQLString);
        contentFields.put(LIVE, GraphQLBoolean);
        contentFields.put(WORKING, GraphQLBoolean);
        contentFields.put(DELETED, GraphQLBoolean);
        contentFields.put(LOCKED, GraphQLBoolean);
        contentFields.put(LANGUAGE_ID, GraphQLBoolean);
        contentFields.put(IDENTIFIER, GraphQLID);
        contentFields.put(INODE, GraphQLID);
        contentFields.put(CONTENTLET_HOST, CustomFieldType.SITE_OR_FOLDER.getType());
        contentFields.put(CONTENTLET_FOLER, CustomFieldType.SITE_OR_FOLDER.getType());
        contentFields.put(PARENT_PATH, GraphQLString);
        contentFields.put(PATH, GraphQLString);
        contentFields.put(WORKFLOW_CREATED_BY, GraphQLString);
        contentFields.put(WORKFLOW_ASSIGN, GraphQLString);
        contentFields.put(WORKFLOW_STEP, GraphQLString);
        contentFields.put(WORKFLOW_MOD_DATE, ExtendedScalars.DateTime);
        contentFields.put(PUBLISH_DATE, ExtendedScalars.DateTime);
        contentFields.put(EXPIRE_DATE, ExtendedScalars.DateTime);
        contentFields.put(URL_MAP, GraphQLString);
        contentFields.put(CATEGORIES, CustomFieldType.CATEGORY.getType());

        interfaceTypes.put("CONTENTLET", createInterfaceType("Contentlet", contentFields, new ContentResolver()));

        final Map<String, GraphQLOutputType> fileAssetFields = new HashMap<>(contentFields);
        fileAssetFields.put(FILEASSET_FILE_NAME_FIELD_VAR, GraphQLString);
        fileAssetFields.put(FILEASSET_DESCRIPTION_FIELD_VAR, GraphQLString);
        fileAssetFields.put(FILEASSET_FILEASSET_FIELD_VAR, CustomFieldType.BINARY.getType());
        fileAssetFields.put(FILEASSET_TITLE_FIELD_VAR, GraphQLString);
        fileAssetFields.put(FILEASSET_METADATA_FIELD_VAR, list(CustomFieldType.KEY_VALUE.getType()));
        fileAssetFields.put(FILEASSET_SITE_OR_FOLDER_FIELD_VAR, GraphQLString);
        fileAssetFields.put(FILEASSET_SHOW_ON_MENU_FIELD_VAR, GraphQLBoolean);
        fileAssetFields.put(FILEASSET_SORT_ORDER_FIELD_VAR, GraphQLInt);

        interfaceTypes.put("FILEASSET", createInterfaceType("Fileasset", fileAssetFields, new ContentResolver()));

        final Map<String, GraphQLOutputType> pageAssetFields = new HashMap<>(contentFields);
        pageAssetFields.put(PAGE_URL_FIELD_VAR, GraphQLString);
        pageAssetFields.put(PAGE_HOST_FOLDER_FIELD_VAR, CustomFieldType.SITE_OR_FOLDER.getType());
        pageAssetFields.put(PAGE_TEMPLATE_FIELD_VAR, GraphQLString);
        pageAssetFields.put(PAGE_SHOW_ON_MENU_FIELD_VAR, GraphQLBoolean);
        pageAssetFields.put(PAGE_SORT_ORDER_FIELD_VAR, GraphQLInt);
        pageAssetFields.put(PAGE_CACHE_TTL_FIELD_VAR, GraphQLInt);
        pageAssetFields.put(PAGE_FRIENDLY_NAME_FIELD_VAR, GraphQLString);
        pageAssetFields.put(PAGE_REDIRECT_URL_FIELD_VAR, GraphQLString);
        pageAssetFields.put(PAGE_HTTP_REQUIRED_FIELD_VAR, GraphQLBoolean);
        pageAssetFields.put(PAGE_SEO_DESCRIPTION_FIELD_VAR, GraphQLString);
        pageAssetFields.put(PAGE_SEO_KEYWORDS_FIELD_VAR, GraphQLString);
        pageAssetFields.put(PAGE_PAGE_METADATA_FIELD_VAR, GraphQLString);

        interfaceTypes.put("HTMLPAGE", createInterfaceType("Htmlpage", pageAssetFields, new ContentResolver()));

        final Map<String, GraphQLOutputType> personaFields = new HashMap<>(contentFields);
        personaFields.put(PERSONA_HOST_FOLDER_FIELD_VAR, CustomFieldType.SITE_OR_FOLDER.getType());
        personaFields.put(PERSONA_NAME_FIELD_VAR, GraphQLString);
        personaFields.put(PERSONA_KEY_TAG_FIELD_VAR, GraphQLString);
        personaFields.put(PERSONA_PHOTO_FIELD_VAR, CustomFieldType.BINARY.getType());
        personaFields.put(PERSONA_OTHER_TAGS_FIELD_VAR, GraphQLString);
        personaFields.put(PERSONA_DESCRIPTION_FIELD_VAR, GraphQLString);

        interfaceTypes.put("PERSONA", createInterfaceType("Persona", personaFields, new ContentResolver()));

        final Map<String, GraphQLOutputType> widgetFields = new HashMap<>(contentFields);
        widgetFields.put(WIDGET_TITLE_FIELD_VAR, GraphQLString);
        widgetFields.put(WIDGET_CODE_FIELD_VAR, GraphQLString);
        widgetFields.put(WIDGET_USAGE_FIELD_VAR, GraphQLString);
        widgetFields.put(WIDGET_PRE_EXECUTE_FIELD_VAR, GraphQLString);

        interfaceTypes.put("WIDGET", createInterfaceType("Widget", widgetFields, new ContentResolver()));

        final Map<String, GraphQLOutputType> vanityUrlFields = new HashMap<>(contentFields);
        vanityUrlFields.put(SITE_FIELD_VAR, CustomFieldType.SITE_OR_FOLDER.getType());
        vanityUrlFields.put(URI_FIELD_VAR, GraphQLString);
        vanityUrlFields.put(FORWARD_TO_FIELD_VAR, GraphQLString);
        vanityUrlFields.put(ACTION_FIELD_VAR, GraphQLString);
        vanityUrlFields.put(ORDER_FIELD_VAR, GraphQLInt);

        interfaceTypes.put("VANITY_URL", createInterfaceType("Vanity_url", vanityUrlFields, new ContentResolver()));

        final Map<String, GraphQLOutputType> keyValueFields = new HashMap<>(contentFields);
        keyValueFields.put(KEY_VALUE_KEY_FIELD_VAR, GraphQLString);
        keyValueFields.put(KEY_VALUE_VALUE_FIELD_VAR, GraphQLString);

        interfaceTypes.put("KEY_VALUE", createInterfaceType("Key_value", keyValueFields, new ContentResolver()));

        final Map<String, GraphQLOutputType> formFields = new HashMap<>(contentFields);
        formFields.put(FORM_TITLE_FIELD_VAR, GraphQLString);
        formFields.put(FORM_EMAIL_FIELD_VAR, GraphQLString);
        formFields.put(FORM_RETURN_PAGE_FIELD_VAR, GraphQLString);
        formFields.put(FORM_HOST_FIELD_VAR, CustomFieldType.SITE_OR_FOLDER.getType());

        interfaceTypes.put("FORM", createInterfaceType("Form", formFields, new ContentResolver()));
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
