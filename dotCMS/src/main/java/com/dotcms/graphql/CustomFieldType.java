package com.dotcms.graphql;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.graphql.datafetcher.BinaryFieldDataFetcher;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotcms.graphql.datafetcher.KeyValueFieldDataFetcher;
import com.dotcms.graphql.datafetcher.MapFieldPropertiesDataFetcher;
import com.dotcms.graphql.datafetcher.MultiValueFieldDataFetcher;
import com.dotcms.graphql.util.TypeUtil;
import com.dotcms.graphql.util.TypeUtil.TypeFetcher;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.PropertyDataFetcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_DESCRIPTION_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_FILE_NAME_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_METADATA_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_SHOW_ON_MENU_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_SORT_ORDER_FIELD_VAR;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HOST_KEY;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.scalars.ExtendedScalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLList.list;

public enum CustomFieldType {
    BINARY("DotBinary"),
    CATEGORY("DotCategory"),
    SITE("DotSite"),
    FOLDER("DotFolder"),
    SITE_OR_FOLDER("DotSiteOrFolder"),
    KEY_VALUE("DotKeyValue"),
    LANGUAGE("DotLanguage"),
    USER("DotUser"),
    FILEASSET("DotFileasset"),
    STORY_BLOCK("DotStoryBlock");

    CustomFieldType(String typeName) {
        this.typeName = typeName;
    }

    final String typeName;

    public String getTypeName() {
        return typeName;
    }

    private static Map<String, GraphQLObjectType> customFieldTypes = new HashMap<>();

