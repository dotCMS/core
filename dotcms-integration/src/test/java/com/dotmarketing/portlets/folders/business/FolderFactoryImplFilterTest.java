package com.dotmarketing.portlets.folders.business;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.folders.business.FolderSearchParams;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link FolderFactoryImpl#searchFolders}.
 */
public class FolderFactoryImplFilterTest {

    private static FolderFactoryImpl folderFactory;
    private static Host site1;
    private static Host site2;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        folderFactory = new FolderFactoryImpl();
        site1 = new SiteDataGen().nextPersisted();
        site2 = new SiteDataGen().nextPersisted();
    }

    /**
     * Method to test: {@link FolderFactoryImpl#searchFolders} <br>
     * Given Scenario: name filter matches two of three folders. <br>
     * Expected Result: Both matching folders are returned; non-matching folder is not.
     */
    @Test
    public void test_searchFolders_matchesPartialName() throws DotDataException {
        final long ts = System.currentTimeMillis();
        new FolderDataGen().site(site1).name("images-" + ts).nextPersisted();
        new FolderDataGen().site(site1).name("my-images-" + ts).nextPersisted();
        new FolderDataGen().site(site1).name("docs-" + ts).nextPersisted();

        final List<Folder> results = folderFactory.searchFolders(FolderSearchParams.builder()
                .name("images-" + ts)
                .siteId(site1.getIdentifier())
                .build());

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(f -> f.getName().contains("images-" + ts)));
        assertFalse(results.stream().anyMatch(f -> f.getName().startsWith("docs")));
    }

    /**
     * Method to test: {@link FolderFactoryImpl#searchFolders} <br>
     * Given Scenario: name filter supplied in uppercase. <br>
     * Expected Result: Same folder is returned regardless of case.
     */
    @Test
    public void test_searchFolders_isCaseInsensitive() throws DotDataException {
        final long ts = System.currentTimeMillis();
        new FolderDataGen().site(site1).name("Assets-" + ts).nextPersisted();

        final List<Folder> lower = folderFactory.searchFolders(FolderSearchParams.builder()
                .name("assets-" + ts)
                .siteId(site1.getIdentifier())
                .build());
        final List<Folder> upper = folderFactory.searchFolders(FolderSearchParams.builder()
                .name("ASSETS-" + ts)
                .siteId(site1.getIdentifier())
                .build());

        assertEquals(1, lower.size());
        assertEquals(1, upper.size());
        assertEquals(lower.get(0).getIdentifier(), upper.get(0).getIdentifier());
    }

    /**
     * Method to test: {@link FolderFactoryImpl#searchFolders} <br>
     * Given Scenario: Same folder name on two sites; searched within one site only. <br>
     * Expected Result: Only the folder on the specified site is returned.
     */
    @Test
    public void test_searchFolders_scopesToSite() throws DotDataException {
        final long ts = System.currentTimeMillis();
        final String sharedName = "shared-folder-" + ts;
        new FolderDataGen().site(site1).name(sharedName).nextPersisted();
        new FolderDataGen().site(site2).name(sharedName).nextPersisted();

        final List<Folder> results = folderFactory.searchFolders(FolderSearchParams.builder()
                .name(sharedName)
                .siteId(site1.getIdentifier())
                .build());

        assertEquals(1, results.size());
        assertEquals(site1.getIdentifier(), results.get(0).getHostId());
    }

    /**
     * Method to test: {@link FolderFactoryImpl#searchFolders} <br>
     * Given Scenario: No name filter — returns all folders in site. <br>
     * Expected Result: At least the folders created for this test are present.
     */
    @Test
    public void test_searchFolders_noName_returnsAllFolders() throws DotDataException {
        final long ts = System.currentTimeMillis();
        final Host freshSite = new SiteDataGen().nextPersisted();
        new FolderDataGen().site(freshSite).name("folder-a-" + ts).nextPersisted();
        new FolderDataGen().site(freshSite).name("folder-b-" + ts).nextPersisted();

        final List<Folder> results = folderFactory.searchFolders(FolderSearchParams.builder()
                .siteId(freshSite.getIdentifier())
                .build());

        assertTrue(results.size() >= 2);
    }

    /**
     * Method to test: {@link FolderFactoryImpl#searchFolders} <br>
     * Given Scenario: name filter + path scope with recursive=true. <br>
     * Expected Result: Only folders matching the name AND within the path subtree are returned.
     */
    @Test
    public void test_searchFolders_nameAndPath_recursive_returnsIntersection() throws DotDataException {
        final Host freshSite = new SiteDataGen().nextPersisted();
        final Folder parent = new FolderDataGen().site(freshSite).name("parent").nextPersisted();
        new FolderDataGen().site(freshSite).parent(parent).name("target").nextPersisted(); // in scope
        new FolderDataGen().site(freshSite).name("target-root").nextPersisted();           // out of scope — "target" is a substring of both names

        // "target" matches both folder names — path scope is what limits results to 1
        final List<Folder> results = folderFactory.searchFolders(FolderSearchParams.builder()
                .name("target")
                .path("/" + parent.getName() + "/")
                .recursive(true)
                .siteId(freshSite.getIdentifier())
                .build());

        assertEquals(1, results.size());
        assertEquals("target", results.get(0).getName());
    }

    /**
     * Method to test: {@link FolderFactoryImpl#searchFolders} <br>
     * Given Scenario: path scope + recursive=false (direct children only). <br>
     * Expected Result: Only the direct child is returned; grandchild is excluded.
     */
    @Test
    public void test_searchFolders_pathNotRecursive_returnsDirectChildOnly() throws DotDataException {
        final long ts = System.currentTimeMillis();
        final Folder parent = new FolderDataGen().site(site1).name("par-" + ts).nextPersisted();
        final Folder child = new FolderDataGen().site(site1).parent(parent).name("child-" + ts).nextPersisted();
        new FolderDataGen().site(site1).parent(child).name("grandchild-" + ts).nextPersisted();

        final List<Folder> results = folderFactory.searchFolders(FolderSearchParams.builder()
                .path("/" + parent.getName() + "/")
                .recursive(false)
                .siteId(site1.getIdentifier())
                .build());

        assertEquals(1, results.size());
        assertEquals("child-" + ts, results.get(0).getName());
    }

    /**
     * Method to test: {@link FolderFactoryImpl#searchFolders} <br>
     * Given Scenario: Filter does not match any existing folder name. <br>
     * Expected Result: Empty list is returned.
     */
    @Test
    public void test_searchFolders_noMatch_returnsEmptyList() throws DotDataException {
        final List<Folder> results = folderFactory.searchFolders(FolderSearchParams.builder()
                .name("zzz-no-match-xyz-" + System.currentTimeMillis())
                .siteId(site1.getIdentifier())
                .build());

        assertTrue(results.isEmpty());
    }
}
