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
import java.util.List;
import java.util.Map;
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
     * Given a contentlet whose {@code parent_path} points to a folder whose identifier was
     * subsequently deleted (simulating data corruption), {@code executeFix()} must recreate the
     * missing folder identifier.
     */
    @Test
    public void test_executeFix_creates_missing_folder_identifier() throws Exception {
        final String suffix  = UUIDGenerator.shorty();
        final String folderName = "fix-l1-" + suffix;
        final String hostId  = host.getIdentifier();
        final String testId  = "fix-test-id-" + suffix;

        // 1. Create the folder properly so its identifier exists.
        final Folder folder = APILocator.getFolderAPI()
                .createFolders("/" + folderName, host, APILocator.systemUser(), false);
        final String folderIdentifierId = folder.getIdentifier();

        // 2. Insert a contentlet identifier whose parent_path is the folder (valid now).
        insertRawIdentifier(testId, folder.getPath(), "asset-" + suffix, hostId, "contentlet");

        try {
            // 3. Delete the folder to simulate a missing identifier (corrupt state).
            deleteFolderRows(folder);

            // Pre-condition: folder identifier must be gone.
            assertEquals("Folder identifier must be absent before fix", 0,
                    countFolderIdentifier(hostId, "/", folderName));

            // 4. Run the fix.
            runFix();

            // Post-condition: the fix must have recreated the folder identifier.
            assertEquals("Folder identifier must be recreated by fix", 1,
                    countFolderIdentifier(hostId, "/", folderName));

        } finally {
            new DotConnect().setSQL("DELETE FROM identifier WHERE id = ?").addParam(testId).loadResult();
            deleteFolderByIdentifierId(folderIdentifierId);
        }
    }

    /**
     * Running {@code executeFix()} a second time must not create duplicate folder identifiers.
     */
    @Test
    public void test_executeFix_is_idempotent() throws Exception {
        final String suffix  = UUIDGenerator.shorty();
        final String folderName = "idem-l1-" + suffix;
        final String hostId  = host.getIdentifier();
        final String testId  = "idem-id-" + suffix;

        final Folder folder = APILocator.getFolderAPI()
                .createFolders("/" + folderName, host, APILocator.systemUser(), false);
        final String folderIdentifierId = folder.getIdentifier();

        insertRawIdentifier(testId, folder.getPath(), "idem-asset-" + suffix, hostId, "contentlet");

        try {
            deleteFolderRows(folder);

            // First run — recreates the missing folder identifier.
            runFix();
            assertEquals("Folder must exist after first fix", 1,
                    countFolderIdentifier(hostId, "/", folderName));

            // Second run — must not duplicate it.
            runFix();
            assertEquals("Folder must still be exactly 1 after second fix", 1,
                    countFolderIdentifier(hostId, "/", folderName));

        } finally {
            new DotConnect().setSQL("DELETE FROM identifier WHERE id = ?").addParam(testId).loadResult();
            deleteFolderByIdentifierId(folderIdentifierId);
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

    /**
     * Deletes the folder's inode and folder rows WITHOUT touching the identifier, so we can
     * simulate a corrupt state where the identifier is then separately removed.
     */
    private void deleteFolderRows(Folder folder) throws Exception {
        new DotConnect().setSQL("DELETE FROM folder WHERE identifier = ?")
                .addParam(folder.getIdentifier()).loadResult();
        new DotConnect().setSQL("DELETE FROM inode WHERE inode = ?")
                .addParam(folder.getInode()).loadResult();
        new DotConnect().setSQL("DELETE FROM identifier WHERE id = ?")
                .addParam(folder.getIdentifier()).loadResult();
    }

    /** Cleans up any folder identifier (and folder/inode rows) left by the fix or test setup. */
    private void deleteFolderByIdentifierId(String identifierId) throws Exception {
        try {
            final Folder f = APILocator.getFolderAPI().find(identifierId, APILocator.systemUser(), false);
            if (f != null && f.getInode() != null) {
                APILocator.getFolderAPI().delete(f, APILocator.systemUser(), false);
                return;
            }
        } catch (Exception e) {
            // incomplete folder — fall back to raw SQL
        }
        new DotConnect().setSQL("DELETE FROM folder WHERE identifier = ?").addParam(identifierId).loadResult();
        new DotConnect().setSQL("DELETE FROM identifier WHERE id = ?").addParam(identifierId).loadResult();
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
}
