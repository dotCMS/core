package com.dotcms.graphql.business;

import static org.junit.Assert.assertEquals;
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
 * <p>Today an asset-pointing field resolves to {@code DotFileasset}, a flat six-property type, so
 * none of the below is reachable at any depth. These tests are expected to FAIL until the new
 * asset description is in place; they are the Red signal for User Story 1.
 *
 * <p>Two traps already paid for in {@link AssetFieldValueContractTest} and repeated here: an
 * asset-reference field needs {@code DataTypes.TEXT} or its value never persists, and the
 * referenced asset must be published to the same state as the holder or the fetcher resolves
 * nothing.
 */
@SuppressWarnings("unchecked")
public class AssetSubtypeAccessTest extends IntegrationTestBase {

    private static final String IMAGE_FIELD_VAR = "subtypeImage";
    private static final String FILE_FIELD_VAR = "subtypeFile";
    private static final String CUSTOM_PROPERTY_VAR = "campaignName";

    /** The new field on {@code DotFileasset} that exposes the properly described asset. */
    private static final String ASSET_CONTENT_FIELD = "content";

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
     * When: that property is requested through the Image field.
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

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { %s { "
                        + "... on %s { %s } } } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR,
                ASSET_CONTENT_FIELD, assetType.variable(), CUSTOM_PROPERTY_VAR);

        final Map<String, Object> assetContent = queryAssetContent(query, holder, IMAGE_FIELD_VAR);
        assertEquals("the customer's own property must be readable through the Image field",
                "Summer Sale", assetContent.get(CUSTOM_PROPERTY_VAR));
    }

    /**
     * Given: a customer-defined content type extending FILEASSET with a property of its own, and a
     * File field pointing at content of it.
     * When: that property is requested through the File field.
     * Then: its stored value is returned.
     */
    @Test
    public void test_customPropertyOnFileAssetSubtype_isReadableThroughFileField() throws Exception {
        final ContentType assetType = newFileAssetSubtype();
        final Contentlet asset = newFileAssetOf(assetType, "Datasheets");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, FILE_FIELD_VAR, asset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { %s { "
                        + "... on %s { %s } } } } }",
                holder.variable(), content.getIdentifier(), FILE_FIELD_VAR,
                ASSET_CONTENT_FIELD, assetType.variable(), CUSTOM_PROPERTY_VAR);

        final Map<String, Object> assetContent = queryAssetContent(query, holder, FILE_FIELD_VAR);
        assertEquals("the customer's own property must be readable through the File field",
                "Datasheets", assetContent.get(CUSTOM_PROPERTY_VAR));
    }

    /**
     * Given: an asset carrying tags.
     * When: its tags are requested through the field pointing at it.
     * Then: they are returned.
     *
     * <p>This is the AI tagging blocker named in the issue: {@code tags} is not on the flat type,
     * so generated tags cannot be read back at all.
     */
    @Test
    public void test_tags_areReadableThroughAssetField() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Tagged");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { %s { "
                        + "... on %s { tags } } } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR,
                ASSET_CONTENT_FIELD, assetType.variable());

