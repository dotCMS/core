package com.dotmarketing.portlets.folders.business;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

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

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link FolderFactoryImpl#getFoldersByParent(Folder, User, boolean)}</li>
     *     <li><b>Given Scenario: The folder is sorted by Title not Name,
     *     subfolders are not sorted alphabetically if the folder Name is changed</b> </li>
     *     <li><b>Expected Result:The subfolder list should be sorted alphabetically </b> </li>
     * </ul>
     * @throws Exception
     */
    @Test
    public void test_getFoldersByParent_shouldRetrieveListSortedAlphabetically() throws DotDataException, DotSecurityException {
        final User adminUser = TestUserUtils.getAdminUser();
        final Host host = new SiteDataGen().nextPersisted();
        final Folder mainFolder = new FolderDataGen().site(host).nextPersisted();
        final Folder subFolderC = new FolderDataGen().name("c"+System.currentTimeMillis()).parent(mainFolder).nextPersisted();
        final Folder subFolderB = new FolderDataGen().name("b"+System.currentTimeMillis()).parent(mainFolder).nextPersisted();
        final Folder subFolderA = new FolderDataGen().name("a"+System.currentTimeMillis()).parent(mainFolder).nextPersisted();
        final FolderFactoryImpl folderFactory = new FolderFactoryImpl();
        //method to test
        List<Folder> folderContent = folderFactory.getFoldersByParent(mainFolder, adminUser, false);
        //assert all the subfolders are alphabetical sorted by name
        assertEquals(folderContent.get(0).getName(),subFolderA.getName());
        //rename the subfolderA to z...
        APILocator.getFolderAPI().renameFolder(subFolderA, "z"+System.currentTimeMillis(), adminUser , false);
        //method to test
        folderContent = folderFactory.getFoldersByParent(mainFolder, adminUser, false);
        //now to subFolderA should be the last one
        assertNotEquals(folderContent.get(0).getName() , subFolderA.getName());
        assertEquals(folderContent.get(folderContent.size()-1).getName(),subFolderA.getName());
    }
}
