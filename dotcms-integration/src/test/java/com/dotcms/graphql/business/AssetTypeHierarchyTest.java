package com.dotcms.graphql.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.graphql.InterfaceType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.liferay.portal.model.User;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Locks the shape of the asset type hierarchy, as opposed to the behaviour of queries against it —
 * that is {@link AssetSubtypeAccessTest}'s job. Issue #34540.
 *
 * <p>Four <b>independent</b> interfaces — none implements another, and graphql-java's
 * interface-implements-interface support is not used here:
 *
 * <pre>
 *   DotContentlet      every content type
 *   ContentBaseType    CONTENT-derived
 *   DotAssetBaseType   DOTASSET-derived
 *   FileBaseType       FILEASSET-derived
 *   DotFileasset       every asset content type, BOTH base types
 * </pre>
 *
 * <p>The only structure is that each object type declares the ones that apply, side by side:
 *
 * <pre>
 *   Images    implements DotContentlet, DotAssetBaseType, DotFileasset
 *   FileAsset implements DotContentlet, FileBaseType,     DotFileasset
 *   Blog      implements DotContentlet, ContentBaseType
 * </pre>
 *
 * <p>They share the flat properties because the same fields are declared on each, not because one
 * inherits from another. Adding a field to one does <b>not</b> give it to the others, and every
 * implementing object type must carry it or the whole schema build fails.
 *
 * <p>Two invariants here are easy to break without noticing, and neither shows up as a failing
 * query — the first fails the <b>entire schema build</b>, the second only makes a client's query
 * change shape depending on which clause it narrows through:
 * <ol>
 *   <li>Every content type derived from an asset base type must implement {@code DotFileasset}.</li>
 *   <li>The flat properties must be reachable through <b>every</b> surface — the asset interface,
 *       both base-type interfaces, and the concrete types — not just some of them.</li>
 * </ol>
 */
public class AssetTypeHierarchyTest extends IntegrationTestBase {

    /**
     * The long-standing properties of an asset-pointing field. All six, {@code description}
     * included — nothing an asset field could select before is unreachable now. What
     * {@code description} answers with depends on how the asset was reached, which is what it
     * already did before this work; see {@code AssetDescriptionDataFetcher}.
     */
    private static final List<String> FLAT_PROPERTIES =
            List.of("fileName", "description", "fileAsset", "metaData", "showOnMenu", "sortOrder");

    /**
     * The binary's own properties, flattened onto the asset so a client need not descend into the
     * binary field. Ten of the thirteen {@code DotBinary} carries. {@code title} and {@code modDate}
     * are excluded because on a contentlet those names already mean the contentlet's own, and
     * {@code focalPoint} because it has no meaning for a contentlet.
     */
    private static final List<String> BINARY_PROPERTIES =
            List.of("name", "size", "mime", "versionPath", "idPath", "path", "sha256",
                    "isImage", "width", "height");

