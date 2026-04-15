package com.dotcms.storage.binary;

import com.dotcms.storage.StoragePersistenceAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BinaryAssetStorageAPIImpl}.
 * Verifies delegation to {@link StoragePersistenceAPI} with correct group names and paths.
 */
class BinaryAssetStorageAPIImplTest {

    private static final String GROUP = BinaryAssetStorageAPI.BINARY_ASSETS_GROUP;
    private static final String INODE = "abc123";
    private static final String FIELD_VAR = "fileAsset";
    private static final String FILE_NAME = "report.pdf";
    private static final String EXPECTED_FILE_PATH = "a" + File.separator
            + "b" + File.separator
            + "abc123" + File.separator
            + "fileAsset" + File.separator
            + "report.pdf";
    private static final String EXPECTED_FIELD_PATH = "a" + File.separator
            + "b" + File.separator
            + "abc123" + File.separator
            + "fileAsset";

    @Mock
    private StoragePersistenceAPI mockStorage;

    private BinaryAssetStorageAPIImpl api;

    @BeforeEach
    void setUp() throws DotDataException {
        MockitoAnnotations.openMocks(this);
        // Skip group creation during tests
        when(mockStorage.existsGroup(GROUP)).thenReturn(true);
        api = new BinaryAssetStorageAPIImpl(mockStorage);
    }

    @Test
    void test_storeBinary_delegates_to_pushFile_with_correct_path() throws Exception {
        final File testFile = File.createTempFile("test", ".pdf");
        testFile.deleteOnExit();

        api.storeBinary(INODE, FIELD_VAR, FILE_NAME, testFile);

        verify(mockStorage).pushFile(
                eq(GROUP),
                eq(EXPECTED_FILE_PATH),
                eq(testFile),
                eq(Map.<String, Serializable>of()));
    }

    @Test
    void test_getBinaryFile_delegates_to_pullFile_with_correct_path() throws Exception {
        final File expectedFile = File.createTempFile("result", ".pdf");
        expectedFile.deleteOnExit();
        when(mockStorage.pullFile(GROUP, EXPECTED_FILE_PATH)).thenReturn(expectedFile);

        final File result = api.getBinaryFile(INODE, FIELD_VAR, FILE_NAME);

        assertSame(expectedFile, result);
        verify(mockStorage).pullFile(GROUP, EXPECTED_FILE_PATH);
    }

    @Test
    void test_getBinaryFile_returns_null_when_file_missing() throws Exception {
        when(mockStorage.pullFile(GROUP, EXPECTED_FILE_PATH))
                .thenThrow(new IllegalArgumentException("does not exists"));

        final File result = api.getBinaryFile(INODE, FIELD_VAR, FILE_NAME);

        assertNull(result);
    }

    @Test
    void test_existsBinary_delegates_to_existsObject_at_field_level() throws Exception {
        when(mockStorage.existsObject(GROUP, EXPECTED_FIELD_PATH)).thenReturn(true);

        final boolean result = api.existsBinary(INODE, FIELD_VAR);

        assertTrue(result);
        verify(mockStorage).existsObject(GROUP, EXPECTED_FIELD_PATH);
    }

    @Test
    void test_deleteBinary_delegates_to_deleteObjectAndReferences_at_field_level() throws Exception {
        when(mockStorage.deleteObjectAndReferences(GROUP, EXPECTED_FIELD_PATH)).thenReturn(true);

        api.deleteBinary(INODE, FIELD_VAR);

        verify(mockStorage).deleteObjectAndReferences(GROUP, EXPECTED_FIELD_PATH);
    }

    @Test
    void test_copyBinary_pulls_source_then_pushes_to_dest() throws Exception {
        final String destInode = "def456";
        final String destPath = "d" + File.separator
                + "e" + File.separator
                + "def456" + File.separator
                + "fileAsset" + File.separator
                + "report.pdf";

        final File sourceFile = File.createTempFile("source", ".pdf");
        sourceFile.deleteOnExit();
        when(mockStorage.pullFile(GROUP, EXPECTED_FILE_PATH)).thenReturn(sourceFile);

        api.copyBinary(INODE, destInode, FIELD_VAR, FILE_NAME);

        verify(mockStorage).pullFile(GROUP, EXPECTED_FILE_PATH);
        verify(mockStorage).pushFile(
                eq(GROUP),
                eq(destPath),
                eq(sourceFile),
                eq(Map.<String, Serializable>of()));
    }

