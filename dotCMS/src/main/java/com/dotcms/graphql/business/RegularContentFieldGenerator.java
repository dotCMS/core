package com.dotcms.graphql.business;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLNonNull.nonNull;

import com.dotcms.contenttype.model.field.Field;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;

/**
 * This implementation generates a {@link GraphQLFieldDefinition} for any type of {@link Field}
 * except for {@link com.dotcms.contenttype.model.field.RelationshipField}
 */
class RegularContentFieldGenerator implements GraphQLFieldGenerator {

    private final ContentAPIGraphQLTypesProvider typesProvider =
            ContentAPIGraphQLTypesProvider.INSTANCE;

    @Override
    public GraphQLFieldDefinition generateField(final Field field) {
        return newFieldDefinition()
                .name(field.variable())
                .argument(GraphQLArgument.newArgument()
                        .name("render")
                        .type(GraphQLBoolean)
                        .defaultValueProgrammatic(null)
                        .build())
                .type(field.required()
                        ? nonNull(typesProvider.getGraphqlTypeForFieldClass(field.type(), field))
                        : typesProvider.getGraphqlTypeForFieldClass(field.type(), field))
                .dataFetcher(typesProvider.getGraphqlDataFetcherForFieldClass(field.type())).build();
    }
}
