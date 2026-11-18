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
import com.dotmarketing.exception.DotDataValidationException;
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
import graphql.schema.GraphQLNonNull;
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

        fieldsByType.replaceAll((type, fieldDefinitions) ->
                InterfaceType.isAssetBaseType(type.baseType())
                        ? widenWholeNumbersToAssetFlatFields(fieldDefinitions)
                        : fieldDefinitions);

        final Set<String> clashingTypes = typesClashingWithAssetFlatFields(fieldsByType);
        final AssetInterfaces assetInterfaces = InterfaceType.getAssetInterfaces();

        contentAPITypes.addAll(InterfaceType.valuesAsSet());
        // Not part of InterfaceType.values(): that enum is keyed by base type, and this interface
        // deliberately spans two of them. It still has to be registered or introspection cannot
        // see it and no fragment can narrow through it. See #34540.
        contentAPITypes.add(assetInterfaces.assetContent());
        // Always registered, so the schema's shape does not depend on the data: a clash appearing
        // or going away changes which content types resolve as these, never which types exist.
        contentAPITypes.add(InterfaceType.getPropertyClashType(BaseContentType.DOTASSET));
        contentAPITypes.add(InterfaceType.getPropertyClashType(BaseContentType.FILEASSET));

        fieldsByType.forEach((type, fieldDefinitions) -> {
            addAssetFlatFields(type, fieldDefinitions);
            contentAPITypes.add(createType(type, fieldDefinitions, assetInterfaces,
                    clashingTypes.contains(type.variable())));
        });

        return contentAPITypes;
    }

    /**
     * Finds the asset content types whose own fields clash with a flat asset property: same name,
     * a GraphQL type the asset interfaces cannot share.
     *
     * <p>graphql-java requires an interface and every type implementing it to agree on each shared
     * field, so such a type cannot implement the asset interfaces. It is left out of them alone --
     * see {@link #createType} -- and its assets resolve as the property-clash type through Image
     * and File fields (see {@link InterfaceType#getPropertyClashType}); the property itself stays
     * on the interfaces for every other asset type. Before, the property was left off the
     * interfaces for the whole instance, failing every query that selected it -- including
     * long-standing ones such as {@code image { sortOrder }}.
     *
     * <p>New fields like these are refused or renamed on save (see
     * {@link #checkAssetPropertyNameIsCompatible}), so only data that predates that check gets
     * here. Logged as a warning because it changes how the type's assets look through asset
     * fields, and the remedy -- renaming the customer's field -- is an administrator's decision.
     *
     * @return the variables of the clashing content types
     */
    private Set<String> typesClashingWithAssetFlatFields(
            final Map<ContentType, List<GraphQLFieldDefinition>> fieldsByType) {

        final Map<String, GraphQLFieldDefinition> flatDefinitions = TypeUtil
                .getGraphQLFieldDefinitionsFromMap(CustomFieldType.getAssetFlatFields()).stream()
                .collect(Collectors.toMap(GraphQLFieldDefinition::getName, Function.identity()));

        final Set<String> clashing = new HashSet<>();
        fieldsByType.forEach((type, fieldDefinitions) -> {
            if (!InterfaceType.isAssetBaseType(type.baseType())) {
                return;
            }
            final List<GraphQLFieldDefinition> clashes = fieldDefinitions.stream()
                    .filter(definition -> flatDefinitions.containsKey(definition.getName()))
                    .filter(definition -> !isCompatible(definition,
                            flatDefinitions.get(definition.getName())))
                    .collect(Collectors.toList());
            if (clashes.isEmpty()) {
                return;
            }
            clashing.add(type.variable());
            final String standIn = InterfaceType.getPropertyClashType(type.baseType()).getName();
            clashes.forEach(definition -> Logger.warn(this, "Content Type '" + type.variable()
                    + "' is left out of the asset interfaces: its field '" + definition.getName()
                    + "' is a " + GraphQLTypeUtil.simplePrint(definition.getType())
                    + ", but every asset field offers '" + definition.getName() + "' as "
                    + GraphQLTypeUtil.simplePrint(
                            flatDefinitions.get(definition.getName()).getType())
                    + ". Assets of this type are still reachable through its own queries; through"
                    + " Image and File fields they resolve as '" + standIn + "', without the"
                    + " type's own fields. Rename the field to restore it."));
        });
        return clashing;
    }

    /**
     * Publishes an asset type's own whole-number field as a {@code Long} when it takes the name
     * of a flat asset property of that type, such as {@code width}, {@code height} or
     * {@code size}.
     *
     * <p>A whole-number field is an {@code Int}, and graphql-java would treat it as a different
     * type from the asset property, so the type would be left out of the asset interfaces (see
     * {@link #typesClashingWithAssetFlatFields}). A {@code Long} holds every {@code Int}, so the customer's
     * value is returned unchanged and the field stays compatible with the interface: the type that
     * declares it answers with its own value and every other asset type keeps the property.
     * Only the schema changes, from {@code Int} to {@code Long}, for that field on that type.
     *
     * @param fieldDefinitions the fields generated for one asset type
     * @return the same fields, with the colliding whole-number ones retyped
     */
    private List<GraphQLFieldDefinition> widenWholeNumbersToAssetFlatFields(
            final List<GraphQLFieldDefinition> fieldDefinitions) {

        final Map<String, TypeFetcher> flatFields = CustomFieldType.getAssetFlatFields();
        return fieldDefinitions.stream().map(definition -> {
            final TypeFetcher flatField = flatFields.get(definition.getName());
            if (null == flatField
                    || !GraphQLTypeUtil.simplePrint(ExtendedScalars.GraphQLLong)
                            .equals(GraphQLTypeUtil.simplePrint(flatField.getType()))
                    || !GraphQLInt.getName().equals(GraphQLTypeUtil.simplePrint(
                            GraphQLTypeUtil.unwrapNonNull(definition.getType())))) {
                return definition;
            }
            final GraphQLOutputType widened = GraphQLTypeUtil.isNonNull(definition.getType())
                    ? GraphQLNonNull.nonNull(ExtendedScalars.GraphQLLong)
                    : ExtendedScalars.GraphQLLong;
            return definition.transform(builder -> builder.type(widened));
        }).collect(Collectors.toList());
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

    /**
     * Builds the object type for a content type.
     *
     * @param clashesWithAssetFlatFields whether the type is an asset type whose own fields clash
     *                                   with a flat asset property, in which case it is left out of
     *                                   the asset interfaces (see
     *                                   {@link #typesClashingWithAssetFlatFields})
     */
    private GraphQLObjectType createType(final ContentType contentType,
            final List<GraphQLFieldDefinition> fieldDefinitions,
            final AssetInterfaces assetInterfaces, final boolean clashesWithAssetFlatFields) {

        final GraphQLObjectType.Builder builder = GraphQLObjectType.newObject()
                .name(contentType.variable());

        final boolean assetType = InterfaceType.isAssetBaseType(contentType.baseType());
        final GraphQLInterfaceType baseTypeInterface = assetType
                ? (clashesWithAssetFlatFields ? null
                        : assetInterfaces.forBaseType(contentType.baseType()))
                : InterfaceType.getInterfaceForBaseType(contentType.baseType());
        if (baseTypeInterface != null) {
            builder.withInterface(baseTypeInterface);
        }

        // Anything derived from an asset base type can sit behind an Image or File field, so it
        // must be reachable through that field's interface. Declaring it here is what puts the
        // type in the interface's possible-type set -- which is why a content type the customer
        // creates later is reachable with no registration step of its own. A clashing type cannot
        // declare it; ContentResolver answers with the property-clash type for it instead.
        if (assetType && !clashesWithAssetFlatFields) {
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
     * @param contentType      the content type the fields belong to
     * @param fieldDefinitions the fields generated for it, completed in place
     */
    private void addAssetFlatFields(final ContentType contentType,
            final List<GraphQLFieldDefinition> fieldDefinitions) {

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
        final Optional<AssetPropertyCollision> collision = assetPropertyCollision(variable, field);
        if (collision.isPresent()) {
            Logger.warn(this, "Field variable '" + variable + "' cannot be used on asset Content "
                    + "Type '" + collision.get().contentType().variable() + "': every asset field "
                    + "offers a property of that name as " + collision.get().propertyType()
                    + " and this field is " + collision.get().fieldType()
                    + "; the generated variable gets a numeric suffix instead.");
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
     * Refuses a new field whose variable, chosen explicitly, is the name of a flat asset property
     * on an asset content type while its type differs from that property's.
     *
     * <p>A field of the property's own type is accepted: its type answers with its own value and
     * every other asset type keeps the property. One of another type would leave its content type
     * out of the asset interfaces, its assets reachable through Image and File fields only as the
     * property-clash type (see {@link #typesClashingWithAssetFlatFields}), so it is refused; renaming it instead, as a generated variable is (see
     * {@link #isFieldVariableGraphQLCompatible}), would silently lose the data of whoever chose the
     * variable -- a CLI, push publishing, a script. Only new fields reach this check: saving a
     * field that already exists by its variable is an update and keeps working, and data that
     * predates the check is protected at schema build instead. See #34540.
     *
     * @param variable the variable the new field is being saved with
     * @param field    the new field
     * @throws DotDataValidationException if the field's type differs from the asset property's
     */
    public void checkAssetPropertyNameIsCompatible(final String variable, final Field field)
            throws DotDataValidationException {
        final Optional<AssetPropertyCollision> collision = assetPropertyCollision(variable, field);
        if (collision.isPresent()) {
            throw new DotDataValidationException(String.format(
                    "Field variable '%s' cannot be used on asset Content Type '%s': every asset "
                            + "field offers a property of that name as %s, and this field is %s. "
                            + "Use a field of that type or choose another variable.", variable,
                    collision.get().contentType().variable(), collision.get().propertyType(),
                    collision.get().fieldType()));
        }
    }

    /**
     * A new field on an asset content type whose variable is the name of a flat asset property
     * with a different GraphQL type.
     *
     * @param contentType  the asset content type the field belongs to
     * @param propertyType the GraphQL type of the asset property, e.g. {@code Long}
     * @param fieldType    the GraphQL type the field would be published with, e.g. {@code String}
     */
    private record AssetPropertyCollision(ContentType contentType, String propertyType,
            String fieldType) {
    }

    /**
     * Finds out whether a field on an asset content type would collide with the flat asset
     * property of the same name.
     *
     * <p>It collides only when their GraphQL types differ, counting a whole number as the
     * {@code Long} it is published as (see {@link #widenWholeNumbersToAssetFlatFields}). A field
     * of the property's own type cannot collide -- which is also why the FileAsset type's own
     * fields ({@code fileName}, {@code fileAsset}, {@code metaData}, {@code showOnMenu},
     * {@code sortOrder}), which the starter, copying a type and push publishing save with these
     * exact variables, need no special case. {@code description} never collides: the schema
     * build drops the type's own definition and answers it through
     * {@code AssetDescriptionDataFetcher}.
     *
     * @param variable the candidate variable
     * @param field    the field it would be given
     * @return the collision, if the field belongs to an asset type and its type differs from the
     * asset property's; empty otherwise
     */
    private Optional<AssetPropertyCollision> assetPropertyCollision(final String variable,
            final Field field) {
        final TypeFetcher property = CustomFieldType.getAssetFlatFields().get(variable);
        if (null == property || FILEASSET_DESCRIPTION_FIELD_VAR.equals(variable)
                || !UtilMethods.isSet(field.contentTypeId())) {
            return Optional.empty();
        }

        final String propertyType = GraphQLTypeUtil.simplePrint(
                GraphQLTypeUtil.unwrapNonNull(property.getType()));
        final String declaredFieldType = GraphQLTypeUtil.simplePrint(
                GraphQLTypeUtil.unwrapNonNull(getGraphqlTypeForFieldClass(field.type(), field)));
        final String fieldType = GraphQLInt.getName().equals(declaredFieldType)
                && GraphQLTypeUtil.simplePrint(ExtendedScalars.GraphQLLong).equals(propertyType)
                ? propertyType : declaredFieldType;
        if (propertyType.equals(fieldType)) {
            return Optional.empty();
        }

        return Try.of(() -> APILocator.getContentTypeAPI(
                        APILocator.systemUser()).find(field.contentTypeId()))
                .toJavaOptional()
                .filter(contentType -> InterfaceType.isAssetBaseType(contentType.baseType()))
                .map(contentType -> new AssetPropertyCollision(contentType, propertyType,
                        declaredFieldType));
    }

    @VisibleForTesting
    protected void setFieldGeneratorFactory(
            GraphQLFieldGeneratorFactory fieldGeneratorFactory) {
        this.fieldGeneratorFactory = fieldGeneratorFactory;
    }


}
