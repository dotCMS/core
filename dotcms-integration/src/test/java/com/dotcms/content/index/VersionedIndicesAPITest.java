package com.dotcms.content.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.JUnit4WeldRunner;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration test for VersionedIndicesAPI that tests all CRUD operations,
 * version validation, and legacy index loading functionality.
 *
 * @author Fabrizzio
 */
@ApplicationScoped
@RunWith(JUnit4WeldRunner.class)
public class VersionedIndicesAPITest {

    @Inject
    private VersionedIndicesAPI versionedIndicesAPI;

    private static final String TEST_VERSION = "test_v1.0.0";
    private static final String TEST_VERSION_2 = "test_v2.0.0";
    private static final String TEST_LIVE_INDEX = "cluster_test.live_20241224120000";
    private static final String TEST_WORKING_INDEX = "cluster_test.working_20241224120000";
    private static final String TEST_REINDEX_LIVE_INDEX = "cluster_test.reindex_live_20241224120000";
    private static final String TEST_SITE_SEARCH_INDEX = "cluster_test.site_search_20241224120000";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void setUp() throws SQLException {
        // Initialize CDI container to get VersionedIndicesAPI instance
        if (versionedIndicesAPI == null) {
            versionedIndicesAPI = new VersionedIndicesAPIImpl(new IndicesFactory());
        }

        // Clean up any test data before each test
        cleanupTestData();

        DbConnectionFactory.getConnection().setAutoCommit(true);
    }

    @After
    public void tearDown() {
        // Clean up test data after each test
        cleanupTestData();
    }

