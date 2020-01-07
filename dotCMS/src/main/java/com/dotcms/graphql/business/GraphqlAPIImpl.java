package com.dotcms.graphql.business;

import static com.dotcms.graphql.util.TypeUtil.BASE_TYPE_SUFFIX;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.GraphQLObjectType.newObject;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.EnterpriseType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
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
import com.dotcms.graphql.datafetcher.TagsFieldDataFetcher;
import com.dotcms.graphql.util.TypeUtil;
import com.dotcms.util.LogTime;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.idl.SchemaPrinter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GraphqlAPIImpl implements GraphqlAPI {

    private Map<Class<? extends Field>, GraphQLOutputType> fieldClassGraphqlTypeMap = new HashMap<>();

    private Map<Class<? extends Field>, DataFetcher> fieldClassGraphqlDataFetcher = new HashMap<>();

    private volatile GraphQLSchema schema;

    public static final String TYPES_AND_FIELDS_VALID_NAME_REGEX = "[_A-Za-z][_0-9A-Za-z]*";

    public GraphqlAPIImpl() {
        // custom type mappings
        this.fieldClassGraphqlTypeMap.put(BinaryField.class, CustomFieldType.BINARY.getType());
        this.fieldClassGraphqlTypeMap.put(CategoryField.class, list(CustomFieldType.CATEGORY.getType()));
        this.fieldClassGraphqlTypeMap.put(ImageField.class, InterfaceType.FILEASSET.getType());
        this.fieldClassGraphqlTypeMap.put(FileField.class, InterfaceType.FILEASSET.getType());
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
        this.fieldClassGraphqlDataFetcher.put(TagField.class, new TagsFieldDataFetcher());
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

    private void createSchemaType(ContentType contentType,
                                              final Map<String, GraphQLObjectType> graphqlObjectTypes) {

        // skip contentType.variable not sticking to the regex
        if(!contentType.variable().matches(TYPES_AND_FIELDS_VALID_NAME_REGEX)) {
            return;
        }

        final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(contentType.variable());

        // add CONTENT interface fields
        builder.fields(InterfaceType.CONTENTLET.getType().getFieldDefinitions());

        if(InterfaceType.getInterfaceForBaseType(contentType.baseType())!=null) {
            builder.withInterface(InterfaceType.getInterfaceForBaseType(contentType.baseType()));
        }

        final List<Field> fields = contentType.fields();

        fields.forEach((field)->{
            // skip field.variable not sticking to the regex
            if(!field.variable().matches(TYPES_AND_FIELDS_VALID_NAME_REGEX)
                || field instanceof RelationshipsTabField) {
                return;
            }

            if(!(field instanceof RowField) && !(field instanceof ColumnField)) {
                if (field instanceof RelationshipField) {
                    handleRelationshipField(contentType, builder, field, graphqlObjectTypes);
                } else {
                    builder.field(newFieldDefinition()
                        .name(field.variable())
                        .type(field.required()
                            ? nonNull(getGraphqlTypeForFieldClass(field.type(), field))
                            : getGraphqlTypeForFieldClass(field.type(), field))
                        .dataFetcher(getGraphqlDataFetcherForFieldClass(field.type()))
                    );
                }
            }
        });

        builder.withInterface(InterfaceType.CONTENTLET.getType());
        final GraphQLObjectType graphQLType = builder.build();

        graphqlObjectTypes.put(graphQLType.getName(), graphQLType);
    }

    private void handleRelationshipField(final ContentType contentType, GraphQLObjectType.Builder builder,
                                         final Field field, final Map<String, GraphQLObjectType> typesMap) {

        final ContentType relatedContentType;
        try {
            relatedContentType = getRelatedContentTypeForField(field, APILocator.systemUser());
        } catch (DotSecurityException | DotDataException e) {
            throw new DotRuntimeException("Unable to create schema type for Content Type: " + contentType.variable(), e);
        }

        Relationship relationship;

        try {
            relationship = APILocator.getRelationshipAPI().getRelationshipFromField(field,
                APILocator.systemUser());
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }

        final ContentletRelationships contentletRelationships = new ContentletRelationships(null);
        final ContentletRelationships.ContentletRelationshipRecords
            records = contentletRelationships.new ContentletRelationshipRecords(
            relationship,
            APILocator.getRelationshipAPI().isChildField(relationship, field));

        GraphQLOutputType outputType = typesMap.get(relatedContentType.variable()) != null
            ? typesMap.get(relatedContentType.variable())
            : GraphQLTypeReference.typeRef(relatedContentType.variable());


        outputType = records.doesAllowOnlyOne()
            ? outputType
            : list(outputType);

        builder.field(newFieldDefinition()
            .name(field.variable())
            .type(field.required()?nonNull(outputType):outputType)
            .dataFetcher(new RelationshipFieldDataFetcher())
        );
    }

    @Override
    public GraphQLOutputType getGraphqlTypeForFieldClass(final Class<? extends Field> fieldClass, final Field field) {
        return fieldClassGraphqlTypeMap.get(fieldClass)!= null
            ? fieldClassGraphqlTypeMap.get(fieldClass)
            : fieldClass.equals(TextField.class) && field.dataType().equals(DataTypes.INTEGER) ? GraphQLInt
                : fieldClass.equals(TextField.class) && field.dataType().equals(DataTypes.FLOAT) ? GraphQLFloat
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
        // exclude ee types when no license
        if(LicenseUtil.getLevel() <= LicenseLevel.COMMUNITY.level) {
            allTypes = allTypes.stream().filter((type) ->!(type instanceof EnterpriseType))
                    .collect(Collectors.toList());
        }

        // create all types
        Map<String, GraphQLObjectType> concreteTypes = new HashMap<>();

        allTypes.forEach((type)->createSchemaType(type, concreteTypes));

        final Set<GraphQLType> graphQLTypes = new HashSet<>(InterfaceType.valuesAsSet());
        // custom scalar types
        graphQLTypes.add(ExtendedScalars.DateTime);
        // add here the rest of types
        graphQLTypes.addAll(concreteTypes.values());

        // Root Type
        GraphQLObjectType.Builder rootTypeBuilder = newObject()
            .name("Query")
            .field(newFieldDefinition()
                .name("search")
                .argument(GraphQLArgument.newArgument()
                    .name("query")
                    .type(nonNull(GraphQLString))
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("limit")
                    .type(GraphQLInt)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("offset")
                    .type(GraphQLInt)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("sortBy")
                    .type(GraphQLString)
                    .build())
                .type(list(InterfaceType.CONTENTLET.getType()))
                .dataFetcher(new ContentletDataFetcher()));

        List<GraphQLFieldDefinition> typesFieldsDefinitions = new ArrayList<>();

        // each content type as query'able collection field
        graphQLTypes.forEach((type)-> {
            if(type.getName().equals(InterfaceType.CONTENTLET.getType().getName())) {
                return;
            }

            final String fieldDescription = type instanceof GraphQLInterfaceType ? BASE_TYPE_SUFFIX : null;

            typesFieldsDefinitions.add(newFieldDefinition()
                .name(TypeUtil.collectionizedName(type.getName()))
                .argument(GraphQLArgument.newArgument()
                    .name("query")
                    .type(GraphQLString)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("limit")
                    .type(GraphQLInt)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("offset")
                    .type(GraphQLInt)
                    .build())
                .argument(GraphQLArgument.newArgument()
                    .name("sortBy")
                    .type(GraphQLString)
                    .build())
                .type(list((type)))
                .description(fieldDescription)
                .dataFetcher(new ContentletDataFetcher()).build());

        });

        rootTypeBuilder = rootTypeBuilder.fields(typesFieldsDefinitions);

        return new GraphQLSchema.Builder().query(rootTypeBuilder.build()).additionalTypes(graphQLTypes).build();
    }

    private ContentType getRelatedContentTypeForField(final Field field, final User user) throws DotSecurityException, DotDataException {
        final Relationship relationship = APILocator.getRelationshipAPI().getRelationshipFromField(field,
            user);

        final String relatedContentTypeId = relationship.getParentStructureInode().equals(field.contentTypeId())
            ? relationship.getChildStructureInode() : relationship.getParentStructureInode();

        return APILocator.getContentTypeAPI(user).find(relatedContentTypeId);
    }
}
