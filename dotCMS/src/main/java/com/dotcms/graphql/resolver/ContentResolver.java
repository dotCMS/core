package com.dotcms.graphql.resolver;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;

import graphql.GraphQLException;
import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.TypeResolver;

public class ContentResolver implements TypeResolver {
    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
        final Contentlet contentlet = env.getObject();
        final GraphQLType type = env.getSchema().getType(contentlet.getContentType().variable());

        if(!UtilMethods.isSet(type)) {
            throw new GraphQLException("Type does not exist");
        }

        return (GraphQLObjectType) type;
    }
}
