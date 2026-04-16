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

    /**
     * Tests for chain-mode behavior. Verifies that when a ChainableStoragePersistenceAPI
     * is passed (simulating BINARY_CHAIN config), the API delegates correctly.
     * Config routing itself is a simple string comparison — tested here via behavior.
     */
    @Nested
    class ChainModeTest {

        @Test
        void test_chain_provider_delegates_operations_through_chain() throws Exception {
            // Build a real chain with two mock providers
            final StoragePersistenceAPI mockFs = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI mockS3 = mock(StoragePersistenceAPI.class);

            final StoragePersistenceAPI chain = new com.dotcms.storage.ChainableStoragePersistenceAPIBuilder()
                    .add(mockFs)
                    .add(mockS3)
                    .get();

            // Both providers report group exists (chain uses allMatch)
            when(mockFs.existsGroup(GROUP)).thenReturn(true);
            when(mockS3.existsGroup(GROUP)).thenReturn(true);

            final BinaryAssetStorageAPIImpl chainApi = new BinaryAssetStorageAPIImpl(chain);

            // Store a binary — chain writes to ALL providers
            final File testFile = File.createTempFile("chain-test", ".txt");
            testFile.deleteOnExit();

            chainApi.storeBinary(INODE, FIELD_VAR, FILE_NAME, testFile);

            // Both FS and S3 should receive the pushFile call
            verify(mockFs).pushFile(eq(GROUP), eq(EXPECTED_FILE_PATH), eq(testFile), anyMap());
            verify(mockS3).pushFile(eq(GROUP), eq(EXPECTED_FILE_PATH), eq(testFile), anyMap());
        }

        @Test
        void test_chain_provider_reads_from_first_provider() throws Exception {
            final StoragePersistenceAPI mockFs = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI mockS3 = mock(StoragePersistenceAPI.class);

            final StoragePersistenceAPI chain = new com.dotcms.storage.ChainableStoragePersistenceAPIBuilder()
                    .add(mockFs)
                    .add(mockS3)
                    .get();

            when(mockFs.existsGroup(GROUP)).thenReturn(true);
            when(mockS3.existsGroup(GROUP)).thenReturn(true);

            // FS has the file (local cache hit)
            final File cachedFile = File.createTempFile("cached", ".pdf");
            cachedFile.deleteOnExit();
            when(mockFs.pullFile(GROUP, EXPECTED_FILE_PATH)).thenReturn(cachedFile);

            final BinaryAssetStorageAPIImpl chainApi = new BinaryAssetStorageAPIImpl(chain);
            final File result = chainApi.getBinaryFile(INODE, FIELD_VAR, FILE_NAME);

            assertSame(cachedFile, result);
            // FS was hit, S3 should NOT be called (cache hit)
            verify(mockFs).pullFile(GROUP, EXPECTED_FILE_PATH);
            verify(mockS3, never()).pullFile(anyString(), anyString());
        }

        @Test
        void test_chain_storeBinary_skips_hardLink_mode() throws Exception {
            // In chain mode, instanceof FileSystemStoragePersistenceAPIImpl is false,
            // so storeBinary(hardLink=true) should fall through to pushFile path
            final StoragePersistenceAPI mockFs = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI mockS3 = mock(StoragePersistenceAPI.class);

            final StoragePersistenceAPI chain = new com.dotcms.storage.ChainableStoragePersistenceAPIBuilder()
                    .add(mockFs)
                    .add(mockS3)
                    .get();

            when(mockFs.existsGroup(GROUP)).thenReturn(true);
            when(mockS3.existsGroup(GROUP)).thenReturn(true);

            final BinaryAssetStorageAPIImpl chainApi = new BinaryAssetStorageAPIImpl(chain);

            final File testFile = File.createTempFile("hardlink-test", ".txt");
            testFile.deleteOnExit();

            // hardLink=true but chain is not instanceof FS — should use pushFile, not FileUtil.copyFile
            chainApi.storeBinary(INODE, FIELD_VAR, FILE_NAME, testFile, true);

            // Should delegate through chain's pushFile (not direct filesystem hard-link)
            verify(mockFs).pushFile(eq(GROUP), eq(EXPECTED_FILE_PATH), eq(testFile), anyMap());
            verify(mockS3).pushFile(eq(GROUP), eq(EXPECTED_FILE_PATH), eq(testFile), anyMap());
        }

        @Test
        void test_chain_copyBinary_uses_pullFile_pushFile() throws Exception {
            final StoragePersistenceAPI mockFs = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI mockS3 = mock(StoragePersistenceAPI.class);

            final StoragePersistenceAPI chain = new com.dotcms.storage.ChainableStoragePersistenceAPIBuilder()
                    .add(mockFs)
                    .add(mockS3)
                    .get();

            when(mockFs.existsGroup(GROUP)).thenReturn(true);
            when(mockS3.existsGroup(GROUP)).thenReturn(true);

            final String destInode = "def456";
            final String destPath = "d" + File.separator + "e" + File.separator
                    + "def456" + File.separator + "fileAsset" + File.separator + "report.pdf";

            final File sourceFile = File.createTempFile("source", ".pdf");
            sourceFile.deleteOnExit();
            // Chain's pullFile returns from first provider that has it
            when(mockFs.pullFile(GROUP, EXPECTED_FILE_PATH)).thenReturn(sourceFile);

            final BinaryAssetStorageAPIImpl chainApi = new BinaryAssetStorageAPIImpl(chain);
            chainApi.copyBinary(INODE, destInode, FIELD_VAR, FILE_NAME);

            // Chain is not instanceof FS, so copyBinary uses pullFile + pushFile path
            // pushFile on chain writes to both providers
            verify(mockFs).pushFile(eq(GROUP), eq(destPath), eq(sourceFile), anyMap());
            verify(mockS3).pushFile(eq(GROUP), eq(destPath), eq(sourceFile), anyMap());
        }
    }

    /**
     * Tests for chain-mode directory operations: filename-less getBinaryFile and deleteAllBinaries.
     * Verifies that these methods correctly discover objects through the chain when not cached locally.
     */
    @Nested
    class ChainDirectoryOperationsTest {

        @TempDir
        Path assetRoot;

        private StoragePersistenceAPI buildChain(StoragePersistenceAPI mockFs,
                                                  StoragePersistenceAPI mockS3) throws DotDataException {

            when(mockFs.existsGroup(GROUP)).thenReturn(true);
            when(mockS3.existsGroup(GROUP)).thenReturn(true);

            return new com.dotcms.storage.ChainableStoragePersistenceAPIBuilder()
                    .add(mockFs)
                    .add(mockS3)
                    .get();
        }

        @Test
        void test_getBinaryFile_filenameLess_discovers_from_chain_when_not_local() throws Exception {
            final StoragePersistenceAPI mockFs = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI mockS3 = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI chain = buildChain(mockFs, mockS3);

            // FS has no objects (cold cache)
            when(mockFs.listObjectPaths(eq(GROUP), eq(EXPECTED_FIELD_PATH)))
                    .thenReturn(java.util.List.of());
            // S3 has the file
            when(mockS3.listObjectPaths(eq(GROUP), eq(EXPECTED_FIELD_PATH)))
                    .thenReturn(java.util.List.of(EXPECTED_FILE_PATH));

            // Chain pullFile returns a file (downloaded from S3)
            final File cachedFile = File.createTempFile("cached", ".pdf");
            cachedFile.deleteOnExit();
            when(mockFs.pullFile(GROUP, EXPECTED_FILE_PATH)).thenReturn(cachedFile);

            final BinaryAssetStorageAPIImpl chainApi = new BinaryAssetStorageAPIImpl(chain);

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                final File result = chainApi.getBinaryFile(INODE, FIELD_VAR);

                assertNotNull(result);
                assertSame(cachedFile, result);
            }
        }

        @Test
        void test_getBinaryFile_filenameLess_prefers_local_fs_listing() throws Exception {
            final StoragePersistenceAPI mockFs = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI mockS3 = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI chain = buildChain(mockFs, mockS3);

            final BinaryAssetStorageAPIImpl chainApi = new BinaryAssetStorageAPIImpl(chain);

            // Create actual file in local FS cache
            final Path fieldDir = assetRoot.resolve(
                    INODE.charAt(0) + File.separator
                    + INODE.charAt(1) + File.separator
                    + INODE + File.separator
                    + FIELD_VAR);
            Files.createDirectories(fieldDir);
            Files.createFile(fieldDir.resolve("report.pdf"));

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                final File result = chainApi.getBinaryFile(INODE, FIELD_VAR);

                assertNotNull(result);
                assertEquals("report.pdf", result.getName());
                // listObjectPaths should NOT be called — local listing found the file
                verify(mockFs, never()).listObjectPaths(anyString(), anyString());
                verify(mockS3, never()).listObjectPaths(anyString(), anyString());
            }
        }

        @Test
        void test_deleteAllBinaries_cleans_chain_providers() throws Exception {
            final StoragePersistenceAPI mockFs = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI mockS3 = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI chain = buildChain(mockFs, mockS3);

            final String inodePath = INODE.charAt(0) + File.separator
                    + INODE.charAt(1) + File.separator + INODE;
            final String filePath1 = inodePath + File.separator + "fileAsset" + File.separator + "doc.pdf";
            final String filePath2 = inodePath + File.separator + "image" + File.separator + "photo.jpg";

            // S3 has objects under this inode
            when(mockFs.listObjectPaths(eq(GROUP), eq(inodePath)))
                    .thenReturn(java.util.List.of());
            when(mockS3.listObjectPaths(eq(GROUP), eq(inodePath)))
                    .thenReturn(java.util.List.of(filePath1, filePath2));

            // deleteObjectAndReferences succeeds
            when(mockFs.deleteObjectAndReferences(eq(GROUP), anyString())).thenReturn(true);
            when(mockS3.deleteObjectAndReferences(eq(GROUP), anyString())).thenReturn(true);

            final BinaryAssetStorageAPIImpl chainApi = new BinaryAssetStorageAPIImpl(chain);

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                chainApi.deleteAllBinaries(INODE);

                // Chain's deleteObjectAndReferences propagates to all providers
                verify(mockFs).deleteObjectAndReferences(GROUP, filePath1);
                verify(mockS3).deleteObjectAndReferences(GROUP, filePath1);
                verify(mockFs).deleteObjectAndReferences(GROUP, filePath2);
                verify(mockS3).deleteObjectAndReferences(GROUP, filePath2);
            }
        }

        @Test
        void test_deleteAllBinaries_fs_mode_no_chain_deletion() throws Exception {
            // Use FileSystemStoragePersistenceAPIImpl mock — instanceof check passes,
            // so chain deletion path should NOT execute
            final com.dotcms.storage.FileSystemStoragePersistenceAPIImpl fsMock =
                    mock(com.dotcms.storage.FileSystemStoragePersistenceAPIImpl.class);
            when(fsMock.existsGroup(GROUP)).thenReturn(true);

            final BinaryAssetStorageAPIImpl fsApi = new BinaryAssetStorageAPIImpl(fsMock);

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                fsApi.deleteAllBinaries(INODE);

                // listObjectPaths should NOT be called (FS mode skips chain deletion)
                verify(fsMock, never()).listObjectPaths(anyString(), anyString());
            }
        }

        @Test
        void test_getBinaryFile_filenameLess_chain_skips_generated_files() throws Exception {
            final StoragePersistenceAPI mockFs = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI mockS3 = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI chain = buildChain(mockFs, mockS3);

            final String generatedPath = EXPECTED_FIELD_PATH + File.separator
                    + Config.GENERATED_FILE + "thumb.jpg";

            // S3 returns generated file AND real file
            when(mockFs.listObjectPaths(eq(GROUP), eq(EXPECTED_FIELD_PATH)))
                    .thenReturn(java.util.List.of());
            when(mockS3.listObjectPaths(eq(GROUP), eq(EXPECTED_FIELD_PATH)))
                    .thenReturn(java.util.List.of(generatedPath, EXPECTED_FILE_PATH));

            final File cachedFile = File.createTempFile("cached", ".pdf");
            cachedFile.deleteOnExit();
            when(mockFs.pullFile(GROUP, EXPECTED_FILE_PATH)).thenReturn(cachedFile);

            final BinaryAssetStorageAPIImpl chainApi = new BinaryAssetStorageAPIImpl(chain);

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                final File result = chainApi.getBinaryFile(INODE, FIELD_VAR);

                assertNotNull(result);
                assertSame(cachedFile, result);
                // Only the real file should be pulled, not the generated one
                verify(mockFs).pullFile(GROUP, EXPECTED_FILE_PATH);
                verify(mockFs, never()).pullFile(eq(GROUP), eq(generatedPath));
            }
        }

        @Test
        void test_deleteAllBinaries_partial_failure_logs_and_throws_summary() throws Exception {
            final StoragePersistenceAPI mockFs = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI mockS3 = mock(StoragePersistenceAPI.class);
            final StoragePersistenceAPI chain = buildChain(mockFs, mockS3);

            final String inodePath = INODE.charAt(0) + File.separator
                    + INODE.charAt(1) + File.separator + INODE;
            final String path1 = inodePath + File.separator + "f1" + File.separator + "a.txt";
            final String path2 = inodePath + File.separator + "f2" + File.separator + "b.txt";
            final String path3 = inodePath + File.separator + "f3" + File.separator + "c.txt";

            // Chain lists 3 objects
            when(mockFs.listObjectPaths(eq(GROUP), eq(inodePath)))
                    .thenReturn(java.util.List.of());
            when(mockS3.listObjectPaths(eq(GROUP), eq(inodePath)))
                    .thenReturn(java.util.List.of(path1, path2, path3));

            // Delete succeeds for 1 and 3, fails for 2
            when(mockFs.deleteObjectAndReferences(eq(GROUP), eq(path1))).thenReturn(true);
            when(mockS3.deleteObjectAndReferences(eq(GROUP), eq(path1))).thenReturn(true);

            when(mockFs.deleteObjectAndReferences(eq(GROUP), eq(path2)))
                    .thenThrow(new DotDataException("S3 delete failed for path2"));

            when(mockFs.deleteObjectAndReferences(eq(GROUP), eq(path3))).thenReturn(true);
            when(mockS3.deleteObjectAndReferences(eq(GROUP), eq(path3))).thenReturn(true);

            final BinaryAssetStorageAPIImpl chainApi = new BinaryAssetStorageAPIImpl(chain);

            try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                         mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
                configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath)
                        .thenReturn(assetRoot.toString());

                final DotDataException ex = assertThrows(DotDataException.class,
                        () -> chainApi.deleteAllBinaries(INODE));

                // Summary exception should mention the failure count
                assertTrue(ex.getMessage().contains("1 of 3"));

                // All 3 deletes should have been attempted (not short-circuited on path2 failure)
                verify(mockFs).deleteObjectAndReferences(GROUP, path1);
                verify(mockFs).deleteObjectAndReferences(GROUP, path3);
            }
        }
    }

}
