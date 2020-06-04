package com.dotcms.graphql.business;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLNonNull.nonNull;

import com.dotcms.contenttype.model.field.Field;
import graphql.schema.GraphQLFieldDefinition;

/**
 * This implementation generates a {@link GraphQLFieldDefinition} for any type of {@link Field}
 * except for {@link com.dotcms.contenttype.model.field.RelationshipField}
 */
class RegularContentFieldGenerator implements GraphQLFieldGenerator {

    private ContentAPIGraphQLTypesProvider typesProvider =
            ContentAPIGraphQLTypesProvider.INSTANCE;

    @Override
    public GraphQLFieldDefinition generateField(final Field field) {
        return newFieldDefinition()
                .name(field.variable())
                .type(field.required()
                        ? nonNull(typesProvider.getGraphqlTypeForFieldClass(field.type(), field))
                        : typesProvider.getGraphqlTypeForFieldClass(field.type(), field))
                .dataFetcher(typesProvider.getGraphqlDataFetcherForFieldClass(field.type())).build();
    }
}
