package com.dotcms.graphql.util;

import com.dotcms.graphql.datafetcher.FieldDataFetcher;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.PropertyDataFetcher;
import graphql.schema.TypeResolver;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

public class TypeUtil {

    public static GraphQLObjectType createObjectType(final String typeName, final Map<String, GraphQLOutputType> typeFields,
                                                     final DataFetcher dataFetcher) {
        final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(typeName);

        typeFields.keySet().forEach((key)->{
            builder.field(newFieldDefinition()
                .name(key)
                .type(typeFields.get(key))
                .dataFetcher(dataFetcher!=null?dataFetcher:new PropertyDataFetcher<String>(key))
            );
        });

        return builder.build();
    }

    public static GraphQLInterfaceType createInterfaceType(final String typeName,
                                                           final Map<String, TypeFetcher> fieldsTypesAndFetchers,
                                                           final TypeResolver typeResolver) {
        final GraphQLInterfaceType.Builder builder = GraphQLInterfaceType.newInterface().name(typeName);

        fieldsTypesAndFetchers.keySet().forEach((key)->{
            builder.field(newFieldDefinition()
                .name(key)
                .type(fieldsTypesAndFetchers.get(key).getType())
                .dataFetcher(fieldsTypesAndFetchers.get(key).getDataFetcher())
            );
        });

        builder.typeResolver(typeResolver);
        return builder.build();
    }

    public static class TypeFetcher {
        private final GraphQLOutputType type;
        private final DataFetcher dataFetcher;

        public TypeFetcher(GraphQLOutputType type) {
            this.type = type;
            this.dataFetcher = new FieldDataFetcher();
        }

        public TypeFetcher(final GraphQLOutputType type, final DataFetcher dataFetcher) {
            this.type = type;
            this.dataFetcher = dataFetcher;
        }

        public GraphQLOutputType getType() {
            return type;
        }

        public DataFetcher getDataFetcher() {
            return dataFetcher;
        }
    }
}
