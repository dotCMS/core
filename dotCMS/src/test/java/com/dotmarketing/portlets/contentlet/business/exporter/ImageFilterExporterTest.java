package com.dotmarketing.portlets.contentlet.business.exporter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.dotmarketing.image.vips.VipsManager;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter.BinaryContentExporterData;
import com.dotmarketing.util.Config;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;

/**
 * Unit tests for the ImageFilterExporter class.
 */
class ImageFilterExporterTest {

    private ImageFilterExporter exporter;

    /**
     * Set up the ImageFilterExporter instance before each test.
     */
    @BeforeEach
    public void setUp() {
        exporter = new ImageFilterExporter();
    }

    /**
     * Given a valid SVG image file and a set of parameters,
     * When exporting the content of the image,
     * Then the method should return a result without modifying the image.
     *
     * @throws Exception if an error occurs during export.
     */
    @Test
    void testExportContentWithSvgImage() throws Exception {
        // Given
        final URL url = getClass().getResource("/images/test.svg");
        File inputFile = new File(Objects.requireNonNull(url).getFile());
        Map<String, String[]> parameters = getSampleParameters();

        // When
        BinaryContentExporterData result = exporter.exportContent(inputFile, parameters);

        // Then
        assertNotNull(result);
        assertTrue(FileUtils.contentEquals(inputFile, result.getDataFile())); // The file should not be transformed
    }

    /**
     * Given a valid EPS image file and a set of parameters,
     * When exporting the content of the image,
     * Then the method should return a result without modifying the image.
     *
     * @throws Exception if an error occurs during export.
     */
    @Test
    void testExportContentWithEpsImage() throws Exception {
        // Given
        final URL url = getClass().getResource("/images/test.eps");
        final File inputFile = new File(Objects.requireNonNull(url).getFile());
        Map<String, String[]> parameters = getSampleParameters();

        // When
        final BinaryContentExporterData result = exporter.exportContent(inputFile, parameters);

        // Then
        assertNotNull(result);
        assertTrue(FileUtils.contentEquals(inputFile, result.getDataFile())); // The file should not be transformed
    }

    /**
     * Given a valid non-vector image file (JPG) and a set of parameters,
     * When exporting the content of the image,
     * Then the method should return a result with the image transformed.
     *
     * @throws Exception if an error occurs during export.
     */
    @Test
    void testExportContentWithNonVectorImage() throws Exception {
        // Given
        final URL url = getClass().getResource("/images/test.jpg");
        final File inputFile = new File(Objects.requireNonNull(url).getFile());
        Map<String, String[]> parameters = getSampleParameters();

        // When
        final BinaryContentExporterData result = exporter.exportContent(inputFile, parameters);

        // Then
        assertNotNull(result);
        assertFalse(FileUtils.contentEquals(inputFile, result.getDataFile())); // The file should be transformed
    }

