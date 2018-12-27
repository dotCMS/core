package com.dotcms.graphql.listener;

import com.dotcms.graphql.datafetcher.RelationshipFieldDataFetcher;
import com.dotcms.graphql.event.GraphqlTypeCreatedEvent;
import com.dotcms.system.event.local.model.EventSubscriber;

import java.util.Map;

import graphql.schema.GraphQLObjectType;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

public class RelationshipFieldTypeCreatedListener implements EventSubscriber<GraphqlTypeCreatedEvent> {

    private final String typeNameToListen;
    private final String graphqlTypeName;
    private final String graphqlFieldName;
    private final Map<String, GraphQLObjectType> graphqlObjectTypes;

    public RelationshipFieldTypeCreatedListener(final String graphqlTypeName, final String graphqlFieldName,
                                                final String typeNameToListen,
                                                final Map<String, GraphQLObjectType> graphqlObjectTypes) {
        this.typeNameToListen = typeNameToListen;
        this.graphqlTypeName = graphqlTypeName;
        this.graphqlFieldName = graphqlFieldName;
        this.graphqlObjectTypes = graphqlObjectTypes;
    }

    @Override
    public void notify(final GraphqlTypeCreatedEvent event) {
        if(event.getType().getName().equals(typeNameToListen)) {
            final GraphQLObjectType relatedType = event.getType();
            final GraphQLObjectType typeToAddRelationshipField = graphqlObjectTypes.get(graphqlTypeName);

            graphqlObjectTypes.put(graphqlTypeName,

                new GraphQLObjectType.Builder(typeToAddRelationshipField)
                    .field(newFieldDefinition()
                        .name(graphqlFieldName)
                        .type(relatedType)
                        .dataFetcher(new RelationshipFieldDataFetcher())
                ).build()

            );
        }
    }
}
