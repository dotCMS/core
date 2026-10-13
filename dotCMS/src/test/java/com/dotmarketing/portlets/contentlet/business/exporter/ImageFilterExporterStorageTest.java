package com.dotmarketing.portlets.contentlet.business.exporter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotcms.storage.binary.BinaryAssetStorageAPIImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.image.ImageEngine;
import com.dotmarketing.image.filter.ImageFilter;
import com.dotmarketing.image.filter.ImageFilterAPI;
import com.dotmarketing.image.filter.JpegImageFilter;
import com.dotmarketing.image.filter.ResizeImageFilter;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks how renditions use S3 asset storage: which paths are looked up, when the remote store is
 * contacted, and what is served when it is unavailable. The storage mock models the real chain:
 * a file already on local disk is returned without contacting S3, and anything else is a remote
 * lookup that fails while {@link #outage} is set.
 */
class ImageFilterExporterStorageTest {

    @TempDir Path root;
    private String previousFlag;
    private final AtomicBoolean outage = new AtomicBoolean();

    @BeforeEach
    void enableFlag() {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
    }

    @AfterEach
    void restoreFlag() {
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    @Test
    void jpegPredictionIsTheWrittenFileOnlyWhenEnabled() throws Exception {
        try (var paths = mockStatic(ConfigUtils.class); var locator = mockStatic(APILocator.class)) {
            paths.when(ConfigUtils::getDotGeneratedPath).thenReturn(generatedRoot().toString());
            final File original = image("Original.png", 80, 60);
            final Map<String, String[]> parameters = parameters("jpeg");

            final File predicted = new JpegImageFilter().getResultsFile(original, parameters);
            final File written = new JpegImageFilter().runFilter(original, parameters);
            assertEquals(written, predicted, "The prediction must be the file the filter writes");
            assertTrue(written.getName().endsWith(".jpg"));
            assertTrue(written.isFile());

            // With the flag off the inherited prediction, and so every cache path, is unchanged.
            Config.setProperty(AssetStorageFeature.FLAG, false);
            final File legacy = new JpegImageFilter().getResultsFile(original, parameters);
            assertEquals(generatedRoot().resolve("a/b").toFile().getCanonicalFile(), legacy.getParentFile());
            assertTrue(legacy.getName().startsWith("dotGenerated_jpeg_"));
            assertTrue(legacy.getName().endsWith(".png"));
        }
    }

    @Test
    void warmJpegRenditionIsServedDuringAnOutageWithoutRemoteCalls() throws Exception {
        try (var paths = mockStatic(ConfigUtils.class); var locator = mockStatic(APILocator.class);
             var engine = mockStatic(ImageEngine.class)) {
            final StoragePersistenceAPI storage = storage(paths, locator);
            chain(engine, "jpeg", JpegImageFilter.class);
            final File original = image("Original.png", 80, 60);
            final var exporter = new ImageFilterExporter();

            final File cold = exporter.exportContent(original, parameters("jpeg")).getDataFile();
            assertTrue(cold.getName().endsWith(".jpg"));
            outage.set(true);
            final File warm = exporter.exportContent(original, parameters("jpeg")).getDataFile();

            // The same file, whichever spelling of the temporary directory each path uses.
            assertEquals(cold.getCanonicalFile(), warm.getCanonicalFile());
            verify(storage, times(1)).pushFile(anyString(), anyString(), any(), any());
            verify(storage, times(1)).hasDurableCopy(anyString(), anyString(), any());
        }
    }

    @Test
    void noOpFiltersStopContactingStorageAfterTheFirstRequest() throws Exception {
        try (var paths = mockStatic(ConfigUtils.class); var locator = mockStatic(APILocator.class);
             var engine = mockStatic(ImageEngine.class)) {
            final StoragePersistenceAPI storage = storage(paths, locator);
            // A maximum width larger than the image leaves the image unchanged.
            final File small = image("Small.png", 80, 60);
            chain(engine, "resize", ResizeImageFilter.class);
            final var exporter = new ImageFilterExporter();
            assertEquals(small, exporter.exportContent(small, resize("resize")).getDataFile());
            outage.set(true);
            assertEquals(small, exporter.exportContent(small, resize("resize")).getDataFile());

            // The same no-op ahead of a real conversion: only the conversion's output is stored.
            outage.set(false);
            final File other = image("Other.png", 80, 60);
            final var filters = new LinkedHashMap<String, Class<? extends ImageFilter>>();
            filters.put("resize", ResizeImageFilter.class);
            filters.put("jpeg", JpegImageFilter.class);
            chain(engine, filters);
            final File converted = exporter.exportContent(other, resize("resize", "jpeg")).getDataFile();
            assertTrue(converted.getName().endsWith(".jpg"));
            outage.set(true);
            assertEquals(converted.getCanonicalFile(),
                    exporter.exportContent(other, resize("resize", "jpeg")).getDataFile().getCanonicalFile());

            verify(storage, times(1)).pushFile(anyString(), anyString(), any(), any());
        }
    }

    @Test
    void failedRenditionUploadStillServesTheLocalOutput() throws Exception {
        try (var paths = mockStatic(ConfigUtils.class); var locator = mockStatic(APILocator.class);
             var engine = mockStatic(ImageEngine.class)) {
            final StoragePersistenceAPI storage = storage(paths, locator);
            when(storage.pushFile(anyString(), anyString(), any(), any()))
                    .thenThrow(new DotRuntimeException("S3 write unavailable"));
            chain(engine, "resize", ResizeImageFilter.class);
            final File original = image("Original.png", 80, 60);
            final Map<String, String[]> parameters = parameters("resize");
            parameters.put("resize_w", new String[]{"32"});

            final File result = new ImageFilterExporter().exportContent(original, parameters).getDataFile();

            assertNotEquals(original, result);
            assertTrue(result.isFile(), "The produced rendition is served from local disk");
            verify(storage).pushFile(anyString(), anyString(), any(), any());
        }
    }

    private Path generatedRoot() {
        return root.resolve("generated");
    }

    private File image(final String name, final int width, final int height) throws Exception {
        final File file = root.resolve(name).toFile();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "PNG", file);
        return file;
    }

    private static Map<String, String[]> parameters(final String... filters) {
        final Map<String, String[]> parameters = new HashMap<>();
        parameters.put("filter", filters);
        parameters.put("fieldVarName", new String[]{"HeroImage"});
        parameters.put("assetInodeOrIdentifier", new String[]{"abc123"});
        return parameters;
    }

    private static Map<String, String[]> resize(final String... filters) {
        final Map<String, String[]> parameters = parameters(filters);
        parameters.put("resize_maxw", new String[]{"200"});
        return parameters;
    }

    private StoragePersistenceAPI storage(final org.mockito.MockedStatic<ConfigUtils> paths,
            final org.mockito.MockedStatic<APILocator> locator) throws DotDataException {
        paths.when(ConfigUtils::getDotGeneratedPath).thenReturn(generatedRoot().toString());
        final StoragePersistenceAPI storage = mock(StoragePersistenceAPI.class);
        when(storage.pullFile(anyString(), anyString())).thenAnswer(call -> {
            final File local = generatedRoot().resolve((String) call.getArgument(1)).toFile();
            if (local.isFile()) {
                return local;
            }
            if (outage.get()) {
                throw new DotDataException("S3 unavailable");
            }
            return null;
        });
        locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(new BinaryAssetStorageAPIImpl(storage));
        return storage;
    }

    private static void chain(final org.mockito.MockedStatic<ImageEngine> engine, final String name,
            final Class<? extends ImageFilter> filter) {
        final var filters = new LinkedHashMap<String, Class<? extends ImageFilter>>();
        filters.put(name, filter);
        chain(engine, filters);
    }

    private static void chain(final org.mockito.MockedStatic<ImageEngine> engine,
            final Map<String, Class<? extends ImageFilter>> filters) {
        final ImageFilterAPI api = mock(ImageFilterAPI.class);
        when(api.resolveFilters(any())).thenReturn(filters);
        engine.when(ImageEngine::resolve).thenReturn(api);
    }
}
