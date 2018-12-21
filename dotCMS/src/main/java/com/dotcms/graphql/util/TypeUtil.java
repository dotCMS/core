package com.dotcms.graphql.util;

import com.dotcms.graphql.ContentResolver;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;

import java.util.Map;

import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.TypeResolver;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

public class TypeUtil {

    public static GraphQLObjectType createObjectType(final String typeName, Map<String, GraphQLOutputType> typeFields) {
        final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(typeName);

        typeFields.keySet().forEach((key)->{
            builder.field(newFieldDefinition()
                .name(key)
                .type(typeFields.get(key))
                .dataFetcher(new FieldDataFetcher())
            );
        });

        return builder.build();
    }

    public static GraphQLInterfaceType createInterfaceType(final String typeName,
                                                           final Map<String, GraphQLOutputType> typeFields,
                                                           final TypeResolver typeResolver) {
        final GraphQLInterfaceType.Builder builder = GraphQLInterfaceType.newInterface().name(typeName);

        typeFields.keySet().forEach((key)->{
            builder.field(newFieldDefinition()
                .name(key)
                .type(typeFields.get(key))
                .dataFetcher(new FieldDataFetcher())
            );
        });

        builder.typeResolver(typeResolver);
        return builder.build();
    }
}
