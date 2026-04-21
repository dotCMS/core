package com.dotmarketing.fixtask.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.fixtask.tasks.FixTask00090RecreateMissingFoldersInParentPath.LiteFolder;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.util.UUIDGenerator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link FixTask00090RecreateMissingFoldersInParentPath}.
 */
public class FixTask00090RecreateMissingFoldersInParentPathTest extends IntegrationTestBase {

    static Host host;

    @BeforeClass
    public static void setup() throws Exception {
        IntegrationTestInitService.getInstance().init();
        host = new SiteDataGen().nextPersisted(true);
    }

    @Before
    public void resetFixStatus() {
        // FixAssetsProcessStatus uses a static running flag; reset it before each test
        FixAssetsProcessStatus.stopProgress();
    }

    @AfterClass
    public static void tearDown() {
        DbConnectionFactory.closeSilently();
    }

    /**
     * Given a parent path whose intermediate folder is absent from the identifier table,
     * when recreateMissingFoldersInParentPath is called,
     * then the missing folder identifier is created.
     */
    @Test
    public void test_recreates_missing_intermediate_folder() throws Exception {
        final FixTask00090RecreateMissingFoldersInParentPath task =
                new FixTask00090RecreateMissingFoldersInParentPath();

        final String missingFolder = "missing-" + UUIDGenerator.shorty();
        final String childFolder  = "child-"   + UUIDGenerator.shorty();
        // path implies /missingFolder/ must exist as an identifier for /missingFolder/childFolder/ to be valid
        final String parentPath = "/" + missingFolder + "/" + childFolder + "/";

        // Build the key set WITHOUT the missing folder
        final Set<String> existingFolderKeys = new HashSet<>();

        task.recreateMissingFoldersInParentPath(parentPath, host.getIdentifier(), existingFolderKeys);

        // The missing intermediate folder (/missingFolder/) should have been created
        final long count = new DotConnect()
                .setSQL("SELECT COUNT(1) AS cnt FROM identifier WHERE host_inode = ? AND lower(asset_name) = ? AND asset_type = 'folder'")
                .addParam(host.getIdentifier())
                .addParam(missingFolder.toLowerCase())
                .getInt("cnt");

        assertTrue("Missing intermediate folder should have been created", count > 0);
    }

    /**
     * Given an existing folder key in the set,
     * when isFolderIdentifierMissing is called,
     * then it returns false (not missing).
     */
    @Test
    public void test_existing_folder_not_flagged_as_missing() {
        final FixTask00090RecreateMissingFoldersInParentPath task =
                new FixTask00090RecreateMissingFoldersInParentPath();

        final String folderName = "existing-" + UUIDGenerator.shorty();
        final Set<String> existingFolderKeys = new HashSet<>();

        LiteFolder folder = liteFolderOf("/", folderName, host.getIdentifier());
        // Pre-populate as if the folder already exists in the identifier table
        existingFolderKeys.add(folderKeyOf(host.getIdentifier(), "/", folderName));

        assertFalse("Folder already in key set should not be flagged as missing",
                task.isFolderIdentifierMissing(folder, existingFolderKeys));
    }

    /**
     * Given recreateMissingFolders is called twice with the same path,
     * when the first call adds keys to the set,
     * then the second call creates no duplicates.
     */
    @Test
    public void test_idempotent_no_duplicate_folders_created() throws Exception {
        final FixTask00090RecreateMissingFoldersInParentPath task =
                new FixTask00090RecreateMissingFoldersInParentPath();

        final String folderName = "idem-" + UUIDGenerator.shorty();
        final String parentPath = "/" + folderName + "/";
        final Set<String> existingFolderKeys = new HashSet<>();

        // First call — should create the intermediate folder
        task.recreateMissingFoldersInParentPath(parentPath, host.getIdentifier(), existingFolderKeys);

        final long countAfterFirst = new DotConnect()
                .setSQL("SELECT COUNT(1) AS cnt FROM identifier WHERE host_inode = ? AND lower(asset_name) = ? AND asset_type = 'folder'")
                .addParam(host.getIdentifier())
                .addParam(folderName.toLowerCase())
                .getInt("cnt");

        // Second call — key set is already updated; must not duplicate
        task.recreateMissingFoldersInParentPath(parentPath, host.getIdentifier(), existingFolderKeys);

        final long countAfterSecond = new DotConnect()
                .setSQL("SELECT COUNT(1) AS cnt FROM identifier WHERE host_inode = ? AND lower(asset_name) = ? AND asset_type = 'folder'")
                .addParam(host.getIdentifier())
                .addParam(folderName.toLowerCase())
                .getInt("cnt");

        assertEquals("Second run must not create duplicate identifier rows",
                countAfterFirst, countAfterSecond);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static LiteFolder liteFolderOf(String parentPath, String name, String hostId) {
        LiteFolder f = new LiteFolder();
        f.parentPath = parentPath;
        f.name = name;
        f.hostId = hostId;
        return f;
    }

    /** Must match the production folderKey() logic: hostId + NUL + lowerParentPath + NUL + lowerAssetName */
    private static String folderKeyOf(String hostId, String parentPath, String assetName) {
        return hostId + "\0" + parentPath.toLowerCase() + "\0" + assetName.toLowerCase();
    }
}
