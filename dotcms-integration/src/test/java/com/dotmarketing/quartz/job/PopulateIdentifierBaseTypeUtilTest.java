package com.dotmarketing.quartz.job;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link PopulateIdentifierBaseTypeUtil}.
 * Verifies that the batched UPDATE correctly sets {@code base_type} on the
 * {@code identifier} table rows that have a matching {@code asset_subtype} in the
 * {@code structure} table.
 */
public class PopulateIdentifierBaseTypeUtilTest extends IntegrationTestBase {

    private static Host site;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        site = new SiteDataGen().nextPersisted();
    }

    /**
     * Method to test: {@link PopulateIdentifierBaseTypeUtil#populate()}
     * When: Contentlet identifiers have base_type set to NULL
     * Should: Correctly populate base_type from structure.structuretype
     */
    @Test
    public void test_populate_setsBaseTypeFromStructure() throws DotDataException, DotSecurityException, SQLException {
        // Create a real contentlet so we have an identifier with asset_subtype set
        final Contentlet contentlet = TestDataUtils.getGenericContentContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), site);
        final String identifierId = contentlet.getIdentifier();

        // Null out base_type to simulate un-migrated row
        nullOutBaseType(identifierId);

        assertBaseTypeIsNull(identifierId);

        // Run the util
        final int updated = new PopulateIdentifierBaseTypeUtil().populate();
        assertTrue("Should have updated at least 1 row", updated > 0);

        // Verify base_type is now set
        final Integer baseType = fetchBaseType(identifierId);
        assertNotNull("base_type should not be null after populate", baseType);
        assertEquals("base_type should match CONTENT base type",
                BaseContentType.CONTENT.getType(), (int) baseType);
    }

    /**
     * Method to test: {@link PopulateIdentifierBaseTypeUtil#populate()}
     * When: All rows already have base_type set
     * Should: Return 0 (nothing to do)
     */
    @Test
    public void test_populate_returnsZeroWhenAlreadyPopulated() throws DotDataException, DotSecurityException, SQLException {
        final Contentlet contentlet = TestDataUtils.getGenericContentContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), site);
        final String identifierId = contentlet.getIdentifier();

        // Ensure base_type is populated
        new DotConnect()
                .setSQL("UPDATE identifier SET base_type = 1 WHERE id = ? AND base_type IS NULL")
                .addParam(identifierId)
                .loadResult();

        // First pass populates everything; second pass should update 0
        new PopulateIdentifierBaseTypeUtil().populate();
        final int secondRun = new PopulateIdentifierBaseTypeUtil().populate();
        assertEquals("Second run should update 0 rows when all are already populated", 0, secondRun);
    }

    /**
     * Method to test: {@link PopulateIdentifierBaseTypeUtil#populate()}
     * When: Identifier has NULL asset_subtype (e.g. folder)
     * Should: Leave base_type NULL — folders have no structure mapping
     */
    @Test
    public void test_populate_ignoresRowsWithNullAssetSubtype() throws DotDataException, SQLException {
        // Folders have asset_subtype = NULL
        final List<Map<String, Object>> folders = new DotConnect()
                .setSQL("SELECT id FROM identifier WHERE asset_type = 'folder' AND asset_subtype IS NULL LIMIT 1")
                .loadObjectResults();

        if (folders.isEmpty()) {
            return; // nothing to test in this environment
        }

        final String folderId = (String) folders.get(0).get("id");

        // Ensure base_type is NULL
        new DotConnect()
                .setSQL("UPDATE identifier SET base_type = NULL WHERE id = ?")
                .addParam(folderId)
                .loadResult();

        new PopulateIdentifierBaseTypeUtil().populate();

        final Integer baseType = fetchBaseType(folderId);
        assertTrue("base_type should remain NULL for folders (no asset_subtype)",
                baseType == null);
    }

    /**
     * Method to test: {@link PopulateIdentifierBaseTypeUtil#populate()}
     * When: Multiple contentlets of different base types exist with NULL base_type
     * Should: Correctly set the right base_type for each
     */
    @Test
    public void test_populate_correctlyMapsMultipleBaseTypes() throws DotDataException, DotSecurityException, SQLException {
        DbConnectionFactory.getConnection().setAutoCommit(true);

        // Use a file asset (FILEASSET = 4) and a generic content (CONTENT = 1)
        final Contentlet fileAsset = TestDataUtils.getFileAssetContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId());
        final Contentlet content = TestDataUtils.getGenericContentContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), site);

        nullOutBaseType(fileAsset.getIdentifier());
        nullOutBaseType(content.getIdentifier());

        new PopulateIdentifierBaseTypeUtil().populate();

        assertEquals("FileAsset identifier should have FILEASSET base type",
                BaseContentType.FILEASSET.getType(), (int) fetchBaseType(fileAsset.getIdentifier()));
        assertEquals("Content identifier should have CONTENT base type",
                BaseContentType.CONTENT.getType(), (int) fetchBaseType(content.getIdentifier()));
    }

    // -----------------------------------------------------------------------

    private static void nullOutBaseType(final String identifierId) throws DotDataException {
        new DotConnect()
                .setSQL("UPDATE identifier SET base_type = NULL WHERE id = ?")
                .addParam(identifierId)
                .loadResult();
    }

    private static void assertBaseTypeIsNull(final String identifierId) throws DotDataException {
        final Integer baseType = fetchBaseType(identifierId);
        assertTrue("base_type should be NULL before populate runs", baseType == null);
    }

    private static Integer fetchBaseType(final String identifierId) throws DotDataException {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("SELECT base_type FROM identifier WHERE id = ?")
                .addParam(identifierId)
                .loadObjectResults();
        if (rows.isEmpty()) {
            return null;
        }
        final Object val = rows.get(0).get("base_type");
        return val == null ? null : ((Number) val).intValue();
    }

}
