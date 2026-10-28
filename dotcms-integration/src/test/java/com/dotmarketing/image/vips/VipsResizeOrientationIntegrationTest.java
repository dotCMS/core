package com.dotmarketing.image.vips;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Verifies displayed geometry and upright pixels for all eight EXIF orientations. */
@RunWith(Parameterized.class)
public class VipsResizeOrientationIntegrationTest {

    private static final int RAW_WIDTH = 2016;
    private static final int RAW_HEIGHT = 1512;
    private static final int[] COLOURS = {0xd03020, 0x20c040, 0x2050d0, 0xd0b020};
    private static final int[] TOP_LEFT = {0, 1, 3, 2, 0, 2, 3, 1};
    private static final File[] INPUTS = new File[8];
    private static File directory;
    private final int orientation;
    private final String prefix;

    /** Covers Resize and its Scale alias with every standard orientation value. */
    @Parameters(name = "orientation={0}, filter={1}")
    public static Collection<Object[]> cases() {
        final Collection<Object[]> cases = new ArrayList<>();
        for (int orientation = 1; orientation <= 8; orientation++) {
            for (final String prefix : new String[]{"resize", "scale"}) {
                cases.add(new Object[]{orientation, prefix});
            }
        }
        return cases;
    }

    /** Selects the orientation and filter parameter prefix for one group of assertions. */
    public VipsResizeOrientationIntegrationTest(final int orientation, final String prefix) {
        this.orientation = orientation;
        this.prefix = prefix;
    }

    /** Creates synthetic landscape JPEGs with orientation metadata, never a user's photograph. */
    @BeforeClass
    public static void setup() throws Exception {
        assertTrue("native libvips is required", VipsManager.isAvailable());
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
        for (int index = 0; index < INPUTS.length; index++) {
            final int orientation = index + 1;
            final File input = new File(directory, "orientation-" + orientation + ".jpg");
            INPUTS[index] = input;
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
    public void widthOnlyPreservesDisplayedAspectRatio() throws Exception {
        assertRendition("width", Map.of(prefix + "_w", new String[]{"500"}),
                500, 500 * displayedHeight() / displayedWidth());
    }

    /** A height-only request derives width from the displayed aspect ratio. */
    @Test
    public void heightOnlyPreservesDisplayedAspectRatio() throws Exception {
        assertRendition("height", Map.of(prefix + "_h", new String[]{"500"}),
                500 * displayedWidth() / displayedHeight(), 500);
    }

    /** Even the no-resampling path must write upright pixels without a second orientation tag. */
    @Test
    public void unchangedSizeStillNormalisesOrientation() throws Exception {
        assertRendition("unchanged", Map.of(), displayedWidth(), displayedHeight());
    }

    /** Explicit width and height remain an intentional forced-size request. */
    @Test
    public void explicitDimensionsRemainForced() throws Exception {
        assertRendition("forced", Map.of(prefix + "_w", new String[]{"500"},
                prefix + "_h", new String[]{"400"}), 500, 400);
    }

    /** Returns the upright width; EXIF orientations 5–8 exchange the stored axes. */
    private int displayedWidth() {
        return orientation >= 5 ? RAW_HEIGHT : RAW_WIDTH;
    }

    /** Returns the upright height for the current orientation. */
    private int displayedHeight() {
        return orientation >= 5 ? RAW_WIDTH : RAW_HEIGHT;
    }

    /** Checks dimensions, an independently mapped corner colour and removal of orientation metadata. */
    private void assertRendition(final String name, final Map<String, String[]> parameters,
            final int width, final int height) throws Exception {
        final File output = new File(directory, orientation + "-" + prefix + "-" + name + ".png");
        final VipsImageFilter filter = "resize".equals(prefix)
                ? new VipsResizeImageFilter() : new VipsScaleImageFilter();
        filter.transform(INPUTS[orientation - 1], output, parameters);
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
