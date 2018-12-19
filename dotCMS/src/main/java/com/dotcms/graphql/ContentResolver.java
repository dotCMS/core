package com.dotcms.graphql;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import graphql.schema.idl.SchemaPrinter;

public class ContentResolver implements TypeResolver {
    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
        final Contentlet contentlet = env.getObject();
//        SchemaPrinter printer = new SchemaPrinter();
//        System.out.println("SCHEMA:  " + printer.print(env.getSchema()));
        return (GraphQLObjectType) env.getSchema().getType(contentlet.getContentType().variable());
    }
}
