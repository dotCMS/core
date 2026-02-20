package com.dotcms.graphql.business;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import com.dotcms.graphql.datafetcher.FolderCollectionDataFetcher;
import com.dotmarketing.exception.DotDataException;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import java.util.Collection;
import java.util.Set;

/**
 * Provider for the {@code DotFolderByPath} GraphQL root query field.
 * Returns a single {@link FolderCollectionTypeProvider#DOT_FOLDER_COLLECTION_ITEM}
 * representing a folder at a given path, with recursive children.
 */
public enum FolderCollectionFieldProvider implements GraphQLFieldsProvider {

    INSTANCE;

    public static final String DOT_FOLDER_BY_PATH = "DotFolderByPath";

    @Override
    public Collection<GraphQLFieldDefinition> getFields() throws DotDataException {
        final GraphQLOutputType outputType = (GraphQLOutputType) FolderCollectionTypeProvider
                .INSTANCE.getTypes().iterator().next();
        return Set.of(newFieldDefinition()
                .name(DOT_FOLDER_BY_PATH)
                .argument(GraphQLArgument.newArgument()
                        .name("path")
                        .type(new GraphQLNonNull(GraphQLString))
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("site")
                        .type(GraphQLString)
                        .build())
                .type(outputType)
                .dataFetcher(new FolderCollectionDataFetcher())
                .build());
    }
}
