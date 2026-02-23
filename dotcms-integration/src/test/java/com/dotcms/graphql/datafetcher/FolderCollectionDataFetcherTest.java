package com.dotcms.graphql.datafetcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import graphql.GraphQLError;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link FolderCollectionDataFetcher#buildFolderMap}.
 * <p>
 * Tests the recursive folder map building with real folders persisted in DB.
 * The {@code get()} method requires an HTTP request for host resolution
 * and is better tested via API/E2E tests.
 */
public class FolderCollectionDataFetcherTest {

    private static User user;
    private static Folder parentFolder;
    private static Folder childFolder1;
    private static Folder childFolder2;
    private static Folder grandchildFolder;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        user = APILocator.getUserAPI().getSystemUser();
        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(user, false);

        // Create folder tree: parent -> child1, child2 -> grandchild
        parentFolder = new FolderDataGen()
                .name("graphql-test-" + System.currentTimeMillis())
                .title("GraphQL Test Folder")
                .site(defaultHost)
                .nextPersisted();

        childFolder1 = new FolderDataGen()
                .name("child1")
                .title("Child Folder 1")
                .parent(parentFolder)
                .nextPersisted();

        childFolder2 = new FolderDataGen()
                .name("child2")
                .title("Child Folder 2")
                .parent(parentFolder)
                .nextPersisted();

