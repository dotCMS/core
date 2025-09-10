package com.dotcms.graphql.util;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import graphql.GraphQLException;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldDefinition.Builder;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedSchemaElement;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.PropertyDataFetcher;
import graphql.schema.TypeResolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TypeUtil {

    public static final String BASE_TYPE_SUFFIX = "BaseType";

    public static GraphQLObjectType createObjectType(final String typeName, final Map<String, GraphQLOutputType> typeFields,
                                                     final DataFetcher dataFetcher) {

        Map<String, TypeUtil.TypeFetcher> fieldsTypesAndFetchersMap = typeFields.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> new TypeFetcher(entry.getValue(), dataFetcher)));

        return createObjectType(typeName, fieldsTypesAndFetchersMap);
    }

    public static GraphQLObjectType createObjectType(final String typeName,
            final Map<String, TypeFetcher> fieldsTypesAndFetchers) {

        final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(typeName);

        List<GraphQLFieldDefinition> fieldDefinitionList = getGraphQLFieldDefinitionsFromMap(
                fieldsTypesAndFetchers);

        builder.fields(fieldDefinitionList);

        return builder.build();
    }

    public static List<GraphQLFieldDefinition> getGraphQLFieldDefinitionsFromMap(
            Map<String, TypeFetcher> fieldsTypesAndFetchers) {
        List<GraphQLFieldDefinition> fieldDefinitionList = new ArrayList<>();
        fieldsTypesAndFetchers.forEach((key, value) -> {
            try {
                Builder fieldDefinitionBuilder = newFieldDefinition();

                if(UtilMethods.isSet(value.getArguments())) {
                    fieldDefinitionBuilder
                            .name(key)
                            .arguments(value.getArguments())
                            .type(value.getType())
                            .dataFetcher(value.getDataFetcher() != null
                                    ? value.getDataFetcher()
                                    : new PropertyDataFetcher<String>(key));
                } else {
                    fieldDefinitionBuilder
                            .name(key)
                            .type(value.getType())
                            .dataFetcher(value.getDataFetcher() != null
                                    ? value.getDataFetcher()
                                    : new PropertyDataFetcher<String>(key));
                }

                fieldDefinitionBuilder.argument(GraphQLArgument.newArgument()
                        .name("render")
                        .type(GraphQLBoolean)
                        .defaultValueProgrammatic(null)
                );

                fieldDefinitionList.add(fieldDefinitionBuilder.build());
            } catch (GraphQLException e) {
                Logger.error("Error creating GraphQL Type. Type name: " + key, e);
            }
        });
        return fieldDefinitionList;
    }

    public static GraphQLInterfaceType createInterfaceType(final String typeName,
                                                           final Map<String, TypeFetcher> fieldsTypesAndFetchers,
                                                           final TypeResolver typeResolver) {
        final GraphQLInterfaceType.Builder builder = GraphQLInterfaceType.newInterface().name(typeName);

        fieldsTypesAndFetchers.forEach((key, value) -> {
            Builder fieldDefinitionBuilder = newFieldDefinition();

            if(UtilMethods.isSet(value.getArguments())) {
                fieldDefinitionBuilder
                        .name(key)
                        .arguments(value.getArguments())
                        .type(value.getType())
                        .dataFetcher(value.getDataFetcher());
            } else {
                fieldDefinitionBuilder
                        .name(key)
                        .type(value.getType())
                        .dataFetcher(value.getDataFetcher());
            }

            fieldDefinitionBuilder.argument(GraphQLArgument.newArgument()
                    .name("render")
                    .type(GraphQLBoolean)
                    .defaultValueProgrammatic(null));

            builder.field(fieldDefinitionBuilder.build());
        });

        builder.typeResolver(typeResolver);
        return builder.build();
    }

    final static String COLLECTION="Collection";

    public static String collectionizedName(final String typeName) {
        return typeName + COLLECTION;
    }

    public static String singularizeCollectionName(final String collectionName) {
        return collectionName.endsWith(COLLECTION) ? collectionName.substring(0,collectionName.lastIndexOf(COLLECTION)) : collectionName;
    }

    public static String singularizeBaseTypeCollectionName(final String baseTypeCollectionName) {
        return singularizeCollectionName(baseTypeCollectionName).replaceAll(BASE_TYPE_SUFFIX, "");
    }

    /**
     * Tries to resolve the name of the type
     * IllegalArgumentException is thrown if the type is not a GraphQLNamedSchemaElement or GraphQLObjectType
     * @param type
     * @return
     */
    public static  String getName (final GraphQLType type) {

        if (type instanceof GraphQLNamedSchemaElement) {
            return GraphQLNamedSchemaElement.class.cast(type).getName();
        }

        if (type instanceof GraphQLObjectType) {
            return GraphQLObjectType.class.cast(type).getName();
        }

        if (type instanceof GraphQLList) {
            final GraphQLType wrappedType = GraphQLList.class.cast(type).getWrappedType();
            if (wrappedType instanceof GraphQLNamedSchemaElement) {
                return GraphQLNamedSchemaElement.class.cast(wrappedType).getName();
            }

            if (wrappedType instanceof GraphQLObjectType) {
                return GraphQLObjectType.class.cast(wrappedType).getName();
            }
        }

        final String typeName = null != type ?type.getClass().getSimpleName():"NULL";
        throw new IllegalArgumentException("Type: " + typeName + " is not a GraphQLNamedSchemaElement or GraphQLObjectType");
    }

    public static class TypeFetcher {
        private final GraphQLOutputType type;
        private final DataFetcher dataFetcher;
        private final List<GraphQLArgument> arguments;

        public TypeFetcher(GraphQLOutputType type) {
            this(type, new FieldDataFetcher(), (GraphQLArgument[]) null);
        }

        public TypeFetcher(final GraphQLOutputType type, final DataFetcher dataFetcher) {
            this(type, dataFetcher, (GraphQLArgument[]) null);
        }

        public TypeFetcher(final GraphQLOutputType type, final DataFetcher dataFetcher,
                final GraphQLArgument...argument) {
            this.type = type;
            this.dataFetcher = dataFetcher;
            this.arguments = argument!=null ? Arrays.asList(argument) : Collections.emptyList();
        }

        public GraphQLOutputType getType() {
            return type;
        }

        public DataFetcher getDataFetcher() {
            return dataFetcher;
        }

        public List<GraphQLArgument> getArguments() {
            return arguments;
        }
    }
}
