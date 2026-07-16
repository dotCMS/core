package com.dotcms.graphql.datafetcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotmarketing.portlets.folders.model.Folder;
import graphql.GraphQLError;
import graphql.language.Field;
import graphql.language.SelectionSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

        final List<GraphQLError> errors = new ArrayList<>();
        // depth >= maxDepth so no API calls for children
        final Map<String, Object> map = fetcher.buildFolderMap(folder, null, 3, 3, errors);

        assertEquals("inode-123", map.get("folderId"));
        assertEquals("media", map.get("folderName"));
        assertEquals("/media/", map.get("folderPath"));
        assertEquals("Media Files", map.get("folderTitle"));
        assertEquals("*.jpg", map.get("folderFileMask"));
        assertEquals(5, map.get("folderSortOrder"));
        assertEquals("fileAsset", map.get("folderDefaultFileType"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void buildFolderMap_returnsEmptyChildrenAtMaxDepth() {
        final Folder folder = createTestFolder(
                "inode-1", "docs", "/docs/", "Documents", null, 0, null);

        final List<GraphQLError> errors = new ArrayList<>();
        final Map<String, Object> map = fetcher.buildFolderMap(folder, null, 5, 5, errors);

        assertNotNull(map.get("children"));
        assertEquals(Collections.emptyList(), map.get("children"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void buildFolderMap_returnsEmptyChildrenWhenDepthExceedsMax() {
        final Folder folder = createTestFolder(
                "inode-2", "deep", "/deep/", "Deep Folder", null, 0, null);

        final List<GraphQLError> errors = new ArrayList<>();
        final Map<String, Object> map = fetcher.buildFolderMap(folder, null, 10, 3, errors);

        assertEquals(Collections.emptyList(), map.get("children"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void buildFolderMap_containsAllExpectedKeys() {
        final Folder folder = createTestFolder(
                "inode-3", "test", "/test/", "Test", null, 0, null);

        final List<GraphQLError> errors = new ArrayList<>();
        final Map<String, Object> map = fetcher.buildFolderMap(folder, null, 1, 1, errors);

        assertTrue(map.containsKey("folderId"));
        assertTrue(map.containsKey("folderFileMask"));
        assertTrue(map.containsKey("folderSortOrder"));
        assertTrue(map.containsKey("folderName"));
        assertTrue(map.containsKey("folderPath"));
        assertTrue(map.containsKey("folderTitle"));
        assertTrue(map.containsKey("folderDefaultFileType"));
        assertTrue(map.containsKey("children"));
        assertTrue(errors.isEmpty());
    }

    // --- computeRequestedDepth tests ---

    @Test
    void computeRequestedDepth_noChildren_returnsOne() {
        // { folderTitle }
        final Field field = Field.newField("DotFolderByPath")
                .selectionSet(SelectionSet.newSelectionSet()
                        .selection(Field.newField("folderTitle").build())
                        .build())
                .build();

        assertEquals(1, FolderCollectionDataFetcher.computeRequestedDepth(field));
    }

    @Test
    void computeRequestedDepth_oneChildLevel_returnsTwo() {
        // { folderTitle children { folderTitle } }
        final Field childrenField = Field.newField("children")
                .selectionSet(SelectionSet.newSelectionSet()
                        .selection(Field.newField("folderTitle").build())
                        .build())
                .build();

        final Field field = Field.newField("DotFolderByPath")
                .selectionSet(SelectionSet.newSelectionSet()
                        .selection(Field.newField("folderTitle").build())
                        .selection(childrenField)
                        .build())
                .build();

        assertEquals(2, FolderCollectionDataFetcher.computeRequestedDepth(field));
    }

    @Test
    void computeRequestedDepth_threeChildLevels_returnsFour() {
        // { children { children { children { folderTitle } } } }
        final Field level3 = Field.newField("children")
                .selectionSet(SelectionSet.newSelectionSet()
                        .selection(Field.newField("folderTitle").build())
                        .build())
                .build();
        final Field level2 = Field.newField("children")
                .selectionSet(SelectionSet.newSelectionSet()
                        .selection(level3)
                        .build())
                .build();
        final Field level1 = Field.newField("children")
                .selectionSet(SelectionSet.newSelectionSet()
                        .selection(level2)
                        .build())
                .build();
        final Field field = Field.newField("DotFolderByPath")
                .selectionSet(SelectionSet.newSelectionSet()
                        .selection(level1)
                        .build())
                .build();

        assertEquals(4, FolderCollectionDataFetcher.computeRequestedDepth(field));
    }

    @Test
    void computeRequestedDepth_noSelectionSet_returnsOne() {
        final Field field = Field.newField("DotFolderByPath").build();

        assertEquals(1, FolderCollectionDataFetcher.computeRequestedDepth(field));
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
