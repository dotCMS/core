package com.dotmarketing.image.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.image.vips.VipsManager;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter.BinaryContentExporterData;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.portlets.contentlet.business.exporter.ImageFilterExporter;
import com.dotmarketing.util.Config;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Issue #38263: on the legacy (pure-JVM) image engine, a truncated source JPEG or PNG must never
 * produce a cached rendition, and once the source is healthy again the same request must render.
 */
public class LegacyTruncatedImageIntegrationTest {

    private static final double TRUNCATED_FRACTION = 0.4;

    private static boolean originalUseLibvips;
    private static File workDir;
    private static final List<File> generatedRenditions = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        originalUseLibvips = Config.getBooleanProperty(VipsManager.USE_LIBVIPS, true);
        Config.setProperty(VipsManager.USE_LIBVIPS, false);
        workDir = Files.createTempDirectory("legacy-truncated-image-").toFile();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        Config.setProperty(VipsManager.USE_LIBVIPS, originalUseLibvips);
        for (final File rendition : generatedRenditions) {
            deleteRenditionAndTemps(rendition);
        }
        if (workDir != null) {
            FileUtils.deleteDirectory(workDir);
        }
    }

    /**
     * Given a truncated source JPEG and a jpeg filter request on the legacy engine, when exported,
     * then the export fails and nothing is cached; after the source is restored the same request
     * renders a valid image.
     */
    @Test
    public void jpegFilter_truncatedJpeg_failsWithoutCaching_thenRendersOnceHealthy() throws Exception {
        final byte[] healthy = healthyImage("jpg");
        final File source = sourceFile("hero.jpg");
        Files.write(source.toPath(), truncate(healthy));

        final Map<String, String[]> params = jpegParams();
        try {
            new ImageFilterExporter().exportContent(source, params);
            fail("a truncated JPEG must not be exported");
        } catch (BinaryContentExporterException e) {
            // expected: the decode failure surfaces as an error instead of an HTTP 200 grey image
        }
        final File rendition = new JpegImageFilter().getResultsFile(source, params, "jpg");
        generatedRenditions.add(rendition);
        assertNothingCached(rendition);

        Files.write(source.toPath(), healthy);
        final BinaryContentExporterData data = new ImageFilterExporter().exportContent(source, jpegParams());
        // the repaired source has a new size and modification time, so it renders under a new name
        generatedRenditions.add(data.getDataFile());

        assertRenders(data.getDataFile());
        assertNothingCached(rendition);
    }

    /**
     * Given a truncated source JPEG and a crop request on the legacy engine, when exported, then the
     * export fails and no rendition is cached; after the source is restored the same request renders a valid crop.
     */
    @Test
    public void cropFilter_truncatedJpeg_failsWithoutCaching_thenRendersOnceHealthy() throws Exception {
        assertCropRecoversFromTruncatedSource("jpg");
    }

    /**
     * Given a truncated source PNG and a crop request on the legacy engine, when exported, then the
     * export fails and no rendition is cached; after the source is restored the same request renders a valid crop.
     */
    @Test
    public void cropFilter_truncatedPng_failsWithoutCaching_thenRendersOnceHealthy() throws Exception {
        assertCropRecoversFromTruncatedSource("png");
    }

    private void assertCropRecoversFromTruncatedSource(final String ext) throws Exception {
        final byte[] healthy = healthyImage(ext);
        final File source = sourceFile("hero." + ext);
        Files.write(source.toPath(), truncate(healthy));

        final Map<String, String[]> params = cropParams();
        try {
            new ImageFilterExporter().exportContent(source, params);
            fail("a truncated " + ext + " must not be cropped");
        } catch (BinaryContentExporterException e) {
            // expected: the crop fails instead of returning a never-written rendition
        }
        // the exporter added its resolved "filter" keys to params, which name the rendition
        final File rendition = new CropImageFilter().getResultsFile(source, params);
        generatedRenditions.add(rendition);
        assertNothingCached(rendition);

        Files.write(source.toPath(), healthy);
        final Map<String, String[]> healthyParams = cropParams();
        final BinaryContentExporterData data = new ImageFilterExporter().exportContent(source, healthyParams);

        // renditions are keyed on the source's size and modification time, so the repaired source gets
        // a fresh rendition rather than anything left from the truncated version
        final File healthyRendition = new CropImageFilter().getResultsFile(source, healthyParams);
        generatedRenditions.add(healthyRendition);
        assertEquals(healthyRendition.getCanonicalPath(), data.getDataFile().getCanonicalPath());
        assertRenders(data.getDataFile());
        assertNothingCached(rendition);
    }

    private static Map<String, String[]> jpegParams() {
        final Map<String, String[]> params = new HashMap<>();
        params.put("jpeg_q", new String[]{"80"});
        return params;
    }

    /**
     * Non-zero x/y so the crop does not use a focal point. {@code fieldVarName} is always sent by
     * BinaryExporterServlet (default "fileAsset"); the crop cache key reads it for the focal-point lookup.
     */
    private static Map<String, String[]> cropParams() {
        final Map<String, String[]> params = new HashMap<>();
        params.put("crop_x", new String[]{"1"});
        params.put("crop_y", new String[]{"1"});
        params.put("crop_w", new String[]{"200"});
        params.put("crop_h", new String[]{"100"});
        params.put("fieldVarName", new String[]{"fileAsset"});
        return params;
    }

    /** Source under a UUID folder: the legacy filters key the dotGenerated path off that inode. */
    private static File sourceFile(final String name) {
        final File dir = new File(workDir, UUID.randomUUID().toString());
        assertTrue(dir.mkdirs());
        return new File(dir, name);
    }

    /** Random noise, so a truncated copy really is missing pixel data. */
    private static byte[] healthyImage(final String ext) throws Exception {
        final BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        final Random random = new Random(38263);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "jpg".equals(ext) ? "jpeg" : ext, out));
        return out.toByteArray();
    }

    private static byte[] truncate(final byte[] bytes) {
        return Arrays.copyOf(bytes, (int) (bytes.length * TRUNCATED_FRACTION));
    }

    private static void assertNothingCached(final File rendition) {
        assertFalse("no rendition may be cached for a truncated source: " + rendition, rendition.exists());
        final File[] temps = rendition.getParentFile().listFiles(
                (dir, name) -> name.startsWith(rendition.getName() + "_"));
        assertEquals("no temp rendition may be left behind", 0, temps == null ? 0 : temps.length);
    }

    private static void assertRenders(final File rendition) throws Exception {
        assertTrue("healthy source must render: " + rendition, rendition.exists());
        final BufferedImage image = StrictImageReader.read(rendition);
        assertNotNull(image);
        assertTrue(image.getWidth() > 0 && image.getHeight() > 0);
    }

    private static void deleteRenditionAndTemps(final File rendition) {
        final File parent = rendition.getParentFile();
        final File[] files = parent == null ? null
                : parent.listFiles((dir, name) -> name.startsWith(rendition.getName()));
        if (files != null) {
            for (final File file : files) {
                file.delete();
            }
        }
    }
}
