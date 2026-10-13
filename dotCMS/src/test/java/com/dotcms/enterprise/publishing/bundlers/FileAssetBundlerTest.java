package com.dotcms.enterprise.publishing.bundlers;

import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.binary.BinaryAssetStorageAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for how {@link FileAssetBundler} copies a File Asset's binary into a static bundle
 * when S3 asset storage is on.
 */
class FileAssetBundlerTest {

    private static final String INODE = "abc123";

    @TempDir Path root;
    private String previousFlag;
    private BinaryAssetStorageAPI binaries;
    private BinaryAssetStorageAPI.CacheLease lease;
    private PublisherConfig config;
    private BundleOutput output;
    private File copied;

    @BeforeEach
    void enable() {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
        binaries = mock(BinaryAssetStorageAPI.class);
        lease = mock(BinaryAssetStorageAPI.CacheLease.class);
        when(binaries.acquireCacheLease()).thenReturn(lease);
        config = mock(PublisherConfig.class);
        when(config.isStatic()).thenReturn(true);
        when(config.liveOnly()).thenReturn(false);
        output = new DirectoryBundleOutput(config, root.resolve("bundle").toFile());
        copied = root.resolve("bundle/live/demo.dotcms.com/1/images/logo.png").toFile();
    }

    @AfterEach
    void restore() {
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    /** A missing binary fails the bundle, as the failed copy does with the flag off, instead of skipping the file. */
    @Test
    void missingBinaryFailsTheBundle() throws Exception {
        when(binaries.getBinaryFile(INODE, FileAssetAPI.BINARY_FIELD)).thenReturn(null);
        try (var locator = mockStatic(APILocator.class)) {
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(binaries);
            final IOException failure = assertThrows(IOException.class, this::writeFileToDisk);
            assertTrue(failure.getMessage().contains(INODE));
        }
        assertFalse(copied.exists());
        verify(lease).close();
    }

    /** A present binary is copied into the bundle before the cache lease is released. */
    @Test
    void presentBinaryIsCopiedWhileTheLeaseIsHeld() throws Exception {
        final File source = Files.writeString(root.resolve("logo.png"), "pixels").toFile();
        when(binaries.getBinaryFile(INODE, FileAssetAPI.BINARY_FIELD)).thenReturn(source);
        final AtomicBoolean copiedBeforeRelease = new AtomicBoolean();
        doAnswer(call -> {
            copiedBeforeRelease.set(copied.exists());
            return null;
        }).when(lease).close();
        try (var locator = mockStatic(APILocator.class)) {
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(binaries);
            writeFileToDisk();
        }
        assertEquals("pixels", Files.readString(copied.toPath()));
        assertTrue(copiedBeforeRelease.get(), "The lease must be held until the copy is done");
    }

    private void writeFileToDisk() throws Exception {
        final FileAssetBundler bundler = new FileAssetBundler();
        final Field configField = FileAssetBundler.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(bundler, config);

        final Host host = mock(Host.class);
        when(host.getHostname()).thenReturn("demo.dotcms.com");
        final FileAsset asset = mock(FileAsset.class);
        when(asset.getInode()).thenReturn(INODE);
        when(asset.getURI()).thenReturn("/images/logo.png");
        when(asset.getUnderlyingFileName()).thenReturn("logo.png");
        final ContentletVersionInfo info = mock(ContentletVersionInfo.class);
        when(info.getLiveInode()).thenReturn(INODE);
        when(info.getVersionTs()).thenReturn(new Date());
        final FileAssetWrapper wrapper = new FileAssetWrapper();
        wrapper.setAsset(asset);
        wrapper.setInfo(info);

        final Method write = FileAssetBundler.class.getDeclaredMethod("writeFileToDisk",
                Host.class, String.class, BundleOutput.class, FileAssetWrapper.class);
        write.setAccessible(true);
        try {
            write.invoke(bundler, host, "1", output, wrapper);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }
}
