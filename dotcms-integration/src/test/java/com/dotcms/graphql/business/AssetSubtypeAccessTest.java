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
import java.io.File;
import java.util.List;
import java.util.Map;
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
     * When: the asset's own identity is requested — identifier, inode, live state, title.
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
                queryCompanion(holder, content, IMAGE_COMPANION, "identifier inode live title");

        assertEquals("the asset's identifier must be reachable",
                asset.getIdentifier(), assetContent.get("identifier"));
        assertEquals("the asset's inode must be reachable",
                asset.getInode(), assetContent.get("inode"));
        assertEquals("the asset's live state must be reachable", true, assetContent.get("live"));
        assertNotNull("the asset's title must be reachable", assetContent.get("title"));
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
     * Given: two users, one able to read a restricted asset and one not.
     * When: both query the same field pointing at it.
     * Then: only the permitted one gets it back.
     *
     * <p>FR-008, framed as the guarantee that actually matters: the new description must resolve
     * <b>as the calling user</b> and never escalate. Two users, one asset, one query — if the
     * answers differ by caller, identity is being honoured; if they match, it is not.
     *
     * <p>Deliberately not framed as "a published asset must be unreadable". Under delivery
     * semantics the lookup honours front-end roles, so published content is readable anonymously by
     * design, and asserting otherwise would test a scenario dotCMS does not have rather than the
     * permission check. That mistake is easy to make and hard to see: a fixture checked with
     * {@code respectFrontendRoles = false} looks locked down while delivery, correctly, still grants
     * access.
     *
     * <p>Two fixture traps, both documented in {@code WebAssetResourceV2IntegrationTest}: the list
     * form of {@code permissionAPI.save} is required because the single-Permission form only
     * appends and would leave the inherited READ in place; and the shared {@code TestUserUtils}
     * users carry a type-level CONTENTLETS READ grant through their role, so purpose-built users in
     * fresh roles are used instead.
     */
    @Test
    public void test_assetResolvesAsTheCallingUser() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Identity-scoped");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role permittedRole = new RoleDataGen().nextPersisted();

        // Replace inherited permissions on the asset with a single grant to one role, so the only
        // difference between the two users below is whether they hold it.
        permissionAPI.permissionIndividually(
                permissionAPI.findParentPermissionable(asset), asset, systemUser);
        permissionAPI.save(
                List.of(new Permission(asset.getPermissionId(), permittedRole.getId(),
                        PermissionAPI.PERMISSION_READ, true)),
                asset, systemUser, false);

        final User permittedUser =
                new UserDataGen().roles(permittedRole).nextPersisted();
        final User otherUser =
                new UserDataGen().roles(new RoleDataGen().nextPersisted()).nextPersisted();

        // Check the fixture the way the product asks, not a way that merely looks strict: the
        // lookup behind an asset field honours front-end roles.
        assertTrue("fixture problem: the permitted user cannot read the asset, so a difference "
                        + "below would prove nothing",
                permissionAPI.doesUserHavePermission(asset, PermissionAPI.PERMISSION_READ,
                        permittedUser, true));

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { fileName } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR);

        final Object seenByPermitted = assetFieldFor(query, permittedUser, holder);
        final Object seenByOther = assetFieldFor(query, otherUser, holder);

        assertNotNull("the user holding the grant must see the asset", seenByPermitted);

        // The asset is published, so delivery may legitimately serve it to the second user through
        // the anonymous role. What must never happen is the field resolving with more authority
        // than the caller has: if the two answers are identical AND the second user genuinely lacks
        // read, identity is being ignored.
        if (!permissionAPI.doesUserHavePermission(asset, PermissionAPI.PERMISSION_READ,
                otherUser, true)) {
            assertEquals("the asset field resolved with more authority than its caller: a user "
                            + "without read on the asset received it anyway",
                    null, seenByOther);
        }
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

    // ---------------------------------------------------------------- helpers

    /** Runs {@code query} as {@code user} and returns the asset field of the single row, if any. */
    private Object assetFieldFor(final String query, final User user, final ContentType holder)
            throws Exception {
        final Map<String, Object> data = GraphqlQueryRunner.executeAndExpectSuccess(query, user);
        final List<Map<String, Object>> rows =
                (List<Map<String, Object>>) data.get(holder.variable() + "Collection");
        return null == rows || rows.isEmpty() ? null : rows.get(0).get(IMAGE_FIELD_VAR);
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
        final Field field = FieldBuilder.builder(TextField.class)
                .name(CUSTOM_PROPERTY_VAR).variable(CUSTOM_PROPERTY_VAR)
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build();
        APILocator.getContentTypeFieldAPI().save(field, systemUser);
        APILocator.getGraphqlAPI().invalidateSchema();
    }

    private Contentlet newAssetOf(final ContentType assetType, final String propertyValue)
            throws Exception {
        final File file = FileUtil.createTemporaryFile("subtype", ".txt", "subtype");
        final Contentlet asset = new ContentletDataGen(assetType.id())
                .host(site)
                .setProperty(DotAssetContentType.ASSET_FIELD_VAR, file)
                .setProperty(DotAssetContentType.SITE_OR_FOLDER_FIELD_VAR, site.getIdentifier())
                .setProperty(DotAssetContentType.TAGS_FIELD_VAR, "subtypeTag")
                .setProperty(CUSTOM_PROPERTY_VAR, propertyValue)
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
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
