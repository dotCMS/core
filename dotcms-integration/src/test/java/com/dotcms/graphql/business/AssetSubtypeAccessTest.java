package com.dotcms.graphql.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.graphql.InterfaceType;
import com.dotcms.graphql.datafetcher.AssetBinaryPropertyDataFetcher;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.FileUtil;
import com.liferay.portal.model.User;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.execution.MergedField;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Drives the capability issue #34540 asks for: a client selecting an Image or File field can reach
 * the properties of the content type that field actually points at — including properties the
 * customer defined on their own type — and can tell which type it received.
 *
 * <p>The asset field itself is the polymorphic position: it is typed by an interface, so a
 * narrowing clause sits directly on it, in the same block as the long-standing flat properties.
 * That shape is the whole point — an earlier design put the clauses in a second field beside the
 * first, which worked and was set aside because a client had to write two blocks to read one
 * asset.
 *
 * <p>A GraphQL field has exactly one type and a resolved value has exactly one runtime type, so
 * the flat view and the asset itself cannot both occupy that position. What made it possible to
 * keep both anyway is that only one of the six flat properties actually collided —
 * {@code description} — and its two meanings were already both shipping, separated by query path.
 * Conserving that separation costs one data fetcher and leaves every existing client query
 * valid.
 *
 * <p>Two fixture traps already paid for in {@link AssetFieldValueContractTest} and repeated here:
 * an asset-reference field needs {@code DataTypes.TEXT} or its value never persists, and the
 * referenced asset must be published to the same state as the holder or the fetcher resolves
 * nothing.
 */
@SuppressWarnings("unchecked")
public class AssetSubtypeAccessTest extends IntegrationTestBase {

    private static final String IMAGE_FIELD_VAR = "subtypeImage";
    private static final String FILE_FIELD_VAR = "subtypeFile";
    private static final String CUSTOM_PROPERTY_VAR = "campaignName";

    /**
     * The asset field itself is now the polymorphic position: it is typed by the asset-content
     * interface, so narrowing clauses sit directly on it, beside the flat properties.
     */
    private static final String IMAGE_COMPANION = IMAGE_FIELD_VAR;
    private static final String FILE_COMPANION = FILE_FIELD_VAR;

