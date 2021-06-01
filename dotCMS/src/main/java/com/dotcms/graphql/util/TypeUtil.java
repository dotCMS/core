package com.dotcms.graphql.util;

import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;

import com.dotcms.util.DotPreconditions;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import graphql.GraphQLException;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldDefinition.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.PropertyDataFetcher;
import graphql.schema.TypeResolver;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

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

                if(value.getArgument()!=null) {
                    fieldDefinitionBuilder
                            .name(key)
                            .argument(value.getArgument())
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
                        .defaultValue(false));

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

            if(value.getArgument()!=null) {
                fieldDefinitionBuilder
                        .name(key)
                        .argument(value.getArgument())
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
                    .defaultValue(false));

            builder.field(fieldDefinitionBuilder.build());
        });

        builder.typeResolver(typeResolver);
        return builder.build();
    }

    public static String collectionizedName(final String typeName) {
        return typeName + "Collection";
    }

    public static String oldCollectionizedName(final String typeName) {
        return typeName.substring(0, 1).toLowerCase() + typeName.substring(1) + "Collection";
    }

    public static String singularizeCollectionName(final String collectionName) {
        return collectionName.replaceAll("Collection", "");
    }

    public static String singularizeBaseTypeCollectionName(final String baseTypeCollectionName) {
        return singularizeCollectionName(baseTypeCollectionName).replaceAll(BASE_TYPE_SUFFIX, "");
    }

    public static class TypeFetcher {
        private final GraphQLOutputType type;
        private final DataFetcher dataFetcher;
        private final GraphQLArgument argument;

        public TypeFetcher(GraphQLOutputType type) {
            this(type, new FieldDataFetcher(), null);
        }

        public TypeFetcher(final GraphQLOutputType type, final DataFetcher dataFetcher) {
            this(type, dataFetcher, null);
        }

        public TypeFetcher(final GraphQLOutputType type, final DataFetcher dataFetcher,
                final GraphQLArgument argument) {
            this.type = type;
            this.dataFetcher = dataFetcher;
            this.argument = argument;
        }

        public GraphQLOutputType getType() {
            return type;
        }

        public DataFetcher getDataFetcher() {
            return dataFetcher;
        }

        public GraphQLArgument getArgument() {
            return argument;
        }
    }
}
