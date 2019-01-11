package com.dotcms.graphql.business;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.CustomFieldType;
import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.datafetcher.BinaryFieldDataFetcher;
import com.dotcms.graphql.datafetcher.CategoryFieldDataFetcher;
import com.dotcms.graphql.datafetcher.ContentletDataFetcher;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotcms.graphql.datafetcher.FileFieldDataFetcher;
import com.dotcms.graphql.datafetcher.KeyValueFieldDataFetcher;
import com.dotcms.graphql.datafetcher.MultiValueFieldDataFetcher;
import com.dotcms.graphql.datafetcher.RelationshipFieldDataFetcher;
import com.dotcms.graphql.datafetcher.SiteOrFolderFieldDataFetcher;
import com.dotcms.graphql.event.GraphqlTypeCreatedEvent;
import com.dotcms.graphql.listener.RelationshipFieldTypeCreatedListener;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.LogTime;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.SchemaPrinter;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLObjectType.newObject;

public class GraphqlAPIImpl implements GraphqlAPI {

    private Map<Class<? extends Field>, GraphQLOutputType> fieldClassGraphqlTypeMap = new HashMap<>();

    private Map<Class<? extends Field>, DataFetcher> fieldClassGraphqlDataFetcher = new HashMap<>();

    private final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();

    private volatile GraphQLSchema schema;

    public GraphqlAPIImpl() {
        // custom type mappings
        this.fieldClassGraphqlTypeMap.put(BinaryField.class, CustomFieldType.BINARY.getType());
        this.fieldClassGraphqlTypeMap.put(CategoryField.class, list(CustomFieldType.CATEGORY.getType()));
        this.fieldClassGraphqlTypeMap.put(ImageField.class, InterfaceType.FILEASSET.getType());
        this.fieldClassGraphqlTypeMap.put(FileField.class, InterfaceType.FILEASSET.getType());
        this.fieldClassGraphqlTypeMap.put(DateTimeField.class, ExtendedScalars.DateTime);
        this.fieldClassGraphqlTypeMap.put(DateField.class, ExtendedScalars.Date);
        this.fieldClassGraphqlTypeMap.put(TimeField.class, ExtendedScalars.Time);
        this.fieldClassGraphqlTypeMap.put(KeyValueField.class, list(CustomFieldType.KEY_VALUE.getType()));
        this.fieldClassGraphqlTypeMap.put(CheckboxField.class, list(GraphQLString));
        this.fieldClassGraphqlTypeMap.put(MultiSelectField.class, list(GraphQLString));
        this.fieldClassGraphqlTypeMap.put(TagField.class, list(GraphQLString));
        this.fieldClassGraphqlTypeMap.put(HostFolderField.class, CustomFieldType.SITE_OR_FOLDER.getType());

        // custom data fetchers
        this.fieldClassGraphqlDataFetcher.put(BinaryField.class, new BinaryFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(CategoryField.class, new CategoryFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(ImageField.class, new FileFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(FileField.class, new FileFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(KeyValueField.class, new KeyValueFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(CheckboxField.class, new MultiValueFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(MultiSelectField.class, new MultiValueFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(TagField.class, new MultiValueFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(HostFolderField.class, new SiteOrFolderFieldDataFetcher());

    }

    @Override
    public GraphQLSchema getSchema() throws DotDataException {
        GraphQLSchema innerSchema = this.schema;
        if(innerSchema == null) {
            synchronized (this) {
                innerSchema = this.schema;
                if(innerSchema == null) {
                    this.schema = innerSchema = generateSchema();
                }
            }
        }

        printSchema();
        return innerSchema;
    }

    @Override
    public void invalidateSchema() {
        this.schema = null;
    }

    private void printSchema() {
        if (Config.getBooleanProperty("PRINT_GRAPHQL_SCHEMA", false)) {
            SchemaPrinter printer = new SchemaPrinter();
            try {
                File graphqlDirectory = new File(ConfigUtils.getGraphqlPath());

                if(!graphqlDirectory.exists()) {
                    graphqlDirectory.mkdirs();
                }

                File schemaFile = new File(graphqlDirectory.getPath() + File.separator + "schema.graphqls");
                schemaFile.createNewFile();
                Files.write(schemaFile.toPath(), printer.print(schema).getBytes());
            } catch (IOException e) {
                Logger.error(this, "Error printing schema", e);
            }
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
            if(!(field instanceof RowField) && !(field instanceof ColumnField)) {
                if (field instanceof RelationshipField) {
                    handleRelationshipField(contentType, graphqlObjectTypes, builder, field);
                } else {
                    builder.field(newFieldDefinition()
                        .name(field.variable())
                        .type(getGraphqlTypeForFieldClass(field.type()))
                        .dataFetcher(getGraphqlDataFetcherForFieldClass(field.type()))
                    );
                }
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
                .type(list(InterfaceType.CONTENT.getType()))
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
