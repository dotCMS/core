package com.dotcms.graphql.business;

import static com.dotcms.graphql.CustomFieldType.isCustomFieldType;
import static com.dotcms.graphql.business.GraphqlAPI.TYPES_AND_FIELDS_VALID_NAME_REGEX;
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
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.graphql.CustomFieldType;
import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.datafetcher.BinaryFieldDataFetcher;
import com.dotcms.graphql.datafetcher.CategoryFieldDataFetcher;
import com.dotcms.graphql.datafetcher.FieldDataFetcher;
import com.dotcms.graphql.datafetcher.FileFieldDataFetcher;
import com.dotcms.graphql.datafetcher.KeyValueFieldDataFetcher;
import com.dotcms.graphql.datafetcher.MultiValueFieldDataFetcher;
import com.dotcms.graphql.datafetcher.SiteOrFolderFieldDataFetcher;
import com.dotcms.graphql.datafetcher.TagsFieldDataFetcher;
import com.dotcms.graphql.exception.FieldGenerationException;
import com.dotcms.graphql.exception.TypeGenerationException;
import com.dotcms.graphql.util.TypeUtil;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import graphql.GraphQLException;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.PropertyDataFetcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    }

    @Override
    public Collection<? extends GraphQLType> getTypes() throws DotDataException {

        // we want to generate them always - no cache
        getContentAPITypes().forEach((graphQLType)->
                typesMap.put(graphQLType.getName(), graphQLType));

        return typesMap.values();
    }

    Map<String, GraphQLType> getCachedTypesAsMap() throws DotDataException {
        if (!UtilMethods.isSet(typesMap)) {
            getContentAPITypes().forEach((graphQLType)->
                    typesMap.put(graphQLType.getName(), graphQLType));
        }
        return typesMap;
    }

    private Set<GraphQLType> getContentAPITypes() throws DotDataException {

        Set<GraphQLType> contentAPITypes = new HashSet<>(InterfaceType.valuesAsSet());

        contentAPITypes.addAll(CustomFieldType.getCustomFieldTypes());

        List<ContentType> allTypes = APILocator.getContentTypeAPI(APILocator.systemUser())
                .findAllRespectingLicense();

        allTypes.forEach((type) -> {
            try {
                contentAPITypes.add(createType(type));
            }catch (TypeGenerationException e) {
                Logger.error(this, "Unable to generate GraphQL Type for type: " + type.variable());
            }
        });

        return contentAPITypes;
    }

    private GraphQLObjectType createType(ContentType contentType) {

        DotPreconditions.checkArgument(contentType.variable()
                .matches(TYPES_AND_FIELDS_VALID_NAME_REGEX),
                "Content Type variable does not conform to naming rules",
                TypeGenerationException.class);

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
                    fieldDefinitions.add(fieldGeneratorFactory.getGenerator(field).generateField(field));
                } catch(FieldGenerationException e) {
                    Logger.error(this, "Unable to generate GraphQL Field for field: " + field.variable(), e);
                }
            }

        });

        // add CONTENT interface fields
        fieldDefinitions.addAll(TypeUtil
                .getGraphQLFieldDefinitionsFromMap(InterfaceType.getContentFields()));

        return fieldDefinitions;
    }

    public GraphQLOutputType getGraphqlTypeForFieldClass(final Class<? extends Field> fieldClass,
            final Field field) {
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
     *     <li>The field variable does not match any of the inherited fields names from the {@link InterfaceType#getContentFields()} </li>
     *     <li>The field variable matches the name of a inherited field but neither of them have a {@link CustomFieldType}
     *     as its mapped GraphQL Type
     * </ul>
     * @param variable the variable whose compatibility will be checked
     * @param field the field which the variable will be set to
     * @return
     */
    public boolean isFieldVariableGraphQLCompatible(final String variable, final Field field) {
        // first let's check if there's an inherited field with the same variable
        if (InterfaceType.getContentFields().containsKey(variable)) {
            // now let's check if the graphql types are compatible

            // get inherited field's graphql type
            final GraphQLType inheritedFieldGraphQLType = InterfaceType
                    .getContentFields()
                    .get(variable).getType();

            // get new field's type
            final GraphQLType fieldGraphQLType = getGraphqlTypeForFieldClass(field.type(), field);

            // if at least one of them is a custom type, they need to be equal to be compatible
            return (!isCustomFieldType(inheritedFieldGraphQLType)
                    && !isCustomFieldType(fieldGraphQLType))
                    || inheritedFieldGraphQLType.equals(fieldGraphQLType);
        }

        return true;
    }

    @VisibleForTesting
    protected void setFieldGeneratorFactory(
            GraphQLFieldGeneratorFactory fieldGeneratorFactory) {
        this.fieldGeneratorFactory = fieldGeneratorFactory;
    }
}
