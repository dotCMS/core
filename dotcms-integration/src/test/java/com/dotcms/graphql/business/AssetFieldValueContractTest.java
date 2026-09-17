package com.dotcms.graphql.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.DotAssetDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.FileUtil;
import com.liferay.portal.model.User;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Locks what the six properties of {@code DotFileasset} return today, so that adding the new
 * asset-subtype capability cannot change any of them. See issue #34540, FR-012 and SC-008.
 *
 * <p>This is deliberately a <b>value</b> contract, not a schema-shape one. {@code GraphqlAPITest}
 * already covers the shape — which types and fields exist — and a change could satisfy every one
 * of those assertions while quietly altering what a field returns. These tests execute real
 * queries through {@link GraphqlQueryRunner} and compare the answers.
 *
 * <p><b>The two that matter most are the ones that look wrong.</b> For image-style content,
 * {@code fileName} does not read a stored file name and {@code description} does not read a stored
 * description: both are synthesized by base-type ternaries in {@code CustomFieldType}, falling
 * back to the contentlet's name and title respectively. So {@code description} returns the file
 * name. That is the shipped contract, customers query it, and "fixing" it would change what a live
 * query returns without failing it — the silent break this whole design exists to avoid. If one of
 * those assertions fails, the correct response is almost certainly to revert the change, not to
 * update the expectation.
 */
public class AssetFieldValueContractTest extends IntegrationTestBase {

    private static final String IMAGE_FIELD_VAR = "assetContractImage";
    private static final String FILE_FIELD_VAR = "assetContractFile";

