package com.dotcms.graphql.business;

import static com.dotcms.graphql.business.PageAPIGraphQLTypesProvider.DOT_PAGE;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import com.dotcms.graphql.datafetcher.page.PageDataFetcher;
import com.dotmarketing.exception.DotDataException;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import java.util.Collection;
import java.util.Set;

/**
 * Singleton class that provides all the {@link GraphQLFieldDefinition}s needed for the Page API
 */
public enum PageAPIGraphQLFieldsProvider implements GraphQLFieldsProvider {

    INSTANCE;

    @Override
    public Collection<GraphQLFieldDefinition> getFields() throws DotDataException {

        return Set.of(newFieldDefinition()
                .name("page")
                .argument(GraphQLArgument.newArgument()
                        .name("url")
                        .type(new GraphQLNonNull(GraphQLString))
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("pageMode")
                        .type(GraphQLString)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("languageId")
                        .type(GraphQLString)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("persona")
                        .type(GraphQLString)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("fireRules")
                        .type(GraphQLBoolean)
                        .defaultValueProgrammatic(false)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("site")
                        .type(GraphQLString)
                        .build())
                .argument(GraphQLArgument.newArgument() //This is time machine
                        .name("publishDate")
                        .type(GraphQLString)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("variantName")
                        .type(GraphQLString)
                        .build())
                .type(PageAPIGraphQLTypesProvider.INSTANCE.getTypesMap().get(DOT_PAGE))
                .dataFetcher(new PageDataFetcher()).build());
    }
}
