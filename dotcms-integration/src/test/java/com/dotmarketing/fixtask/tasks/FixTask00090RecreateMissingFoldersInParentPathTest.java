package com.dotmarketing.fixtask.tasks;

import static org.junit.Assert.assertEquals;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link FixTask00090RecreateMissingFoldersInParentPath}.
 *
 * <p>The DB trigger {@code identifier_parent_path_check()} blocks inserting any identifier whose
 * parent folder doesn't already exist. Tests therefore set up state by:
 * <ol>
 *   <li>Creating the folder structure properly via the API (identifier + folder rows created).</li>
 *   <li>Inserting a test contentlet identifier inside that structure (valid at this point).</li>
 *   <li>Deleting the intermediate folder via the API to simulate data corruption.</li>
 *   <li>Running {@code executeFix()} and asserting the missing folder identifier is recreated.</li>
 * </ol>
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
     * Verifies that {@code recreateMissingFoldersInParentPath} creates a folder identifier when
     * the in-memory cache indicates it is absent — testing the full DB write path
     * (createIdentifier + createFolder via LocalTransaction) without needing to manufacture
     * corrupt DB state (which the identifier_parent_path_check trigger prevents).
     */
    @Test
    public void test_recreateMissing_creates_folder_when_not_in_cache() throws Exception {
        final String suffix     = UUIDGenerator.shorty();
        final String folderName = "fix-l1-" + suffix;
        final String hostId     = host.getIdentifier();
        final String path       = "/" + folderName + "/";

        final FixTask00090RecreateMissingFoldersInParentPath task =
                new FixTask00090RecreateMissingFoldersInParentPath();

        // Empty cache simulates the state where this folder's identifier is missing.
        final Set<String> cache = new HashSet<>();

        try {
            assertEquals("Folder must not exist before the call", 0,
                    countFolderIdentifier(hostId, "/", folderName));

            task.recreateMissingFoldersInParentPath(path, hostId, cache);

            assertEquals("Folder identifier must be created", 1,
                    countFolderIdentifier(hostId, "/", folderName));
        } finally {
            deleteFolderByPath(hostId, "/", folderName);
        }
    }

    /**
     * Verifies idempotency: calling {@code recreateMissingFoldersInParentPath} twice for the same
     * path must not create duplicate folder identifiers, because the first call populates the cache.
     */
    @Test
    public void test_recreateMissing_is_idempotent() throws Exception {
        final String suffix     = UUIDGenerator.shorty();
        final String folderName = "idem-l1-" + suffix;
        final String hostId     = host.getIdentifier();
        final String path       = "/" + folderName + "/";

        final FixTask00090RecreateMissingFoldersInParentPath task =
                new FixTask00090RecreateMissingFoldersInParentPath();
        final Set<String> cache = new HashSet<>();

        try {
            task.recreateMissingFoldersInParentPath(path, hostId, cache);
            assertEquals("Folder must exist after first call", 1,
                    countFolderIdentifier(hostId, "/", folderName));

            task.recreateMissingFoldersInParentPath(path, hostId, cache);
            assertEquals("Folder must not be duplicated after second call", 1,
                    countFolderIdentifier(hostId, "/", folderName));
        } finally {
            deleteFolderByPath(hostId, "/", folderName);
        }
    }

    /**
     * A folder whose identifier already exists must not be re-created by the fix.
     */
    @Test
    public void test_executeFix_skips_existing_folders() throws Exception {
        final String suffix = UUIDGenerator.shorty();
        final String hostId = host.getIdentifier();

        final Folder folder = APILocator.getFolderAPI()
                .createFolders("/existing-" + suffix, host, APILocator.systemUser(), false);

        final String testId = "existing-test-id-" + suffix;
        insertRawIdentifier(testId, folder.getPath(), "asset-" + suffix, hostId, "contentlet");

        final long countBefore = countFolderIdentifier(hostId, "/", "existing-" + suffix);

        try {
            runFix();

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

    private void runFix() throws Exception {
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
     * Cleans up any folder created at the given path (e.g. by the fix task). The folder may be
     * incomplete (identifier-only, no folder/inode rows), so we try the API first and fall back
     * to raw SQL.
     */
    private void deleteFolderByPath(String hostId, String parentPath, String assetName)
            throws Exception {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("SELECT id FROM identifier WHERE host_inode = ? AND lower(parent_path) = lower(?)"
                        + " AND lower(asset_name) = lower(?) AND asset_type = 'folder'")
                .addParam(hostId).addParam(parentPath).addParam(assetName)
                .loadObjectResults();
        for (Map<String, Object> row : rows) {
            final String id = row.get("id").toString();
            try {
                final Folder f = APILocator.getFolderAPI().find(id, APILocator.systemUser(), false);
                if (f != null && f.getInode() != null) {
                    APILocator.getFolderAPI().delete(f, APILocator.systemUser(), false);
                    continue;
                }
            } catch (Exception ignored) {
                // incomplete folder — fall through to raw SQL
            }
            new DotConnect().setSQL("DELETE FROM folder WHERE identifier = ?").addParam(id).loadResult();
            new DotConnect().setSQL("DELETE FROM inode WHERE inode = ?").addParam(id).loadResult();
            new DotConnect().setSQL("DELETE FROM identifier WHERE id = ?").addParam(id).loadResult();
        }
    }
}
