package com.dotmarketing.image.vips;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.util.Config;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/** Reproduces the 500px resize of the orientation-6 iPhone JPEG supplied for issue #38079. */
public class VipsResizePhotoIntegrationTest {

    private static File directory;

    /** Creates isolated output; missing libvips skips locally but fails when required by CI. */
    @BeforeClass
    public static void setup() throws Exception {
        final boolean available = VipsManager.isAvailable();
        assertTrue("native libvips is required when DOT_TEST_REQUIRE_LIBVIPS=true",
                available || !Config.getBooleanProperty("TEST_REQUIRE_LIBVIPS", false));
        Assume.assumeTrue("native libvips is not available on this host", available);
        directory = Files.createTempDirectory("vips-resize-photo").toFile();
    }

    /** Removes the generated rendition without changing the original fixture. */
    @AfterClass
    public static void cleanup() throws Exception {
        if (directory != null) {
            FileUtils.deleteDirectory(directory);
        }
    }

    /** Uses displayed 1512x2016 dimensions rather than the JPEG's stored 2016x1512 pixels. */
    @Test
    public void suppliedPhotoResizesTo500By666() throws Exception {
        final URL resource = VipsResizePhotoIntegrationTest.class.getResource("/images/vips/IMG_5277.JPG");
        assertNotNull("regression photograph must be available", resource);
        final File output = new File(directory, "500.png");
        new VipsResizeImageFilter().transform(new File(resource.toURI()), output,
                Map.of("resize_w", new String[]{"500"}));
        final BufferedImage resized = ImageIO.read(output);
        assertNotNull("resized PNG must decode", resized);
        assertEquals(500, resized.getWidth());
        assertEquals("proportional upright portrait height", 666, resized.getHeight());
    }
}
