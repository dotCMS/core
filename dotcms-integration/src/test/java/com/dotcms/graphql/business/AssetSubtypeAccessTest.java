package com.dotcms.graphql.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

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
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.FileUtil;
import com.liferay.portal.model.User;
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
 * <p>The new description arrives as a <b>companion field</b> beside the asset field
 * ({@code image} gains {@code imageContent}) rather than as a property of the flat
 * {@code DotFileasset} view. A GraphQL field has exactly one type and a resolved value has exactly
 * one runtime type, so the flat view and the asset itself — two descriptions of the same thing —
 * cannot occupy the same position. Putting the new one beside the old is what lets a client narrow
 * to a concrete asset type at the same level as the flat properties, with none of those properties
 * changing.
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
     * When: that property is requested through the companion field.
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
     * When: that property is requested through the companion field.
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
     * Given: an asset carrying tags.
     * When: its tags are requested through the companion field.
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
     * When: the companion field is selected.
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

    // ---------------------------------------------------------------- helpers

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