    static {
        final Map<String, GraphQLOutputType> binaryTypeFields = new HashMap<>();
        binaryTypeFields.put("versionPath", GraphQLString);
        binaryTypeFields.put("idPath", GraphQLString);
        binaryTypeFields.put("path", GraphQLString);
        binaryTypeFields.put("sha256", GraphQLString);
        binaryTypeFields.put("name", GraphQLString);
        binaryTypeFields.put("title", GraphQLString);
        binaryTypeFields.put("size", GraphQLLong);
        binaryTypeFields.put("mime", GraphQLString);
        binaryTypeFields.put("isImage", GraphQLBoolean);
        binaryTypeFields.put("width", GraphQLLong);
        binaryTypeFields.put("height", GraphQLLong);
        binaryTypeFields.put("modDate", GraphQLLong);
        binaryTypeFields.put("focalPoint", GraphQLString);
        customFieldTypes.put("BINARY", TypeUtil.createObjectType(BINARY.getTypeName(), binaryTypeFields,
            new MapFieldPropertiesDataFetcher()));

        final Map<String, GraphQLOutputType> categoryTypeFields = new HashMap<>();
        categoryTypeFields.put("inode", GraphQLID);
        categoryTypeFields.put("active", GraphQLBoolean);
        categoryTypeFields.put("name", GraphQLString);
        categoryTypeFields.put("key", GraphQLString);
        categoryTypeFields.put("keywords", GraphQLString);
        categoryTypeFields.put("velocityVar", GraphQLString);
        customFieldTypes.put("CATEGORY", TypeUtil.createObjectType(CATEGORY.getTypeName(), categoryTypeFields,
            new MapFieldPropertiesDataFetcher()));

        final Map<String, GraphQLOutputType> folderTypeFields = new HashMap<>();
        folderTypeFields.put("folderId", GraphQLString);
        folderTypeFields.put("folderFileMask", GraphQLString);
        folderTypeFields.put("folderSortOrder", GraphQLInt);
        folderTypeFields.put("folderName", GraphQLString);
        folderTypeFields.put("folderPath", GraphQLString);
        folderTypeFields.put("folderTitle", GraphQLString);
        folderTypeFields.put("folderDefaultFileType", GraphQLString);
        customFieldTypes.put("FOLDER", TypeUtil.createObjectType(FOLDER.getTypeName(), folderTypeFields,
            new MapFieldPropertiesDataFetcher()));

        final Map<String, GraphQLOutputType> siteOrFolderTypeFields = new HashMap<>();
        // folder fields
        siteOrFolderTypeFields.put("folderId", GraphQLString);
        siteOrFolderTypeFields.put("folderFileMask", GraphQLString);
        siteOrFolderTypeFields.put("folderSortOrder", GraphQLInt);
        siteOrFolderTypeFields.put("folderName", GraphQLString);
        siteOrFolderTypeFields.put("folderPath", GraphQLString);
        siteOrFolderTypeFields.put("folderTitle", GraphQLString);
        siteOrFolderTypeFields.put("folderDefaultFileType", GraphQLString);
        // site fields
        siteOrFolderTypeFields.put("hostId", GraphQLString);
        siteOrFolderTypeFields.put("hostName", GraphQLString);
        siteOrFolderTypeFields.put("hostAliases", GraphQLString);
        siteOrFolderTypeFields.put("hostTagStorage", GraphQLString);
        customFieldTypes.put("SITE_OR_FOLDER", TypeUtil.createObjectType(SITE_OR_FOLDER.getTypeName(), siteOrFolderTypeFields,
            new MapFieldPropertiesDataFetcher()));

        final Map<String, GraphQLOutputType> keyValueTypeFields = new HashMap<>();
        keyValueTypeFields.put("key", GraphQLString);
        keyValueTypeFields.put("value", GraphQLString);
        customFieldTypes.put("KEY_VALUE", TypeUtil.createObjectType(KEY_VALUE.getTypeName(), keyValueTypeFields, null));

        final Map<String, GraphQLOutputType> languageTypeFields = new HashMap<>();
        languageTypeFields.put("id", GraphQLLong);
        languageTypeFields.put("languageCode", GraphQLString);
        languageTypeFields.put("countryCode", GraphQLString);
        languageTypeFields.put("language", GraphQLString);
        languageTypeFields.put("country", GraphQLString);
        customFieldTypes.put("LANGUAGE", TypeUtil.createObjectType(LANGUAGE.getTypeName(), languageTypeFields, null));

        final Map<String, GraphQLOutputType> storyBlockTypeFields = new HashMap<>();
        storyBlockTypeFields.put("json", ExtendedScalars.Json);
        customFieldTypes.put("STORY_BLOCK", TypeUtil.createObjectType(STORY_BLOCK.getTypeName(), storyBlockTypeFields, null));

        final Map<String, GraphQLOutputType> userTypeFields = new HashMap<>();
        userTypeFields.put("userId", GraphQLID);
        userTypeFields.put("firstName", GraphQLString);
        userTypeFields.put("lastName", GraphQLString);
        userTypeFields.put("email", GraphQLString);
        customFieldTypes.put("USER", TypeUtil.createObjectType(USER.getTypeName(), userTypeFields, null));

        final Map<String, TypeFetcher> fileAssetTypeFields = new HashMap<>();
        fileAssetTypeFields.put(FILEASSET_FILE_NAME_FIELD_VAR,
                new TypeFetcher(GraphQLString, PropertyDataFetcher.fetching((Function<Contentlet, String>)
                        (contentlet)-> contentlet.getContentType().baseType()
                                == BaseContentType.FILEASSET ? ((FileAsset) contentlet).getFileName()
                                : contentlet.getName())));
        fileAssetTypeFields.put(FILEASSET_DESCRIPTION_FIELD_VAR, new TypeFetcher(GraphQLString,
                PropertyDataFetcher.fetching((Function<Contentlet, String>)
                (contentlet)-> contentlet.getContentType().baseType()
                        == BaseContentType.DOTASSET ? contentlet.getTitle()
                        : (String) contentlet.get(FILEASSET_DESCRIPTION_FIELD_VAR))));
        fileAssetTypeFields.put(FILEASSET_FILEASSET_FIELD_VAR,
                new TypeFetcher(CustomFieldType.BINARY.getType(),new BinaryFieldDataFetcher()));
        fileAssetTypeFields.put(FILEASSET_METADATA_FIELD_VAR,
                new TypeFetcher(list(CustomFieldType.KEY_VALUE.getType()), new KeyValueFieldDataFetcher()));
        fileAssetTypeFields.put(FILEASSET_SHOW_ON_MENU_FIELD_VAR, new TypeFetcher(list(GraphQLString), new MultiValueFieldDataFetcher()));
        fileAssetTypeFields.put(FILEASSET_SORT_ORDER_FIELD_VAR, new TypeFetcher(GraphQLInt, new FieldDataFetcher()));
        customFieldTypes.put("FILEASSET", TypeUtil.createObjectType(FILEASSET.getTypeName(), fileAssetTypeFields));

        final Map<String, TypeFetcher> siteTypeFields = new HashMap<>(ContentFields.getContentFields());
        siteTypeFields.remove(HOST_KEY); // remove myself
        siteTypeFields.put("hostId", new TypeFetcher(GraphQLString));
        siteTypeFields.put("hostName", new TypeFetcher(GraphQLString));
        siteTypeFields.put("hostAliases", new TypeFetcher(GraphQLString));
        siteTypeFields.put("hostTagStorage", new TypeFetcher(GraphQLString));
        siteTypeFields.put("tagStorage", new TypeFetcher(GraphQLString));
        siteTypeFields.put("aliases", new TypeFetcher(GraphQLString));
        siteTypeFields.put("isDefault", new TypeFetcher(GraphQLBoolean));
        siteTypeFields.put("hostThumbnail", new TypeFetcher(CustomFieldType.BINARY.getType(),new BinaryFieldDataFetcher()));
        siteTypeFields.put("googleMap", new TypeFetcher(GraphQLString));
        siteTypeFields.put("googleAnalytics", new TypeFetcher(GraphQLString));
        siteTypeFields.put("addThis", new TypeFetcher(GraphQLString));
        siteTypeFields.put("runDashboard", new TypeFetcher(GraphQLBoolean));
        siteTypeFields.put("keywords", new TypeFetcher(GraphQLString));
        siteTypeFields.put("description", new TypeFetcher(GraphQLString));
        siteTypeFields.put("embeddedDashboard", new TypeFetcher(GraphQLString));
        customFieldTypes.put("SITE", TypeUtil.createObjectType(SITE.getTypeName(), siteTypeFields));
    }

    public GraphQLObjectType getType() {
        return customFieldTypes.get(this.name());
    }

    public static Collection<GraphQLObjectType> getCustomFieldTypes() {
        return customFieldTypes.values();
    }

    public static boolean isCustomFieldType(final GraphQLType type) {
        boolean isCustomField = false;

        if(type instanceof GraphQLList) {
            isCustomField = getCustomFieldTypes()
                    .contains(((GraphQLList) type).getWrappedType());
        }
        else if(type instanceof GraphQLTypeReference) {
            isCustomField = getCustomFieldTypes().stream().anyMatch(customType->
                    customType.getName().equals(TypeUtil.getName(type)));
        } else {
            isCustomField = getCustomFieldTypes().contains(type);
        }

        return isCustomField;
    }
}
