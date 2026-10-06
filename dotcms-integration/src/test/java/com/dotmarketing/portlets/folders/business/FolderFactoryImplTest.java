package com.dotmarketing.portlets.folders.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This Integration Test verifies that the {@link FolderFactoryImpl} class is working as expected.
 *
 * @author Jose Castro
 * @since Aug 20th, 2020
 */
public class FolderFactoryImplTest extends IntegrationTestBase {

    private static HostAPI hostAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static FolderAPI folderAPI;

    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting the web app environment
        IntegrationTestInitService.getInstance().init();

        hostAPI = APILocator.getHostAPI();
        user = APILocator.getUserAPI().getSystemUser();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        folderAPI = APILocator.getFolderAPI();
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
     *     <li><b>Method to test:</b> {@link FolderFactoryImpl#save(Folder)} + the Folder DB
     *     transformer read path.</li>
     *     <li><b>Given Scenario:</b> Save a folder with {@code defaultBaseType} set to
     *     {@code DOTASSET}, then {@code FILEASSET}, then {@code null}, clearing the cache and
     *     re-reading from the DB each time.</li>
     *     <li><b>Expected Result:</b> Each value round-trips through the upsert and the transformer;
     *     a freshly-created folder reads back {@code null} (no preference).</li>
     * </ul>
     */
    @Test
    public void test_DefaultBaseType_Persistence_RoundTrip() throws DotDataException {
        final FolderFactoryImpl folderFactory = new FolderFactoryImpl();
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().name("dbt-" + System.currentTimeMillis())
                .site(host).nextPersisted();

        // A freshly-created folder has no preference
        CacheLocator.getFolderCache().clearCache();
        Folder read = folderFactory.findFolderByPath(folder.getPath(), host);
        assertNull(read.getDefaultBaseType());

        // DOTASSET round-trips
        folder.setDefaultBaseType("DOTASSET");
        folderFactory.save(folder);
        CacheLocator.getFolderCache().clearCache();
        read = folderFactory.findFolderByPath(folder.getPath(), host);
        assertEquals("DOTASSET", read.getDefaultBaseType());

        // FILEASSET round-trips
        folder.setDefaultBaseType("FILEASSET");
        folderFactory.save(folder);
        CacheLocator.getFolderCache().clearCache();
        read = folderFactory.findFolderByPath(folder.getPath(), host);
        assertEquals("FILEASSET", read.getDefaultBaseType());

        // null clears the preference
        folder.setDefaultBaseType(null);
        folderFactory.save(folder);
        CacheLocator.getFolderCache().clearCache();
        read = folderFactory.findFolderByPath(folder.getPath(), host);
        assertNull(read.getDefaultBaseType());
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

    /**
     * <ul>
     *     <li><b>Method to test:</b>
     *     {@link FolderFactoryImpl#getContentReport(String, String, String, String, int, int)}</li>
     *     <li>Given Scenario: Create a test Folder, add an HTML Page to it, and generate a content
     *     report for it.</li>
     * 	   <li>ExpectedResult: The report must contain only one Content Type -- Page -- and the
     * 	   number of entries must be 1.</li>
     * </ul>
     */
    @Test
    public void getContentReportForFolder() throws DotDataException, DotSecurityException {
        Folder testFolder = null;
        try {
            // ╔══════════════════╗
            // ║  Initialization  ║
            // ╚══════════════════╝
            final Host defaultSite = hostAPI.findDefaultHost(user, false);
            final String testFolderName = "my-test-folder-" + System.currentTimeMillis();

            // ╔════════════════════════╗
            // ║  Generating Test data  ║
            // ╚════════════════════════╝
            testFolder =
                    new FolderDataGen().title(testFolderName).name(testFolderName).site(defaultSite).nextPersisted();
            TestDataUtils.getPageContent(true, defaultSite.getLanguageId(), testFolder);
            final FolderFactoryImpl folderFactory = new FolderFactoryImpl();
            final List<Map<String, Object>> report =
                    folderFactory.getContentReport(testFolder.getPath(),
                            defaultSite.getIdentifier(),
                            "name", "ASC", 100, 0);

            // ╔══════════════╗
            // ║  Assertions  ║
            // ╚══════════════╝
            assertNotNull("The 'report' object cannot be null", report);
            assertEquals("There must be only one Content Report for the test Folder", 1,
                    report.size());
            final Map<String, Object> reportEntry = report.get(0);
            assertTrue("There must be an '" + IdentifierFactory.ASSET_SUBTYPE + "' key in the " +
                            "Content Report",
                    reportEntry.containsKey(IdentifierFactory.ASSET_SUBTYPE));
            assertTrue("There must be an 'total' key in the Content Report",
                    reportEntry.containsKey("total"));
            assertEquals("The Content Report must contain the type '" + HTMLPageAssetAPI.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_VARNAME + "'", HTMLPageAssetAPI.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_VARNAME,
                    reportEntry.get(IdentifierFactory.ASSET_SUBTYPE).toString());

            assertEquals("The Content Report must return a total of 1 content for this Content " +
                            "Type",
                    1, Integer.parseInt(reportEntry.get("total").toString()));
        } finally {
            // ╔═══════════╗
            // ║  Cleanup  ║
            // ╚═══════════╝
            if (null != testFolder) {
                folderAPI.delete(testFolder, user, false);
            }
        }
    }

    /**
     * <ul>
     *     <li>Method to test:
     *     {@link FolderFactory#getContentTypeCount(String, String)}</li>
     *     <li>Given Scenario: Create a test Folder, add an HTML Page to it, and get the total
     *     count of Content Types that live under a Folder.</li>
     *     <li>ExpectedResult: The report must contain only one Content Type -- Page -- and the
     *     number of entries must be 1.</li>
     * </ul>
     */
    @Test
    public void getContentTypeCountForFolder() throws DotDataException, DotSecurityException {
        Folder testFolder = null;
        try {
            // ╔══════════════════╗
            // ║  Initialization  ║
            // ╚══════════════════╝
            final Host defaultSite = hostAPI.findDefaultHost(user, false);
            final String testFolderName = "my-test-folder-" + System.currentTimeMillis();

            // ╔════════════════════════╗
            // ║  Generating Test data  ║
            // ╚════════════════════════╝
            testFolder =
                    new FolderDataGen().title(testFolderName).name(testFolderName).site(defaultSite).nextPersisted();
            TestDataUtils.getPageContent(true, defaultSite.getLanguageId(), testFolder);
            final FolderFactoryImpl folderFactory = new FolderFactoryImpl();
            final int count = folderFactory.getContentTypeCount(testFolder.getPath(),
                    defaultSite.getIdentifier());

            // ╔══════════════╗
            // ║  Assertions  ║
            // ╚══════════════╝
            assertTrue("There must be no -1 count. That means something failed", count >= 0);
            assertEquals("The count must be 1, as we added a single tst HTML Page to the folder",
                    1, count);
        } finally {
            // ╔═══════════╗
            // ║  Cleanup  ║
            // ╚═══════════╝
            if (null != testFolder) {
                folderAPI.delete(testFolder, user, false);
            }
        }
    }

    // ── findDirectChildFolders ────────────────────────────────────────────────

    /**
     * Method to test: {@link FolderFactoryImpl#findDirectChildFolders} <br>
     * Given Scenario: Two parent folders each have one direct child; both parent paths are queried. <br>
     * Expected Result: Both children are returned; an unrelated folder is not included.
     */
    @Test
    public void test_findDirectChildFolders_returnsDirectChildren()
            throws DotDataException {
        final FolderFactoryImpl factory = new FolderFactoryImpl();
        final Host site        = new SiteDataGen().nextPersisted();
        final Folder p1        = new FolderDataGen().site(site).name("parent-a").nextPersisted();
        final Folder p2        = new FolderDataGen().site(site).name("parent-b").nextPersisted();
        final Folder c1        = new FolderDataGen().site(site).parent(p1).name("child-a").nextPersisted();
        final Folder c2        = new FolderDataGen().site(site).parent(p2).name("child-b").nextPersisted();
        new FolderDataGen().site(site).name("unrelated").nextPersisted();

        final List<Folder> result = factory.findDirectChildFolders(
                site.getIdentifier(), Set.of(p1.getPath(), p2.getPath()));

        final List<String> ids = result.stream().map(Folder::getIdentifier).toList();
        assertTrue("child-a should be returned", ids.contains(c1.getIdentifier()));
        assertTrue("child-b should be returned", ids.contains(c2.getIdentifier()));
        assertEquals("only 2 direct children expected", 2, result.size());
    }

    /**
     * Method to test: {@link FolderFactoryImpl#findDirectChildFolders} <br>
     * Given Scenario: An empty path collection is passed. <br>
     * Expected Result: Empty list returned immediately without hitting the database.
     */
    @Test
    public void test_findDirectChildFolders_emptyPaths_returnsEmpty()
            throws DotDataException {
        final FolderFactoryImpl factory = new FolderFactoryImpl();
        final List<Folder> result = factory.findDirectChildFolders("any-host", Set.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Method to test: {@link FolderFactoryImpl#findDirectChildFolders} <br>
     * Given Scenario: Parent has a direct child and a grandchild; only the parent path is queried. <br>
     * Expected Result: Only the direct child is returned; the grandchild is not included.
     */
    @Test
    public void test_findDirectChildFolders_doesNotReturnGrandchildren()
            throws DotDataException {
        final FolderFactoryImpl factory = new FolderFactoryImpl();
        final Host site         = new SiteDataGen().nextPersisted();
        final Folder parent     = new FolderDataGen().site(site).name("gp-parent").nextPersisted();
        final Folder child      = new FolderDataGen().site(site).parent(parent).name("gp-child").nextPersisted();
        final Folder grandchild = new FolderDataGen().site(site).parent(child).name("gp-grandchild").nextPersisted();

        final List<Folder> result = factory.findDirectChildFolders(
                site.getIdentifier(), Set.of(parent.getPath()));

        final List<String> ids = result.stream().map(Folder::getIdentifier).toList();
        assertTrue("direct child should be returned", ids.contains(child.getIdentifier()));
        assertTrue("grandchild must not be returned",
                ids.stream().noneMatch(id -> id.equals(grandchild.getIdentifier())));
    }

}
