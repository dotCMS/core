package com.dotcms.graphql.business;

import static com.dotcms.graphql.business.GraphqlAPI.TYPES_AND_FIELDS_VALID_NAME_REGEX;
import static com.dotcms.graphql.util.TypeUtil.BASE_TYPE_SUFFIX;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.datafetcher.ContentletDataFetcher;
import com.dotcms.graphql.util.TypeUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;

/**
 * This singleton class provides all the {@link GraphQLFieldDefinition}s needed for the Content Delivery API
 */
enum ContentAPIGraphQLFieldsProvider implements GraphQLFieldsProvider {

    INSTANCE;

    @Override
    public Collection<GraphQLFieldDefinition> getFields() throws DotDataException {

        Logger.debug(this, ()-> "Getting fields for ContentAPIGraphQLFieldsProvider");
        // Each ContentType as query'able collection field
        final List<ContentType> contentTypeList = APILocator.getContentTypeAPI(APILocator.systemUser())
                .findAllRespectingLicense();

        final List<ContentType> validContentTypeList = contentTypeList.stream().filter((type)->type.variable()
                .matches(TYPES_AND_FIELDS_VALID_NAME_REGEX)).collect(Collectors.toList());

        List<GraphQLFieldDefinition> fieldDefinitions = new ArrayList<>();

        validContentTypeList.forEach((type) -> {

            Logger.debug(this, ()-> "Creating collection field for type: " + type.variable());
            fieldDefinitions.add(createCollectionField(type, TypeUtil.collectionizedName(type.variable())));
        });

        // Each BaseType as query'able collection field
        InterfaceType.valuesAsSet().forEach((type)->{
            Logger.debug(this, ()-> "Creating collection field for BaseType: " + type.getName());
            fieldDefinitions.add(createCollectionFieldForBaseType(type));
        });

        CollectionUtils.filter(fieldDefinitions, PredicateUtils.notNullPredicate());
        return fieldDefinitions;
    }

    /**
     * Given a {@link ContentType} it creates a {@link GraphQLFieldDefinition} that represents a queryable collection for the type
     * The name of the field will follow the convention "{typeVariable}Collection".
     * <p>
     * E.g Content Type Variable: Product. Resulting field name: ProductCollection
     * @param type the type we want to build a collection for
     * @return the field definition representing the collection
     */

    private GraphQLFieldDefinition createCollectionField(final ContentType type, final String name) {
        try {

            return newFieldDefinition()
                    .name(name)
                    .argument(GraphQLArgument.newArgument()
                            .name("query")
                            .type(GraphQLString)
                            .build())
                    .argument(GraphQLArgument.newArgument()
                            .name("limit")
                            .type(GraphQLInt)
                            .build())
                    .argument(GraphQLArgument.newArgument()
                            .name("offset")
                            .type(GraphQLInt)
                            .build())
                    .argument(GraphQLArgument.newArgument()
                            .name("page")
                            .type(GraphQLInt)
                            .build())
                    .argument(GraphQLArgument.newArgument()
                            .name("sortBy")
                            .type(GraphQLString)
                            .build())
                    .type(list(ContentAPIGraphQLTypesProvider.
                            INSTANCE.getCachedTypesAsMap().get(type.variable())))
                    .dataFetcher(new ContentletDataFetcher()).build();
        } catch (DotDataException e) {
            Logger.error("Unable to generate Collection for type: " + type.variable(), e);
        }
        return null;
    }

    /**
     * Given a {@link GraphQLType} it creates a {@link GraphQLFieldDefinition} that represents a queryable collection for the type
     * The name of the field will follow the convention "{typeName}Collection".
     * <p>
     * E.g Type Name: WidgetBaseType. Resulting field name: WidgetBaseTypeCollection
     * <p>
     * This method is meant to be used for BaseTypes
     *
     * @param type the type we want to build a collection for
     * @return the field definition representing the collection
     */

    private GraphQLFieldDefinition createCollectionFieldForBaseType(GraphQLInterfaceType type) {

        if(type.getName().equals(InterfaceType.CONTENTLET.getType().getName())) {
            return null;
        }

        return newFieldDefinition()
                .name(TypeUtil.collectionizedName(type.getName()))
                .argument(GraphQLArgument.newArgument()
                        .name("query")
                        .type(GraphQLString)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("limit")
                        .type(GraphQLInt)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("offset")
                        .type(GraphQLInt)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("page")
                        .type(GraphQLInt)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("sortBy")
                        .type(GraphQLString)
                        .build())
                .type(list(type))
                .description(BASE_TYPE_SUFFIX)
                .dataFetcher(new ContentletDataFetcher()).build();
    }
}
