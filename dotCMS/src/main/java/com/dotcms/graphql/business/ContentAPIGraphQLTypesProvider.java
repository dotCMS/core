package com.dotcms.graphql.business;

import static com.dotcms.contenttype.model.type.WidgetContentType.WIDGET_CODE_JSON_FIELD_VAR;
import static com.dotcms.graphql.CustomFieldType.isCustomFieldType;
import static com.dotcms.graphql.business.GraphqlAPI.TYPES_AND_FIELDS_VALID_NAME_REGEX;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLList.list;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.JSONField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.ContentFields;
import com.dotcms.graphql.CustomFieldType;
import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.datafetcher.BinaryFieldDataFetcher;
import com.dotcms.graphql.datafetcher.CategoryFieldDataFetcher;
import com.dotcms.graphql.datafetcher.DotJSONDataFetcher;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotcms.graphql.datafetcher.FileFieldDataFetcher;
import com.dotcms.graphql.datafetcher.JSONFieldDataFetcher;
import com.dotcms.graphql.datafetcher.KeyValueFieldDataFetcher;
import com.dotcms.graphql.datafetcher.MultiValueFieldDataFetcher;
import com.dotcms.graphql.datafetcher.SiteOrFolderFieldDataFetcher;
import com.dotcms.graphql.datafetcher.StoryBlockFieldDataFetcher;
import com.dotcms.graphql.datafetcher.TagsFieldDataFetcher;
import com.dotcms.graphql.exception.FieldGenerationException;
import com.dotcms.graphql.util.TypeUtil;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.JsonUtil;
import com.dotcms.util.LowerKeyMap;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNamedSchemaElement;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.PropertyDataFetcher;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This singleton class provides all the {@link GraphQLType}s needed for the Content Delivery API
 */
public enum ContentAPIGraphQLTypesProvider implements GraphQLTypesProvider {

    INSTANCE;

    private GraphQLFieldGeneratorFactory fieldGeneratorFactory = new GraphQLFieldGeneratorFactory();

    private final Map<Class<? extends Field>, GraphQLOutputType> fieldClassGraphqlTypeMap = new HashMap<>();

    private final Map<Class<? extends Field>, DataFetcher> fieldClassGraphqlDataFetcher = new HashMap<>();

    private final Map<String, GraphQLType> typesMap = new HashMap<>();

