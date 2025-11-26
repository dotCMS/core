package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WebsiteToolTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testGetSubFoldersUnderRoot() throws DotDataException, DotSecurityException {

        Host testSite = null;
        try {
            // Create test host and folders
            testSite = new SiteDataGen().nextPersisted();
            final Folder testFolder1 = new FolderDataGen()
                    .site(testSite).title("testFolder1").nextPersisted();
            final Folder testFolder2 = new FolderDataGen()
                    .site(testSite).title("testFolder2").nextPersisted();

            // Test getSubFolders
            WebsiteWebAPI websiteTool = new WebsiteWebAPI();
            final List<Folder> folderList = websiteTool.getSubFolders(
                    "/", testSite.getIdentifier());

            // Verify results
            assertNotNull(folderList);
            assertEquals(2, folderList.size());
            assertTrue(folderList.stream().map(Folder::getInode)
                    .allMatch(inode -> inode.equals(testFolder1.getInode())
                            || inode.equals(testFolder2.getInode())));

        } finally {
            if (testSite != null) {
                APILocator.getHostAPI().archive(testSite, APILocator.getUserAPI().getSystemUser(), false);
                APILocator.getHostAPI().delete(testSite, APILocator.getUserAPI().getSystemUser(), false);
            }
        }
    }

}