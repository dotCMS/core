package com.dotcms.graphql.listener;

import com.dotcms.graphql.datafetcher.RelationshipFieldDataFetcher;
import com.dotcms.graphql.event.GraphqlTypeCreatedEvent;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;

import java.util.Map;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;

public class RelationshipFieldTypeCreatedListener implements EventSubscriber<GraphqlTypeCreatedEvent> {

    private final String typeNameToListen;
    private final String graphqlTypeName;
    private final String graphqlFieldName;
    private final Map<String, GraphQLObjectType> graphqlObjectTypes;
    private final int cardinality;

    public RelationshipFieldTypeCreatedListener(final String graphqlTypeName, final String graphqlFieldName,
                                                final String typeNameToListen,
                                                final Map<String, GraphQLObjectType> graphqlObjectTypes,
                                                final int cardinality) {
        this.typeNameToListen = typeNameToListen;
        this.graphqlTypeName = graphqlTypeName;
        this.graphqlFieldName = graphqlFieldName;
        this.graphqlObjectTypes = graphqlObjectTypes;
        this.cardinality = cardinality;
    }

    @Override
    public void notify(final GraphqlTypeCreatedEvent event) {
        if(event.getType().getName().equals(typeNameToListen)) {
            // TODO: make return type an object or list based on cardinality using RelAPI method from Nolly

            final GraphQLObjectType relatedType = event.getType();
            final GraphQLObjectType typeToAddRelationshipField = graphqlObjectTypes.get(graphqlTypeName);

            final GraphQLOutputType outputType = cardinality == RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal()
                ? relatedType
                : list(relatedType);

            graphqlObjectTypes.put(graphqlTypeName,

                new GraphQLObjectType.Builder(typeToAddRelationshipField)
                    .field(newFieldDefinition()
                        .name(graphqlFieldName)
                        .type(outputType)
                        .dataFetcher(new RelationshipFieldDataFetcher())
                ).build()

            );
        }
    }
}
