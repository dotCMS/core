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
}