        grandchildFolder = new FolderDataGen()
                .name("grandchild")
                .title("Grandchild Folder")
                .parent(childFolder1)
                .nextPersisted();
    }

    @AfterClass
    public static void cleanup() {
        FolderDataGen.remove(grandchildFolder);
        FolderDataGen.remove(childFolder2);
        FolderDataGen.remove(childFolder1);
        FolderDataGen.remove(parentFolder);
    }

    /**
     * Given a folder with children and grandchildren
     * When buildFolderMap is called with sufficient depth
     * Then the map should contain correct folder properties
     */
    @Test
    public void buildFolderMap_mapsPropertiesCorrectly() {
        final FolderCollectionDataFetcher fetcher = new FolderCollectionDataFetcher();

        final List<GraphQLError> errors = new ArrayList<>();
        final Map<String, Object> map = fetcher.buildFolderMap(
                parentFolder, user, 1, 5, errors);

        assertEquals(parentFolder.getInode(), map.get("folderId"));
        assertEquals(parentFolder.getName(), map.get("folderName"));
        assertEquals(parentFolder.getPath(), map.get("folderPath"));
        assertEquals(parentFolder.getTitle(), map.get("folderTitle"));
        assertEquals(parentFolder.getFilesMasks(), map.get("folderFileMask"));
        assertEquals(parentFolder.getSortOrder(), map.get("folderSortOrder"));
        assertEquals(parentFolder.getDefaultFileType(), map.get("folderDefaultFileType"));
        assertTrue("No permission errors expected for system user", errors.isEmpty());
    }

    /**
     * Given a folder with two children
     * When buildFolderMap is called with sufficient depth
     * Then children should be loaded and contain the two child folders
     */
    @Test
    @SuppressWarnings("unchecked")
    public void buildFolderMap_loadsChildren() {
        final FolderCollectionDataFetcher fetcher = new FolderCollectionDataFetcher();

        final List<GraphQLError> errors = new ArrayList<>();
        final Map<String, Object> map = fetcher.buildFolderMap(
                parentFolder, user, 1, 5, errors);

        final List<Map<String, Object>> children =
                (List<Map<String, Object>>) map.get("children");
        assertNotNull(children);
        assertEquals(2, children.size());

        assertTrue(children.stream()
                .anyMatch(c -> childFolder1.getName().equals(c.get("folderName"))));
        assertTrue(children.stream()
                .anyMatch(c -> childFolder2.getName().equals(c.get("folderName"))));
        assertTrue(errors.isEmpty());
    }

    /**
     * Given a folder tree with grandchildren
     * When buildFolderMap is called with sufficient depth
     * Then grandchildren should be loaded recursively
     */
    @Test
    @SuppressWarnings("unchecked")
    public void buildFolderMap_loadsGrandchildrenRecursively() {
        final FolderCollectionDataFetcher fetcher = new FolderCollectionDataFetcher();

        final List<GraphQLError> errors = new ArrayList<>();
        final Map<String, Object> map = fetcher.buildFolderMap(
                parentFolder, user, 1, 5, errors);

        final List<Map<String, Object>> children =
                (List<Map<String, Object>>) map.get("children");
        final Map<String, Object> child1Map = children.stream()
                .filter(c -> childFolder1.getName().equals(c.get("folderName")))
                .findFirst()
                .orElse(null);

        assertNotNull("child1 should be present", child1Map);

        final List<Map<String, Object>> grandchildren =
                (List<Map<String, Object>>) child1Map.get("children");
        assertNotNull(grandchildren);
        assertEquals(1, grandchildren.size());
        assertEquals(grandchildFolder.getName(),
                grandchildren.get(0).get("folderName"));
        assertTrue(errors.isEmpty());
    }

    /**
     * Given a folder with children
     * When buildFolderMap is called at max depth
     * Then children should be an empty list (not loaded)
     */
    @Test
    @SuppressWarnings("unchecked")
    public void buildFolderMap_stopsAtMaxDepth() {
        final FolderCollectionDataFetcher fetcher = new FolderCollectionDataFetcher();

        final List<GraphQLError> errors = new ArrayList<>();
        // depth == maxDepth, so children should NOT be loaded
        final Map<String, Object> map = fetcher.buildFolderMap(
                parentFolder, user, 3, 3, errors);

        final List<Map<String, Object>> children =
                (List<Map<String, Object>>) map.get("children");
        assertNotNull(children);
        assertTrue("Children should be empty at max depth", children.isEmpty());
        assertTrue(errors.isEmpty());
    }

    /**
     * Given a folder with children and maxDepth=2
     * When buildFolderMap is called at depth 1
     * Then children are loaded but grandchildren are NOT (depth limit)
     */
    @Test
    @SuppressWarnings("unchecked")
    public void buildFolderMap_respectsMaxDepthForGrandchildren() {
        final FolderCollectionDataFetcher fetcher = new FolderCollectionDataFetcher();

        final List<GraphQLError> errors = new ArrayList<>();
        // maxDepth=2: parent(depth=1) -> children loaded(depth=2) -> grandchildren NOT loaded
        final Map<String, Object> map = fetcher.buildFolderMap(
                parentFolder, user, 1, 2, errors);

        final List<Map<String, Object>> children =
                (List<Map<String, Object>>) map.get("children");
        assertNotNull(children);
        assertEquals(2, children.size());

        // child1 has a grandchild, but at depth=2 it should be empty
        final Map<String, Object> child1Map = children.stream()
                .filter(c -> childFolder1.getName().equals(c.get("folderName")))
                .findFirst()
                .orElse(null);
        assertNotNull(child1Map);

        final List<Map<String, Object>> grandchildren =
                (List<Map<String, Object>>) child1Map.get("children");
        assertNotNull(grandchildren);
        assertTrue("Grandchildren should be empty at maxDepth=2",
                grandchildren.isEmpty());
        assertTrue(errors.isEmpty());
    }

    /**
     * Given a leaf folder with no sub-folders
     * When buildFolderMap is called
     * Then children should be an empty list
     */
    @Test
    @SuppressWarnings("unchecked")
    public void buildFolderMap_returnsEmptyChildrenForLeafFolder() {
        final FolderCollectionDataFetcher fetcher = new FolderCollectionDataFetcher();

        final List<GraphQLError> errors = new ArrayList<>();
        final Map<String, Object> map = fetcher.buildFolderMap(
                grandchildFolder, user, 1, 5, errors);

        final List<Map<String, Object>> children =
                (List<Map<String, Object>>) map.get("children");
        assertNotNull(children);
        assertTrue("Leaf folder should have empty children", children.isEmpty());
        assertTrue(errors.isEmpty());
    }

    /**
     * Given a folder with a child that a limited user cannot access
     * When buildFolderMap is called with that limited user
     * Then the restricted child is excluded from children
     * And a permission error is collected in the errors list
     * And the permitted sibling is still returned
     */
    @Test
    @SuppressWarnings("unchecked")
    public void buildFolderMap_surfacesPermissionErrorsForRestrictedChildren() throws Exception {
        final FolderCollectionDataFetcher fetcher = new FolderCollectionDataFetcher();

        // Create a limited user with a custom role
        final com.dotmarketing.business.Role limitedRole = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen().roles(limitedRole).nextPersisted();

        // Grant READ on parent folder to the limited user
        APILocator.getPermissionAPI().save(
                new Permission(parentFolder.getPermissionId(),
                        limitedRole.getId(),
                        PermissionAPI.PERMISSION_READ),
                parentFolder, user, false);

        // Grant READ on child1 only (child2 remains restricted)
        APILocator.getPermissionAPI().save(
                new Permission(childFolder1.getPermissionId(),
                        limitedRole.getId(),
                        PermissionAPI.PERMISSION_READ),
                childFolder1, user, false);

        // Do NOT grant permission on childFolder2

        final List<GraphQLError> errors = new ArrayList<>();
        final Map<String, Object> map = fetcher.buildFolderMap(
                parentFolder, limitedUser, 1, 5, errors);

        // child1 should be present, child2 should be excluded
        final List<Map<String, Object>> children =
                (List<Map<String, Object>>) map.get("children");
        assertNotNull(children);

        assertTrue("child1 should be accessible",
                children.stream().anyMatch(
                        c -> childFolder1.getName().equals(c.get("folderName"))));
        assertFalse("child2 should NOT be accessible",
                children.stream().anyMatch(
                        c -> childFolder2.getName().equals(c.get("folderName"))));

        // Permission error should be collected (generic, no folder path for security)
        assertFalse("Should have permission errors", errors.isEmpty());
        assertTrue("Error should mention denied subfolder count",
                errors.stream().anyMatch(
                        e -> e.getMessage().contains("do not have permission")));
    }
}
