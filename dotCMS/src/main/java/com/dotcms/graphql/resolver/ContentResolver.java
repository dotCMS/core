package com.dotcms.graphql.resolver;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;

public class ContentResolver implements TypeResolver {
    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
        final Contentlet contentlet = env.getObject();
        return (GraphQLObjectType) env.getSchema().getType(contentlet.getContentType().variable());
    }
}
