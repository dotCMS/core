package com.dotcms.graphql;

import static com.dotcms.graphql.util.TypeUtil.TypeFetcher;
import static com.dotcms.graphql.util.TypeUtil.createInterfaceType;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.contenttype.model.type.EnterpriseType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.contenttype.model.type.FormContentType;
import com.dotcms.contenttype.model.type.ImmutableDotAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFormContentType;
import com.dotcms.contenttype.model.type.ImmutableKeyValueContentType;
import com.dotcms.contenttype.model.type.ImmutablePageContentType;
import com.dotcms.contenttype.model.type.ImmutablePersonaContentType;
import com.dotcms.contenttype.model.type.ImmutableVanityUrlContentType;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.contenttype.model.type.PageContentType;
import com.dotcms.contenttype.model.type.PersonaContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.graphql.business.ContentAPIGraphQLTypesProvider;
import com.dotcms.graphql.resolver.ContentResolver;
import com.dotmarketing.util.Logger;
import graphql.schema.GraphQLInterfaceType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum InterfaceType {

    CONTENTLET(SimpleContentType.class),
    CONTENT(SimpleContentType.class),
    FILEASSET(FileAssetContentType.class),
    HTMLPAGE(PageContentType.class),
    PERSONA(PersonaContentType.class),
    WIDGET(WidgetContentType.class),
    VANITY_URL(VanityUrlContentType.class),
    KEY_VALUE(KeyValueContentType.class),
    FORM(FormContentType.class),
    DOTASSET(DotAssetContentType.class);

    private final Class<? extends ContentType> baseContentType;

    InterfaceType(final Class<? extends ContentType> baseContentType) {
        this.baseContentType = baseContentType;
    }

    public static Set<String> CONTENT_INTERFACE_FIELDS = new HashSet<>();

    private static final Map<String, GraphQLInterfaceType> interfaceTypes = new HashMap<>();

    public static final String CONTENT_INTERFACE_NAME = "ContentBaseType";
    public static final String FILE_INTERFACE_NAME = "FileBaseType";
    public static final String PAGE_INTERFACE_NAME = "PageBaseType";
    public static final String PERSONA_INTERFACE_NAME = "PersonaBaseType";
    public static final String WIDGET_INTERFACE_NAME = "WidgetBaseType";
    public static final String VANITY_URL_INTERFACE_NAME = "VanityURLBaseType";
    public static final String KEY_VALUE_INTERFACE_NAME = "KeyValueBaseType";
    public static final String FORM_INTERFACE_NAME = "FormBaseType";
    public static final String DOTASSET_INTERFACE_NAME = "DotAssetBaseType";

    /**
     * Describes the content an Image or File field points at, by whatever content type it actually
     * is. Unlike the interfaces above it is not tied to a single base type: an asset-pointing field
     * can hold either DOTASSET- or FILEASSET-based content — an Image field resolves a FileAsset
     * perfectly well today — so neither {@link #DOTASSET_INTERFACE_NAME} nor
     * {@link #FILE_INTERFACE_NAME} alone can describe what such a field may return.
     *
     * <p><b>It deliberately keeps the name the flat object type used to carry.</b> That name is
     * what clients already write in {@code ... on DotFileasset} clauses, and a fragment on the
     * position's own interface always matches — so those clauses keep working and keep returning
     * data. Introducing a new name instead would have left every such clause invalid. The kind
     * does change, from object to interface, which query text does not notice but client code
     * generators do: anyone with generated types must regenerate them. See #34540.
     */
    public static final String ASSET_INTERFACE_NAME = "DotFileasset";

    public static final String DOT_CONTENTLET = "DotContentlet";

    /** Content fields plus the fixed fields of each base type, before the flat properties. */
    private static final Map<String, TypeFetcher> fileAssetBaseFields = new HashMap<>();
    private static final Map<String, TypeFetcher> dotAssetBaseFields = new HashMap<>();
    private static final Map<String, TypeFetcher> assetContentBaseFields = new HashMap<>();

    // Declared before the static block on purpose: static initializers run in textual order.
    private static AssetInterfaces defaultAssetInterfaces;

    static {

        Map<String, TypeFetcher> contentFields = ContentFields.getContentFields();

        CONTENT_INTERFACE_FIELDS.addAll(contentFields.keySet());

        GraphQLInterfaceType contentletInterface = createInterfaceType(DOT_CONTENTLET,
                contentFields, new ContentResolver());

        interfaceTypes.put("CONTENTLET", contentletInterface);

        interfaceTypes.put("CONTENT", createInterfaceType(CONTENT_INTERFACE_NAME, contentFields, new ContentResolver()));

        fileAssetBaseFields.putAll(contentFields);
        addBaseTypeFields(fileAssetBaseFields, ImmutableFileAssetContentType.builder().name("dummy")
                .build().requiredFields());

        final Map<String, TypeFetcher> pageAssetFields = new HashMap<>(contentFields);
        addBaseTypeFields(pageAssetFields, ImmutablePageContentType.builder().name("dummy")
                .build().requiredFields());
        interfaceTypes.put("HTMLPAGE", createInterfaceType(PAGE_INTERFACE_NAME, pageAssetFields, new ContentResolver()));

        final Map<String, TypeFetcher> personaFields = new HashMap<>(contentFields);
        addBaseTypeFields(personaFields, ImmutablePersonaContentType.builder().name("dummy")
                .build().requiredFields());
        interfaceTypes.put("PERSONA", createInterfaceType(PERSONA_INTERFACE_NAME, personaFields, new ContentResolver()));

        final Map<String, TypeFetcher> widgetFields = new HashMap<>(contentFields);
        addBaseTypeFields(widgetFields, ImmutableWidgetContentType.builder().name("dummy")
                .build().requiredFields());

        interfaceTypes.put("WIDGET", createInterfaceType(WIDGET_INTERFACE_NAME, widgetFields, new ContentResolver()));

        final Map<String, TypeFetcher> vanityUrlFields = new HashMap<>(contentFields);
        addBaseTypeFields(vanityUrlFields, ImmutableVanityUrlContentType.builder().name("dummy")
                .build().requiredFields());
        interfaceTypes.put("VANITY_URL", createInterfaceType(VANITY_URL_INTERFACE_NAME, vanityUrlFields, new ContentResolver()));

        final Map<String, TypeFetcher> keyValueFields = new HashMap<>(contentFields);
        addBaseTypeFields(keyValueFields, ImmutableKeyValueContentType.builder().name("dummy")
                .build().requiredFields());
        interfaceTypes.put("KEY_VALUE", createInterfaceType(KEY_VALUE_INTERFACE_NAME, keyValueFields, new ContentResolver()));

        final Map<String, TypeFetcher> formFields = new HashMap<>(contentFields);
        addBaseTypeFields(formFields, ImmutableFormContentType.builder().name("dummy")
                .build().requiredFields());
        interfaceTypes.put("FORM", createInterfaceType(FORM_INTERFACE_NAME, formFields,
                new ContentResolver()));

        dotAssetBaseFields.putAll(contentFields);
        addBaseTypeFields(dotAssetBaseFields, ImmutableDotAssetContentType.builder().name("dummy")
                .build().requiredFields());

        assetContentBaseFields.putAll(contentFields);

        // Built once with nothing excluded; a schema build whose content types collide with the
        // flat properties asks for its own copies through assetInterfacesExcluding.
        final AssetInterfaces defaults = buildAssetInterfaces(Set.of(), Set.of());
        interfaceTypes.put("FILEASSET", defaults.fileBaseType());
        interfaceTypes.put("DOTASSET", defaults.dotAssetBaseType());
        defaultAssetInterfaces = defaults;
    }

    /**
     * The three interfaces that carry the flat asset properties, built together so that a schema
     * always uses one consistent set of them.
     */
    public record AssetInterfaces(GraphQLInterfaceType fileBaseType,
                                  GraphQLInterfaceType dotAssetBaseType,
                                  GraphQLInterfaceType assetContent) {

        /** @return the base-type interface for an asset base type, or null for any other. */
        public GraphQLInterfaceType forBaseType(final BaseContentType baseContentType) {
            if (BaseContentType.FILEASSET == baseContentType) {
                return fileBaseType;
            }
            if (BaseContentType.DOTASSET == baseContentType) {
                return dotAssetBaseType;
            }
            return null;
        }
    }

    /**
     * Builds the asset interfaces leaving out the flat properties that some content type on this
     * instance already defines with an incompatible GraphQL type.
     *
     * <p>An interface and every object type implementing it must agree on each shared field's
     * type, and graphql-java rejects the <b>whole</b> schema otherwise. A customer asset type with,
     * say, a text field named {@code size} would therefore take every GraphQL query on the instance
     * down. Leaving {@code size} off the interfaces instead keeps the schema valid: the customer's
     * field keeps answering on its own type, the binary's size stays reachable through the binary
     * field, and selecting {@code size} directly on an asset field fails validation with an error
     * naming it rather than returning different data. See #34540.
     *
     * @param excludedForFile     names colliding on some FILEASSET-derived type
     * @param excludedForDotAsset names colliding on some DOTASSET-derived type
     * @return the defaults when nothing collides, which is the common case
     */
    public static AssetInterfaces assetInterfacesExcluding(final Set<String> excludedForFile,
            final Set<String> excludedForDotAsset) {
        if (excludedForFile.isEmpty() && excludedForDotAsset.isEmpty()) {
            return defaultAssetInterfaces;
        }
        return buildAssetInterfaces(excludedForFile, excludedForDotAsset);
    }

    private static AssetInterfaces buildAssetInterfaces(final Set<String> excludedForFile,
            final Set<String> excludedForDotAsset) {

        // Same flat properties the asset interface carries. Every possible type of these
        // interfaces has them, and leaving them off would make `fileName` selectable on the
        // concrete types and on the asset interface but not through a base-type clause — the same
        // query changing shape depending on which clause a client narrows through. DOTASSET
        // content has no stored file name, but every DOTASSET-derived type carries the synthesized
        // one. Only the flat overlay is filtered: a base type's own fixed fields are present on
        // every type of that base, so they cannot collide.
        final Map<String, TypeFetcher> fileAssetFields = new HashMap<>(fileAssetBaseFields);
        fileAssetFields.putAll(flatFieldsExcluding(excludedForFile));

        final Map<String, TypeFetcher> dotAssetFields = new HashMap<>(dotAssetBaseFields);
        dotAssetFields.putAll(flatFieldsExcluding(excludedForDotAsset));

        // Spans both base types, so a name colliding on either is left off. Carries the common
        // content fields plus the flat properties an asset-pointing field has always exposed, so
        // that retyping such a field to this interface leaves those selections working.
        final Set<String> excludedForAsset = new HashSet<>(excludedForFile);
        excludedForAsset.addAll(excludedForDotAsset);
        final Map<String, TypeFetcher> assetContentFields = new HashMap<>(assetContentBaseFields);
        assetContentFields.putAll(flatFieldsExcluding(excludedForAsset));

        return new AssetInterfaces(
                createInterfaceType(FILE_INTERFACE_NAME, fileAssetFields, new ContentResolver()),
                createInterfaceType(DOTASSET_INTERFACE_NAME, dotAssetFields, new ContentResolver()),
                createInterfaceType(ASSET_INTERFACE_NAME, assetContentFields,
                        new ContentResolver()));
    }

    private static Map<String, TypeFetcher> flatFieldsExcluding(final Set<String> excluded) {
        final Map<String, TypeFetcher> flatFields = new HashMap<>(
                CustomFieldType.getAssetFlatFields());
        flatFields.keySet().removeAll(excluded);
        return flatFields;
    }

    /**
     * @return the interface describing what an asset-pointing field returns, implemented by every
     * content type derived from either asset base type, as built when nothing collides.
     */
    public static GraphQLInterfaceType getAssetContentInterface() {
        return defaultAssetInterfaces.assetContent();
    }

    /**
     * @return whether content of this base type can sit behind an Image or File field, and must
     * therefore implement {@link #getAssetContentInterface()}.
     */
    public static boolean isAssetBaseType(final BaseContentType baseContentType) {
        return BaseContentType.DOTASSET == baseContentType
                || BaseContentType.FILEASSET == baseContentType;
    }

    /**
     * Adds the official list of fields to a given Base Content Type. Keep in mind that not all fields in the Content
     * Type definition will be part of it, only the ones that meet at least one of the following criteria:
     * <ol>
     *     <li>The field is NOT removable (is fixed).</li>
     *     <li>The field is forced to be included in the API response (i.e., {@link Field#forceIncludeInApi()}).</li>
     * </ol>
     *
     * @param baseTypeFields     The {@link Map} containing the Base Types and their respective required fields.
     * @param requiredFormFields The list of fields from a given Base Type.
     */
    private static void addBaseTypeFields(final Map<String, TypeFetcher> baseTypeFields,
            final List<Field> requiredFormFields) {
        final ContentAPIGraphQLTypesProvider instance = ContentAPIGraphQLTypesProvider.INSTANCE;
        //Only Add the fixed fields to the base type fields as others can be removed breaking the GraphQLSchema
        for (final Field formField : requiredFormFields) {
            if (!formField.fixed()) {
                Logger.warn(InterfaceType.class, "Field " + formField.variable() + " is not fixed, skipping.");
                continue;
            }
            baseTypeFields.put(formField.variable(), new TypeFetcher(
                    instance.getGraphqlTypeForFieldClass(formField.type(), formField)));
        }
    }

    public GraphQLInterfaceType getType() {
        return interfaceTypes.get(this.name());
    }

    public static Set<GraphQLInterfaceType> valuesAsSet() {
        final Set<GraphQLInterfaceType> types = new HashSet<>();

        for(final InterfaceType type : InterfaceType.values()) {
            if(type.getType()!=null) {
                if(!EnterpriseType.class.isAssignableFrom(type.baseContentType)
                        || LicenseUtil.getLevel() > LicenseLevel.COMMUNITY.level) {
                    types.add(type.getType());
                }
            }
        }

        return types;
    }

    public static GraphQLInterfaceType getInterfaceForBaseType(final BaseContentType baseContentType) {
        GraphQLInterfaceType type = null;
        try {
            type = InterfaceType.valueOf(baseContentType.name()).getType();
        } catch (IllegalArgumentException e) {
            Logger.debug(InterfaceType.class, "No GraphQL Interface for this base type: " + baseContentType.name());
        }

        return type;
    }

}