    /**
     * Helper method to provide a sample parameters map for use in the exportContent tests.
     *
     * @return a Map containing sample parameters.
     */
    final Map<String, String[]> getSampleParameters() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("contentAsset", new String[]{"image"});
        parameters.put("r", new String[]{"1728068872149"});
        parameters.put("e5150266-7e28-445b-b5dd-193dc38922c7", new String[]{"asset"});
        parameters.put("byInode", new String[]{"true"});
        parameters.put("resize_w", new String[]{"500"});
        parameters.put("quality_q", new String[]{"50"});
        parameters.put("webp_q", new String[]{"50"});
        parameters.put("fieldVarName", new String[]{"asset"});
        parameters.put("assetInodeOrIdentifier", new String[]{"e5150266-7e28-445b-b5dd-193dc38922c7"});
        return parameters;
    }

    /**
     * Given a cached dotGenerated PNG intermediate that was cut short,
     * When the chain that read it fails,
     * Then the intermediate is discarded so the next request regenerates it (issue #38263).
     */
    @Test
    void discardBrokenIntermediate_deletesTruncatedGeneratedPng(@TempDir final Path dir) throws Exception {
        final File intermediate = dir.resolve("dotGenerated_929f6225-7c89-4cdf-9867-0bfb0c57d7b5_resize_abc.png").toFile();
        final byte[] png = png();
        Files.write(intermediate.toPath(), Arrays.copyOf(png, png.length / 4));

        ImageFilterExporter.discardBrokenIntermediate(intermediate, dir.toFile());

        assertFalse(intermediate.exists(), "a damaged intermediate must be removed so it is regenerated");
    }

    /**
     * Given a damaged file named like an intermediate but outside the rendition cache (an upload
     * whose name starts with dotGenerated_, passed straight to the exporter),
     * When the chain that read it fails,
     * Then it is not deleted: only files inside the local rendition cache are ever discarded.
     */
    @Test
    void discardBrokenIntermediate_neverDeletesFilesOutsideTheRenditionCache(@TempDir final Path dir) throws Exception {
        final File cache = Files.createDirectory(dir.resolve("dotGenerated")).toFile();
        final File upload = dir.resolve("dotGenerated_upload.png").toFile();
        final byte[] png = png();
        Files.write(upload.toPath(), Arrays.copyOf(png, png.length / 4));

        ImageFilterExporter.discardBrokenIntermediate(upload, cache);

        assertTrue(upload.exists(), "a file outside the rendition cache must never be deleted");
    }

    /**
     * Given a healthy dotGenerated intermediate or the asset's own (truncated) binary,
     * When a chain fails,
     * Then neither is deleted: only damaged generated intermediates are discarded.
     */
    @Test
    void discardBrokenIntermediate_keepsHealthyIntermediatesAndSourceBinaries(@TempDir final Path dir) throws Exception {
        final byte[] png = png();
        final File healthy = dir.resolve("dotGenerated_929f6225-7c89-4cdf-9867-0bfb0c57d7b5_resize_ok.png").toFile();
        Files.write(healthy.toPath(), png);
        final File source = dir.resolve("hero.png").toFile();
        Files.write(source.toPath(), Arrays.copyOf(png, png.length / 4));

        ImageFilterExporter.discardBrokenIntermediate(healthy, dir.toFile());
        ImageFilterExporter.discardBrokenIntermediate(source, dir.toFile());

        assertTrue(healthy.exists(), "a healthy intermediate must be kept");
        assertTrue(source.exists(), "the asset's own binary must never be deleted");
    }

    /**
     * Given a cached animated GIF intermediate whose last frame is cut off (its first frames decode),
     * stored under a .gif name and under a .png name (the legacy resize can write GIF bytes there),
     * When the chain that read it fails on the legacy engine,
     * Then it is discarded: the legacy GIF resize needs every frame, so keeping it fails every request.
     */
    @Test
    void discardBrokenIntermediate_deletesGifWhoseLastFrameIsCutOff(@TempDir final Path dir) throws Exception {
        final byte[] gif = animatedGif(3);
        final byte[] cut = Arrays.copyOf(gif, lastImageDescriptor(gif) + 200);
        for (final String ext : new String[]{"gif", "png"}) {
            final File intermediate = dir.resolve("dotGenerated_929f6225-7c89-4cdf-9867-0bfb0c57d7b5_resize_cut." + ext).toFile();
            Files.write(intermediate.toPath(), cut);

            onLegacyEngine(() -> ImageFilterExporter.discardBrokenIntermediate(intermediate, dir.toFile()));

            assertFalse(intermediate.exists(), "a GIF with a cut-off frame must be removed (." + ext + ")");
        }
    }

    /**
     * Given a cached animated GIF intermediate that decodes fully,
     * When a chain that read it fails for another reason,
     * Then it is kept.
     */
    @Test
    void discardBrokenIntermediate_keepsHealthyAnimatedGif(@TempDir final Path dir) throws Exception {
        final File intermediate = dir.resolve("dotGenerated_929f6225-7c89-4cdf-9867-0bfb0c57d7b5_resize_ok.gif").toFile();
        Files.write(intermediate.toPath(), animatedGif(3));

        onLegacyEngine(() -> ImageFilterExporter.discardBrokenIntermediate(intermediate, dir.toFile()));

        assertTrue(intermediate.exists(), "a healthy animated GIF must be kept");
    }

    /**
     * Given a cached WebP intermediate that was cut short,
     * When the chain that read it fails on the legacy engine,
     * Then it is discarded so the next request regenerates it.
     */
    @Test
    void discardBrokenIntermediate_deletesTruncatedWebp(@TempDir final Path dir) throws Exception {
        // as in production, the image API deregisters the lenient javavp8 WebP reader on startup
        com.dotmarketing.image.filter.ImageFilterAPI.apiInstance.apply();
        final byte[] webp = Files.readAllBytes(Path.of(getClass().getResource("/images/test.webp").toURI()));
        final File intermediate = dir.resolve("dotGenerated_929f6225-7c89-4cdf-9867-0bfb0c57d7b5_webp_abc.webp").toFile();
        Files.write(intermediate.toPath(), Arrays.copyOf(webp, webp.length / 3));

        onLegacyEngine(() -> ImageFilterExporter.discardBrokenIntermediate(intermediate, dir.toFile()));

        assertFalse(intermediate.exists(), "a damaged WebP intermediate must be removed so it is regenerated");
    }

    /**
     * Runs a check with libvips turned off, so the JVM decoders do it whatever the host has installed.
     *
     * @param check the check to run
     */
    private static void onLegacyEngine(final Runnable check) {
        final String previous = Config.getStringProperty(VipsManager.USE_LIBVIPS, null);
        Config.setProperty(VipsManager.USE_LIBVIPS, false);
        try {
            check.run();
        } finally {
            Config.setProperty(VipsManager.USE_LIBVIPS, previous);
        }
    }

    /**
     * Writes an animated GIF whose frames are noise, so each frame holds enough data to be cut inside.
     *
     * @param frames the number of frames
     * @return the GIF bytes
     * @throws Exception if the GIF cannot be written
     */
    private static byte[] animatedGif(final int frames) throws Exception {
        final javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        final java.util.Random random = new java.util.Random(1);
        try (javax.imageio.stream.ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            for (int i = 0; i < frames; i++) {
                final BufferedImage frame = new BufferedImage(60, 40, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < frame.getHeight(); y++) {
                    for (int x = 0; x < frame.getWidth(); x++) {
                        frame.setRGB(x, y, random.nextInt());
                    }
                }
                writer.writeToSequence(new javax.imageio.IIOImage(frame, null, null), null);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    /**
     * Finds where the last frame starts: its image descriptor (0x2C) right after a block terminator.
     *
     * @param gif the GIF bytes
     * @return the offset of the last image descriptor
     */
    private static int lastImageDescriptor(final byte[] gif) {
        for (int i = gif.length - 11; i > 0; i--) {
            if (gif[i] == 0x2C && gif[i - 1] == 0x00) {
                return i;
            }
        }
        throw new IllegalStateException("no image descriptor in the GIF fixture");
    }

    private static byte[] png() throws Exception {
        final BufferedImage image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, (x * 7919 + y * 104729) & 0xFFFFFF);
            }
        }
        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