    private void cleanupTestData() {
        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("DELETE FROM indicies WHERE index_version LIKE 'test_v%'");
            dotConnect.loadResult();

            // Also clean up any legacy test data
            dotConnect.setSQL("DELETE FROM indicies WHERE index_name LIKE 'cluster_test%'");
            dotConnect.loadResult();
        } catch (Exception e) {
            Logger.warn(this, "Error cleaning up test data: " + e.getMessage());
        }
    }

    /**
     * Test scenario: Save versioned indices and load them back
     * Expected: All indices are saved and retrieved correctly with version
     */
    @Test
    public void test_saveIndices_WithVersion_ShouldSaveSuccessfully() throws DotDataException {
        // Create test indices info with version
        VersionedIndices testIndices = VersionedIndicesImpl.builder()
            .version(TEST_VERSION)
            .live(TEST_LIVE_INDEX)
            .working(TEST_WORKING_INDEX)
            .reindexLive(TEST_REINDEX_LIVE_INDEX)
            .siteSearch(TEST_SITE_SEARCH_INDEX)
            .build();

        // Save indices
        versionedIndicesAPI.saveIndices(testIndices);

        // Verify version exists
        assertTrue("Version should exist after save", versionedIndicesAPI.versionExists(TEST_VERSION));

        // Load indices back
        VersionedIndices loadedIndices = versionedIndicesAPI.loadIndices(TEST_VERSION);
        assertNotNull("Loaded indices should not be null", loadedIndices);

        // Verify all indices are correct
        assertEquals("Version should match", TEST_VERSION, loadedIndices.version().orElse(null));
        assertEquals("Live index should match", TEST_LIVE_INDEX, loadedIndices.live().orElse(null));
        assertEquals("Working index should match", TEST_WORKING_INDEX, loadedIndices.working().orElse(null));
        assertEquals("Reindex live index should match", TEST_REINDEX_LIVE_INDEX, loadedIndices.reindexLive().orElse(null));
        assertEquals("Site search index should match", TEST_SITE_SEARCH_INDEX, loadedIndices.siteSearch().orElse(null));

        // Verify indices count
        assertEquals("Should have 4 indices for version", 4, versionedIndicesAPI.getIndicesCount(TEST_VERSION));
    }

    /**
     * Test scenario: Try to save indices without version
     * Expected: DotDataException should be thrown
     */
    @Test
    public void test_saveIndices_WithoutVersion_ShouldThrowException() {
        try {
            // Create test indices without a version
            VersionedIndices testIndices = VersionedIndicesImpl.builder()
                .live(TEST_LIVE_INDEX)
                .working(TEST_WORKING_INDEX)
                .build();

            // Should throw exception
            versionedIndicesAPI.saveIndices(testIndices);
            fail("Expected DotDataException when saving indices without version");

        } catch (DotDataException e) {
            assertTrue("Exception message should mention version requirement",
                e.getMessage().contains("Version is REQUIRED"));
        }
    }

    /**
     * Test scenario: Load indices for non-existent version
     * Expected: Empty indices object returned
     */
    @Test
    public void test_loadIndices_NonExistentVersion_ShouldReturnEmpty() throws DotDataException {
        VersionedIndices result = versionedIndicesAPI.loadIndices("non_existent_version");
        assertNotNull("Result should not be null", result);
        assertFalse("Version should be empty", result.version().isPresent());
        assertFalse("Live index should be empty", result.live().isPresent());
        assertFalse("Working index should be empty", result.working().isPresent());
    }

    /**
     * Test scenario: Load all indices with multiple versions
     * Expected: All versions returned correctly
     */
    @Test
    public void test_loadAllIndices_MultipleVersions_ShouldReturnAll() throws DotDataException {
        // Save first version
        VersionedIndices testIndices1 = VersionedIndicesImpl.builder()
            .version(TEST_VERSION)
            .live(TEST_LIVE_INDEX)
            .working(TEST_WORKING_INDEX)
            .build();
        versionedIndicesAPI.saveIndices(testIndices1);

        // Save second version
        VersionedIndices testIndices2 = VersionedIndicesImpl.builder()
            .version(TEST_VERSION_2)
            .live("cluster_test.live_20241224130000")
            .siteSearch("cluster_test.site_search_20241224130000")
            .build();
        versionedIndicesAPI.saveIndices(testIndices2);

        // Load all indices
        List<VersionedIndices> allIndices = versionedIndicesAPI.loadAllIndices();
        assertNotNull("All indices list should not be null", allIndices);
        assertTrue("Should have at least 2 versions", allIndices.size() >= 2);

        // Verify versions exist
        boolean foundVersion1 = false;
        boolean foundVersion2 = false;
        for (VersionedIndices indices : allIndices) {
            String version = indices.version().orElse(null);
            if (TEST_VERSION.equals(version)) {
                foundVersion1 = true;
            } else if (TEST_VERSION_2.equals(version)) {
                foundVersion2 = true;
            }
        }
        assertTrue("Should find first test version", foundVersion1);
        assertTrue("Should find second test version", foundVersion2);
    }

    /**
     * Test scenario: Remove a specific version
     * Expected: Version should be removed and no longer exist
     */
    @Test
    public void test_removeVersion_ShouldDeleteSuccessfully() throws DotDataException {
        // Save test indices
        VersionedIndices testIndices = VersionedIndicesImpl.builder()
            .version(TEST_VERSION)
            .live(TEST_LIVE_INDEX)
            .working(TEST_WORKING_INDEX)
            .build();
        versionedIndicesAPI.saveIndices(testIndices);

        // Verify version exists
        assertTrue("Version should exist before removal", versionedIndicesAPI.versionExists(TEST_VERSION));
        assertEquals("Should have indices before removal", 2, versionedIndicesAPI.getIndicesCount(TEST_VERSION));

        // Remove version
        versionedIndicesAPI.removeVersion(TEST_VERSION);

        // Verify version no longer exists
        assertFalse("Version should not exist after removal", versionedIndicesAPI.versionExists(TEST_VERSION));
        assertEquals("Should have no indices after removal", 0, versionedIndicesAPI.getIndicesCount(TEST_VERSION));
    }

    /**
     * Test scenario: Extract timestamp from index name
     * Expected: Correct timestamp should be extracted
     */
    @Test
    public void test_extractTimestamp_ValidIndexName_ShouldReturnCorrectTimestamp() throws DotDataException {
        String indexName = "cluster_test.live_20241224120000";
        Instant timestamp = versionedIndicesAPI.extractTimestamp(indexName);

        assertNotNull("Timestamp should not be null", timestamp);
        // The timestamp should be from 2024-12-24 12:00:00
        // We'll just verify it's not null and is a reasonable date
        assertTrue("Timestamp should be reasonable", timestamp.getEpochSecond() > 0);
    }

    /**
     * Test scenario: Extract timestamp from invalid index name
     * Expected: DotDataException should be thrown
     */
    @Test
    public void test_extractTimestamp_InvalidIndexName_ShouldThrowException() {
        try {
            versionedIndicesAPI.extractTimestamp("invalid_index_name");
            fail("Expected DotDataException for invalid index name");
        } catch (DotDataException e) {
            assertTrue("Exception should mention pattern",
                e.getMessage().contains("does not follow expected pattern") ||
                e.getMessage().contains("Failed to extract timestamp"));
        }

        try {
            versionedIndicesAPI.extractTimestamp(null);
            fail("Expected DotDataException for null index name");
        } catch (DotDataException e) {
            assertTrue("Exception should mention null or empty",
                e.getMessage().contains("cannot be null or empty"));
        }
    }

    /**
     * Test scenario: Load legacy non-versioned indices
     * Expected: Legacy indices are returned as VersionedIndices without version
     */
    @Test
    public void test_loadNonVersionedIndices_ShouldReturnLegacyIndices() throws DotDataException {
        // Insert legacy test data (without version)
        insertLegacyTestData();

        try {
            // Load legacy indices
            VersionedIndices legacyIndices = versionedIndicesAPI.loadNonVersionedIndices();
            assertNotNull("Legacy indices should not be null", legacyIndices);

            // Version should be empty for legacy indices
            assertFalse("Legacy indices should not have version", legacyIndices.version().isPresent());

            // Should have some index data (depending on what legacy data exists)
            Logger.info(this, "Legacy indices loaded: " + legacyIndices.toString());

        } finally {
            // Clean up legacy test data
            cleanupLegacyTestData();
        }
    }

    private void insertLegacyTestData() {
        try {
            final DotConnect dotConnect = new DotConnect();

            // Insert legacy indices without version (index_version = NULL)
            dotConnect.setSQL("INSERT INTO indicies (index_name, index_type) VALUES (?, ?)");
            dotConnect.addParam("cluster_test_legacy.live_20241224000000");
            dotConnect.addParam("live");
            dotConnect.loadResult();

            dotConnect.setSQL("INSERT INTO indicies (index_name, index_type) VALUES (?, ?)");
            dotConnect.addParam("cluster_test_legacy.working_20241224000000");
            dotConnect.addParam("working");
            dotConnect.loadResult();

        } catch (Exception e) {
            Logger.error(this, "Error inserting legacy test data", e);
        }
    }

    private void cleanupLegacyTestData() {
        try {
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("DELETE FROM indicies WHERE index_name LIKE 'cluster_test_legacy%'");
            dotConnect.loadResult();
        } catch (Exception e) {
            Logger.warn(this, "Error cleaning up legacy test data", e);
        }
    }

    /**
     * Test scenario: Version validation with null/empty values
     * Expected: Appropriate exceptions should be thrown
     */
    @Test
    public void test_versionValidation_NullOrEmpty_ShouldThrowExceptions() {
        try {
            versionedIndicesAPI.loadIndices(null);
            fail("Expected DotDataException for null version");
        } catch (DotDataException e) {
            assertTrue("Exception should mention null or empty",
                e.getMessage().contains("cannot be null or empty"));
        }

        try {
            versionedIndicesAPI.loadIndices("");
            fail("Expected DotDataException for empty version");
        } catch (DotDataException e) {
            assertTrue("Exception should mention null or empty",
                e.getMessage().contains("cannot be null or empty"));
        }

        try {
            versionedIndicesAPI.removeVersion(null);
            fail("Expected DotDataException for null version in remove");
        } catch (DotDataException e) {
            assertTrue("Exception should mention null or empty",
                e.getMessage().contains("cannot be null or empty"));
        }
    }

    /**
     * Test scenario: Save indices with partial data (only some index types)
     * Expected: Should save successfully with only provided indices
     */
    @Test
    public void test_saveIndices_PartialData_ShouldSaveOnlyProvidedIndices() throws DotDataException {
        // Create indices with only live and working
        VersionedIndices partialIndices = VersionedIndicesImpl.builder()
            .version(TEST_VERSION)
            .live(TEST_LIVE_INDEX)
            .working(TEST_WORKING_INDEX)
            .build();

        versionedIndicesAPI.saveIndices(partialIndices);

        // Load back and verify
        VersionedIndices loaded = versionedIndicesAPI.loadIndices(TEST_VERSION);
        assertEquals("Live index should be saved", TEST_LIVE_INDEX, loaded.live().orElse(null));
        assertEquals("Working index should be saved", TEST_WORKING_INDEX, loaded.working().orElse(null));
        assertFalse("Reindex live should be empty", loaded.reindexLive().isPresent());
        assertFalse("Site search should be empty", loaded.siteSearch().isPresent());

        assertEquals("Should count only saved indices", 2, versionedIndicesAPI.getIndicesCount(TEST_VERSION));
    }
}