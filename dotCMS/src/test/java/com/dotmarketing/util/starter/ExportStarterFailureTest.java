package com.dotmarketing.util.starter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.binary.BinaryAssetStorageAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.ZipUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * What a client receives when an asset export fails part way: one file is exported, then reading
 * the next one fails.
 */
class ExportStarterFailureTest {
    @TempDir
    Path assets;
    @TempDir
    Path downloads;
    private MockedStatic<APILocator> locator;
    private MockedStatic<ConfigUtils> paths;
    private MockedStatic<ZipUtil> zips;
    private MockedConstruction<DotConnect> queries;
    private String previousFlag;

    @BeforeEach
    void configure() throws Exception {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Files.createDirectories(assets.resolve("0/0/aa"));
        Files.writeString(assets.resolve("0/0/aa/one.txt"), "first asset");
        Files.createDirectories(assets.resolve("0/1/bb"));
        Files.writeString(assets.resolve("0/1/bb/two.txt"), "second asset");
        paths = mockStatic(ConfigUtils.class);
        paths.when(ConfigUtils::getAssetPath).thenReturn(assets.toString());
        final var binaries = mock(BinaryAssetStorageAPI.class);
        when(binaries.acquireCacheLease()).thenReturn(() -> { });
        locator = mockStatic(APILocator.class);
        locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(binaries);
        // No content rows reference binaries, so only the local folder walk exports files.
        queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                (query, context) -> when(query.loadObjectResults()).thenReturn(List.of()));
        zips = mockStatic(ZipUtil.class, CALLS_REAL_METHODS);
        zips.when(() -> ZipUtil.addZipEntry(any(), eq("assets/0/1/bb/two.txt"), any(), anyBoolean()))
                .thenThrow(new IOException("read failed"));
    }

    @AfterEach
    void close() {
        zips.close();
        queries.close();
        locator.close();
        paths.close();
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    @Test
    void failedExportWithS3StorageIsNotAReadableArchive() throws Exception {
        Config.setProperty(AssetStorageFeature.FLAG, true);
        final var output = new ByteArrayOutputStream();

        assertThrows(DotRuntimeException.class, () -> new ExportStarterUtil().streamCompressedAssets(output, true, -1));

        assertTrue(output.size() > 0, "The first asset was already streamed to the client");
        final Path download = Files.write(downloads.resolve("failed.zip"), output.toByteArray());
        assertThrows(ZipException.class, () -> new ZipFile(download.toFile()).close(),
                "A partial export must not look like a complete backup");
    }

    @Test
    void flagOffExportStillLogsTheFailedFileAndFinishesTheArchive() throws Exception {
        Config.setProperty(AssetStorageFeature.FLAG, false);
        final var output = new ByteArrayOutputStream();

        new ExportStarterUtil().streamCompressedAssets(output, true, -1);

        final Path download = Files.write(downloads.resolve("partial.zip"), output.toByteArray());
        try (var archive = new ZipFile(download.toFile())) {
            assertEquals(List.of("assets/0/0/aa/one.txt"),
                    archive.stream().map(entry -> entry.getName()).toList());
        }
        locator.verifyNoInteractions();
    }
}
