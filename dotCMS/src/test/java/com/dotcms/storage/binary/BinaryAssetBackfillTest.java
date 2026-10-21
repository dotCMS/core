package com.dotcms.storage.binary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.FileStorageAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/** Batch paging and the skip policy shared by the backfill job and starter import, without a database. */
class BinaryAssetBackfillTest {
    @TempDir
    Path assets;
    private final BinaryAssetStorageAPI binaries = mock(BinaryAssetStorageAPI.class);
    private final ContentletAPI contents = mock(ContentletAPI.class);
    private MockedStatic<APILocator> locator;
    private MockedStatic<ConfigUtils> paths;
    private MockedStatic<HibernateUtil> transactions;
    private MockedStatic<DbConnectionFactory> connections;
    private String previousFlag;

    @BeforeEach
    void configure() throws Exception {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
        locator = mockStatic(APILocator.class);
        paths = mockStatic(ConfigUtils.class);
        transactions = mockStatic(HibernateUtil.class);
        connections = mockStatic(DbConnectionFactory.class);
        paths.when(ConfigUtils::getAssetPath).thenReturn(assets.toString());
        final var json = mock(ContentletJsonAPI.class);
        // Legacy rows (no persisted JSON) are serialized from the found content: one Binary field per inode.
        when(json.toJson(any())).thenAnswer(call -> "{\"fields\":{\"asset\":{\"type\":\"Binary\",\"value\":\""
                + ((Contentlet) call.getArgument(0)).getInode() + ".txt\"}}}");
        final var metadata = mock(FileMetadataAPI.class);
        when(metadata.getFileName(any(), anyString())).thenReturn("/metadata.json");
        final var files = mock(FileStorageAPI.class);
        when(files.backfillMetadata(any())).thenReturn(true);
        when(binaries.acquireCacheLease()).thenReturn(() -> { });
        when(binaries.backfillBinary(anyString(), anyString(), any())).thenReturn(true);
        locator.when(APILocator::getContentletJsonAPI).thenReturn(json);
        locator.when(APILocator::getFileMetadataAPI).thenReturn(metadata);
        locator.when(APILocator::getFileStorageAPI).thenReturn(files);
        locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(binaries);
        locator.when(APILocator::getContentletAPI).thenReturn(contents);
    }

    @AfterEach
    void close() {
        connections.close();
        transactions.close();
        paths.close();
        locator.close();
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    @Test
    void missingOrUnreadableDataIsSkippedAndReportedWhileTheBatchContinues() throws Exception {
        found("bb");
        found("cc");
        when(binaries.openLocalFile(any())).thenThrow(new NoSuchFileException("absent from S3"));
        final File local = assets.resolve("c/c/cc/asset/cc.txt").toFile();
        Files.createDirectories(local.toPath().getParent());
        Files.writeString(local.toPath(), "local bytes");
        final AtomicInteger heartbeats = new AtomicInteger();

        try (var queries = rows("aa", "bb", "cc")) {
            final var result = BinaryAssetBackfill.runBatch("", 3, heartbeats::incrementAndGet);

            // "aa" has no content, "bb" has a binary in neither place, "cc" is copied.
            assertEquals(new BinaryAssetBackfill.Result("cc", 1, false, List.of("aa", "bb")), result);
            assertEquals("select inode from contentlet where inode > ? order by inode limit 3",
                    queries.constructed().get(0).getSQL(), "The page size must be applied by the database");
            assertEquals(3, heartbeats.get());
            verify(binaries).backfillBinary("cc", "asset", local);
            verify(binaries, never()).backfillBinary(eq("bb"), anyString(), any());
        }
    }

    @Test
    void storageErrorsStillFailTheBatchAndNameTheInode() throws Exception {
        found("bb");
        when(binaries.openLocalFile(any())).thenThrow(new DotDataException("S3 unavailable"));

        try (var queries = rows("bb")) {
            final var failure = assertThrows(DotDataException.class,
                    () -> BinaryAssetBackfill.runBatch("", 3, () -> { }));
            assertTrue(failure.getMessage().contains("bb"), failure.getMessage());
            assertEquals("S3 unavailable", failure.getCause().getMessage());
        }
    }

    /** Makes a legacy row resolve to content with the given inode. */
    private void found(final String inode) throws Exception {
        final Contentlet content = new Contentlet();
        content.setInode(inode);
        when(contents.find(eq(inode), any(), eq(false))).thenReturn(content);
    }

    /** The first query pages the given inodes; each later row lock returns a legacy row without JSON. */
    private MockedConstruction<DotConnect> rows(final String... inodes) {
        final List<Map<String, Object>> page = new ArrayList<>();
        for (final String inode : inodes) {
            page.add(Map.of("inode", inode));
        }
        return mockConstruction(DotConnect.class, withSettings().defaultAnswer(CALLS_REAL_METHODS),
                (query, context) -> doReturn(context.getCount() == 1 ? page
                        : List.of(Collections.singletonMap("contentlet_as_json", null)))
                        .when(query).loadObjectResults());
    }
}
