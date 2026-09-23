package com.dotcms.graphql.business;

import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_DESCRIPTION_FIELD_VAR;

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
import com.dotcms.contenttype.model.type.BaseContentType;
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
import com.dotcms.graphql.InterfaceType.AssetInterfaces;
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
import com.dotcms.graphql.util.TypeUtil.TypeFetcher;
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
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedSchemaElement;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.PropertyDataFetcher;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
        // An asset-pointing field is described by what it actually points at, not by a single flat
        // view. A GraphQL object type has no subtypes, so while these were typed
        // `CustomFieldType.FILEASSET` no client could narrow to a concrete asset type; an interface
        // can. Referenced by name to avoid resolving InterfaceType during this enum's own
        // initialization, which would close a cycle. See #34540.
        this.fieldClassGraphqlTypeMap.put(ImageField.class,
                new GraphQLTypeReference(InterfaceType.ASSET_INTERFACE_NAME));
        this.fieldClassGraphqlTypeMap.put(FileField.class,
                new GraphQLTypeReference(InterfaceType.ASSET_INTERFACE_NAME));
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
        final Set<GraphQLType> contentAPITypes = new HashSet<>();

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

        // Fields are generated for every type before any type is built: which flat asset
        // properties the asset interfaces may declare depends on the fields of ALL asset types, and
        // every type must be built against the same interfaces. See #34540.
        final Map<ContentType, List<GraphQLFieldDefinition>> fieldsByType = new LinkedHashMap<>();
        allTypes.forEach((type) -> {
            try {
                DotPreconditions.checkArgument(type.variable()
                        .matches(TYPES_AND_FIELDS_VALID_NAME_REGEX),
                        "Content Type variable does not conform to naming rules");
                Logger.debug(this, ()-> "Generating GraphQL Type for type: " + type.variable());
                fieldsByType.put(type, createFieldsForType(type));
            }catch (IllegalArgumentException e) {
                Logger.error(this, "Unable to generate GraphQL Type for type: " + type.variable());
            }
        });

        final Set<String> excludedForFile = incompatibleAssetFlatFields(fieldsByType,
                BaseContentType.FILEASSET);
        final Set<String> excludedForDotAsset = incompatibleAssetFlatFields(fieldsByType,
                BaseContentType.DOTASSET);
        final AssetInterfaces assetInterfaces = InterfaceType.assetInterfacesExcluding(
                excludedForFile, excludedForDotAsset);

        // The asset-carrying interfaces come from this build, not from InterfaceType's defaults:
        // two different interface objects under one name would be rejected by the schema.
        final Set<GraphQLInterfaceType> baseTypeInterfaces = InterfaceType.valuesAsSet();
        baseTypeInterfaces.forEach(type -> {
            if (InterfaceType.FILE_INTERFACE_NAME.equals(type.getName())) {
                contentAPITypes.add(assetInterfaces.fileBaseType());
            } else if (InterfaceType.DOTASSET_INTERFACE_NAME.equals(type.getName())) {
                contentAPITypes.add(assetInterfaces.dotAssetBaseType());
            } else {
                contentAPITypes.add(type);
            }
        });
        // Not part of InterfaceType.values(): that enum is keyed by base type, and this interface
        // deliberately spans two of them. It still has to be registered or introspection cannot
        // see it and no fragment can narrow through it. See #34540.
        contentAPITypes.add(assetInterfaces.assetContent());

        fieldsByType.forEach((type, fieldDefinitions) -> {
            final Set<String> excluded = BaseContentType.FILEASSET == type.baseType()
                    ? excludedForFile : excludedForDotAsset;
            addAssetFlatFields(type, fieldDefinitions, excluded);
            contentAPITypes.add(createType(type, fieldDefinitions, assetInterfaces));
        });

        return contentAPITypes;
    }

    /**
     * Finds the flat asset properties that some content type of the given base type already
     * defines as a field of its own, with a GraphQL type the asset interfaces could not share.
     *
     * <p>The customer's field always wins its name (see {@link #addAssetFlatFields}). When the two
     * types agree that is all there is to it; when they do not, no interface may declare the name,
     * because graphql-java would reject the whole schema. Logged as a warning because it changes
     * what an asset field offers directly for the whole instance, and the remedy -- renaming the
     * customer's field -- is an administrator's decision.
     */
    private Set<String> incompatibleAssetFlatFields(
            final Map<ContentType, List<GraphQLFieldDefinition>> fieldsByType,
            final BaseContentType baseType) {

        final Map<String, GraphQLFieldDefinition> flatDefinitions = TypeUtil
                .getGraphQLFieldDefinitionsFromMap(CustomFieldType.getAssetFlatFields()).stream()
                .collect(Collectors.toMap(GraphQLFieldDefinition::getName, Function.identity()));

        final Set<String> incompatible = new HashSet<>();
        fieldsByType.forEach((type, fieldDefinitions) -> {
            if (baseType != type.baseType()) {
                return;
            }
            fieldDefinitions.stream()
                    .filter(definition -> flatDefinitions.containsKey(definition.getName()))
                    .filter(definition -> !isCompatible(definition,
                            flatDefinitions.get(definition.getName())))
                    .forEach(definition -> {
                        Logger.warn(this, "Field '" + definition.getName() + "' of Content Type '"
                                + type.variable() + "' is a "
                                + GraphQLTypeUtil.simplePrint(definition.getType())
                                + ", which conflicts with the asset property of the same name ("
                                + GraphQLTypeUtil.simplePrint(
                                        flatDefinitions.get(definition.getName()).getType())
                                + "). The asset property is left off the asset interfaces; it is"
                                + " still reachable through the binary field.");
                        incompatible.add(definition.getName());
                    });
        });
        return incompatible;
    }

    /**
     * Mirrors the check graphql-java applies to an object type implementing an interface: the
     * object's field must have the same type, or a non-null version of it, and must accept every
     * argument the interface's field declares, with the same type.
     */
    static boolean isCompatible(final GraphQLFieldDefinition implementation,
            final GraphQLFieldDefinition declared) {

        final String declaredType = GraphQLTypeUtil.simplePrint(declared.getType());
        final String implementationType = GraphQLTypeUtil.simplePrint(implementation.getType());
        if (!declaredType.equals(implementationType)
                && !(declaredType + "!").equals(implementationType)) {
            return false;
        }

        return declared.getArguments().stream().allMatch(argument -> {
            final GraphQLArgument implemented = implementation.getArgument(argument.getName());
            return null != implemented && GraphQLTypeUtil.simplePrint(argument.getType())
                    .equals(GraphQLTypeUtil.simplePrint(implemented.getType()));
        });
    }

    private GraphQLObjectType createType(final ContentType contentType,
            final List<GraphQLFieldDefinition> fieldDefinitions,
            final AssetInterfaces assetInterfaces) {

        final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject()
                .name(contentType.variable());

        final GraphQLInterfaceType baseTypeInterface =
                InterfaceType.isAssetBaseType(contentType.baseType())
                        ? assetInterfaces.forBaseType(contentType.baseType())
                        : InterfaceType.getInterfaceForBaseType(contentType.baseType());
        if (baseTypeInterface != null) {
            builder.withInterface(baseTypeInterface);
        }

        // Anything derived from an asset base type can sit behind an Image or File field, so it
        // must be reachable through that field's interface. Declaring it here is what puts the
        // type in the interface's possible-type set -- which is why a content type the customer
        // creates later is reachable with no registration step of its own.
        if (InterfaceType.isAssetBaseType(contentType.baseType())) {
            builder.withInterface(assetInterfaces.assetContent());
        }

        builder.fields(fieldDefinitions);

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

        if (InterfaceType.isAssetBaseType(contentType.baseType())) {
            // `description` is the one flat property that must OVERRIDE rather than fill in. Most
            // asset types define a `description` of their own, so filling in would leave their
            // stored-value fetcher in place -- and an asset-pointing field would silently start
            // answering with the stored value instead of the title it has always returned.
            // Dropping the type's own definition here hands the name to
            // AssetDescriptionDataFetcher, which returns the right one of the two depending on how
            // the asset was reached, so neither reading breaks. Done before collisions are
            // computed, so `description` can never count as one.
            fieldDefinitions.removeIf(
                    definition -> FILEASSET_DESCRIPTION_FIELD_VAR.equals(definition.getName()));
        }

        return fieldDefinitions;
    }

    /**
     * Gives a DOTASSET-derived object type the flat properties an asset-pointing field has always
     * exposed, so that retyping such a field to the asset-content interface leaves those selections
     * working.
     *
     * <p>FILEASSET-derived types already carry them as real fields and are skipped. For
     * DOTASSET-derived ones the properties do not exist at all — {@code fileName} was never stored
     * there, the flat view answered it with the contentlet name — so they are synthesized with the
     * very same fetchers the flat view uses, which is what makes them answer identically.
     *
     * <p>A field the customer already defined always wins: a duplicate definition would fail the
     * whole schema build and take every other content type down with it, and its value predates
     * this feature, so answering with anything else would be a silent change.
     *
     * @param excluded flat properties this build's asset interfaces leave off because some type of
     *                 the same base collides with them; they are not synthesized either, so the
     *                 property is offered the same way on every type of that base
     */
    private void addAssetFlatFields(final ContentType contentType,
            final List<GraphQLFieldDefinition> fieldDefinitions, final Set<String> excluded) {

        if (!InterfaceType.isAssetBaseType(contentType.baseType())) {
            return;
        }

        final Set<String> alreadyDefined = fieldDefinitions.stream()
                .map(GraphQLFieldDefinition::getName).collect(Collectors.toSet());

        // Only what this type is actually missing. A FILEASSET-derived type usually defines most
        // of these itself -- but not always: a customer-created one carries only the required
        // fields, so `showOnMenu` and `sortOrder` can be absent and must be filled in too. A field
        // the customer already defined always wins; a duplicate would fail the whole schema build.
        final Map<String, TypeFetcher> missing = CustomFieldType.getAssetFlatFields().entrySet()
                .stream()
                .filter(entry -> !alreadyDefined.contains(entry.getKey()))
                .filter(entry -> !excluded.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (missing.isEmpty()) {
            return;
        }

        Logger.debug(this, () -> "Synthesizing asset properties " + missing.keySet()
                + " on Content Type '" + contentType.variable() + "'");

        // Built through TypeUtil rather than by hand: it attaches the `render` argument every
        // generated field carries, and an interface field and its implementation must agree on
        // arguments or the schema is rejected.
        fieldDefinitions.addAll(TypeUtil.getGraphQLFieldDefinitionsFromMap(missing));
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
        if (collidesWithAssetFlatField(variable, field)) {
            return false;
        }

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

    /**
     * Whether a new field on an asset content type would take the name of a flat asset property
     * with a different GraphQL type.
     *
     * <p>Such a field cannot break the schema -- the build leaves the asset property off the
     * interfaces instead, see {@link #incompatibleAssetFlatFields} -- but it would stop the asset
     * field offering that property directly for the whole instance. Steering the suggested
     * variable away from the name prevents that for new fields; the build-time rule is what
     * protects data that predates the check, and variables chosen explicitly. See #34540.
     */
    private boolean collidesWithAssetFlatField(final String variable, final Field field) {
        final TypeFetcher flatField = CustomFieldType.getAssetFlatFields().get(variable);
        if (null == flatField || !UtilMethods.isSet(field.contentTypeId())) {
            return false;
        }

        final boolean onAssetType = Try.of(() -> APILocator.getContentTypeAPI(
                        APILocator.systemUser()).find(field.contentTypeId()))
                .map(contentType -> InterfaceType.isAssetBaseType(contentType.baseType()))
                .getOrElse(false);

        return onAssetType && !GraphQLTypeUtil.simplePrint(flatField.getType())
                .equals(GraphQLTypeUtil.simplePrint(getGraphqlTypeForFieldClass(field.type(), field)));
    }

    @VisibleForTesting
    protected void setFieldGeneratorFactory(
            GraphQLFieldGeneratorFactory fieldGeneratorFactory) {
        this.fieldGeneratorFactory = fieldGeneratorFactory;
    }


}
