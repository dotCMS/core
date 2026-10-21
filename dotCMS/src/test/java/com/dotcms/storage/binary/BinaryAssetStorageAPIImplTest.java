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
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BinaryAssetStorageAPIImpl}.
 * Verifies delegation to {@link StoragePersistenceAPI} with correct group names and paths.
 */
class BinaryAssetStorageAPIImplTest {
    private String previousFeatureFlag;

    @org.junit.jupiter.api.BeforeEach
    void enableS3FeatureForTest() {
        previousFeatureFlag = Config.getStringProperty(com.dotcms.storage.AssetStorageFeature.FLAG, null);
        Config.setProperty(com.dotcms.storage.AssetStorageFeature.FLAG, true);
    }

    @org.junit.jupiter.api.AfterEach
    void restoreS3FeatureAfterTest() {
        Config.setProperty(com.dotcms.storage.AssetStorageFeature.FLAG, previousFeatureFlag);
    }


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
                () -> api.getBinaryFile(INODE, FIELD_VAR, ""));
    }

    @Test
    void test_null_fieldVarName_throws_IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> api.getBinaryFile(INODE, null));
    }

    /**
     * Field and file names that are dot segments or contain a separator would move the built path out
     * of its {@code inode/field} directory, so they are rejected before storage is touched.
     */
    @Test
    void test_dot_segments_and_separators_in_names_are_rejected() throws Exception {
        for (final String bad : java.util.List.of(".", "..", "../x", "x/y", "x\\y")) {
            assertThrows(IllegalArgumentException.class, () -> api.getBinaryFile(INODE, FIELD_VAR, bad), bad);
            assertThrows(IllegalArgumentException.class, () -> api.getBinaryStream(INODE, FIELD_VAR, bad), bad);
            assertThrows(IllegalArgumentException.class, () -> api.getBinaryFile(INODE, bad, FILE_NAME), bad);
            assertThrows(IllegalArgumentException.class, () -> api.getBinaryFile(INODE, bad), bad);
        }
        verifyNoInteractions(mockStorage);
    }

    /** An inode is at least two letters, digits, underscores or hyphens; anything else is rejected. */
    @Test
    void test_invalid_inodes_are_rejected() throws Exception {
        for (final String bad : java.util.List.of("x", "..", "../..", "ab/cd", "ab\\cd", "ab.cd")) {
            assertThrows(IllegalArgumentException.class, () -> api.getBinaryFile(bad, FIELD_VAR, FILE_NAME), bad);
            assertThrows(IllegalArgumentException.class, () -> api.getBinaryFile(bad, FIELD_VAR), bad);
        }
        verifyNoInteractions(mockStorage);
    }

    /**
     * Verification can download the whole object, and the final lock attempt fails while a lease is
     * held, so eviction must give up before verifying. Once the lease closes, the same file is verified
     * and evicted.
     */
    @Test
    void test_evictLocalFile_skips_verification_while_lease_is_held(@TempDir final Path assetRoot) throws Exception {
        final Path cached = Files.createDirectories(assetRoot.resolve("a/b/abc123/fileAsset")).resolve(FILE_NAME);
        Files.writeString(cached, "cached bytes");
        when(mockStorage.hasDurableCopy(eq(GROUP), anyString(), any(File.class))).thenReturn(true);
        try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                     mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
            configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath).thenReturn(assetRoot.toString());
            configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath)
                    .thenReturn(assetRoot.resolve("dotGenerated").toString());

            try (var lease = api.acquireCacheLease()) {
                assertFalse(api.evictLocalFile(cached.toFile()));
            }
            verify(mockStorage, never()).hasDurableCopy(anyString(), anyString(), any(File.class));
            assertTrue(Files.exists(cached));

            assertTrue(api.evictLocalFile(cached.toFile()));
            assertFalse(Files.exists(cached));
        }
    }

    /**
     * With the flag on, handing a cached file to a reader sets its access time, which orders eviction,
     * and leaves its modification time, which HTTP ETags use, unchanged. With the flag off nothing changes.
     */
    @Test
    void test_read_sets_access_time_only_with_flag_on(@TempDir final Path assetRoot) throws Exception {
        final Path cached = Files.createDirectories(assetRoot.resolve("a/b/abc123/fileAsset")).resolve(FILE_NAME);
        Files.writeString(cached, "cached bytes");
        final FileTime old = FileTime.fromMillis(System.currentTimeMillis() - 86_400_000L);
        try (MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                     mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
            configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath).thenReturn(assetRoot.toString());

            Config.setProperty(com.dotcms.storage.AssetStorageFeature.FLAG, false);
            Files.getFileAttributeView(cached, BasicFileAttributeView.class).setTimes(old, old, null);
            assertEquals(cached.toFile(), api.getBinaryFile(INODE, FIELD_VAR));
            BasicFileAttributes attributes = Files.readAttributes(cached, BasicFileAttributes.class);
            assertEquals(old, attributes.lastAccessTime());
            assertEquals(old, attributes.lastModifiedTime());

            Config.setProperty(com.dotcms.storage.AssetStorageFeature.FLAG, true);
            assertEquals(cached.toFile(), api.getBinaryFile(INODE, FIELD_VAR));
            attributes = Files.readAttributes(cached, BasicFileAttributes.class);
            assertTrue(attributes.lastAccessTime().compareTo(old) > 0);
            assertEquals(old, attributes.lastModifiedTime());
        }
    }

    /**
     * A row whose {@code contentlet_as_json} is null (JSON persistence off, or not yet populated) says
     * nothing about its binaries, so lookups fall back to the {@code inode/field/fileName} layout.
     */
    @Test
    void test_row_without_content_json_uses_layout_lookup(@TempDir final Path assetRoot) throws Exception {
        final File expectedFile = File.createTempFile("result", ".pdf");
        expectedFile.deleteOnExit();
        when(mockStorage.pullFile(GROUP, EXPECTED_FILE_PATH)).thenReturn(expectedFile);
        when(mockStorage.listObjectPaths(GROUP, EXPECTED_FIELD_PATH))
                .thenReturn(java.util.List.of(EXPECTED_FILE_PATH));
        final BinaryAssetStorageAPIImpl referencesApi = new BinaryAssetStorageAPIImpl(mockStorage, true);
        try (MockedStatic<BinaryAssetReference> references = mockStatic(BinaryAssetReference.class,
                call -> "contentJson".equals(call.getMethod().getName()) ? null : call.callRealMethod());
             MockedStatic<com.dotmarketing.util.ConfigUtils> configUtils =
                     mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
            configUtils.when(com.dotmarketing.util.ConfigUtils::getAssetPath).thenReturn(assetRoot.toString());

            assertSame(expectedFile, referencesApi.getBinaryFile(INODE, FIELD_VAR, FILE_NAME));
            assertSame(expectedFile, referencesApi.getBinaryFile(INODE, FIELD_VAR));
            references.verify(() -> BinaryAssetReference.contentJson(INODE), atLeast(2));
        }
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
                configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath)
                        .thenReturn(assetRoot.resolve("dotGenerated").toString());

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
                configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath)
                        .thenReturn(assetRoot.resolve("dotGenerated").toString());

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
                configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath)
                        .thenReturn(assetRoot.resolve("dotGenerated").toString());

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
                configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath)
                        .thenReturn(assetRoot.resolve("dotGenerated").toString());

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
                configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath)
                        .thenReturn(assetRoot.resolve("dotGenerated").toString());

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
                configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath)
                        .thenReturn(assetRoot.resolve("dotGenerated").toString());

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
                configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath)
                        .thenReturn(assetRoot.resolve("dotGenerated").toString());

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
                configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath)
                        .thenReturn(assetRoot.resolve("dotGenerated").toString());

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
     * Tests for chain-mode behavior. Verifies that when a ChainableStoragePersistenceAPI
     * is passed (simulating BINARY_CHAIN config), the API delegates correctly.
     * Config routing itself is a simple string comparison — tested here via behavior.
     */
    @Nested
    class ChainModeTest {

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
    }

    /**
     * Tests for chain-mode directory operations: filename-less getBinaryFile.
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
                configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath)
                        .thenReturn(assetRoot.resolve("dotGenerated").toString());

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
                configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath)
                        .thenReturn(assetRoot.resolve("dotGenerated").toString());

                final File result = chainApi.getBinaryFile(INODE, FIELD_VAR);

                assertNotNull(result);
                assertEquals("report.pdf", result.getName());
                // listObjectPaths should NOT be called — local listing found the file
                verify(mockFs, never()).listObjectPaths(anyString(), anyString());
                verify(mockS3, never()).listObjectPaths(anyString(), anyString());
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
                configUtils.when(com.dotmarketing.util.ConfigUtils::getDotGeneratedPath)
                        .thenReturn(assetRoot.resolve("dotGenerated").toString());

                final File result = chainApi.getBinaryFile(INODE, FIELD_VAR);

                assertNotNull(result);
                assertSame(cachedFile, result);
                // Only the real file should be pulled, not the generated one
                verify(mockFs).pullFile(GROUP, EXPECTED_FILE_PATH);
                verify(mockFs, never()).pullFile(eq(GROUP), eq(generatedPath));
            }
        }
    }

}
