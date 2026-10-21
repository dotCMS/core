package com.dotcms.storage.binary;

import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.FileStorageAPI;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Field archives list files that were already gone in {@code missing.json}, but a file that was
 * listed and then disappears, or an upload S3 does not confirm, still fails the archive.
 */
class ContentletBackupStorageFieldArchiveTest {
    private static final String IDENTIFIER = "identifier-1";
    private static final String INODE = "ab12cd34";
    private static final String BINARY = "a/b/ab12cd34/heroImage/.revisions/r1/Hero.Txt";
    private static final String METADATA = "/a/b/ab12cd34/heroImage/.revisions/r1/Hero.Txt-metadata.json";

    @TempDir Path assets;
    private String previousFlag;
    private MockedStatic<APILocator> locator;
    private MockedStatic<DbConnectionFactory> database;
    private BinaryAssetStorageAPI binaries;
    private FileStorageAPI files;
    private StoragePersistenceAPI remote;
    private final Map<String, byte[]> uploaded = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
        locator = mockStatic(APILocator.class);
        database = mockStatic(DbConnectionFactory.class);
        database.when(DbConnectionFactory::inTransaction).thenReturn(true);
        binaries = mock(BinaryAssetStorageAPI.class);
        files = mock(FileStorageAPI.class);
        remote = mock(StoragePersistenceAPI.class);
        locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(binaries);
        locator.when(APILocator::getFileStorageAPI).thenReturn(files);
        when(binaries.acquireCacheLease()).thenReturn(() -> { });
        when(remote.backfillFile(eq(ContentletBackupStorage.GROUP), anyString(), any(File.class))).thenAnswer(call -> {
            final File archive = call.getArgument(2);
            final var entries = new LinkedHashMap<String, byte[]>();
            try (var zip = new ZipInputStream(Files.newInputStream(archive.toPath()))) {
                for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    entries.put(entry.getName(), zip.readAllBytes());
                }
            }
            uploaded.putAll(entries);
            return true;
        });
    }

    @AfterEach
    void tearDown() {
        database.close();
        locator.close();
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    @Test
    void missingFilesAreListedInTheManifest() throws Exception {
        givenBinary("hero bytes");
        givenMetadata(Map.of("name", "Hero.Txt"));
        final List<Map<String, String>> missing = List.of(
                Map.of("kind", "binary", "field", "heroImage", "path", "binary-assets/a/b/ab12cd34/heroImage/Lost.Txt"));
        store(missing);
        assertArrayEquals("hero bytes".getBytes(StandardCharsets.UTF_8), uploaded.get("binary-assets/" + BINARY));
        assertNotNull(uploaded.get("dotmetadata/" + METADATA.substring(1)));
        assertEquals(new ObjectMapper().valueToTree(missing),
                new ObjectMapper().readTree(uploaded.get(ContentletBackupStorage.MISSING_MANIFEST)));
    }

    @Test
    void completeArchivesHaveNoManifest() throws Exception {
        givenBinary("hero bytes");
        givenMetadata(Map.of("name", "Hero.Txt"));
        store(List.of());
        assertFalse(uploaded.containsKey(ContentletBackupStorage.MISSING_MANIFEST));
        assertTrue(uploaded.containsKey("binary-assets/" + BINARY));
    }

    @Test
    void listedBinaryThatVanishesStillAborts() throws Exception {
        // The path was in the storage listing, so its absence now is a concurrent change: abort so a
        // retry relists, instead of recording it as missing.
        when(binaries.openLocalFile(any(File.class))).thenThrow(new NoSuchFileException(BINARY));
        givenMetadata(Map.of("name", "Hero.Txt"));
        assertThrows(DotDataException.class, () -> store(List.of()));
        verify(remote, never()).backfillFile(anyString(), anyString(), any(File.class));
    }

    @Test
    void listedMetadataThatVanishesStillAborts() throws Exception {
        givenBinary("hero bytes");
        when(files.retrieveRawMetaData(any())).thenReturn(null);
        assertThrows(DotDataException.class, () -> store(List.of()));
        verify(remote, never()).backfillFile(anyString(), anyString(), any(File.class));
    }

    @Test
    void unconfirmedUploadStillAborts() throws Exception {
        givenBinary("hero bytes");
        givenMetadata(Map.of("name", "Hero.Txt"));
        when(remote.backfillFile(eq(ContentletBackupStorage.GROUP), anyString(), any(File.class))).thenReturn(false);
        assertThrows(DotDataException.class, () -> store(List.of(
                Map.of("kind", "binary", "field", "heroImage", "path", "binary-assets/lost"))));
    }

    private void store(final List<Map<String, String>> missing) throws DotDataException {
        Config.setProperty("ASSET_REAL_PATH", assets.toString());
        new ContentletBackupStorage(remote).storeField(IDENTIFIER, INODE, "{\"fields\":{}}",
                List.of(BINARY), List.of(METADATA), missing);
    }

    private void givenBinary(final String bytes) throws Exception {
        when(binaries.openLocalFile(any(File.class))).thenAnswer(
                call -> new ByteArrayInputStream(bytes.getBytes(StandardCharsets.UTF_8)));
    }

    private void givenMetadata(final Map<String, Serializable> metadata) throws Exception {
        when(files.retrieveRawMetaData(any())).thenReturn(metadata);
    }
}