        final Map<String, Object> assetContent = queryAssetContent(query, holder, IMAGE_FIELD_VAR);
        assertNotNull("tags must be reachable through an asset field", assetContent.get("tags"));
    }

    /**
     * Given: an asset behind an Image field.
     * When: the asset's own identity is requested — identifier, inode, live state, title.
     * Then: each is returned and matches the asset's record.
     *
     * <p>None of these are on the flat type today, so an Image field cannot currently tell a
     * client *which* asset it is pointing at.
     */
    @Test
    public void test_assetOwnIdentity_isReadableThroughAssetField() throws Exception {
        final ContentType assetType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(assetType, "Identity");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { %s { "
                        + "identifier inode live title } } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR, ASSET_CONTENT_FIELD);

        final Map<String, Object> assetContent = queryAssetContent(query, holder, IMAGE_FIELD_VAR);
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
     * immediately queries a property of it through an asset field.
     * Then: it works, with no administrative step in between.
     *
     * <p>This is the test that distinguishes a derived set of asset types from an enumerated one
     * (FR-001a). A hardcoded list of known asset types would satisfy every other test in this
     * class and fail this one.
     */
    @Test
    public void test_contentTypeCreatedAfterSchemaBuild_isImmediatelyReachable() throws Exception {
        // Build the schema first, so the type below cannot have been present when it was made.
        APILocator.getGraphqlAPI().getSchema(systemUser);

        final ContentType lateType = newDotAssetSubtype();
        final Contentlet asset = newAssetOf(lateType, "Created Late");

        final ContentType holder = newHolderType();
        final Contentlet content = newHolderContent(holder, IMAGE_FIELD_VAR, asset);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { %s { "
                        + "__typename ... on %s { %s } } } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR,
                ASSET_CONTENT_FIELD, lateType.variable(), CUSTOM_PROPERTY_VAR);

        final Map<String, Object> assetContent = queryAssetContent(query, holder, IMAGE_FIELD_VAR);
        assertEquals("a type created after the schema was built must still be reachable",
                "Created Late", assetContent.get(CUSTOM_PROPERTY_VAR));
        assertEquals("__typename must name the customer's own type",
                lateType.variable(), assetContent.get("__typename"));
    }

    /**
     * Given: an Image field pointing at content that is not an asset at all — nothing stops this,
     * since the field stores a bare identifier and {@code FileFieldDataFetcher} falls back to the
     * raw contentlet when {@code FileAssetAPI.fromContentlet} cannot convert it.
     * When: the new asset description is selected.
     * Then: it resolves to nothing, and the request still succeeds.
     *
     * <p>A resolved type that does not implement the interface must not surface a GraphQL error:
     * the misconfiguration is in the data, and failing the whole request would take the rest of
     * the query down with it.
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
                "{ %sCollection(query: \"+identifier:%s\") { identifier %s { fileName %s { "
                        + "identifier } } } }",
                holder.variable(), content.getIdentifier(), IMAGE_FIELD_VAR, ASSET_CONTENT_FIELD);

        final Map<String, Object> data =
                GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser);

        final List<Map<String, Object>> rows =
                (List<Map<String, Object>>) data.get(holder.variable() + "Collection");
        assertNotNull("the rest of the query must still be delivered", rows);
        assertEquals("expected exactly one row", 1, rows.size());
        assertEquals("the rest of the row must still be delivered",
                content.getIdentifier(), rows.get(0).get("identifier"));

        final Map<String, Object> assetField =
                (Map<String, Object>) rows.get(0).get(IMAGE_FIELD_VAR);
        assertNotNull("the flat view must still resolve", assetField);
        assertEquals("non-asset content must resolve to nothing, not an error",
                null, assetField.get(ASSET_CONTENT_FIELD));
    }

    // ---------------------------------------------------------------- helpers

    @SuppressWarnings("unchecked")
    private Map<String, Object> queryAssetContent(final String query, final ContentType holder,
            final String fieldVar) throws Exception {
        final Map<String, Object> data =
                GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser);
        final List<Map<String, Object>> rows =
                (List<Map<String, Object>>) data.get(holder.variable() + "Collection");
        assertNotNull("no collection returned for " + holder.variable(), rows);
        assertEquals("expected exactly one row", 1, rows.size());

        final Map<String, Object> assetField = (Map<String, Object>) rows.get(0).get(fieldVar);
        assertNotNull("the asset field resolved to nothing — check the fixture", assetField);

        final Map<String, Object> assetContent =
                (Map<String, Object>) assetField.get(ASSET_CONTENT_FIELD);
        assertNotNull("the asset field exposed no '" + ASSET_CONTENT_FIELD + "'", assetContent);
        return assetContent;
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
}
