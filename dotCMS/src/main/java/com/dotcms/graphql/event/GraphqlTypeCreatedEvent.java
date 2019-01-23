package com.dotcms.graphql.event;

import graphql.schema.GraphQLObjectType;

public class GraphqlTypeCreatedEvent {
    private GraphQLObjectType type;

    public GraphqlTypeCreatedEvent(GraphQLObjectType type) {
        this.type = type;
    }

    public GraphQLObjectType getType() {
        return type;
    }
}
