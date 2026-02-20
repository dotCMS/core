package com.dotcms.graphql.datafetcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotmarketing.portlets.folders.model.Folder;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FolderCollectionDataFetcherTest {

    private final FolderCollectionDataFetcher fetcher = new FolderCollectionDataFetcher();

    @Test
    void defaultMaxDepth_isThree() {
        assertEquals(3, FolderCollectionDataFetcher.DEFAULT_MAX_DEPTH);
    }

    @Test
    void buildFolderMap_mapsAllFolderProperties() {
        final Folder folder = createTestFolder(
                "inode-123", "media", "/media/", "Media Files", "*.jpg", 5, "fileAsset");

        // depth >= maxDepth so no API calls for children
        final Map<String, Object> map = fetcher.buildFolderMap(folder, null, 3, 3);

        assertEquals("inode-123", map.get("folderId"));
        assertEquals("media", map.get("folderName"));
        assertEquals("/media/", map.get("folderPath"));
        assertEquals("Media Files", map.get("folderTitle"));
        assertEquals("*.jpg", map.get("folderFileMask"));
        assertEquals(5, map.get("folderSortOrder"));
        assertEquals("fileAsset", map.get("folderDefaultFileType"));
    }

    @Test
    void buildFolderMap_returnsEmptyChildrenAtMaxDepth() {
        final Folder folder = createTestFolder(
                "inode-1", "docs", "/docs/", "Documents", null, 0, null);

        final Map<String, Object> map = fetcher.buildFolderMap(folder, null, 5, 5);

        assertNotNull(map.get("children"));
        assertEquals(Collections.emptyList(), map.get("children"));
    }

    @Test
    void buildFolderMap_returnsEmptyChildrenWhenDepthExceedsMax() {
        final Folder folder = createTestFolder(
                "inode-2", "deep", "/deep/", "Deep Folder", null, 0, null);

        final Map<String, Object> map = fetcher.buildFolderMap(folder, null, 10, 3);

        assertEquals(Collections.emptyList(), map.get("children"));
    }

    @Test
    void buildFolderMap_containsAllExpectedKeys() {
        final Folder folder = createTestFolder(
                "inode-3", "test", "/test/", "Test", null, 0, null);

        final Map<String, Object> map = fetcher.buildFolderMap(folder, null, 1, 1);

        assertTrue(map.containsKey("folderId"));
        assertTrue(map.containsKey("folderFileMask"));
        assertTrue(map.containsKey("folderSortOrder"));
        assertTrue(map.containsKey("folderName"));
        assertTrue(map.containsKey("folderPath"));
        assertTrue(map.containsKey("folderTitle"));
        assertTrue(map.containsKey("folderDefaultFileType"));
        assertTrue(map.containsKey("children"));
    }

    private Folder createTestFolder(final String inode, final String name,
            final String path, final String title, final String filesMask,
            final int sortOrder, final String defaultFileType) {

        final Folder folder = new Folder();
        folder.setInode(inode);
        folder.setName(name);
        folder.setPath(path);
        folder.setTitle(title);
        folder.setFilesMasks(filesMask);
        folder.setSortOrder(sortOrder);
        folder.setDefaultFileType(defaultFileType);
        return folder;
    }
}
