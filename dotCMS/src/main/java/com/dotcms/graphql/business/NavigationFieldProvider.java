package com.dotcms.graphql.business;

import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import com.dotcms.graphql.datafetcher.NavigationDataFetcher;
import com.dotmarketing.exception.DotDataException;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import java.util.Collection;
import java.util.Set;

/**
 * Provider for the Navigation GraphQL field.
 */
public enum NavigationFieldProvider implements GraphQLFieldsProvider {

    INSTANCE;

    public static final String DOT_NAVIGATION = "DotNavigation";

    /**
     * Creates the GraphQL field for Navigation.
     * @return a collection of GraphQLFieldDefinition representing Navigation
     * @throws DotDataException if an error occurs while creating the field
     */
    @Override
    public Collection<GraphQLFieldDefinition> getFields() throws DotDataException {
        final GraphQLOutputType outputType = (GraphQLOutputType)NavigationTypeProvider.INSTANCE.getTypes().iterator().next();
        return Set.of(newFieldDefinition()
                .name(DOT_NAVIGATION)
                .argument(GraphQLArgument.newArgument()
                        .name("uri")
                        .type(new GraphQLNonNull(GraphQLString))
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("depth")
                        .type(GraphQLInt)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("languageId")
                        .type(GraphQLInt)
                        .build())
                .type(outputType)
                .dataFetcher(new NavigationDataFetcher()).build());
    }
}
