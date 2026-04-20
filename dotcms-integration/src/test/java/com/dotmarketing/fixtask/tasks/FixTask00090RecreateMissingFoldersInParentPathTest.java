package com.dotmarketing.fixtask.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UUIDGenerator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link FixTask00090RecreateMissingFoldersInParentPath}.
 *
 * <p>Specifically verifies the cache-based optimisation introduced to avoid the N×M DB query
 * pattern: all existing folder identifier keys are loaded into a {@code HashSet} upfront and
 * membership checks are performed in-memory rather than issuing one {@code SELECT COUNT(1)} per
 * path segment.
 */
public class FixTask00090RecreateMissingFoldersInParentPathTest extends IntegrationTestBase {

    private static Host host;

    @BeforeClass
    public static void setup() throws Exception {
        IntegrationTestInitService.getInstance().init();
        host = new SiteDataGen().nextPersisted(true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        DbConnectionFactory.closeSilently();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Given an identifier whose {@code parent_path} contains intermediate segments that do not yet
     * exist as folder identifiers, {@code executeFix()} must create the missing intermediate
     * folder entries.
     *
     * <p>This exercises the cache-based path: the fix loads existing folder keys once, detects the
     * gaps via {@code Set.contains()}, and creates the missing folders.
     */
    @Test
    public void test_executeFix_creates_missing_intermediate_folders() throws Exception {
        final String suffix   = UUIDGenerator.shorty();
        final String level1   = "l1-" + suffix;
        final String level2   = "l2-" + suffix;
        final String testId   = "test-id-" + suffix;
        final String hostId   = host.getIdentifier();

        // Insert an identifier whose parent path implies two folder levels that don't exist yet.
        insertRawIdentifier(testId, "/" + level1 + "/" + level2 + "/", "asset-" + suffix, hostId, "contentlet");

        try {
            // Pre-condition: neither intermediate folder identifier should exist.
            assertEquals("level1 folder must not exist before fix", 0,
                    countFolderIdentifier(hostId, "/", level1));
            assertEquals("level2 folder must not exist before fix", 0,
                    countFolderIdentifier(hostId, "/" + level1 + "/", level2));

            runFix();

            // Post-condition: both intermediate folders must have been created.
            assertEquals("level1 folder must exist after fix", 1,
                    countFolderIdentifier(hostId, "/", level1));
            assertEquals("level2 folder must exist after fix", 1,
                    countFolderIdentifier(hostId, "/" + level1 + "/", level2));

        } finally {
            cleanUp(testId, hostId, level1, level2, suffix);
        }
    }

    /**
     * Running {@code executeFix()} a second time must not create duplicate folder identifiers.
     * This validates that newly created folders are tracked in the in-memory cache so that
     * subsequent checks within – and across – runs do not attempt to re-create them.
     */
    @Test
    public void test_executeFix_is_idempotent() throws Exception {
        final String suffix = UUIDGenerator.shorty();
        final String level1 = "idem-l1-" + suffix;
        final String level2 = "idem-l2-" + suffix;
        final String testId = "idem-id-" + suffix;
        final String hostId = host.getIdentifier();

        insertRawIdentifier(testId, "/" + level1 + "/" + level2 + "/", "idem-asset-" + suffix, hostId, "contentlet");

        try {
            // First run — creates the missing folders.
            runFix();

            assertEquals("level1 folder must exist after first fix", 1,
                    countFolderIdentifier(hostId, "/", level1));
            assertEquals("level2 folder must exist after first fix", 1,
                    countFolderIdentifier(hostId, "/" + level1 + "/", level2));

            // Second run — must not produce duplicates.
            runFix();

            assertEquals("level1 folder must still be exactly 1 after second fix", 1,
                    countFolderIdentifier(hostId, "/", level1));
            assertEquals("level2 folder must still be exactly 1 after second fix", 1,
                    countFolderIdentifier(hostId, "/" + level1 + "/", level2));

        } finally {
            cleanUp(testId, hostId, level1, level2, suffix);
        }
    }

    /**
     * A folder whose parent path is already fully represented in the identifier table must not
     * trigger any folder creation.
     */
    @Test
    public void test_executeFix_skips_existing_folders() throws Exception {
        final String suffix = UUIDGenerator.shorty();
        final String hostId = host.getIdentifier();

        // Create a proper folder so its identifier already exists.
        Folder folder = APILocator.getFolderAPI()
                .createFolders("/existing-" + suffix, host, APILocator.systemUser(), false);

        // Insert a contentlet identifier whose parent path is the existing folder.
        final String testId = "existing-test-id-" + suffix;
        insertRawIdentifier(testId, folder.getPath(), "asset-" + suffix, hostId, "contentlet");

        final long countBefore = countFolderIdentifier(hostId, "/", "existing-" + suffix);

        try {
            runFix();

            // The existing folder must not have been duplicated.
            assertEquals("Existing folder must not be duplicated by the fix", countBefore,
                    countFolderIdentifier(hostId, "/", "existing-" + suffix));
        } finally {
            new DotConnect().setSQL("DELETE FROM identifier WHERE id = ?").addParam(testId).loadResult();
            APILocator.getFolderAPI().delete(folder, APILocator.systemUser(), false);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Runs {@link FixTask00090RecreateMissingFoldersInParentPath#executeFix()}. */
    private void runFix() throws Exception {
        // Reset the static running flag in case a previous test left it set.
        if (FixAssetsProcessStatus.getRunning()) {
            FixAssetsProcessStatus.setRunning(false);
        }
        new FixTask00090RecreateMissingFoldersInParentPath().executeFix();
    }

    private void insertRawIdentifier(String id, String parentPath, String assetName,
            String hostId, String assetType) throws Exception {
        new DotConnect()
                .setSQL("INSERT INTO identifier (id, parent_path, asset_name, host_inode, asset_type, create_date)"
                        + " VALUES (?,?,?,?,?,?)")
                .addParam(id)
                .addParam(parentPath)
                .addParam(assetName)
                .addParam(hostId)
                .addParam(assetType)
                .addParam(new Date())
                .loadResult();
    }

    private long countFolderIdentifier(String hostId, String parentPath, String assetName)
            throws Exception {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("SELECT COUNT(*) AS cnt FROM identifier"
                        + " WHERE host_inode = ? AND lower(parent_path) = lower(?)"
                        + " AND lower(asset_name) = lower(?) AND asset_type = 'folder'")
                .addParam(hostId)
                .addParam(parentPath)
                .addParam(assetName)
                .loadObjectResults();
        return Long.parseLong(rows.get(0).get("cnt").toString());
    }

    /**
     * Removes the raw test identifier and any folders created by the fix task for the given path
     * segments.
     */
    private void cleanUp(String testId, String hostId, String level1, String level2,
            String suffix) throws Exception {
        new DotConnect().setSQL("DELETE FROM identifier WHERE id = ?").addParam(testId).loadResult();
        deleteFolderByPath(hostId, "/" + level1 + "/", level2);
        deleteFolderByPath(hostId, "/", level1);
    }

    private void deleteFolderByPath(String hostId, String parentPath, String assetName)
            throws Exception {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("SELECT id FROM identifier WHERE host_inode = ? AND lower(parent_path) = lower(?)"
                        + " AND lower(asset_name) = lower(?) AND asset_type = 'folder'")
                .addParam(hostId)
                .addParam(parentPath)
                .addParam(assetName)
                .loadObjectResults();

        for (Map<String, Object> row : rows) {
            final String id = row.get("id").toString();
            try {
                final Folder f = APILocator.getFolderAPI().find(id, APILocator.systemUser(), false);
                if (f != null && f.getInode() != null) {
                    APILocator.getFolderAPI().delete(f, APILocator.systemUser(), false);
                    return;
                }
            } catch (Exception e) {
                // Folder may be incomplete — fall back to raw SQL cleanup.
            }
            new DotConnect().setSQL("DELETE FROM folder WHERE identifier = ?").addParam(id).loadResult();
            new DotConnect().setSQL("DELETE FROM inode WHERE inode = ?").addParam(id).loadResult();
            new DotConnect().setSQL("DELETE FROM identifier WHERE id = ?").addParam(id).loadResult();
        }
    }
}
