package com.dotcms.graphql;

import com.dotcms.graphql.datafetcher.MapFieldPropertiesDataFetcher;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotcms.graphql.util.TypeUtil;

import java.util.HashMap;
import java.util.Map;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import static graphql.Scalars.GraphQLString;

public enum CustomFieldType {
    BINARY,
    CATEGORY,
    IMAGE,
    SITE_OR_FOLDER;

    private static Map<String, GraphQLObjectType> customFieldTypes = new HashMap<>();

    static {
        final Map<String, GraphQLOutputType> binaryTypeFields = new HashMap<>();
        binaryTypeFields.put("versionPath", GraphQLString);
        binaryTypeFields.put("idPath", GraphQLString);
        binaryTypeFields.put("name", GraphQLString);
        binaryTypeFields.put("size", GraphQLString);
        binaryTypeFields.put("mime", GraphQLString);
        binaryTypeFields.put("isImage", GraphQLString);

        customFieldTypes.put("BINARY", TypeUtil.createObjectType("Binary", binaryTypeFields,
            new MapFieldPropertiesDataFetcher()));

        final Map<String, GraphQLOutputType> categoryTypeFields = new HashMap<>();
        categoryTypeFields.put("inode", GraphQLString);
        categoryTypeFields.put("active", GraphQLString);
        categoryTypeFields.put("name", GraphQLString);
        categoryTypeFields.put("key", GraphQLString);
        categoryTypeFields.put("keywords", GraphQLString);
        categoryTypeFields.put("velocityVar", GraphQLString);

        customFieldTypes.put("CATEGORY", TypeUtil.createObjectType("Category", categoryTypeFields,
            new MapFieldPropertiesDataFetcher()));

        final Map<String, GraphQLOutputType> imageTypeFields = new HashMap<>();
        imageTypeFields.put("name", GraphQLString);
        imageTypeFields.put("link", GraphQLString);
        imageTypeFields.put("size", GraphQLString);
        imageTypeFields.put("width", GraphQLString);
        imageTypeFields.put("height", GraphQLString);
        imageTypeFields.put("contentType", GraphQLString);
        imageTypeFields.put("metadata", GraphQLString);

        customFieldTypes.put("IMAGE", TypeUtil.createObjectType("Image", imageTypeFields,
            new MapFieldPropertiesDataFetcher()));

        final Map<String, GraphQLOutputType> siteOrFolderTypeFields = new HashMap<>();
        // folder fields
        siteOrFolderTypeFields.put("id", GraphQLString);
        siteOrFolderTypeFields.put("fileMask", GraphQLString);
        siteOrFolderTypeFields.put("sortOrder", GraphQLString);
        siteOrFolderTypeFields.put("name", GraphQLString);
        siteOrFolderTypeFields.put("path", GraphQLString);
        siteOrFolderTypeFields.put("title", GraphQLString);
        siteOrFolderTypeFields.put("defaultFileType", GraphQLString);
        siteOrFolderTypeFields.put("aliases", GraphQLString);
        siteOrFolderTypeFields.put("tagStorage", GraphQLString);

        // site fields


        customFieldTypes.put("SITE_OR_FOLDER", TypeUtil.createObjectType("SiteOrFolder", siteOrFolderTypeFields,
            new MapFieldPropertiesDataFetcher()));
    }

    public GraphQLObjectType getType() {
        return customFieldTypes.get(this.name());
    }
}