    ContentAPIGraphQLTypesProvider() {
        // custom type mappings
        this.fieldClassGraphqlTypeMap.put(BinaryField.class, CustomFieldType.BINARY.getType());
        this.fieldClassGraphqlTypeMap
                .put(CategoryField.class, list(CustomFieldType.CATEGORY.getType()));
        this.fieldClassGraphqlTypeMap.put(ImageField.class, CustomFieldType.FILEASSET.getType());
        this.fieldClassGraphqlTypeMap.put(FileField.class, CustomFieldType.FILEASSET.getType());
        this.fieldClassGraphqlTypeMap
                .put(KeyValueField.class, list(CustomFieldType.KEY_VALUE.getType()));
        this.fieldClassGraphqlTypeMap.put(CheckboxField.class, list(GraphQLString));
        this.fieldClassGraphqlTypeMap.put(MultiSelectField.class, list(GraphQLString));
        this.fieldClassGraphqlTypeMap.put(TagField.class, list(GraphQLString));
        this.fieldClassGraphqlTypeMap
                .put(HostFolderField.class, CustomFieldType.SITE_OR_FOLDER.getType());
        this.fieldClassGraphqlTypeMap.put(StoryBlockField.class,CustomFieldType.STORY_BLOCK.getType());
        this.fieldClassGraphqlTypeMap.put(JSONField.class,ExtendedScalars.Json);

        // custom data fetchers
        this.fieldClassGraphqlDataFetcher.put(BinaryField.class, new BinaryFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(CategoryField.class, new CategoryFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(ImageField.class, new FileFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(FileField.class, new FileFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(KeyValueField.class, new KeyValueFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher
                .put(CheckboxField.class, new MultiValueFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher
                .put(MultiSelectField.class, new MultiValueFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(TagField.class, new TagsFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher
                .put(HostFolderField.class, new SiteOrFolderFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(StoryBlockField.class,new StoryBlockFieldDataFetcher());
        this.fieldClassGraphqlDataFetcher.put(JSONField.class, new JSONFieldDataFetcher());
    }

    @Override
    public Collection<? extends GraphQLType> getTypes() throws DotDataException {
        fillTypesMap();

        return typesMap.values();
    }

    private void fillTypesMap() throws DotDataException {
        typesMap.clear();
        // we want to generate them always - no cache
        final Set<GraphQLType> contentAPITypes = getContentAPITypes();

        for (GraphQLType graphQLType : contentAPITypes) {
            typesMap.put(TypeUtil.getName(graphQLType), graphQLType);
        }
    }

    Map<String, GraphQLType> getCachedTypesAsMap() throws DotDataException {
        if (!UtilMethods.isSet(typesMap)) {
            fillTypesMap();
        }
        return typesMap;
    }

    private Set<GraphQLType> getContentAPITypes() throws DotDataException {

        Logger.debug(this, ()-> "Getting all Content Types for GraphQL Schema");
        final Set<GraphQLType> contentAPITypes = new HashSet<>(InterfaceType.valuesAsSet());

        contentAPITypes.addAll(CustomFieldType.getCustomFieldTypes());

        List<ContentType> allTypes = APILocator.getContentTypeAPI(APILocator.systemUser())
                .search("", null, 100000, 0);

        // let's log if we are including dupe types
        final Map<String, ContentType> localTypesMap = new HashMap<>();
        allTypes.forEach((type)-> {
            if(localTypesMap.containsKey(type.variable())) {
                Logger.warn(this, "Dupe Content Type detected!: " + type.variable());
            }
            localTypesMap.put(type.variable(), type);
        });

        allTypes.forEach((type) -> {
            try {

                Logger.debug(this, ()-> "Generating GraphQL Type for type: " + type.variable());
                contentAPITypes.add(createType(type));
            }catch (IllegalArgumentException e) {
                Logger.error(this, "Unable to generate GraphQL Type for type: " + type.variable());
            }
        });

        return contentAPITypes;
    }

    private GraphQLObjectType createType(ContentType contentType) {
        DotPreconditions.checkArgument(contentType.variable()
                .matches(TYPES_AND_FIELDS_VALID_NAME_REGEX),
                "Content Type variable does not conform to naming rules");

        final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject()
                .name(contentType.variable());

        if (InterfaceType.getInterfaceForBaseType(contentType.baseType()) != null) {
            builder.withInterface(InterfaceType.getInterfaceForBaseType(contentType.baseType()));
        }

        builder.fields(createFieldsForType(contentType));

        builder.withInterface(InterfaceType.CONTENTLET.getType());
        return builder.build();
    }

    private List<GraphQLFieldDefinition> createFieldsForType(ContentType contentType) {
        final List<Field> fields = contentType.fields();

        final List<GraphQLFieldDefinition> fieldDefinitions = new ArrayList<>();

        fields.forEach((field) -> {
            // skip field.variable not sticking to the regex
            if (!field.variable().matches(TYPES_AND_FIELDS_VALID_NAME_REGEX)
                    || field instanceof RelationshipsTabField) {
                return;
            }

            if (!(field instanceof RowField) && !(field instanceof ColumnField)) {
                try {

                    Logger.debug(this, ()-> "Generating GraphQL Field for field: " + field.variable()
                            + " of type: " + contentType.variable());
                    fieldDefinitions.add(fieldGeneratorFactory.getGenerator(field).generateField(field));
                } catch(FieldGenerationException e) {
                    Logger.error(this, "Unable to generate GraphQL Field for field: " + field.variable(), e);
                }
            }
        });

        fieldDefinitions.add(newFieldDefinition()
                .name(WIDGET_CODE_JSON_FIELD_VAR)
                .argument(GraphQLArgument.newArgument()
                        .name("render")
                        .type(GraphQLBoolean)
                        .defaultValueProgrammatic(null)
                        .build())
                .type(ExtendedScalars.Json)
                .dataFetcher(new DotJSONDataFetcher()).build());

        // add CONTENT interface fields
        fieldDefinitions.addAll(TypeUtil
                .getGraphQLFieldDefinitionsFromMap(ContentFields.getContentFields()));

        return fieldDefinitions;
    }

    public GraphQLOutputType getGraphqlTypeForFieldClass(final Class<? extends Field> fieldClass,
            final Field field) {

        if(UtilMethods.isSet(field.variable()) && field.variable().equals(WIDGET_CODE_JSON_FIELD_VAR)) {
            return ExtendedScalars.Json;
        }

        return fieldClassGraphqlTypeMap.get(fieldClass) != null
                ? fieldClassGraphqlTypeMap.get(fieldClass)
                : fieldClass.equals(TextField.class) && field.dataType().equals(DataTypes.INTEGER)
                        ? GraphQLInt
                        : fieldClass.equals(TextField.class) && field.dataType()
                                .equals(DataTypes.FLOAT) ? GraphQLFloat
                                : GraphQLString;
    }

    public DataFetcher getGraphqlDataFetcherForFieldClass(final Class<Field> fieldClass) {
        return fieldClassGraphqlDataFetcher.get(fieldClass) != null
                ? fieldClassGraphqlDataFetcher.get(fieldClass)
                : new FieldDataFetcher();
    }

    /**
     * This method determines whether a {@link Field}'s variable is compatible with the
     * current GraphQL Schema.
     *<p>
     * The {@link Field} is deemed compatibly if any of the followings conditions are true:
     * <ul>
     *     <li>The field variable does not match any of the inherited fields names from the {@link ContentFields#getContentFields()} </li>
     *     <li>The field variable matches the name of a inherited field but neither of them have a {@link CustomFieldType}
     *     as its mapped GraphQL Type
     * </ul>
     * @param variable the variable whose compatibility will be checked
     * @param field the field which the variable will be set to
     * @return
     */
    public boolean isFieldVariableGraphQLCompatible(final String variable, final Field field) {
        final Map<String, TypeUtil.TypeFetcher> lowerNewFieldMap = new LowerKeyMap<>();
        // making the keys lowercase
        lowerNewFieldMap.putAll(ContentFields.getContentFields());

        // first let's check if there's an inherited field with the same variable
            if (lowerNewFieldMap.containsKey(variable.toLowerCase())) {
                // now let's check if the graphql types are compatible

                // get inherited field's graphql type
                final GraphQLType inheritedFieldGraphQLType = lowerNewFieldMap.get(variable).getType();

                // get new field's type
                final GraphQLType fieldGraphQLType = getGraphqlTypeForFieldClass(field.type(), field);

                // if at least one of them is a custom type, they need to be equal to be compatible
                return (!isCustomFieldType(inheritedFieldGraphQLType)
                        && !isCustomFieldType(fieldGraphQLType))
                        || inheritedFieldGraphQLType.equals(fieldGraphQLType)
                        || TypeUtil.getName(inheritedFieldGraphQLType).equals(TypeUtil.getName(fieldGraphQLType));
            }

        return true;
    }

    @VisibleForTesting
    protected void setFieldGeneratorFactory(
            GraphQLFieldGeneratorFactory fieldGeneratorFactory) {
        this.fieldGeneratorFactory = fieldGeneratorFactory;
    }


}
