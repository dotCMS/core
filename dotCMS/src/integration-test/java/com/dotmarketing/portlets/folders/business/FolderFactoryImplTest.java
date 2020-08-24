package com.dotmarketing.portlets.folders.business;

import static com.dotmarketing.portlets.folders.business.FolderAPI.SYSTEM_FOLDER_PARENT_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import org.junit.BeforeClass;
import org.junit.Test;

public class FolderFactoryImplTest {

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testFindFolderByPathUsingRootFolder() throws DotDataException {
        final FolderFactoryImpl folderFactory = new FolderFactoryImpl();
        CacheLocator.getFolderCache().clearCache();
        final Folder folder = folderFactory.findFolderByPath("/", APILocator.systemHost());
        assertNotNull(folder);
        assertEquals("/", folder.getPath());
        assertEquals("System folder", folder.getTitle());
    }

    @Test
    public void testFindFolderByPathUsingSystemFolderParentPath() throws DotDataException {
        final FolderFactoryImpl folderFactory = new FolderFactoryImpl();
        CacheLocator.getFolderCache().clearCache();
        final Folder folder = folderFactory.findFolderByPath("/", APILocator.systemHost());
        assertNotNull(folder);
        assertEquals("/", folder.getPath());
        assertEquals("System folder", folder.getTitle());
    }

    @Test
    public void testFindFolderByPathUsingNonRootFolder() throws DotDataException {
        final FolderFactoryImpl folderFactory = new FolderFactoryImpl();
        final Host host = new SiteDataGen().nextPersisted();
        final Folder parentFolder = new FolderDataGen().name("parentFolder").site(host)
                .nextPersisted();

        final Folder childFolder = new FolderDataGen().name("childFolder").parent(parentFolder)
                .site(host).nextPersisted();
        CacheLocator.getFolderCache().clearCache();
        final Folder result = folderFactory.findFolderByPath(childFolder.getPath(), host);
        assertNotNull(result);
        assertEquals(childFolder.getInode(), result.getInode());
        assertEquals(childFolder.getTitle(), result.getTitle());
    }

}
