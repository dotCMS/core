package com.dotcms.graphql.business;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableRelationshipField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.CustomFieldType;
import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.datafetcher.BinaryFieldDataFetcher;
import com.dotcms.graphql.datafetcher.CategoryFieldDataFetcher;
import com.dotcms.graphql.datafetcher.ContentletDataFetcher;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotcms.graphql.datafetcher.RelationshipFieldDataFetcher;
import com.dotcms.graphql.event.GraphqlTypeCreatedEvent;
import com.dotcms.graphql.listener.RelationshipFieldTypeCreatedListener;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.LogTime;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.SchemaPrinter;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public class GraphqlAPIImpl implements GraphqlAPI {

    private Map<Class<? extends Field>, GraphQLOutputType> fieldClassGraphqlTypeMap = new HashMap<>();

    private Map<Class<? extends Field>, DataFetcher> fieldClassGraphqlDataFetcher = new HashMap<>();

    private final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();

    private volatile GraphQLSchema schema;

    public GraphqlAPIImpl() {
        // custom type mappings
        this.fieldClassGraphqlTypeMap.put(BinaryField.class, CustomFieldType.BINARY.getType());
        this.fieldClassGraphqlTypeMap.put(CategoryField.class, CustomFieldType.CATEGORY.getType());
        this.fieldClassGraphqlTypeMap.put(DateTimeField.class, ExtendedScalars.DateTime);
        this.fieldClassGraphqlTypeMap.put(DateField.class, ExtendedScalars.Date);
        this.fieldClassGraphqlTypeMap.put(TimeField.class, ExtendedScalars.Time);

        // custom data fetchers
        this.fieldClassGraphqlDataFetcher.put(BinaryField.class, new BinaryFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(CategoryField.class, new CategoryFieldDataFetcher());

    }

    @Override
    public GraphQLSchema getSchema() throws DotDataException {
        GraphQLSchema innerSchema = schema;
//        if(this.schema == null) {
//            synchronized (this) {
//                if(this.schema == null) {
                    this.schema = generateSchema();
//                }
//            }
//        }

        printSchema();
        return innerSchema;
    }

    private void printSchema() {
        // TODO: remove printing the schema or make it a config tool
        SchemaPrinter printer = new SchemaPrinter();
        try {
            Files.write(Paths.get("/Users/danielsilva/Documents/schema.graphqls"), printer.print(schema).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private GraphQLObjectType createSchemaType(ContentType contentType,
                                              final Map<String, GraphQLObjectType> graphqlObjectTypes) {

        final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(contentType.variable());

        // add CONTENT interface fields
        builder.fields(InterfaceType.CONTENT.getType().getFieldDefinitions());

        if(InterfaceType.getInterfaceForBaseType(contentType.baseType())!=null) {
            builder.withInterface(InterfaceType.getInterfaceForBaseType(contentType.baseType()));
        }

        final List<Field> fields = contentType.fields();

        fields.forEach((field)->{

            if(field.getClass().getName().equals(ImmutableRelationshipField.class.getName())) {
                handleRelationshipField(contentType, graphqlObjectTypes, builder, field);
            } else {
                builder.field(newFieldDefinition()
                    .name(field.variable())
                    .type(getGraphqlTypeForFieldClass(field.type()))
                    .dataFetcher(getGraphqlDataFetcherForFieldClass(field.type()))
                );
            }
        });

        builder.withInterface(InterfaceType.CONTENT.getType());
        final GraphQLObjectType graphQLType = builder.build();

        graphqlObjectTypes.put(graphQLType.getName(), graphQLType);

        localSystemEventsAPI.notify(new GraphqlTypeCreatedEvent(graphQLType));

        return graphQLType;
    }

    private void handleRelationshipField(ContentType contentType, Map<String, GraphQLObjectType> graphqlObjectTypes,
                                         GraphQLObjectType.Builder builder, Field field) {
        final ContentType relatedContentType;
        try {
            relatedContentType = getRelatedContentTypeForField(field, APILocator.systemUser());
        } catch (DotSecurityException | DotDataException e) {
            throw new DotRuntimeException("Unable to create schema type for Content Type: " + contentType.variable(), e);
        }

        // If the type exists already let's use it
        if(UtilMethods.isSet(graphqlObjectTypes.get(relatedContentType.variable()))) {
            builder.field(newFieldDefinition()
                .name(field.variable())
                .type(graphqlObjectTypes.get(relatedContentType.variable()))
                .dataFetcher(new RelationshipFieldDataFetcher())
            );
        } else { // if type is not created yet, let's listen for when the needed type gets created, so we can add the relationship field to this graphql type
            Relationship relationship;

            try {
                 relationship = APILocator.getRelationshipAPI().getRelationshipFromField(field,
                    APILocator.systemUser());
            } catch (DotDataException | DotSecurityException e) {
               throw new DotRuntimeException(e);
            }

            RelationshipFieldTypeCreatedListener relationshipFieldTypeCreatedListener =
                new RelationshipFieldTypeCreatedListener(contentType.variable(), field.variable(),
                    relatedContentType.variable(), graphqlObjectTypes, relationship.getCardinality());

            localSystemEventsAPI.subscribe(GraphqlTypeCreatedEvent.class, relationshipFieldTypeCreatedListener);

        }
    }

    private GraphQLOutputType getGraphqlTypeForFieldClass(final Class<Field> fieldClass) {
        return fieldClassGraphqlTypeMap.get(fieldClass)!= null
            ? fieldClassGraphqlTypeMap.get(fieldClass)
            : GraphQLString;
    }

    private DataFetcher getGraphqlDataFetcherForFieldClass(final Class<Field> fieldClass) {
        return fieldClassGraphqlDataFetcher.get(fieldClass)!= null
            ? fieldClassGraphqlDataFetcher.get(fieldClass)
            : new FieldDataFetcher();
    }

    @Override
    public void updateSchemaType(ContentType contentType) {

    }

    @Override
    public void deleteSchemaType(String contentTypeVar) {

    }

    @Override
    public void createSchemaTypeField(ContentType contentType, Field field) {

    }

    @Override
    public void updateSchemaTypeField(ContentType contentType, Field field) {

    }

    @Override
    public void deleteSchemaTypeField(ContentType contentType, String fieldVar) {

    }

    @LogTime(loggingLevel = "INFO")
    private GraphQLSchema generateSchema() throws DotDataException {
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        List<ContentType> allTypes = contentTypeAPI.findAll();

        // create all types
        Map<String, GraphQLObjectType> allSchemaTypes = new HashMap<>();

        allTypes.forEach((type)->createSchemaType(type, allSchemaTypes));

        // Root Type
        GraphQLObjectType rootType = newObject()
            .name("Query")
            .field(newFieldDefinition()
                .name("search")
                .argument(GraphQLArgument.newArgument()
                    .name("query")
                    .type(GraphQLString)
                    .build())
                .type(GraphQLList.list(InterfaceType.CONTENT.getType()))
                .dataFetcher(new ContentletDataFetcher()))
            .build();

        final Set<GraphQLType> graphQLTypes = new HashSet<>(InterfaceType.valuesAsSet());
        // custom scalar types
        graphQLTypes.add(ExtendedScalars.DateTime);
        // add here the rest of types
        graphQLTypes.addAll(allSchemaTypes.values());

        return new GraphQLSchema(rootType, null, graphQLTypes);
    }

    private ContentType getRelatedContentTypeForField(final Field field, final User user) throws DotSecurityException, DotDataException {
        final Relationship relationship = APILocator.getRelationshipAPI().getRelationshipFromField(field,
            user);

        final String relatedContentTypeId = relationship.getParentStructureInode().equals(field.contentTypeId())
            ? relationship.getChildStructureInode() : relationship.getParentStructureInode();

        return APILocator.getContentTypeAPI(user).find(relatedContentTypeId);
    }

}
