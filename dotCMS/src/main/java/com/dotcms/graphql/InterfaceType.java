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
    public static final String ASSET_CONTENT_INTERFACE_NAME = "DotFileasset";

    public static final String DOT_CONTENTLET = "DotContentlet";

    static {

        Map<String, TypeFetcher> contentFields = ContentFields.getContentFields();

        CONTENT_INTERFACE_FIELDS.addAll(contentFields.keySet());

        GraphQLInterfaceType contentletInterface = createInterfaceType(DOT_CONTENTLET,
                contentFields, new ContentResolver());

        interfaceTypes.put("CONTENTLET", contentletInterface);

        interfaceTypes.put("CONTENT", createInterfaceType(CONTENT_INTERFACE_NAME, contentFields, new ContentResolver()));

        final Map<String, TypeFetcher> fileAssetFields = new HashMap<>(contentFields);
        addBaseTypeFields(fileAssetFields, ImmutableFileAssetContentType.builder().name("dummy")
                .build().requiredFields());
        // Same flat properties the asset interface carries. Every possible type of this interface
        // already has them, and leaving them off would make `fileName` selectable on the concrete
        // types and on the asset interface but not here — the same query changing shape depending
        // on which clause a client happens to narrow through. See #34540.
        fileAssetFields.putAll(CustomFieldType.getAssetFlatFields());
        interfaceTypes.put("FILEASSET", createInterfaceType(FILE_INTERFACE_NAME, fileAssetFields, new ContentResolver()));

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

        final Map<String, TypeFetcher> dotAssetFields = new HashMap<>(contentFields);
        addBaseTypeFields(dotAssetFields, ImmutableDotAssetContentType.builder().name("dummy")
                .build().requiredFields());
        // See the note on the FILEASSET interface above: DOTASSET content has no stored file name,
        // but every DOTASSET-derived object type carries the synthesized one, so the interface must
        // too or `fileName` is reachable everywhere except through this clause.
        dotAssetFields.putAll(CustomFieldType.getAssetFlatFields());
        interfaceTypes.put("DOTASSET", createInterfaceType(DOTASSET_INTERFACE_NAME, dotAssetFields, new ContentResolver()));

        // Carries the common content fields plus the flat properties an asset-pointing field has
        // always exposed, so that retyping such a field to this interface leaves those selections
        // working. Every implementing object type must therefore carry them too -- synthesized for
        // DOTASSET-derived types, already present on FILEASSET-derived ones.
        //
        // `description` is deliberately absent. FILEASSET content stores one; DOTASSET content does
        // not, and the flat view answered it with the contentlet title instead. Declaring it here
        // would make DOTASSET-derived types answer with their own stored description -- the same
        // name quietly returning a different value, which is the one outcome this work refuses.
        // It is reachable, correctly, through a narrowing clause on the concrete type.
        final Map<String, TypeFetcher> assetContentFields = new HashMap<>(contentFields);
        assetContentFields.putAll(CustomFieldType.getAssetFlatFields());

        assetContentInterface = createInterfaceType(ASSET_CONTENT_INTERFACE_NAME,
                assetContentFields, new ContentResolver());
    }

    private static GraphQLInterfaceType assetContentInterface;


    /**
     * @return the interface describing what an asset-pointing field returns, implemented by every
     * content type derived from either asset base type.
     */
    public static GraphQLInterfaceType getAssetContentInterface() {
        return assetContentInterface;
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