    private static User systemUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
    }

    /**
     * Given: content types derived from each asset base type.
     * When: the schema is built.
     * Then: each declares its base-type interface, the asset interface and the contentlet
     * interface — all three, side by side.
     */
    @Test
    public void test_assetContentTypes_declareAllThreeInterfaces() throws Exception {
        final ContentType dotAssetType = newDotAssetType();
        final ContentType fileAssetType = newFileAssetType();

        final GraphQLSchema schema = rebuiltSchema();

        assertEquals("a DOTASSET-derived type must declare exactly these interfaces",
                Set.of(InterfaceType.DOTASSET_INTERFACE_NAME,
                        InterfaceType.ASSET_INTERFACE_NAME,
                        InterfaceType.DOT_CONTENTLET),
                interfacesOf(schema, dotAssetType.variable()));

        assertEquals("a FILEASSET-derived type must declare exactly these interfaces",
                Set.of(InterfaceType.FILE_INTERFACE_NAME,
                        InterfaceType.ASSET_INTERFACE_NAME,
                        InterfaceType.DOT_CONTENTLET),
                interfacesOf(schema, fileAssetType.variable()));
    }

    /**
     * Given: the asset interface.
     * When: its possible types are inspected.
     * Then: they include content derived from <b>both</b> base types.
     *
     * <p>This is what lets an Image field return file-style content, which dotCMS permits and does
     * today. An interface covering only one base type would silently drop the other.
     */
    @Test
    public void test_assetInterface_spansBothBaseTypes() throws Exception {
        final ContentType dotAssetType = newDotAssetType();
        final ContentType fileAssetType = newFileAssetType();

        final GraphQLSchema schema = rebuiltSchema();
        final Set<String> possible = schema
                .getImplementations((GraphQLInterfaceType) schema
                        .getType(InterfaceType.ASSET_INTERFACE_NAME))
                .stream().map(GraphQLObjectType::getName).collect(Collectors.toSet());

        assertTrue("the DOTASSET-derived type must be a possible type",
                possible.contains(dotAssetType.variable()));
        assertTrue("the FILEASSET-derived type must be a possible type",
                possible.contains(fileAssetType.variable()));
    }

    /**
     * Given: the asset interface, both base-type interfaces, and a concrete type of each kind.
     * When: the flat properties are looked for.
     * Then: every one of them carries every property.
     *
     * <p>A gap here does not fail anything outright — it makes the same property selectable through
     * one clause and not another, so a client's query changes shape depending on how it narrows.
     * That is exactly what this test exists to prevent; the DOTASSET interface was missing them at
     * one point and nothing else noticed.
     */
    @Test
    public void test_flatProperties_reachableThroughEverySurface() throws Exception {
        final ContentType dotAssetType = newDotAssetType();
        final ContentType fileAssetType = newFileAssetType();

        final GraphQLSchema schema = rebuiltSchema();

        for (final String surface : List.of(
                InterfaceType.ASSET_INTERFACE_NAME,
                InterfaceType.DOTASSET_INTERFACE_NAME,
                InterfaceType.FILE_INTERFACE_NAME,
                dotAssetType.variable(),
                fileAssetType.variable())) {

            final Set<String> fields = fieldNamesOf(schema, surface);
            for (final String property : BINARY_PROPERTIES) {
                assertTrue("binary property '" + property + "' must be reachable through '"
                                + surface + "' — a client writing the same selection against a "
                                + "Binary field and against an asset field should not need two "
                                + "different queries",
                        fields.contains(property));
            }
            for (final String property : FLAT_PROPERTIES) {
                assertTrue("'" + property + "' must be reachable through '" + surface
                                + "' — otherwise the same query changes shape depending on which "
                                + "clause a client narrows through",
                        fields.contains(property));
            }
        }
    }

    /**
     * Given: the interfaces an asset field can be narrowed through.
     * When: {@code description} is looked for.
     * Then: every one of them declares it.
     *
     * <p>The hard case of this work, and the reason it breaks nobody. Two meanings have always
     * shared this name — an asset-pointing field answered it with the contentlet title, while the
     * content type's own field holds what an editor typed — so a single declaration would have
     * had to pick one and silently change the other. Declaring it everywhere and letting the
     * query path select the meaning keeps both contracts, which is why no client has to touch a
     * query.
     *
     * @see com.dotcms.graphql.datafetcher.AssetDescriptionDataFetcher
     */
    @Test
    public void test_description_isOnEveryAssetSurface() throws Exception {
        final GraphQLSchema schema = rebuiltSchema();

        for (final String surface : List.of(
                InterfaceType.ASSET_INTERFACE_NAME,
                InterfaceType.DOTASSET_INTERFACE_NAME,
                InterfaceType.FILE_INTERFACE_NAME)) {
            assertTrue("'description' must be declared on '" + surface
                            + "': an asset field could always select it, and a selection that "
                            + "stops validating is the one break this work refuses",
                    fieldNamesOf(schema, surface).contains("description"));
        }
    }

    /**
     * Given: a content type with an Image field.
     * When: the field's declared type is inspected.
     * Then: it is the asset interface, under the name clients already write in their clauses.
     *
     * <p>Keeping that name is what lets an existing {@code ... on DotFileasset} clause stay valid
     * and keep returning data: a fragment on the position's own interface always matches.
     */
    @Test
    public void test_assetPointingField_isTypedByTheInterfaceUnderTheFamiliarName()
            throws Exception {
        final ContentType holder = new ContentTypeDataGen().nextPersisted();
        new FieldDataGen().contentTypeId(holder.id()).type(ImageField.class)
                .dataType(DataTypes.TEXT).velocityVarName("hierarchyImage").nextPersisted();

        final GraphQLSchema schema = rebuiltSchema();
        final GraphQLFieldDefinition field = schema.getObjectType(holder.variable())
                .getFieldDefinition("hierarchyImage");

        assertNotNull("the Image field must be in the schema", field);
        assertTrue("an asset-pointing field must be typed by an interface, so clauses can narrow",
                field.getType() instanceof GraphQLInterfaceType);
        assertEquals("and it must keep the name clients already write in their clauses",
                "DotFileasset", ((GraphQLInterfaceType) field.getType()).getName());
    }

    // ---------------------------------------------------------------- helpers

    private GraphQLSchema rebuiltSchema() throws Exception {
        APILocator.getGraphqlAPI().invalidateSchema();
        return APILocator.getGraphqlAPI().getSchema(systemUser);
    }

    private Set<String> interfacesOf(final GraphQLSchema schema, final String typeName) {
        final GraphQLObjectType type = schema.getObjectType(typeName);
        assertNotNull("type '" + typeName + "' is not in the schema", type);
        return type.getInterfaces().stream()
                .map(iface -> ((GraphQLInterfaceType) iface).getName())
                .collect(Collectors.toSet());
    }

    private Set<String> fieldNamesOf(final GraphQLSchema schema, final String typeName) {
        final graphql.schema.GraphQLType type = schema.getType(typeName);
        assertNotNull("type '" + typeName + "' is not in the schema", type);

        final List<GraphQLFieldDefinition> definitions = type instanceof GraphQLInterfaceType
                ? ((GraphQLInterfaceType) type).getFieldDefinitions()
                : ((GraphQLObjectType) type).getFieldDefinitions();

        return definitions.stream().map(GraphQLFieldDefinition::getName)
                .collect(Collectors.toSet());
    }

    private ContentType newDotAssetType() throws Exception {
        final String variable = "hierarchyDotAsset" + System.nanoTime();
        return APILocator.getContentTypeAPI(systemUser).save(
                ContentTypeBuilder.builder(DotAssetContentType.class)
                        .folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .name(variable).variable(variable)
                        .owner(systemUser.getUserId()).build());
    }

    private ContentType newFileAssetType() throws Exception {
        final String variable = "hierarchyFileAsset" + System.nanoTime();
        return APILocator.getContentTypeAPI(systemUser).save(
                ContentTypeBuilder.builder(FileAssetContentType.class)
                        .folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .name(variable).variable(variable)
                        .owner(systemUser.getUserId()).build());
    }
}
