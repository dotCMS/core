package com.dotmarketing.image.vips;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.portlets.contentlet.business.exporter.ImageFilterExporter;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A source image truncated on disk must never produce a cached black/grey rendition through the
 * libvips engine: the read fails loudly ({@code fail_on=error}), the failure goes through the
 * {@code IMAGE_API_LIBVIPS_FALLBACK} branch, nothing is left under {@code dotGenerated}, and the
 * same request renders correctly once the source is healthy again.
 */
@RunWith(DataProviderRunner.class)
public class VipsTruncatedImageIntegrationTest {

    private static final String FALLBACK_PROPERTY = "IMAGE_API_LIBVIPS_FALLBACK";
    private static final int SOURCE_WIDTH = 800;
    private static final int SOURCE_HEIGHT = 600;
    private static final int RESIZE_WIDTH = 200;

    private static File sourceRoot;
    private static String previousUseLibvips;
    private static String previousFallback;
    private static final Set<File> renditionsCreated = new HashSet<>();

    /** Truncated JPEG and PNG, each with the legacy fallback on and off. */
    @DataProvider(format = "%m[%p[0], fallback=%p[1]]")
    public static Object[][] cases() {
        return new Object[][] {
                {"jpg", false},
                {"jpg", true},
                {"png", false},
                {"png", true},
        };
    }

    /** Starts dotCMS, requires libvips (skip locally, fail in CI) and forces the libvips engine on. */
    @BeforeClass
    public static void setup() throws Exception {
        IntegrationTestInitService.getInstance().init();
        // capture before any skip: @AfterClass still runs when the class is skipped and restores these
        previousUseLibvips = Config.getStringProperty(VipsManager.USE_LIBVIPS, null);
        previousFallback = Config.getStringProperty(FALLBACK_PROPERTY, null);
        final boolean available = VipsManager.isAvailable();
        assertTrue("native libvips is required when DOT_TEST_REQUIRE_LIBVIPS=true",
                available || !Config.getBooleanProperty("TEST_REQUIRE_LIBVIPS", false));
        Assume.assumeTrue("native libvips is not available on this host", available);

        Config.setProperty(VipsManager.USE_LIBVIPS, true);
        sourceRoot = Files.createTempDirectory("vips-truncated-it").toFile();
    }

    /** Restores the engine flags and removes every source and rendition this class created. */
    @AfterClass
    public static void cleanup() throws Exception {
        Config.setProperty(VipsManager.USE_LIBVIPS, previousUseLibvips);
        Config.setProperty(FALLBACK_PROPERTY, previousFallback);
        for (final File rendition : renditionsCreated) {
            FileUtils.deleteQuietly(rendition);
        }
        if (sourceRoot != null) {
            FileUtils.deleteDirectory(sourceRoot);
        }
    }

    /**
     * Exports a resize of a source truncated to 35% of its bytes, then of the same source repaired.
     *
     * @param format source format, jpg or png
     * @param fallback value of {@code IMAGE_API_LIBVIPS_FALLBACK} for this case
     * @throws Exception if the fixture cannot be written
     */
    @Test
    @UseDataProvider("cases")
    public void truncatedSourceIsNeverCachedAndRendersOnceHealthy(final String format,
            final boolean fallback) throws Exception {
        Config.setProperty(FALLBACK_PROPERTY, fallback);

        // a unique inode in the source path so the rendition lands in its own dotGenerated bucket
        final String inode = UUID.randomUUID().toString();
        final File sourceDir = new File(sourceRoot, inode);
        assertTrue(sourceDir.mkdirs());
        final File source = new File(sourceDir, "photo." + format);
        assertTrue("fixture writer must be available", ImageIO.write(gradient(), format, source));
        final byte[] healthy = Files.readAllBytes(source.toPath());
        Files.write(source.toPath(), Arrays.copyOf(healthy, (int) (healthy.length * 0.35)));

        final File bucket = new File(ConfigUtils.getDotGeneratedPath() + File.separator
                + inode.charAt(0) + File.separator + inode.charAt(1));
        final Set<File> before = filesIn(bucket);

        final File truncatedResult = exportResize(source);
        if (truncatedResult == null) {
            final Set<File> leaked = newFiles(bucket, before);
            assertTrue("a failed render must cache nothing, found " + leaked, leaked.isEmpty());
        } else {
            // only reachable through the legacy engine; it must not serve a black/grey filled image
            assertTrue("libvips must not succeed on truncated input with fallback off", fallback);
            assertNotFilled(truncatedResult);
        }

        Files.write(source.toPath(), healthy);
        final File healthyResult = exportResize(source);
        assertNotNull("the repaired source must render", healthyResult);
        final BufferedImage rendered = ImageIO.read(healthyResult);
        assertNotNull("healthy rendition must decode", rendered);
        assertEquals(RESIZE_WIDTH, rendered.getWidth());
        assertEquals(RESIZE_WIDTH * SOURCE_HEIGHT / SOURCE_WIDTH, rendered.getHeight());
        assertNotFilled(healthyResult);
        newFiles(bucket, before);
    }

    /**
     * Runs a resize through the real exporter.
     *
     * @param source image to resize
     * @return the rendition, or null when the export failed
     */
    private File exportResize(final File source) {
        final Map<String, String[]> parameters = new HashMap<>();
        parameters.put("resize_w", new String[] {String.valueOf(RESIZE_WIDTH)});
        try {
            final File result = new ImageFilterExporter().exportContent(source, parameters).getDataFile();
            renditionsCreated.add(result);
            return result;
        } catch (BinaryContentExporterException e) {
            return null;
        }
    }

    /**
     * Fails when the bottom rows of a rendition are one flat colour, which is what a truncated read
     * filled with black/grey looks like. The gradient fixture never has a flat bottom row.
     *
     * @param rendition file to inspect
     * @throws Exception if the file cannot be read
     */
    private void assertNotFilled(final File rendition) throws Exception {
        final BufferedImage image = ImageIO.read(rendition);
        assertNotNull("rendition must decode: " + rendition, image);
        final int y = image.getHeight() - 1;
        final int first = image.getRGB(0, y);
        boolean varies = false;
        for (int x = 1; x < image.getWidth() && !varies; x++) {
            varies = Math.abs(((image.getRGB(x, y) >> 8) & 0xff) - ((first >> 8) & 0xff)) > 8;
        }
        assertTrue("bottom row is a flat fill, the truncated read was cached: " + rendition, varies);
    }

    /** A horizontal gradient: every row varies across its width, so a filled row is detectable. */
    private static BufferedImage gradient() {
        final BufferedImage image = new BufferedImage(SOURCE_WIDTH, SOURCE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < SOURCE_HEIGHT; y++) {
            for (int x = 0; x < SOURCE_WIDTH; x++) {
                final int value = 255 * x / SOURCE_WIDTH;
                image.setRGB(x, y, (value << 16) | (value << 8) | (255 - value));
            }
        }
        return image;
    }

    private static Set<File> filesIn(final File dir) {
        final File[] files = dir.listFiles();
        return files == null ? new HashSet<>() : new HashSet<>(Arrays.asList(files));
    }

    /**
     * @return files that appeared in {@code dir} since {@code before}; they are also scheduled for
     *         cleanup so a failing assertion never leaks them into other tests
     */
    private static Set<File> newFiles(final File dir, final Set<File> before) {
        final Set<File> after = filesIn(dir);
        after.removeAll(before);
        renditionsCreated.addAll(after);
        return after;
    }
}
