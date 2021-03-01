package com.dotmarketing.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

public class FolderCacheImplIntegrationTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment o
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Scenario under test: When adding SYSTEM_FOLDER and its host is not SYSTEM_HOST
     * Expected result: folder added in cache should have SYSTEM_HOST set as host
     */

    @Test
    public void testPUT_GivenSystemFolderWithHostDifferentFromSystemHost_ShouldSetSystemHostToFolder()
            throws DotDataException {
        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();
        // set a different inode
        final String hostIdDifferentThanSystemHost = UUIDGenerator.generateUuid();
        systemFolder.setHostId(hostIdDifferentThanSystemHost);

        final FolderCache folderCache = CacheLocator.getFolderCache();
        folderCache.clearCache();
        Identifier id = APILocator.getIdentifierAPI().find(systemFolder.getIdentifier());
        folderCache.addFolder(systemFolder, id);

        // let's get the added folder from cache. Host should be SYSTEM_HOST
        assertNotNull(folderCache.getFolder(Folder.SYSTEM_FOLDER));
        assertEquals(Host.SYSTEM_HOST, folderCache.getFolder(Folder.SYSTEM_FOLDER).getHostId());
    }

    /**
     * Scenario under test: When adding SYSTEM_FOLDER and its host is SYSTEM_HOST
     * Expected result: folder in cache should have SYSTEM_HOST set as host
     */

    @Test
    public void testPUT_GivenSystemFolderWithSystemHost_ShouldStillHaveSystemHostInFolder()
            throws DotDataException {
        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        assertEquals(Host.SYSTEM_HOST, systemFolder.getHostId());

        final FolderCache folderCache = CacheLocator.getFolderCache();
        folderCache.clearCache();
        Identifier id = APILocator.getIdentifierAPI().find(systemFolder.getIdentifier());
        folderCache.addFolder(systemFolder, id);

        // let's get the added folder from cache. Host should be SYSTEM_HOST
        assertNotNull(folderCache.getFolder(Folder.SYSTEM_FOLDER));
        assertEquals(Host.SYSTEM_HOST, folderCache.getFolder(Folder.SYSTEM_FOLDER).getHostId());
    }
}
