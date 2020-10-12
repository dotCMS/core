package com.dotmarketing.portlets.folders.model;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;

public class FolderTest {


    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    /**
     * this tests that the isSystemFolder return appropiatly
     * 
     * @throws Exception
     */
    @Test
    public void test_folder_isSystemFolder() throws Exception {

        Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();
        assert (systemFolder.isSystemFolder());

        assertFalse(new FolderDataGen().nextPersisted().isSystemFolder());

        Folder folder = new FolderDataGen().next();

        assertFalse(folder.isSystemFolder());

        folder = new FolderDataGen().nextPersisted();

        assertFalse(folder.isSystemFolder());


    }


    /**
     * this tests the convience method of getting the host from the folder.getHost() method
     * 
     * @throws Exception
     */
    @Test
    public void test_folder_host_method() throws Exception {

        // tests that the system folder lives on system host
        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        assert (systemFolder.getHost().equals(APILocator.systemHost()));

        // this is a folder created on the default host
        Host folderHost = new FolderDataGen().nextPersisted().getHost();
        Host defautlHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        assertEquals(folderHost, defautlHost);


    }

}
