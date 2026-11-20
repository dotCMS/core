package com.dotmarketing.portlets.contentlet.business.exporter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter.BinaryContentExporterData;
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

        ImageFilterExporter.discardBrokenIntermediate(intermediate);

        assertFalse(intermediate.exists(), "a damaged intermediate must be removed so it is regenerated");
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

        ImageFilterExporter.discardBrokenIntermediate(healthy);
        ImageFilterExporter.discardBrokenIntermediate(source);

        assertTrue(healthy.exists(), "a healthy intermediate must be kept");
        assertTrue(source.exists(), "the asset's own binary must never be deleted");
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