    @Test
    void test_path_construction_uses_inode_based_pattern() throws Exception {
        final File expectedFile = File.createTempFile("result", ".pdf");
        expectedFile.deleteOnExit();
        when(mockStorage.pullFile(anyString(), anyString())).thenReturn(expectedFile);

        api.getBinaryFile("xyz789", "image", "photo.jpg");

        final String expectedPath = "x" + File.separator
                + "y" + File.separator
                + "xyz789" + File.separator
                + "image" + File.separator
                + "photo.jpg";
        verify(mockStorage).pullFile(GROUP, expectedPath);
    }

    @Test
    void test_null_inode_throws_IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> api.getBinaryFile(null, FIELD_VAR, FILE_NAME));
    }

    @Test
    void test_empty_inode_throws_IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> api.getBinaryFile("", FIELD_VAR, FILE_NAME));
    }

    @Test
    void test_null_fileName_throws_IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> api.getBinaryFile(INODE, FIELD_VAR, null));
    }

    @Test
    void test_empty_fileName_throws_IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> api.storeBinary(INODE, FIELD_VAR, "", File.createTempFile("t", ".tmp")));
    }

    @Test
    void test_null_fieldVarName_throws_IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> api.existsBinary(INODE, null));
    }

    @Test
    void test_storeBinary_null_sourceFile_throws_IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> api.storeBinary(INODE, FIELD_VAR, FILE_NAME, null));
    }

    @Test
    void test_copyBinary_null_destInode_throws_IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> api.copyBinary(INODE, null, FIELD_VAR, FILE_NAME));
    }

    /**
     * Tests for the filename-less {@code getBinaryFile(inode, fieldVarName)} overload.
     * These use real temp directories since we need actual filesystem listing.
     * The impl's {@code resolveFieldDirectory()} uses {@code ConfigUtils.getAssetPath()},
     * so we mock that static method to point at our temp dir.
     */
    @Nested
    class FilenameLessLookupTest {

        @TempDir
        Path assetRoot;

        private Path createFieldDir(String inode, String fieldVarName) throws IOException {
            final Path fieldDir = assetRoot.resolve(
                    inode.charAt(0) + File.separator
                    + inode.charAt(1) + File.separator
                    + inode + File.separator
                    + fieldVarName);
            Files.createDirectories(fieldDir);
            return fieldDir;
        }

        @Test
        void test_filenameLess_lookup_finds_real_file() throws Exception {
            final Path fieldDir = createFieldDir(INODE, FIELD_VAR);
            Files.createFile(fieldDir.resolve("report.pdf"));

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                final File result = api.getBinaryFile(INODE, FIELD_VAR);

                assertNotNull(result);
                assertEquals("report.pdf", result.getName());
            }
        }

        @Test
        void test_filenameLess_lookup_excludes_generated_files() throws Exception {
            final Path fieldDir = createFieldDir(INODE, FIELD_VAR);
            Files.createFile(fieldDir.resolve(Config.GENERATED_FILE + "thumb.jpg"));
            Files.createFile(fieldDir.resolve("report.pdf"));

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                final File result = api.getBinaryFile(INODE, FIELD_VAR);

                assertNotNull(result);
                assertEquals("report.pdf", result.getName());
            }
        }

        @Test
        void test_filenameLess_lookup_excludes_hidden_files() throws Exception {
            final Path fieldDir = createFieldDir(INODE, FIELD_VAR);
            Files.createFile(fieldDir.resolve(".hidden"));
            Files.createFile(fieldDir.resolve("report.pdf"));

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                final File result = api.getBinaryFile(INODE, FIELD_VAR);

                assertNotNull(result);
                assertEquals("report.pdf", result.getName());
            }
        }

        @Test
        void test_filenameLess_lookup_empty_directory_returns_null() throws Exception {
            createFieldDir(INODE, FIELD_VAR);

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                final File result = api.getBinaryFile(INODE, FIELD_VAR);

                assertNull(result);
            }
        }

        @Test
        void test_filenameLess_lookup_only_generated_files_returns_null() throws Exception {
            final Path fieldDir = createFieldDir(INODE, FIELD_VAR);
            Files.createFile(fieldDir.resolve(Config.GENERATED_FILE + "thumb.jpg"));
            Files.createFile(fieldDir.resolve(".hidden"));

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                final File result = api.getBinaryFile(INODE, FIELD_VAR);

                assertNull(result);
            }
        }

        @Test
        void test_filenameLess_lookup_nonexistent_directory_returns_null() throws Exception {
            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                final File result = api.getBinaryFile(INODE, FIELD_VAR);

                assertNull(result);
            }
        }

        @Test
        void test_filenameLess_stream_returns_inputStream() throws Exception {
            final Path fieldDir = createFieldDir(INODE, FIELD_VAR);
            Files.write(fieldDir.resolve("report.pdf"), "content".getBytes());

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                try (InputStream result = api.getBinaryStream(INODE, FIELD_VAR)) {
                    assertNotNull(result);
                    assertEquals("content", new String(result.readAllBytes()));
                }
            }
        }

        @Test
        void test_filenameLess_stream_returns_null_when_no_file() throws Exception {
            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                final InputStream result = api.getBinaryStream(INODE, FIELD_VAR);

                assertNull(result);
            }
        }

        @Test
        void test_filenameLess_null_inode_throws_IllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> api.getBinaryFile(null, FIELD_VAR));
        }

        @Test
        void test_filenameLess_null_fieldVarName_throws_IllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> api.getBinaryFile(INODE, (String) null));
        }
    }

    /**
     * Tests for hard-link-aware {@code storeBinary} and updated {@code copyBinary}.
     * These use real temp directories and mock {@code ConfigUtils.getAssetPath()}.
     * A separate {@code api} is created backed by a {@code FileSystemStoragePersistenceAPIImpl}
     * mock so that the {@code instanceof} check in the impl passes.
     */
    @Nested
    class HardLinkAwareOperationsTest {

        @TempDir
        Path assetRoot;

        private BinaryAssetStorageAPIImpl fsApi;

        @BeforeEach
        void setUp() throws DotDataException {
            final com.dotcms.storage.FileSystemStoragePersistenceAPIImpl fsMock =
                    mock(com.dotcms.storage.FileSystemStoragePersistenceAPIImpl.class);
            when(fsMock.existsGroup(GROUP)).thenReturn(true);
            fsApi = new BinaryAssetStorageAPIImpl(fsMock);
        }

        private Path createFieldDir(String inode, String fieldVarName) throws IOException {
            final Path fieldDir = assetRoot.resolve(
                    inode.charAt(0) + File.separator
                    + inode.charAt(1) + File.separator
                    + inode + File.separator
                    + fieldVarName);
            Files.createDirectories(fieldDir);
            return fieldDir;
        }

        @Test
        void test_storeBinary_hardLink_creates_file_at_correct_path() throws Exception {
            // Create source file with content
            final Path sourceDir = assetRoot.resolve("source");
            Files.createDirectories(sourceDir);
            final File sourceFile = sourceDir.resolve("Report.PDF").toFile();
            Files.write(sourceFile.toPath(), "pdf-content".getBytes());

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                fsApi.storeBinary(INODE, FIELD_VAR, "Report.PDF", sourceFile, true);

                // Verify file exists at expected path
                final Path expectedPath = assetRoot.resolve(
                        "a" + File.separator + "b" + File.separator + "abc123"
                        + File.separator + "fileAsset" + File.separator + "Report.PDF");
                assertTrue(expectedPath.toFile().exists());
                assertEquals("pdf-content", Files.readString(expectedPath));
            }
        }

        @Test
        void test_storeBinary_hardLink_preserves_file_name_case() throws Exception {
            final Path sourceDir = assetRoot.resolve("source");
            Files.createDirectories(sourceDir);
            final File sourceFile = sourceDir.resolve("MyImage.JPG").toFile();
            Files.write(sourceFile.toPath(), "image-data".getBytes());

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                fsApi.storeBinary(INODE, FIELD_VAR, "MyImage.JPG", sourceFile, true);

                final Path expectedPath = assetRoot.resolve(
                        "a" + File.separator + "b" + File.separator + "abc123"
                        + File.separator + "fileAsset" + File.separator + "MyImage.JPG");
                assertTrue(expectedPath.toFile().exists());
                // Verify actual file name on disk preserves case
                assertEquals("MyImage.JPG", expectedPath.toFile().getName());
            }
        }

        @Test
        void test_storeBinary_hardLink_creates_parent_directories() throws Exception {
            final Path sourceDir = assetRoot.resolve("source");
            Files.createDirectories(sourceDir);
            final File sourceFile = sourceDir.resolve("test.txt").toFile();
            Files.write(sourceFile.toPath(), "content".getBytes());

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                // Field dir doesn't exist yet — storeBinary should create it
                fsApi.storeBinary(INODE, FIELD_VAR, "test.txt", sourceFile, true);

                final Path expectedPath = assetRoot.resolve(
                        "a" + File.separator + "b" + File.separator + "abc123"
                        + File.separator + "fileAsset" + File.separator + "test.txt");
                assertTrue(expectedPath.toFile().exists());
            }
        }

        @Test
        void test_copyBinary_creates_file_at_destination() throws Exception {
            // Create source file in source inode directory
            final Path sourceFieldDir = createFieldDir(INODE, FIELD_VAR);
            Files.write(sourceFieldDir.resolve("report.pdf"), "original-content".getBytes());

            final String destInode = "def456";

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                fsApi.copyBinary(INODE, destInode, FIELD_VAR, "report.pdf");

                // Verify file exists at destination
                final Path destPath = assetRoot.resolve(
                        "d" + File.separator + "e" + File.separator + "def456"
                        + File.separator + "fileAsset" + File.separator + "report.pdf");
                assertTrue(destPath.toFile().exists());
                assertEquals("original-content", Files.readString(destPath));
            }
        }

        @Test
        void test_copyBinary_preserves_file_name_case() throws Exception {
            final Path sourceFieldDir = createFieldDir(INODE, FIELD_VAR);
            Files.write(sourceFieldDir.resolve("MyDoc.PDF"), "data".getBytes());

            final String destInode = "def456";

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                fsApi.copyBinary(INODE, destInode, FIELD_VAR, "MyDoc.PDF");

                final Path destPath = assetRoot.resolve(
                        "d" + File.separator + "e" + File.separator + "def456"
                        + File.separator + "fileAsset" + File.separator + "MyDoc.PDF");
                assertTrue(destPath.toFile().exists());
                assertEquals("MyDoc.PDF", destPath.toFile().getName());
            }
        }

        @Test
        void test_storeBinary_hardLink_null_source_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> fsApi.storeBinary(INODE, FIELD_VAR, FILE_NAME, null, true));
        }
    }

    /**
     * Tests for {@code deleteAllBinaries(String inode)}.
     * Uses real temp directories since the implementation goes direct to filesystem.
     */
    @Nested
    class DeleteAllBinariesTest {

        @TempDir
        Path assetRoot;

        @Test
        void test_deleteAllBinaries_deletesInodeDirectory() throws Exception {
            // Create inode dir structure: {assetRoot}/a/b/abc123/fileAsset/test.txt
            final Path inodeDir = assetRoot.resolve(
                    "a" + File.separator + "b" + File.separator + "abc123");
            final Path fieldDir = inodeDir.resolve("fileAsset");
            Files.createDirectories(fieldDir);
            Files.write(fieldDir.resolve("test.txt"), "content".getBytes());

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                api.deleteAllBinaries(INODE);

                // Inode directory should be gone
                assertFalse(inodeDir.toFile().exists(),
                        "Inode directory should be deleted");
                // Parent directories should still exist
                assertTrue(assetRoot.resolve("a" + File.separator + "b").toFile().exists(),
                        "Parent directories should remain");
            }
        }

        @Test
        void test_deleteAllBinaries_nonExistentInode_noError() throws Exception {
            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                // Should not throw — non-existent inode is a no-op
                assertDoesNotThrow(() -> api.deleteAllBinaries(INODE));
            }
        }

        @Test
        void test_deleteAllBinaries_nullInode_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> api.deleteAllBinaries(null));
        }

        @Test
        void test_deleteAllBinaries_emptyInode_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> api.deleteAllBinaries(""));
        }
    }

}
