package com.dotcms.graphql;

import com.dotcms.graphql.util.TypeUtil;

import java.util.HashMap;
import java.util.Map;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import static graphql.Scalars.GraphQLString;

public enum CustomFieldType {
    BINARY,
    CATEGORY;

    private static Map<String, GraphQLObjectType> customFieldTypes = new HashMap<>();

    static {
        final Map<String, GraphQLOutputType> binaryTypeFields = new HashMap<>();
        binaryTypeFields.put("versionPath", GraphQLString);
        binaryTypeFields.put("idPath", GraphQLString);
        binaryTypeFields.put("name", GraphQLString);
        binaryTypeFields.put("size", GraphQLString);
        binaryTypeFields.put("mime", GraphQLString);
        binaryTypeFields.put("isImage", GraphQLString);

        customFieldTypes.put("BINARY", TypeUtil.createObjectType("Binary", binaryTypeFields));

        final Map<String, GraphQLOutputType> categoryTypeFields = new HashMap<>();
        categoryTypeFields.put("inode", GraphQLString);
        categoryTypeFields.put("active", GraphQLString);
        categoryTypeFields.put("name", GraphQLString);
        categoryTypeFields.put("key", GraphQLString);
        categoryTypeFields.put("keywords", GraphQLString);
        categoryTypeFields.put("velocityVar", GraphQLString);

        customFieldTypes.put("CATEGORY", TypeUtil.createObjectType("Category", categoryTypeFields));
    }

    public GraphQLObjectType getType() {
        return customFieldTypes.get(this.name());
    }
}
