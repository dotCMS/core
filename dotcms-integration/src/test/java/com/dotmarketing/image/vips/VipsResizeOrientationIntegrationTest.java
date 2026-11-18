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
import java.awt.Color;
import java.awt.Graphics2D;
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

/** Verifies displayed geometry and upright pixels for all eight EXIF orientations. */
@RunWith(DataProviderRunner.class)
public class VipsResizeOrientationIntegrationTest {

    private static final int RAW_WIDTH = 2016;
    private static final int RAW_HEIGHT = 1512;
    private static final int[] COLOURS = {0xd03020, 0x20c040, 0x2050d0, 0xd0b020};
    private static final int[] TOP_LEFT = {0, 1, 3, 2, 0, 2, 3, 1};
    private static File directory;

    /** Covers all JPEG orientations plus orientation-6 PNG/WebP for Resize and its Scale alias. */
    @DataProvider(format = "%m[%p[0], orientation=%p[1], filter=%p[2]]")
    public static Object[][] cases() {
        final Collection<Object[]> cases = new ArrayList<>();
        for (int orientation = 1; orientation <= 8; orientation++) {
            for (final String prefix : new String[]{"resize", "scale"}) {
                cases.add(new Object[]{"jpg", orientation, prefix});
                if (orientation == 6) {
                    cases.add(new Object[]{"png", orientation, prefix});
                    cases.add(new Object[]{"webp", orientation, prefix});
                }
            }
        }
        return cases.toArray(new Object[0][]);
    }

    /** Creates oriented synthetic images; missing libvips skips locally but fails in strict CI. */
    @BeforeClass
    public static void setup() throws Exception {
        final boolean available = VipsManager.isAvailable();
        assertTrue("native libvips is required when DOT_TEST_REQUIRE_LIBVIPS=true",
                available || !Config.getBooleanProperty("TEST_REQUIRE_LIBVIPS", false));
        Assume.assumeTrue("native libvips is not available on this host", available);
        directory = Files.createTempDirectory("vips-resize-orientation").toFile();
        final BufferedImage image = new BufferedImage(RAW_WIDTH, RAW_HEIGHT, BufferedImage.TYPE_INT_RGB);
        final Graphics2D graphics = image.createGraphics();
        try {
            for (int row = 0; row < 2; row++) {
                for (int column = 0; column < 2; column++) {
                    graphics.setColor(new Color(COLOURS[row * 2 + column]));
                    graphics.fillRect(column * RAW_WIDTH / 2, row * RAW_HEIGHT / 2,
                            RAW_WIDTH / 2, RAW_HEIGHT / 2);
                }
            }
        } finally {
            graphics.dispose();
        }
        final File raw = new File(directory, "raw.jpg");
        assertTrue("JPEG fixture writer must be available", ImageIO.write(image, "jpg", raw));
        for (final Object[] testCase : cases()) {
            final String format = (String) testCase[0];
            final int orientation = (int) testCase[1];
            if (!"resize".equals(testCase[2])) {
                continue;
            }
            final File input = new File(directory, "orientation-" + orientation + "." + format);
            VipsManager.run(arena -> {
                VImage.newFromFile(arena, raw.getAbsolutePath()).set("orientation", orientation)
                        .writeToFile(input.getAbsolutePath(), VipsOption.Int("Q", 100));
                final VImage encoded = VImage.newFromFile(arena, input.getAbsolutePath());
                assertEquals(RAW_WIDTH, encoded.getWidth());
                assertEquals(RAW_HEIGHT, encoded.getHeight());
                assertEquals(orientation, VipsManager.metaInt(encoded, "orientation", 1));
            });
        }
    }

    /** Removes synthetic inputs and every generated rendition, including after failures. */
    @AfterClass
    public static void cleanup() throws Exception {
        if (directory != null) {
            FileUtils.deleteDirectory(directory);
        }
    }

    /** A width-only request derives height from the displayed, not stored, aspect ratio. */
    @Test
    @UseDataProvider("cases")
    public void widthOnlyPreservesDisplayedAspectRatio(final String format, final int orientation,
            final String prefix) throws Exception {
        assertRendition(format, orientation, prefix, "width",
                Map.of(prefix + "_w", new String[]{"500"}),
                500, 500 * displayedHeight(orientation) / displayedWidth(orientation));
    }

    /** A height-only request derives width from the displayed aspect ratio. */
    @Test
    @UseDataProvider("cases")
    public void heightOnlyPreservesDisplayedAspectRatio(final String format, final int orientation,
            final String prefix) throws Exception {
        assertRendition(format, orientation, prefix, "height",
                Map.of(prefix + "_h", new String[]{"500"}),
                500 * displayedWidth(orientation) / displayedHeight(orientation), 500);
    }

    /** Even the no-resampling path must write upright pixels without a second orientation tag. */
    @Test
    @UseDataProvider("cases")
    public void unchangedSizeStillNormalisesOrientation(final String format, final int orientation,
            final String prefix) throws Exception {
        assertRendition(format, orientation, prefix, "unchanged", Map.of(),
                displayedWidth(orientation), displayedHeight(orientation));
    }

    /** Explicit width and height remain an intentional forced-size request. */
    @Test
    @UseDataProvider("cases")
    public void explicitDimensionsRemainForced(final String format, final int orientation,
            final String prefix) throws Exception {
        assertRendition(format, orientation, prefix, "forced",
                Map.of(prefix + "_w", new String[]{"500"}, prefix + "_h", new String[]{"400"}),
                500, 400);
    }

    /** Returns the upright width; EXIF orientations 5–8 exchange the stored axes. */
    private int displayedWidth(final int orientation) {
        return orientation >= 5 ? RAW_HEIGHT : RAW_WIDTH;
    }

    /** Returns the upright height for the current orientation. */
    private int displayedHeight(final int orientation) {
        return orientation >= 5 ? RAW_WIDTH : RAW_HEIGHT;
    }

    /** Checks dimensions, an independently mapped corner colour and removal of orientation metadata. */
    private void assertRendition(final String format, final int orientation, final String prefix,
            final String name, final Map<String, String[]> parameters, final int width,
            final int height) throws Exception {
        final File input = new File(directory, "orientation-" + orientation + "." + format);
        final File output = new File(directory,
                format + "-" + orientation + "-" + prefix + "-" + name + ".png");
        final VipsImageFilter filter = "resize".equals(prefix)
                ? new VipsResizeImageFilter() : new VipsScaleImageFilter();
        filter.transform(input, output, parameters);
        final BufferedImage resized = ImageIO.read(output);
        assertNotNull("resized PNG must decode", resized);
        assertEquals("upright width", width, resized.getWidth());
        assertEquals("upright height", height, resized.getHeight());
        final int expected = COLOURS[TOP_LEFT[orientation - 1]];
        final int actual = resized.getRGB(width / 4, height / 4);
        for (int shift = 0; shift <= 16; shift += 8) {
            assertEquals("upright corner channel " + shift,
                    (expected >>> shift) & 255, (actual >>> shift) & 255, 6.0);
        }
        VipsManager.run(arena -> assertEquals("orientation must not be applied twice", 1,
                VipsManager.metaInt(VImage.newFromFile(arena, output.getAbsolutePath()), "orientation", 1)));
    }
}
