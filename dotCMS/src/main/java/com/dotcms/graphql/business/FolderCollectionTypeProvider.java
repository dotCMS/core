package com.dotcms.graphql.business;

import static com.dotcms.graphql.util.TypeUtil.createObjectType;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;

import com.dotmarketing.util.Logger;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider for the DotFolderCollectionItem GraphQL type.
 * Creates a recursive type with a self-referencing 'children' field,
 * following the same pattern as {@link NavigationTypeProvider}.
 */
public enum FolderCollectionTypeProvider implements GraphQLTypesProvider {

    INSTANCE;

    public static final String DOT_FOLDER_COLLECTION_ITEM = "DotFolderCollectionItem";

    private Map<String, GraphQLOutputType> createFolderCollectionFields() {
        final Map<String, GraphQLOutputType> fields = new HashMap<>();
        fields.put("folderId", GraphQLString);
        fields.put("folderFileMask", GraphQLString);
        fields.put("folderSortOrder", GraphQLInt);
        fields.put("folderName", GraphQLString);
        fields.put("folderPath", GraphQLString);
        fields.put("folderTitle", GraphQLString);
        fields.put("folderDefaultFileType", GraphQLString);
        // Recursive children field using GraphQLTypeReference (same pattern as DotNavigation)
        fields.put("children", GraphQLList.list(
                new GraphQLTypeReference(DOT_FOLDER_COLLECTION_ITEM)));
        return fields;
    }

    final Map<String, GraphQLOutputType> folderCollectionFields = createFolderCollectionFields();

    final GraphQLObjectType folderCollectionType = createObjectType(
            DOT_FOLDER_COLLECTION_ITEM, folderCollectionFields, null);

    @Override
    public Collection<? extends GraphQLType> getTypes() {
        Logger.debug(this, ()->"Creating DotFolderCollectionItem types");
        return List.of(folderCollectionType);
    }
}
