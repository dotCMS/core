package com.dotmarketing.image.vips;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import com.dotmarketing.util.Config;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Exercises the real Java resize filter and native library across the 4x shrink boundary. */
@RunWith(DataProviderRunner.class)
public class VipsResizeImageFilterIntegrationTest {

    private static File directory;

    /** Supplies JPEG, RGB/RGBA PNG and lossy WebP cases below, at and above 4x shrink. */
    @DataProvider(format = "%m[%p[0], alpha=%p[1], width=%p[2]]")
    public static Object[][] cases() {
        final Collection<Object[]> cases = new ArrayList<>();
        for (final String format : new String[]{"png", "jpg", "webp"}) {
            for (final boolean alpha : new boolean[]{false, true}) {
                if (alpha && "jpg".equals(format)) {
                    continue;
                }
                final int[] widths = "webp".equals(format)
                        ? new int[]{176, 175, 130} : new int[]{379, 378, 189};
                for (final int width : widths) {
                    cases.add(new Object[]{format, alpha, width});
                }
            }
        }
        return cases.toArray(new Object[0][]);
    }

    /** Creates isolated output; missing libvips skips locally but fails when required by CI. */
    @BeforeClass
    public static void setup() throws Exception {
        final boolean available = VipsManager.isAvailable();
        assertTrue("native libvips is required when DOT_TEST_REQUIRE_LIBVIPS=true",
                available || !Config.getBooleanProperty("TEST_REQUIRE_LIBVIPS", false));
        Assume.assumeTrue("native libvips is not available on this host", available);
        directory = Files.createTempDirectory("vips-resize-colours").toFile();
    }

    /** Removes all generated source files and renditions, including after assertion failures. */
    @AfterClass
    public static void cleanup() throws Exception {
        if (directory != null) {
            FileUtils.deleteDirectory(directory);
        }
    }

    /** Checks geometry, alpha and each colour channel against known Java-generated pixels. */
    @Test
    @UseDataProvider("cases")
    public void resizePreservesColoursAcrossShrinkBoundary(final String format, final boolean alpha,
            final int width) throws Exception {
        final BufferedImage original = fixture(format, alpha);
        final File input = source(original, format, alpha, width);
        final File output = new File(directory, format + "-" + alpha + "-" + width + "-resized.png");

        // Call transform directly: runFilter is allowed to fall back to the legacy engine.
        new VipsResizeImageFilter().transform(input, output,
                Map.of("resize_w", new String[]{Integer.toString(width)}));

        final BufferedImage resized = ImageIO.read(output);
        assertNotNull("resized PNG must decode", resized);
        assertEquals(width, resized.getWidth());
        assertEquals("height for source " + original.getWidth() + "x" + original.getHeight(),
                width * original.getHeight() / original.getWidth(), resized.getHeight());
        assertEquals(alpha, resized.getColorModel().hasAlpha());
        for (final double horizontal : new double[]{0.25, 0.75}) {
            for (final double vertical : new double[]{0.25, 0.75}) {
                final int x = (int) (resized.getWidth() * horizontal);
                final int y = (int) (resized.getHeight() * vertical);
                final int sourceX = (int) ((x + 0.5) * original.getWidth() / resized.getWidth());
                final int sourceY = (int) ((y + 0.5) * original.getHeight() / resized.getHeight());
                final int expected = original.getRGB(sourceX, sourceY);
                final int actual = resized.getRGB(x, y);
                for (int shift = 0; shift <= 24; shift += 8) {
                    assertEquals("channel " + shift + " at " + x + "," + y,
                            (expected >>> shift) & 255, (actual >>> shift) & 255, 6.0);
                }
            }
        }
    }

    /** Creates a navy logo region beside a spatially varying RGB gradient with optional alpha. */
    private BufferedImage fixture(final String format, final boolean alpha) {
        final int sourceWidth = "webp".equals(format) ? 701 : 1512;
        final int sourceHeight = "webp".equals(format) ? 700 : 1512;
        final BufferedImage image = new BufferedImage(sourceWidth, sourceHeight,
                alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < sourceWidth; x++) {
                final int red = x < sourceWidth / 2 ? 16 : 32 + 160 * x / sourceWidth;
                final int green = x < sourceWidth / 2 ? 70 : 24 + 160 * y / sourceHeight;
                final int blue = x < sourceWidth / 2 ? 129 : 224 - 128 * x / sourceWidth;
                image.setRGB(x, y, ((alpha ? 128 : 255) << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return image;
    }

    /** Encodes reference pixels without resizing; no WebP ImageIO decoder is used as an oracle. */
    private File source(final BufferedImage image, final String format, final boolean alpha,
            final int width) throws Exception {
        final File input = new File(directory, format + "-" + alpha + "-" + width + "." + format);
        if ("webp".equals(format)) {
            final File png = new File(directory, input.getName() + ".png");
            assertTrue("PNG fixture writer must be available", ImageIO.write(image, "png", png));
            VipsManager.run(arena -> VImage.newFromFile(arena, png.getAbsolutePath())
                    .writeToFile(input.getAbsolutePath(), VipsOption.Int("Q", 100)));
        } else {
            assertTrue("fixture writer must be available", ImageIO.write(image, format, input));
        }
        return input;
    }
}
