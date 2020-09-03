package com.dotcms.graphql;

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
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLList.list;

import com.dotcms.graphql.datafetcher.BinaryFieldDataFetcher;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotcms.graphql.datafetcher.KeyValueFieldDataFetcher;
import com.dotcms.graphql.datafetcher.MapFieldPropertiesDataFetcher;
import com.dotcms.graphql.datafetcher.MultiValueFieldDataFetcher;
import com.dotcms.graphql.util.TypeUtil;
import com.dotcms.graphql.util.TypeUtil.TypeFetcher;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public enum CustomFieldType {
    BINARY,
    CATEGORY,
    SITE,
    FOLDER,
    SITE_OR_FOLDER,
    KEY_VALUE,
    LANGUAGE,
    USER,
    FILEASSET;

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

        final Map<String, TypeFetcher> fileAssetTypeFields = new HashMap<>();
        fileAssetTypeFields.put(FILEASSET_FILE_NAME_FIELD_VAR, new TypeFetcher(GraphQLString, new FieldDataFetcher()));
        fileAssetTypeFields.put(FILEASSET_DESCRIPTION_FIELD_VAR, new TypeFetcher(GraphQLString, new FieldDataFetcher()));
        fileAssetTypeFields.put(FILEASSET_FILEASSET_FIELD_VAR,
                new TypeFetcher(CustomFieldType.BINARY.getType(),new BinaryFieldDataFetcher()));
        fileAssetTypeFields.put(FILEASSET_METADATA_FIELD_VAR,
                new TypeFetcher(list(CustomFieldType.KEY_VALUE.getType()), new KeyValueFieldDataFetcher()));
        fileAssetTypeFields.put(FILEASSET_SHOW_ON_MENU_FIELD_VAR, new TypeFetcher(list(GraphQLString), new MultiValueFieldDataFetcher()));
        fileAssetTypeFields.put(FILEASSET_SORT_ORDER_FIELD_VAR, new TypeFetcher(GraphQLInt, new FieldDataFetcher()));
        customFieldTypes.put("FILEASSET", TypeUtil.createObjectType("Fileasset", fileAssetTypeFields));

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
        customFieldTypes.put("SITE", TypeUtil.createObjectType("Site", siteTypeFields));
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
                    customType.getName().equals(type.getName()));
        } else {
            isCustomField = getCustomFieldTypes().contains(type);
        }

        return isCustomField;
    }
}