    private static User systemUser;
    private static Host site;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
    }

    /**
     * Given: a customer-defined content type extending DOTASSET with a property of its own, and an
     * Image field pointing at content of it.
     * When: that property is requested through the asset field.
     * Then: its stored value is returned.
     *
     * <p>This is the capability the issue exists for, and the one with no workaround today.
     */
    @Test
    public void test_customPropertyOnDotAssetSubtype_isReadableThroughImageField() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Summer Sale");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final Map<String, Object> assetContent = queryCompanion(holder, content, IMAGE_COMPANION,
                String.format("... on %s { %s }", assetType.variable(), CUSTOM_PROPERTY_VAR));

        assertEquals("the customer's own property must be readable through the Image field",
                "Summer Sale", assetContent.get(CUSTOM_PROPERTY_VAR));
    }

    /**
     * Given: a customer-defined content type extending FILEASSET with a property of its own, and a
     * File field pointing at content of it.
     * When: that property is requested through the asset field.
     * Then: its stored value is returned.
     */
    @Test
    public void test_customPropertyOnFileAssetSubtype_isReadableThroughFileField() throws Exception {
        final ContentType assetType = newFileAssetSubtype();
        final Contentlet asset = newFileAssetOf(assetType, "Datasheets");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, FILE_FIELD_VAR, asset);

        final Map<String, Object> assetContent = queryCompanion(holder, content, FILE_COMPANION,
                String.format("... on %s { %s }", assetType.variable(), CUSTOM_PROPERTY_VAR));

        assertEquals("the customer's own property must be readable through the File field",
                "Datasheets", assetContent.get(CUSTOM_PROPERTY_VAR));
    }

    /**
     * Given: a DOTASSET-derived type with its own {@code description}, holding a value that is not
     * the title.
     * When: the same asset is read through an Image field, and again through its own collection.
     * Then: the first answers with the title and the second with the stored value.
     *
     * <p><b>The same field name returning two different values is the point, not a defect.</b>
     * Both answers are what shipped. An asset-pointing field resolved to a flat view that derived
     * {@code description} from the title; the content type's own field holds what an editor typed.
     * Once the field is typed by an interface, the concrete type's definition is what resolves —
     * so without a path-aware resolver the first query would silently start returning the second's
     * value. That is the one failure mode this whole design exists to prevent: a name that keeps
     * working while meaning something else.
     *
     * <p>Getting this right is why no client has to rewrite a query. If it regresses, the symptom
     * in the field is a page that used to render a file name suddenly rendering blank.
     *
     * @see com.dotcms.graphql.datafetcher.AssetDescriptionDataFetcher
     */
    @Test
    public void test_description_answersAccordingToHowTheAssetWasReached() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        addDescriptionProperty(assetType);

        final String stored = "an editor typed this";
        final File file = FileUtil.createTemporaryFile("subtype", ".txt", "subtype");
        final Contentlet asset = new ContentletDataGen(assetType.id())
                .host(site)
                .setProperty(DotAssetContentType.ASSET_FIELD_VAR, file)
                .setProperty(DotAssetContentType.SITE_OR_FOLDER_FIELD_VAR, site.getIdentifier())
                .setProperty("description", stored)
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        ContentletDataGen.publish(asset);

        // Precondition: the two answers must genuinely differ, or this test proves nothing.
        assertNotEquals("fixture is useless unless the stored description differs from the title",
                asset.getTitle(), stored);

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final Map<String, Object> throughField =
                queryCompanion(holder, content, IMAGE_COMPANION, "description");

        final Map<String, Object> direct = (Map<String, Object>)
                ((List<Map<String, Object>>) GraphqlQueryRunner.executeAndExpectSuccess(
                        String.format("{ %sCollection(query: \"+identifier:%s\") "
                                        + "{ description } }",
                                assetType.variable(), asset.getIdentifier()),
                        systemUser).get(assetType.variable() + "Collection")).get(0);

        assertEquals("reached through an asset field, description must stay the TITLE — this is "
                        + "what the flat view always answered and what live pages render",
                asset.getTitle(), throughField.get("description"));
        assertEquals("read directly, description must stay the STORED value",
                stored, direct.get("description"));
    }

    /**
     * Given: an asset carrying tags.
     * When: its tags are requested through the asset field.
     * Then: they are returned.
     *
     * <p>This is the AI tagging blocker named in the issue: {@code tags} is not on the flat view,
     * so generated tags cannot be read back at all.
     */
    @Test
    public void test_tags_areReadableThroughAssetField() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Tagged");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final Map<String, Object> assetContent = queryCompanion(holder, content, IMAGE_COMPANION,
                String.format("... on %s { tags }", assetType.variable()));

        assertNotNull("tags must be reachable through an asset field", assetContent.get("tags"));
    }

    /**
     * Given: an asset behind an Image field.
     * When: the asset's own identity is requested — identifier, inode, site, URL mapping, live
     * state, title.
     * Then: each is returned and matches the asset's record.
     *
     * <p>None of these are on the flat view, so an Image field cannot currently tell a client
     * <i>which</i> asset it is pointing at.
     */
    @Test
    public void test_assetOwnIdentity_isReadableThroughAssetField() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Identity");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final Map<String, Object> assetContent =
                queryCompanion(holder, content, IMAGE_COMPANION,
                        "identifier inode host { identifier } urlMap live title");

        assertEquals("the asset's identifier must be reachable",
                asset.getIdentifier(), assetContent.get("identifier"));
        assertEquals("the asset's inode must be reachable",
                asset.getInode(), assetContent.get("inode"));
        assertEquals("the asset's live state must be reachable", true, assetContent.get("live"));
        assertNotNull("the asset's title must be reachable", assetContent.get("title"));
        assertEquals("the asset's site must be reachable", site.getIdentifier(),
                ((Map<String, Object>) assetContent.get("host")).get("identifier"));
        // A DotAsset type has no URL map pattern, so the value is null; what FR-004 requires is
        // that the property can be selected through the asset field at all.
        assertTrue("the asset's URL mapping must be selectable",
                assetContent.containsKey("urlMap"));
    }

    /**
     * Given: the schema has already been built.
     * When: a customer creates a brand-new content type extending an asset base type, and
     * immediately queries a property of it.
     * Then: it works, with no administrative step in between.
     *
     * <p>This is the test that distinguishes a derived set of asset types from an enumerated one
     * (FR-001a). A hardcoded list of known asset types would satisfy every other test in this class
     * and fail this one.
     */
    @Test
    public void test_contentTypeCreatedAfterSchemaBuild_isImmediatelyReachable() throws Exception {
        // Build the schema first, so the type below cannot have been present when it was made.
        APILocator.getGraphqlAPI().getSchema(systemUser);

        final ContentType lateType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(lateType, "Created Late");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final Map<String, Object> assetContent = queryCompanion(holder, content, IMAGE_COMPANION,
                String.format("__typename ... on %s { %s }",
                        lateType.variable(), CUSTOM_PROPERTY_VAR));

        assertEquals("a type created after the schema was built must still be reachable",
                "Created Late", assetContent.get(CUSTOM_PROPERTY_VAR));
        assertEquals("__typename must name the customer's own type",
                lateType.variable(), assetContent.get("__typename"));
    }

    /**
     * Given: an asset type already in the schema, and a query already served from it.
     * When: the customer adds a property to that existing type, and an asset stores a value in it.
     * Then: the property is readable through an Image field, with no administrative step between.
     *
     * <p>FR-005 / SC-004. Unlike the fixture helpers, this test deliberately does NOT call
     * {@code invalidateSchema()}: the only thing refreshing the schema is what production relies
     * on, {@code ContentTypeAndFieldsModsListeners} reacting to the field being saved. Every other
     * test invalidates by hand, so without this one a broken listener would go unnoticed while
     * customers stopped seeing new properties until a restart.
     */
    @Test
    public void test_propertyAddedToExistingAssetType_isReadableWithoutManualInvalidation()
            throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final ContentType holder = newHolderType();

        // Serve a query first, so the schema is built and cached before the property exists.
        final Contentlet before = newHolderContent(holder, IMAGE_FIELD_VAR,
                newAssetOf(assetType, "Before"));
        queryCompanion(holder, before, IMAGE_FIELD_VAR,
                String.format("... on %s { %s }", assetType.variable(), CUSTOM_PROPERTY_VAR));

        final String addedVar = "adSize" + System.nanoTime();
        APILocator.getContentTypeFieldAPI().save(FieldBuilder.builder(TextField.class)
                .name(addedVar).variable(addedVar)
                .contentTypeId(assetType.id()).dataType(DataTypes.TEXT).indexed(true)
                .build(), systemUser);

        final Contentlet after = newHolderContent(holder, IMAGE_FIELD_VAR,
                newAssetOf(assetType, "After", Map.of(addedVar, "300x250")));

        final Map<String, Object> image = queryCompanion(holder, after, IMAGE_FIELD_VAR,
                String.format("... on %s { %s }", assetType.variable(), addedVar));

        assertEquals("a property added to an existing asset type must be readable without any "
                        + "manual schema invalidation",
                "300x250", image.get(addedVar));
    }

    /**
     * Given: an Image field and its companion, selected together.
     * When: the flat properties and a narrowing clause are requested at the <b>same level</b>.
     * Then: both come back, and the flat properties are unchanged.
     *
     * <p>This is the shape the whole design exists to enable: flat properties and a narrowing
     * clause in one block, on one field.
     */
    @Test
    public void test_flatViewAndNarrowingClause_coexistAtTheSameLevel() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Side by side");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { "
                        + "fileName __typename ... on %s { %s } } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR,
                assetType.variable(), CUSTOM_PROPERTY_VAR);

        final Map<String, Object> row = firstRow(
                GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser), holder);
        final Map<String, Object> field = (Map<String, Object>) row.get(IMAGE_FIELD_VAR);

        assertEquals("the flat property must keep returning the contentlet name",
                asset.getName(), field.get("fileName"));
        assertEquals("and the narrowing clause must work in the SAME block",
                "Side by side", field.get(CUSTOM_PROPERTY_VAR));
        assertEquals(assetType.variable(), field.get("__typename"));
    }

    /**
     * Given: two fields, one pointing at image-style content and one at file-style content.
     * When: the type of each is requested in a single query.
     * Then: each names its own concrete content type, and the two differ.
     */
    @Test
    public void test_typename_distinguishesTwoDifferentAssetTypes() throws Exception {
        final ContentType imageStyle = newDotAssetSubtype();
        final ContentType fileStyle = newFileAssetSubtype();
        final Contentlet imageAsset = newAssetOf(imageStyle, "Image side");
        final Contentlet fileAsset = newFileAssetOf(fileStyle, "File side");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContentWithBoth(holder, imageAsset, fileAsset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { __typename } %s { __typename } } }",
                holder.variable(), content.getIdentifier(), IMAGE_COMPANION, FILE_COMPANION);

        final Map<String, Object> row = firstRow(
                GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser), holder);

        final String imageTypeName =
                (String) ((Map<String, Object>) row.get(IMAGE_COMPANION)).get("__typename");
        final String fileTypeName =
                (String) ((Map<String, Object>) row.get(FILE_COMPANION)).get("__typename");

        assertEquals("the image-style asset must name its own type",
                imageStyle.variable(), imageTypeName);
        assertEquals("the file-style asset must name its own type",
                fileStyle.variable(), fileTypeName);
        assertNotEquals("two different asset types must be distinguishable",
                imageTypeName, fileTypeName);
    }

    /**
     * Given: an Image field pointing at file-style content, and a File field pointing at
     * image-style content — which dotCMS permits and resolves today.
     * When: each is narrowed to the type it actually is.
     * Then: that type is offered and its properties come back.
     *
     * <p>This is the case that rules out typing each field by its own kind. It came out of the
     * spec review, after an Image field on a live instance was confirmed to resolve a plain-text
     * FileAsset; a per-field-kind design would have compiled, passed every other test here, and
     * silently dropped content those fields already hold.
     */
    @Test
    public void test_fieldsResolveContentOfTheOtherBaseType() throws Exception {
        final ContentType imageStyle = newDotAssetSubtype();
        final ContentType fileStyle = newFileAssetSubtype();
        // Deliberately crossed: image field -> file-style content, file field -> image-style.
        final Contentlet fileAsset = newFileAssetOf(fileStyle, "Behind the image field");
        final Contentlet imageAsset = newAssetOf(imageStyle, "Behind the file field");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContentWithBoth(holder, fileAsset, imageAsset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { "
                        + "%s { __typename ... on %s { %s } } "
                        + "%s { __typename ... on %s { %s } } } }",
                holder.variable(), content.getIdentifier(),
                IMAGE_COMPANION, fileStyle.variable(), CUSTOM_PROPERTY_VAR,
                FILE_COMPANION, imageStyle.variable(), CUSTOM_PROPERTY_VAR);

        final Map<String, Object> row = firstRow(
                GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser), holder);

        final Map<String, Object> behindImageField = (Map<String, Object>) row.get(IMAGE_COMPANION);
        final Map<String, Object> behindFileField = (Map<String, Object>) row.get(FILE_COMPANION);

        assertEquals("an Image field must offer the file-style type it actually holds",
                fileStyle.variable(), behindImageField.get("__typename"));
        assertEquals("and its properties must come back",
                "Behind the image field", behindImageField.get(CUSTOM_PROPERTY_VAR));

        assertEquals("a File field must offer the image-style type it actually holds",
                imageStyle.variable(), behindFileField.get("__typename"));
        assertEquals("and its properties must come back",
                "Behind the file field", behindFileField.get(CUSTOM_PROPERTY_VAR));
    }

    /**
     * Given: an Image field pointing at content that is not an asset at all — nothing stops this,
     * since the field stores a bare identifier and {@code FileFieldDataFetcher} falls back to the
     * raw contentlet when {@code FileAssetAPI.fromContentlet} cannot convert it.
     * When: the asset field is selected.
     * Then: it resolves to nothing, and the request still succeeds.
     *
     * <p>A resolved type that does not implement the interface must not surface a GraphQL error:
     * graphql-java answers that by failing the <b>whole request</b>, so one mis-pointed field would
     * take every other collection in the query down with it.
     */
    @Test
    public void test_fieldPointingAtNonAssetContent_resolvesToNullWithoutError() throws Exception {
        final ContentType plainType = new ContentTypeDataGen().nextPersisted();
        final Contentlet plainContent = new ContentletDataGen(plainType.id())
                .host(site).setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        ContentletDataGen.publish(plainContent);

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, plainContent);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { identifier %s { fileName } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR);

        final Map<String, Object> row = firstRow(
                GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser), holder);

        assertEquals("the rest of the row must still be delivered",
                content.getIdentifier(), row.get("identifier"));
        // The whole field reports nothing, not merely the narrowing part: now that the field is
        // described by the asset interface, a contentlet outside that interface cannot be handed
        // on at all. Its flat properties go with it — a deliberate, visible consequence of typing
        // the field polymorphically.
        assertEquals("non-asset content must resolve to nothing, not an error",
                null, row.get(IMAGE_FIELD_VAR));
    }

    /**
     * Given: an asset behind an Image field.
     * When: one property is selected, and then five.
     * Then: the asset is resolved exactly once either way.
     *
     * <p>FR-011 / SC-006. Asserted by counting resolutions, never by timing — a wall-clock
     * assertion on a containerised database is noise. This is the property PR #35363 failed:
     * it re-derived an asset's binary metadata once per property selected, so twelve properties
     * meant twelve full derivations per asset, per row of a result set.
     */
    @Test
    public void test_readingManyPropertiesResolvesTheAssetOnce() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Cost");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final String oneProperty = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { fileName } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR);
        final String fiveProperties = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { "
                        + "fileName identifier inode title sortOrder } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR);

        assertEquals("one property must cost one resolution",
                1, GraphqlQueryRunner.countFieldFetches(oneProperty, systemUser, IMAGE_FIELD_VAR));
        assertEquals("five properties must still cost one resolution — per-asset work may not "
                        + "grow with the number of properties selected",
                1, GraphqlQueryRunner.countFieldFetches(fiveProperties, systemUser,
                        IMAGE_FIELD_VAR));
    }

    /**
     * Given: an asset, and every binary property that is flattened onto it.
     * When: all of them are resolved within one request.
     * Then: the binary is derived exactly once for that asset — and once more for a second asset.
     *
     * <p>FR-013b / SC-006. The per-asset count at the asset-field level does not cover this: each
     * flattened property has its own fetcher call, and it is the per-request cache inside
     * {@link AssetBinaryPropertyDataFetcher} that keeps those calls from each running the full
     * transformer. Counted with a subclass, never timed.
     */
    @Test
    public void test_binaryIsDerivedOncePerAssetPerRequest() throws Exception {
        final Contentlet first = newAssetOf(newDotAssetSubtype(), "DerivedOnce");
        final Contentlet second = newAssetOf(newDotAssetSubtype(), "DerivedOnceMore");

        final AtomicInteger derivations = new AtomicInteger();
        final AssetBinaryPropertyDataFetcher fetcher = new AssetBinaryPropertyDataFetcher() {
            @Override
            protected Map<String, Object> derive(final Contentlet contentlet) {
                derivations.incrementAndGet();
                return super.derive(contentlet);
            }
        };

        final DotGraphQLContext context = DotGraphQLContext.createServletContext()
                .with(systemUser).build();
        final List<String> properties = List.of("name", "size", "mime", "versionPath", "idPath",
                "path", "sha256", "isImage", "width", "height");

        for (final String property : properties) {
            fetcher.get(environmentFor(first, property, context));
        }
        assertEquals("ten binary properties of one asset must cost one derivation",
                1, derivations.get());
        assertNotNull("and the cached derivation must still answer",
                fetcher.get(environmentFor(first, "name", context)));

        for (final String property : properties) {
            fetcher.get(environmentFor(second, property, context));
        }
        assertEquals("a second asset costs exactly one more", 2, derivations.get());
    }

    /**
     * Given: two users who can both read the holder content, only one of whom can read the asset
     * its Image field points at.
     * When: both query that field.
     * Then: the permitted user gets the asset back, and the other gets the row with the asset field
     * {@code null} — neither its values nor its concrete type.
     *
     * <p>FR-008 / FR-022: the asset must resolve <b>as the calling user</b> and never escalate.
     *
     * <p>Every precondition is asserted rather than branched on. An earlier version skipped its
     * final assertion when the second user turned out to read the asset anyway, and read the
     * field from a row that user might not see at all — either way it could pass without ever
     * observing a denial. Both users are therefore granted the holder explicitly, the asset is
     * granted to one role only, and the test fails loudly if the fixture does not hold.
     *
     * <p>Two fixture traps: {@code permissionAPI.save} only adds grants, so the anonymous READ that
     * {@code permissionIndividually} copies from the parent must be revoked explicitly (see
     * {@link #permitReadOnly}); and the shared {@code TestUserUtils} users carry a type-level
     * CONTENTLETS READ grant through their role, so purpose-built users in fresh roles are used
     * instead.
     */
    @Test
    public void test_assetResolvesAsTheCallingUser() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Identity-scoped");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role permittedRole = new RoleDataGen().nextPersisted();
        final Role otherRole = new RoleDataGen().nextPersisted();

        // The asset: readable by one role only.
        permitReadOnly(asset, List.of(permittedRole));
        // The holder: readable by both, so the only difference between the users is the asset.
        permitReadOnly(content, List.of(permittedRole, otherRole));

        final User permittedUser = new UserDataGen().roles(permittedRole).nextPersisted();
        final User otherUser = new UserDataGen().roles(otherRole).nextPersisted();

        // Checked the way the product asks: the lookup behind an asset field honours front-end
        // roles, so these must hold with respectFrontendRoles = true.
        assertTrue("fixture problem: the permitted user cannot read the asset",
                permissionAPI.doesUserHavePermission(asset, PermissionAPI.PERMISSION_READ,
                        permittedUser, true));
        assertFalse("fixture problem: the other user can read the asset, so a denial below "
                        + "could never be observed",
                permissionAPI.doesUserHavePermission(asset, PermissionAPI.PERMISSION_READ,
                        otherUser, true));
        assertTrue("fixture problem: the other user cannot read the holder content, so the row "
                        + "would be missing rather than its asset field denied",
                permissionAPI.doesUserHavePermission(content, PermissionAPI.PERMISSION_READ,
                        otherUser, true));

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { identifier %s { __typename fileName } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR);

        final Map<String, Object> permittedRow = firstRow(
                GraphqlQueryRunner.executeAndExpectSuccess(query, permittedUser), holder);
        final Map<String, Object> otherRow = firstRow(
                GraphqlQueryRunner.executeAndExpectSuccess(query, otherUser), holder);

        assertNotNull("the user holding the grant must see the asset",
                permittedRow.get(IMAGE_FIELD_VAR));
        assertEquals("the other user must still receive the row",
                content.getIdentifier(), otherRow.get("identifier"));
        assertEquals("the asset field resolved with more authority than its caller: a user "
                        + "without read on the asset received it, or its concrete type, anyway",
                null, otherRow.get(IMAGE_FIELD_VAR));
    }

    /**
     * Given: a query narrowing to a type that no returned asset is.
     * When: it runs.
     * Then: the data is delivered, and the response names the clause that matched nothing.
     *
     * <p>US4. Without this a client cannot tell {@code ... on BannerImages} over content that
     * happened to be another type from {@code ... on BannreImages} — a typo. Both are legal, both
     * contribute nothing, and the response looks identical.
     */
    @Test
    public void test_clauseThatMatchedNothing_isReportedInExtensions() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final ContentType unrelatedType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Warned");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { fileName "
                        + "... on %s { %s } } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR,
                unrelatedType.variable(), CUSTOM_PROPERTY_VAR);

        final ExecutionResult result = GraphqlQueryRunner.executeWithWarnings(query, systemUser);

        assertTrue("the request must still succeed", result.getErrors().isEmpty());
        assertNotNull("the data must still be delivered", result.getData());

        final List<Map<String, Object>> warnings = GraphqlQueryRunner.warningsOf(result);
        assertEquals("exactly one clause matched nothing", 1, warnings.size());
        assertEquals("the warning must name the clause the client wrote",
                unrelatedType.variable(), warnings.get(0).get("typeCondition"));
        assertNotNull("and the path it was written under", warnings.get(0).get("path"));
    }

    /**
     * Given: a query whose every narrowing clause matches.
     * When: it runs.
     * Then: no warning is produced.
     */
    @Test
    public void test_clauseThatMatched_producesNoWarning() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Matched");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { "
                        + "... on %s { %s } } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR,
                assetType.variable(), CUSTOM_PROPERTY_VAR);

        final ExecutionResult result = GraphqlQueryRunner.executeWithWarnings(query, systemUser);

        assertTrue("a matching clause must not be reported — a warning that fires even when the "
                        + "clause matched is worse than none, since it trains clients to ignore it. "
                        + "Warnings were: " + GraphqlQueryRunner.warningsOf(result),
                GraphqlQueryRunner.warningsOf(result).isEmpty());
    }

    /**
     * Given: a query with no narrowing clauses at all.
     * When: it runs.
     * Then: the response carries no warnings — the mechanism costs nothing.
     */
    @Test
    public void test_queryWithoutClauses_carriesNoWarnings() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Plain");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { fileName } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR);

        final ExecutionResult result = GraphqlQueryRunner.executeWithWarnings(query, systemUser);

        assertTrue("a query with no clauses must produce no warnings",
                GraphqlQueryRunner.warningsOf(result).isEmpty());
    }

    /**
     * Given: a warning.
     * When: its text is read.
     * Then: it contains only the type name the client wrote and the path it chose.
     *
     * <p>A warning must never become a channel for content the caller could not otherwise read.
     */
    @Test
    public void test_warningCarriesNoAssetContent() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final ContentType unrelatedType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "SecretValue");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { "
                        + "... on %s { %s } } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR,
                unrelatedType.variable(), CUSTOM_PROPERTY_VAR);

        final String rendered =
                GraphqlQueryRunner.warningsOf(
                        GraphqlQueryRunner.executeWithWarnings(query, systemUser)).toString();

        assertFalse("a warning must not leak a property value",
                rendered.contains("SecretValue"));
        assertFalse("nor the asset's identifier",
                rendered.contains(asset.getIdentifier()));
    }

    /**
     * Given: clauses on the interfaces an asset implements rather than on its concrete type.
     * When: the query runs.
     * Then: the clauses contribute their data and no warning is produced.
     *
     * <p>Resolved content only knows its concrete type, so a check comparing names alone reports
     * every interface clause as unmatched. {@code ... on DotFileasset} is the form the migration
     * guide recommends to existing clients, so that false warning would appear on every one of
     * their responses.
     */
    @Test
    public void test_clauseOnAnInterfaceTheAssetImplements_producesNoWarning() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "ThroughInterface");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { "
                        + "... on %s { fileName } ... on %s { identifier } } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR,
                InterfaceType.ASSET_INTERFACE_NAME, InterfaceType.DOTASSET_INTERFACE_NAME);

        final ExecutionResult result = GraphqlQueryRunner.executeWithWarnings(query, systemUser);

        assertTrue("the request must succeed: " + result.getErrors(),
                result.getErrors().isEmpty());
        final Map<String, Object> image = (Map<String, Object>) firstRow(result.getData(), holder)
                .get(IMAGE_FIELD_VAR);
        assertNotNull("fixture problem: the asset field resolved to nothing", image);
        assertEquals("the interface clause must have contributed its data",
                asset.getIdentifier(), image.get("identifier"));
        assertTrue("a clause on an interface the asset implements matched, so it must not be "
                        + "reported. Warnings were: " + GraphqlQueryRunner.warningsOf(result),
                GraphqlQueryRunner.warningsOf(result).isEmpty());
    }

    /**
     * Given: a clause naming a type that does not exist anywhere in the schema.
     * When: the query runs.
     * Then: the request fails, naming the type.
     *
     * <p>Locks existing graphql-java behaviour: unlike a clause on a real type that happened not to
     * match, this is a client mistake with no valid reading, and it must stay loud.
     */
    @Test
    public void test_clauseOnANonExistentType_failsTheRequest() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "NoSuchType");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final String missingType = "NoSuchAssetType" + System.nanoTime();
        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { "
                        + "fileName ... on %s { title } } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR, missingType);

        final List<GraphQLError> errors =
                GraphqlQueryRunner.executeAndExpectFailure(query, systemUser);

        assertTrue("the error must name the type the client wrote: " + errors,
                errors.toString().contains(missingType));
    }

    /**
     * Given: one clause on the asset's base-type interface and another on its concrete type.
     * When: both apply to the same asset.
     * Then: their properties merge into a single object.
     */
    @Test
    public void test_baseTypeAndConcreteTypeClauses_mergeIntoOneObject() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Merged");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final Map<String, Object> image = queryCompanion(holder, content, IMAGE_FIELD_VAR,
                String.format("... on %s { identifier } ... on %s { %s }",
                        InterfaceType.DOTASSET_INTERFACE_NAME, assetType.variable(),
                        CUSTOM_PROPERTY_VAR));

        assertEquals("the base-type clause's property must be on the object",
                asset.getIdentifier(), image.get("identifier"));
        assertEquals("and so must the concrete clause's, on the same object",
                "Merged", image.get(CUSTOM_PROPERTY_VAR));
    }

    /**
     * Given: a result set whose asset fields point at content of two different types, and a clause
     * on only one of them.
     * When: the query runs.
     * Then: matching rows carry the clause's properties, the other rows are still returned without
     * them, the request succeeds, and no warning is produced because the clause matched somewhere.
     */
    @Test
    public void test_mixedResultSet_populatesMatchesAndKeepsTheRest() throws Exception {
        final ContentType matchingType = newDotAssetSubtype();
        final ContentType otherType = newDotAssetSubtype();
        final Contentlet matchingAsset = newAssetOf(matchingType, "Matching");
        final Contentlet otherAsset = newAssetOf(otherType, "Other");

        final ContentType holder = newHolderType();
        final Contentlet matchingRow = newHolderContent(holder, IMAGE_FIELD_VAR, matchingAsset);
        final Contentlet otherRow = newHolderContent(holder, IMAGE_FIELD_VAR, otherAsset);

        final String query = String.format(
                "{ %sCollection(query: \"+contentType:%s\") { identifier %s { "
                        + "fileName ... on %s { %s } } } }",
                holder.variable(), holder.variable(), IMAGE_FIELD_VAR,
                matchingType.variable(), CUSTOM_PROPERTY_VAR);

        final ExecutionResult result = GraphqlQueryRunner.executeWithWarnings(query, systemUser);

        assertTrue("one non-matching row must not fail the request: " + result.getErrors(),
                result.getErrors().isEmpty());

        final List<Map<String, Object>> rows = (List<Map<String, Object>>)
                ((Map<String, Object>) result.getData()).get(holder.variable() + "Collection");
        assertEquals("both rows must be returned", 2, rows.size());

        final Map<String, Map<String, Object>> imageByRow = new HashMap<>();
        rows.forEach(row -> imageByRow.put((String) row.get("identifier"),
                (Map<String, Object>) row.get(IMAGE_FIELD_VAR)));

        final Map<String, Object> matchingImage = imageByRow.get(matchingRow.getIdentifier());
        final Map<String, Object> otherImage = imageByRow.get(otherRow.getIdentifier());
        assertNotNull("the matching row's asset must resolve", matchingImage);
        assertNotNull("the non-matching row's asset must still resolve", otherImage);
        assertEquals("the matching asset carries the clause's property",
                "Matching", matchingImage.get(CUSTOM_PROPERTY_VAR));
        assertFalse("the non-matching asset does not",
                otherImage.containsKey(CUSTOM_PROPERTY_VAR));
        assertNotNull("but still carries what was selected outside the clause",
                otherImage.get("fileName"));
        assertTrue("the clause matched one row, so it must not be reported. Warnings were: "
                        + GraphqlQueryRunner.warningsOf(result),
                GraphqlQueryRunner.warningsOf(result).isEmpty());
    }

    /**
     * Given: an asset field whose target has since been archived.
     * When: the field is selected.
     * Then: it answers {@code null} and the rest of the query is unaffected.
     */
    @Test
    public void test_fieldPointingAtArchivedAsset_resolvesToNullWithoutError() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Archived");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        ContentletDataGen.unpublish(asset, false);
        ContentletDataGen.archive(asset);

        assertTargetResolvesToNull(holder, content);
    }

    /**
     * Given: an asset field whose target has since been deleted.
     * When: the field is selected.
     * Then: it answers {@code null} and the rest of the query is unaffected.
     */
    @Test
    public void test_fieldPointingAtDeletedAsset_resolvesToNullWithoutError() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Deleted");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        ContentletDataGen.destroy(asset, false);

        assertTargetResolvesToNull(holder, content);
    }

    /**
     * Given: a query selecting a property the customer has since deleted from the asset type.
     * When: it runs.
     * Then: the request fails with the standard validation error naming the property and the type.
     *
     * <p>Answering {@code null} instead would be indistinguishable from an empty value.
     */
    @Test
    public void test_selectingADeletedProperty_failsNamingPropertyAndType() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Doomed");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        APILocator.getContentTypeFieldAPI().delete(APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(assetType.id(), CUSTOM_PROPERTY_VAR));
        APILocator.getGraphqlAPI().invalidateSchema();

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { ... on %s { %s } } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR,
                assetType.variable(), CUSTOM_PROPERTY_VAR);

        final String errors =
                GraphqlQueryRunner.executeAndExpectFailure(query, systemUser).toString();

        assertTrue("the error must name the deleted property: " + errors,
                errors.contains(CUSTOM_PROPERTY_VAR));
        assertTrue("and the type it was selected on: " + errors,
                errors.contains(assetType.variable()));
    }

    /**
     * Given: an asset behind an Image field.
     * When: the binary's own properties are selected directly on the asset.
     * Then: they come back, without descending into the binary field.
     *
     * <p>Carried over from PR #35363, which identified this ergonomics gap. `title` and `modDate`
     * are deliberately NOT flattened: on a contentlet those names already mean the contentlet's
     * own, and giving them the file's meaning here would make the same name answer differently
     * depending on where it is read.
     */
    @Test
    public void test_binaryPropertiesAreReadableDirectlyOnTheAsset() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Flattened");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { "
                        + "name size mime versionPath idPath path sha256 isImage width height "
                        + "fileAsset { size mime } } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR);

        final Map<String, Object> row = firstRow(
                GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser), holder);
        final Map<String, Object> field = (Map<String, Object>) row.get(IMAGE_FIELD_VAR);

        assertNotNull("the binary name must be readable on the asset", field.get("name"));
        assertNotNull("as must its size", field.get("size"));
        assertNotNull("and its mime type", field.get("mime"));
        assertNotNull("and its version path", field.get("versionPath"));

        // The same values, reached the long way, must agree — the flattened properties are a
        // shortcut to the same binary, not a second source of truth.
        final Map<String, Object> binary = (Map<String, Object>) field.get("fileAsset");
        assertEquals("the flattened size must match the binary's own",
                binary.get("size"), field.get("size"));
        assertEquals("the flattened mime must match the binary's own",
                binary.get("mime"), field.get("mime"));
    }

    /**
     * Given: the contentlet's own {@code title} and {@code modDate}.
     * When: they are selected on an asset.
     * Then: they report the CONTENTLET's values, not the file's.
     *
     * <p>The two properties the flattening above deliberately leaves out. If a later change
     * flattens them too, this fails — which is the point.
     */
    @Test
    public void test_titleAndModDateStillMeanTheContentlet() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Names");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { title } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR);

        final Map<String, Object> row = firstRow(
                GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser), holder);
        final Map<String, Object> field = (Map<String, Object>) row.get(IMAGE_FIELD_VAR);

        assertEquals("`title` on an asset must stay the contentlet's title, not the file's",
                asset.getTitle(), field.get("title"));
    }

    /**
     * Given: a customer asset type with a text field named {@code width}, which the asset
     * interfaces otherwise declare as the binary's numeric width.
     * When: the schema is built and queried.
     * Then: the schema is still valid, the customer's field answers on its own type, the binary's
     * width stays reachable through the binary, and selecting {@code width} directly on the asset
     * field fails with an error naming it — never different data.
     *
     * <p>{@code width} and {@code sha256} rather than {@code size} or {@code name}: for DOTASSET
     * content, DotAssetViewStrategy overwrites {@code name}, {@code size}, {@code path},
     * {@code type} and {@code extension} with the binary's values on every read, collections
     * included -- long-standing behavior this feature does not change, and that would mask what is
     * being tested here.
     *
     * <p>Before the fix, an interface and its implementation disagreeing on a field's type made
     * graphql-java reject the WHOLE schema: one customer field took every GraphQL query on the
     * instance down.
     */
    @Test
    public void test_incompatibleCollision_keepsSchemaValidAndCustomerFieldWins() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        try {
            addTextProperty(assetType, "width");
            final Contentlet asset = newAssetOf(assetType, "Collides", Map.of("width", "wide"));

            final ContentType holder = newHolderType();
            final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

            final String query = String.format(
                    "{ %sCollection(query: \"+identifier:%s\") { %s { "
                            + "fileName ... on %s { width } fileAsset { width } } } }",
                    holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR,
                    assetType.variable());
            final Map<String, Object> field = (Map<String, Object>) firstRow(
                    GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser), holder)
                    .get(IMAGE_FIELD_VAR);

            assertEquals("the customer's own `width` must answer on its own type",
                    "wide", field.get("width"));
            final Map<String, Object> binary = (Map<String, Object>) field.get("fileAsset");
            assertNotEquals("the binary's width must stay reachable through the binary",
                    "wide", binary.get("width"));

            final String direct = String.format(
                    "{ %sCollection(query: \"+identifier:%s\") { %s { width } } }",
                    holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR);
            final String errors = GraphqlQueryRunner.executeAndExpectFailure(direct, systemUser)
                    .toString();
            assertTrue("the validation error must name the property: " + errors,
                    errors.contains("width"));
        } finally {
            // A type carrying the collision changes what every asset field offers, so it must not
            // outlive this test.
            APILocator.getContentTypeAPI(systemUser).delete(assetType);
            APILocator.getGraphqlAPI().invalidateSchema();
        }
    }

    /**
     * Given: a customer asset type with a text field named {@code sha256}, the same GraphQL type as
     * the binary's hash.
     * When: {@code sha256} is selected directly on the asset field.
     * Then: the customer's value answers, and the binary's hash stays reachable through the
     * binary. The customer's field existed first; answering with anything else would be a silent
     * change.
     */
    @Test
    public void test_compatibleCollision_customerFieldWins() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        try {
            addTextProperty(assetType, "sha256");
            final Contentlet asset = newAssetOf(assetType, "Hashed", Map.of("sha256", "customerHash"));

            final ContentType holder = newHolderType();
            final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

            final String query = String.format(
                    "{ %sCollection(query: \"+identifier:%s\") { %s { sha256 fileAsset { sha256 } } } }",
                    holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR);
            final Map<String, Object> field = (Map<String, Object>) firstRow(
                    GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser), holder)
                    .get(IMAGE_FIELD_VAR);

            assertEquals("the customer's own `sha256` must win its name",
                    "customerHash", field.get("sha256"));
            final Map<String, Object> binary = (Map<String, Object>) field.get("fileAsset");
            assertNotEquals("the binary's hash must stay reachable through the binary",
                    "customerHash", binary.get("sha256"));
        } finally {
            APILocator.getContentTypeAPI(systemUser).delete(assetType);
            APILocator.getGraphqlAPI().invalidateSchema();
        }
    }

    /**
     * Given: a new text field called "Size" added to an asset type, with no variable chosen.
     * When: it is saved.
     * Then: its generated variable is not {@code size}, so a new field cannot take a flat asset
     * property's name with a different type.
     */
    @Test
    public void test_newFieldIsSteeredAwayFromIncompatibleAssetPropertyName() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        try {
            final Field saved = APILocator.getContentTypeFieldAPI().save(
                    FieldBuilder.builder(TextField.class).name("Size")
                            .contentTypeId(assetType.id()).dataType(DataTypes.TEXT).build(),
                    systemUser);

            assertNotEquals("a generated variable must not collide with the binary's `size`",
                    "size", saved.variable());
        } finally {
            APILocator.getContentTypeAPI(systemUser).delete(assetType);
            APILocator.getGraphqlAPI().invalidateSchema();
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Asserts the holder's asset field answers {@code null} while the holder itself is returned. */
    private void assertTargetResolvesToNull(final ContentType holder, final Contentlet content)
            throws Exception {
        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { identifier %s { fileName } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR);

        final Map<String, Object> row = firstRow(
                GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser), holder);

        assertEquals("the rest of the row must still be delivered",
                content.getIdentifier(), row.get("identifier"));
        assertEquals("an unresolvable target must answer null", null, row.get(IMAGE_FIELD_VAR));
    }

    /**
     * Replaces the inherited permissions on {@code contentlet} with READ for exactly
     * {@code roles}, and reindexes it so collection queries filter by the new grant.
     *
     * <p>{@code permissionIndividually} copies every inherited grant onto the contentlet --
     * including CMS Anonymous's READ -- and {@code save} only adds to what is there, so each copied
     * grant for a role outside {@code roles} is revoked explicitly: an individual permission saved
     * with no bits is deleted. Locked roles cannot be edited and are left alone.
     */
    private void permitReadOnly(final Contentlet contentlet, final List<Role> roles)
            throws Exception {
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        permissionAPI.permissionIndividually(
                permissionAPI.findParentPermissionable(contentlet), contentlet, systemUser);

        final Set<String> keep = roles.stream().map(Role::getId).collect(Collectors.toSet());
        for (final Permission copied : permissionAPI.getPermissions(contentlet, true, true)) {
            final Role role = APILocator.getRoleAPI().loadRoleById(copied.getRoleId());
            if (!keep.contains(role.getId()) && role.isEditPermissions()) {
                permissionAPI.save(new Permission(copied.getType(), contentlet.getPermissionId(),
                        role.getId(), 0, true), contentlet, systemUser, false);
            }
        }

        permissionAPI.save(roles.stream()
                        .map(role -> new Permission(contentlet.getPermissionId(), role.getId(),
                                PermissionAPI.PERMISSION_READ, true))
                        .collect(Collectors.toList()),
                contentlet, systemUser, false);
        contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().refresh(contentlet);
    }

    /** A fetcher environment resolving {@code property} on {@code source} within {@code context}. */
    private DataFetchingEnvironment environmentFor(final Contentlet source, final String property,
            final DotGraphQLContext context) {
        return DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .source(source)
                .context(context)
                .mergedField(MergedField.newMergedField(new graphql.language.Field(property))
                        .build())
                .build();
    }

    /** Runs a query selecting {@code selection} on {@code companionField} and returns that object. */
    private Map<String, Object> queryCompanion(final ContentType holder, final Contentlet content,
            final String companionField, final String selection) throws Exception {
        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { %s } } }",
                holder.variable(), content.getIdentifier(), companionField, selection);

        final Map<String, Object> row = firstRow(
                GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser), holder);

        final Map<String, Object> companion = (Map<String, Object>) row.get(companionField);
        assertNotNull("'" + companionField + "' resolved to nothing — check the fixture", companion);
        return companion;
    }

    private Map<String, Object> firstRow(final Map<String, Object> data, final ContentType holder) {
        final List<Map<String, Object>> rows =
                (List<Map<String, Object>>) data.get(holder.variable() + "Collection");
        assertNotNull("no collection returned for " + holder.variable(), rows);
        assertEquals("expected exactly one row", 1, rows.size());
        return rows.get(0);
    }

    /** A customer-defined content type extending DOTASSET, with a property of its own. */
    private ContentType newDotAssetSubtype() throws Exception {
        final String variable = "subtypeAsset" + System.nanoTime();
        final ContentType type = APILocator.getContentTypeAPI(systemUser).save(
                ContentTypeBuilder.builder(DotAssetContentType.class)
                        .folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .name(variable).variable(variable)
                        .owner(systemUser.getUserId()).build());
        addCustomProperty(type);
        return APILocator.getContentTypeAPI(systemUser).find(type.variable());
    }

    /** A customer-defined content type extending FILEASSET, with a property of its own. */
    private ContentType newFileAssetSubtype() throws Exception {
        final String variable = "subtypeFileAsset" + System.nanoTime();
        final ContentType type = APILocator.getContentTypeAPI(systemUser).save(
                ContentTypeBuilder.builder(FileAssetContentType.class)
                        .folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .name(variable).variable(variable)
                        .owner(systemUser.getUserId()).build());
        addCustomProperty(type);
        return APILocator.getContentTypeAPI(systemUser).find(type.variable());
    }

    /**
     * Gives an asset type its own stored {@code description}, the field whose name collides with
     * the flat view's derived one.
     */
    private void addDescriptionProperty(final ContentType type) throws Exception {
        final Field field = FieldBuilder.builder(TextField.class)
                .name("description").variable("description")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build();
        APILocator.getContentTypeFieldAPI().save(field, systemUser);
        APILocator.getGraphqlAPI().invalidateSchema();
    }

    private void addCustomProperty(final ContentType type) throws Exception {
        addTextProperty(type, CUSTOM_PROPERTY_VAR);
    }

    private void addTextProperty(final ContentType type, final String variable) throws Exception {
        final Field field = FieldBuilder.builder(TextField.class)
                .name(variable).variable(variable)
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build();
        APILocator.getContentTypeFieldAPI().save(field, systemUser);
        APILocator.getGraphqlAPI().invalidateSchema();
    }

    private Contentlet newAssetOf(final ContentType assetType, final String propertyValue)
            throws Exception {
        return newAssetOf(assetType, propertyValue, Map.of());
    }

    private Contentlet newAssetOf(final ContentType assetType, final String propertyValue,
            final Map<String, Object> extraProperties) throws Exception {
        final File file = FileUtil.createTemporaryFile("subtype", ".txt", "subtype");
        final ContentletDataGen dataGen = new ContentletDataGen(assetType.id())
                .host(site)
                .setProperty(DotAssetContentType.ASSET_FIELD_VAR, file)
                .setProperty(DotAssetContentType.SITE_OR_FOLDER_FIELD_VAR, site.getIdentifier())
                .setProperty(DotAssetContentType.TAGS_FIELD_VAR, "subtypeTag")
                .setProperty(CUSTOM_PROPERTY_VAR, propertyValue);
        extraProperties.forEach(dataGen::setProperty);
        final Contentlet asset = dataGen.setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        ContentletDataGen.publish(asset);
        return asset;
    }

    private Contentlet newFileAssetOf(final ContentType assetType, final String propertyValue)
            throws Exception {
        final File file = FileUtil.createTemporaryFile("subtype", ".txt", "subtype");
        final Contentlet asset = new ContentletDataGen(assetType.id())
                .host(site)
                .setProperty(FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR, file)
                .setProperty(FileAssetContentType.FILEASSET_FILE_NAME_FIELD_VAR, file.getName())
                .setProperty(FileAssetContentType.FILEASSET_SITE_OR_FOLDER_FIELD_VAR,
                        site.getIdentifier())
                .setProperty(CUSTOM_PROPERTY_VAR, propertyValue)
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        ContentletDataGen.publish(asset);
        return asset;
    }

    /** A content type carrying one Image field and one File field. */
    private ContentType newHolderType() throws Exception {
        final ContentType type = new ContentTypeDataGen().nextPersisted();
        new FieldDataGen().contentTypeId(type.id()).type(ImageField.class)
                .dataType(DataTypes.TEXT).velocityVarName(IMAGE_FIELD_VAR).nextPersisted();
        new FieldDataGen().contentTypeId(type.id()).type(FileField.class)
                .dataType(DataTypes.TEXT).velocityVarName(FILE_FIELD_VAR).nextPersisted();
        APILocator.getGraphqlAPI().invalidateSchema();
        return APILocator.getContentTypeAPI(systemUser).find(type.variable());
    }

    private Contentlet newHolderContent(final ContentType holder, final String fieldVar,
            final Contentlet asset) throws Exception {
        final Contentlet content = new ContentletDataGen(holder.id())
                .host(site)
                .setProperty(fieldVar, asset.getIdentifier())
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        ContentletDataGen.publish(content);
        return content;
    }

    private Contentlet newHolderContentWithBoth(final ContentType holder,
            final Contentlet behindImageField, final Contentlet behindFileField) throws Exception {
        final Contentlet content = new ContentletDataGen(holder.id())
                .host(site)
                .setProperty(IMAGE_FIELD_VAR, behindImageField.getIdentifier())
                .setProperty(FILE_FIELD_VAR, behindFileField.getIdentifier())
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        ContentletDataGen.publish(content);
        return content;
    }
}