    private static User systemUser;
    private static Host site;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
    }

    /**
     * Given: an Image field pointing at DOTASSET-based content.
     * When: the six long-standing properties are selected.
     * Then: each returns exactly what it returns today — including the two synthesized values.
     */
    @Test
    public void test_dotAsset_throughImageField_sixPropertiesUnchanged() throws Exception {
        final File file = FileUtil.createTemporaryFile("value-contract", ".txt", "contract");
        final Contentlet dotAsset = new DotAssetDataGen(site, file)
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        // The asset must be published too: the fetcher looks it up with the *holder's* live
        // state, so a working-only asset resolves to nothing behind a published holder.
        ContentletDataGen.publish(dotAsset);

        final ContentType holder = newHolderType();
        final String imageVar = IMAGE_FIELD_VAR;

        final Contentlet content = new ContentletDataGen(holder.id())
                .host(site)
                .setProperty(imageVar, dotAsset.getIdentifier())
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        ContentletDataGen.publish(content);

        final Map<String, Object> asset = queryAssetField(holder, imageVar, content);

        // fileName is NOT a stored value here: the ternary falls back to the contentlet name.
        assertEquals("fileName must keep returning the contentlet name for DOTASSET content",
                dotAsset.getName(), asset.get("fileName"));

        assertNotNull("fileAsset must still resolve", asset.get("fileAsset"));
        assertNotNull("metaData must still resolve", asset.get("metaData"));
        assertTrue("all six properties must still be selectable",
                asset.keySet().containsAll(
                        List.of("fileName", "fileAsset", "metaData", "showOnMenu", "sortOrder")));
    }

    /**
     * Given: a File field pointing at FILEASSET-based content.
     * When: the six long-standing properties are selected.
     * Then: each returns exactly what it returns today. Here the values are stored rather than
     * synthesized, which is precisely why this case must be asserted separately from the one above.
     */
    @Test
    public void test_fileAsset_throughFileField_sixPropertiesUnchanged() throws Exception {
        final File file = FileUtil.createTemporaryFile("value-contract", ".txt", "contract");
        final Contentlet fileAsset = new FileAssetDataGen(site, file)
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        // See the note in the DOTASSET test: the asset's live state must match the holder's.
        ContentletDataGen.publish(fileAsset);

        final ContentType holder = newHolderType();
        final String fileVar = FILE_FIELD_VAR;

        final Contentlet content = new ContentletDataGen(holder.id())
                .host(site)
                .setProperty(fileVar, fileAsset.getIdentifier())
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        ContentletDataGen.publish(content);

        final Map<String, Object> asset = queryAssetField(holder, fileVar, content);

        assertEquals("fileName must keep returning the stored file name for FILEASSET content",
                fileAsset.getStringProperty("fileName"), asset.get("fileName"));
        assertNotNull("fileAsset must still resolve", asset.get("fileAsset"));
        assertNotNull("metaData must still resolve", asset.get("metaData"));
        assertTrue("all six properties must still be selectable",
                asset.keySet().containsAll(
                        List.of("fileName", "fileAsset", "metaData", "showOnMenu", "sortOrder")));
    }

    /**
     * Given: an empty asset field.
     * When: it is selected.
     * Then: an explicit empty result, no error, and the rest of the query is unaffected (FR-007).
     */
    @Test
    public void test_emptyAssetField_returnsNullWithoutError() throws Exception {
        final ContentType holder = newHolderType();
        final String imageVar = IMAGE_FIELD_VAR;

        final Contentlet content = new ContentletDataGen(holder.id())
                .host(site)
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        ContentletDataGen.publish(content);

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { identifier %s { fileName } } }",
                holder.variable(), content.getIdentifier(), imageVar);

        final Map<String, Object> data =
                GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser);

        final Map<String, Object> row = firstRow(data, holder);
        assertEquals("the rest of the query must still be delivered",
                content.getIdentifier(), row.get("identifier"));
        assertEquals("an empty asset field must resolve to null, not an error",
                null, row.get(imageVar));
    }

    /**
     * Selects the six properties on {@code fieldVar} for {@code content} and returns the asset map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> queryAssetField(final ContentType holder, final String fieldVar,
            final Contentlet content) throws Exception {
        // Precondition, so a fixture problem is never mistaken for a product one: confirm the
        // reference actually persisted before blaming the query for returning nothing.
        final Contentlet reloaded = APILocator.getContentletAPI()
                .find(content.getInode(), systemUser, false);
        assertNotNull("the holder contentlet did not persist at all", reloaded);
        assertNotNull("the asset reference never persisted on field '" + fieldVar
                        + "' — fixture problem, not a product one. Persisted keys: "
                        + reloaded.getMap().keySet(),
                reloaded.get(fieldVar));

        final String query = String.format(
                "{ %sCollection(query: \"+identifier:%s\") { %s { fileName "
                        + "showOnMenu sortOrder fileAsset { name size mime } "
                        + "metaData { key value } } } }",
                holder.variable(), content.getIdentifier(), fieldVar);

        final Map<String, Object> data =
                GraphqlQueryRunner.executeAndExpectSuccess(query, systemUser);
        final Map<String, Object> asset =
                (Map<String, Object>) firstRow(data, holder).get(fieldVar);
        assertNotNull("the asset field resolved to nothing — the fixture is wrong, not the code",
                asset);
        return asset;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstRow(final Map<String, Object> data, final ContentType holder) {
        final List<Map<String, Object>> rows =
                (List<Map<String, Object>>) data.get(holder.variable() + "Collection");
        assertNotNull("no collection returned for " + holder.variable(), rows);
        assertEquals("expected exactly one row", 1, rows.size());
        return rows.get(0);
    }

    /**
     * A content type carrying one Image field and one File field.
     *
     * <p>Two details here are easy to get wrong and both fail silently. First, an asset-reference
     * field stores an identifier in a <b>text</b> column, so it needs {@code DataTypes.TEXT};
     * without it the field lands on {@code system_field}, the schema still advertises it, and any
     * value set on it simply never persists. Second, {@code FieldDataGen} derives its default
     * variable name from the current millisecond, so two fields created back to back can collide —
     * hence the explicit names.
     */
    private ContentType newHolderType() throws Exception {
        final ContentType type = new ContentTypeDataGen().nextPersisted();
        new FieldDataGen().contentTypeId(type.id()).type(ImageField.class)
                .dataType(DataTypes.TEXT).velocityVarName(IMAGE_FIELD_VAR).nextPersisted();
        new FieldDataGen().contentTypeId(type.id()).type(FileField.class)
                .dataType(DataTypes.TEXT).velocityVarName(FILE_FIELD_VAR).nextPersisted();
        APILocator.getGraphqlAPI().invalidateSchema();
        return APILocator.getContentTypeAPI(systemUser).find(type.variable());
    }
}
