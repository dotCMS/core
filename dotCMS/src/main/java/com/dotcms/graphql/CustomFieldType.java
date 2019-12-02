package com.dotcms.graphql;

import com.dotcms.graphql.datafetcher.MapFieldPropertiesDataFetcher;
import com.dotcms.graphql.util.TypeUtil;

import java.util.HashMap;
import java.util.Map;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;

public enum CustomFieldType {
    BINARY,
    CATEGORY,
    SITE,
    FOLDER,
    SITE_OR_FOLDER,
    KEY_VALUE,
    LANGUAGE,
    USER;

    private static Map<String, GraphQLObjectType> customFieldTypes = new HashMap<>();

    static {
        final Map<String, GraphQLOutputType> binaryTypeFields = new HashMap<>();
        binaryTypeFields.put("versionPath", GraphQLString);
        binaryTypeFields.put("idPath", GraphQLString);
        binaryTypeFields.put("name", GraphQLString);
        binaryTypeFields.put("size", GraphQLLong);
        binaryTypeFields.put("mime", GraphQLString);
        binaryTypeFields.put("isImage", GraphQLBoolean);
        customFieldTypes.put("BINARY", TypeUtil.createObjectType("Binary", binaryTypeFields,
            new MapFieldPropertiesDataFetcher()));

        final Map<String, GraphQLOutputType> categoryTypeFields = new HashMap<>();
        categoryTypeFields.put("inode", GraphQLID);
        categoryTypeFields.put("active", GraphQLBoolean);
        categoryTypeFields.put("name", GraphQLString);
        categoryTypeFields.put("key", GraphQLString);
        categoryTypeFields.put("keywords", GraphQLString);
        categoryTypeFields.put("velocityVar", GraphQLString);
        customFieldTypes.put("CATEGORY", TypeUtil.createObjectType("Category", categoryTypeFields,
            new MapFieldPropertiesDataFetcher()));

        final Map<String, GraphQLOutputType> siteTypeFields = new HashMap<>();
        siteTypeFields.put("hostId", GraphQLString);
        siteTypeFields.put("hostName", GraphQLString);
        siteTypeFields.put("hostAliases", GraphQLString);
        siteTypeFields.put("hostTagStorage", GraphQLString);
        customFieldTypes.put("SITE", TypeUtil.createObjectType("Site", siteTypeFields,
            new MapFieldPropertiesDataFetcher()));


        final Map<String, GraphQLOutputType> folderTypeFields = new HashMap<>();
        folderTypeFields.put("folderId", GraphQLString);
        folderTypeFields.put("folderFileMask", GraphQLString);
        folderTypeFields.put("folderSortOrder", GraphQLInt);
        folderTypeFields.put("folderName", GraphQLString);
        folderTypeFields.put("folderPath", GraphQLString);
        folderTypeFields.put("folderTitle", GraphQLString);
        folderTypeFields.put("folderDefaultFileType", GraphQLString);
        customFieldTypes.put("FOLDER", TypeUtil.createObjectType("Folder", folderTypeFields,
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
        customFieldTypes.put("SITE_OR_FOLDER", TypeUtil.createObjectType("SiteOrFolder", siteOrFolderTypeFields,
            new MapFieldPropertiesDataFetcher()));

        final Map<String, GraphQLOutputType> keyValueTypeFields = new HashMap<>();
        keyValueTypeFields.put("key", GraphQLString);
        keyValueTypeFields.put("value", GraphQLString);
        customFieldTypes.put("KEY_VALUE", TypeUtil.createObjectType("KeyValue", keyValueTypeFields, null));

        final Map<String, GraphQLOutputType> languageTypeFields = new HashMap<>();
        languageTypeFields.put("id", GraphQLLong);
        languageTypeFields.put("languageCode", GraphQLString);
        languageTypeFields.put("countryCode", GraphQLString);
        languageTypeFields.put("language", GraphQLString);
        languageTypeFields.put("country", GraphQLString);
        customFieldTypes.put("LANGUAGE", TypeUtil.createObjectType("Language", languageTypeFields, null));

        final Map<String, GraphQLOutputType> userTypeFields = new HashMap<>();
        userTypeFields.put("userId", GraphQLID);
        userTypeFields.put("firstName", GraphQLString);
        userTypeFields.put("lastName", GraphQLString);
        userTypeFields.put("email", GraphQLString);
        customFieldTypes.put("USER", TypeUtil.createObjectType("User", userTypeFields, null));
    }

    public GraphQLObjectType getType() {
        return customFieldTypes.get(this.name());
    }
}
