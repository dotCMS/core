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
        interfaceTypes.put("DOTASSET", createInterfaceType(DOTASSET_INTERFACE_NAME, dotAssetFields, new ContentResolver()));
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