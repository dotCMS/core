package com.dotmarketing.image.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.WebKeys;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Regression tests for issue #38263: the legacy (AWT/ImageIO) image engine must fail on a truncated
 * source image instead of silently decoding it to a grey/black image that then gets cached.
 */
public class StrictImageReaderTest {

    private static final double TRUNCATED_FRACTION = 0.4;
    private static final String PDF_SOURCE = "test.pdf";

    private File workDir;

    @Before
    public void setUp() throws IOException {
        workDir = Files.createTempDirectory("strict-image-reader-test-").toFile();
    }

    @After
    public void tearDown() {
        deleteRecursively(workDir);
    }

    /**
     * Given a healthy JPEG and PNG, when read with StrictImageReader, then the image decodes with the
     * same dimensions as the default ImageIO reader (no behavior change for healthy images).
     */
    @Test
    public void read_healthyImages_decodesSameAsImageIO() throws Exception {
        for (final String ext : new String[]{"jpg", "png"}) {
            final File source = resource("test." + ext);
            final BufferedImage expected = javax.imageio.ImageIO.read(source);
            final BufferedImage actual = StrictImageReader.read(source);
            assertNotNull(actual);
            assertEquals(expected.getWidth(), actual.getWidth());
            assertEquals(expected.getHeight(), actual.getHeight());
        }
    }

    /**
     * Given a truncated PNG, when read, then an IOException is thrown (unchanged behavior).
     */
    @Test(expected = IOException.class)
    public void read_truncatedPng_throws() throws Exception {
        StrictImageReader.read(truncatedCopy("test.png"));
    }

    /**
     * Given a truncated JPEG, when read, then an IOException is thrown instead of returning a
     * partially decoded (grey/black) image.
     */
    @Test(expected = IOException.class)
    public void read_truncatedJpeg_throws() throws Exception {
        StrictImageReader.read(truncatedCopy("test.jpg"));
    }

    /**
     * Given a truncated JPEG source, when the legacy crop filter runs, then it fails instead of
     * returning a never-written result file, and no rendition is written.
     */
    @Test
    public void cropFilter_truncatedJpeg_failsAndWritesNoRendition() throws Exception {
        assertCropFailsWithoutRendition(truncatedCopy("test.jpg"));
    }

    /**
     * Given a truncated PNG source, when the legacy crop filter runs, then it fails and no rendition
     * is written.
     */
    @Test
    public void cropFilter_truncatedPng_failsAndWritesNoRendition() throws Exception {
        assertCropFailsWithoutRendition(truncatedCopy("test.png"));
    }

    private void assertCropFailsWithoutRendition(final File source) {
        final Map<String, String[]> params = cropParams(100, 50);
        final CropImageFilter filter = cropFilter(Optional.empty());
        final File expectedResult = filter.getResultsFile(source, params);
        try {
            filter.runFilter(source, params);
            fail("a truncated source must not be cropped");
        } catch (DotRuntimeException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
        assertFalse("a rendition must not be cached for a truncated source", expectedResult.exists());
        assertNoTempFilesLeft();
    }

    /**
     * Given a truncated JPEG source, when the legacy jpeg filter runs, then it fails and no rendition
     * is written.
     */
    @Test
    public void jpegFilter_truncatedJpeg_failsAndWritesNoRendition() throws Exception {
        final File source = truncatedCopy("test.jpg");
        final Map<String, String[]> params = new HashMap<>();
        params.put("filter", new String[]{"Jpeg"});
        params.put("jpeg_q", new String[]{"80"});
        final JpegImageFilter filter = new JpegImageFilter();
        final File expectedResult = filter.getResultsFile(source, params, "jpg");

        try {
            filter.runFilter(source, params);
            fail("a truncated JPEG must not be converted");
        } catch (DotRuntimeException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
        assertFalse(expectedResult.exists());
        assertNoTempFilesLeft();
    }

    /**
     * Given a healthy JPEG and a focal-point crop larger than the remaining area, when cropped, then
     * the shorter-than-requested result is still written (no dimension check).
     */
    @Test
    public void cropFilter_healthyJpegFocalPointShorterResult_isAccepted() throws Exception {
        final File source = healthyCopy("test.jpg");
        final BufferedImage original = StrictImageReader.read(source);
        final Map<String, String[]> params = cropParams(original.getWidth(), original.getHeight());
        final Point focalPoint = new Point(original.getWidth() / 2, (int) (original.getHeight() * 0.9));

        final File result = cropFilter(Optional.of(focalPoint)).runFilter(source, params);

        assertTrue(result.exists());
        final BufferedImage cropped = StrictImageReader.read(result);
        assertTrue(cropped.getHeight() < original.getHeight());
    }

    /**
     * Given a truncated JPEG, when the image API resizes or subsamples it, then it fails instead of
     * returning a partially decoded image; a healthy JPEG still resizes.
     */
    @Test
    public void imageApi_truncatedJpeg_resizeAndSubsampleFail() throws Exception {
        final ImageFilterApiImpl imageApi = new ImageFilterApiImpl();
        final File truncated = truncatedCopy("test.jpg");

        assertDecodeFails(() -> imageApi.resizeImage(truncated, 100, 50));
        assertDecodeFails(() -> imageApi.subsampleImage(truncated, 100, 50));
        assertEquals(100, imageApi.resizeImage(healthyCopy("test.jpg"), 100, 50).getWidth());
    }

    /**
     * Given a healthy JPEG with extraneous bytes before a marker (the reader warns "Corrupt JPEG
     * data: N extraneous bytes before marker" and recovers), when read through StrictImageReader and
     * the image API, then it still decodes: only truncation warnings are fatal.
     */
    @Test
    public void read_healthyJpegWithRecoverableWarning_decodes() throws Exception {
        final File source = jpegWithExtraneousBytes();
        final BufferedImage expected = javax.imageio.ImageIO.read(resource("test.jpg"));

        final BufferedImage actual = StrictImageReader.read(source);

        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        assertEquals(100, new ImageFilterApiImpl().resizeImage(source, 100, 50).getWidth());
    }

    /**
     * Given reader warning messages, when classified, then only truncation / short-read warnings are
     * fatal; recoverable ones (bogus APPn segments, extraneous bytes, unknown transforms) are not.
     */
    @Test
    public void isTruncationWarning_onlyShortReadWarningsAreFatal() {
        assertTrue(StrictImageReader.isTruncationWarning("Truncated File - Missing EOI marker"));
        assertTrue(StrictImageReader.isTruncationWarning("Corrupt JPEG data: premature end of data segment"));
        assertTrue(StrictImageReader.isTruncationWarning("Premature end of JPEG file"));
        assertTrue(StrictImageReader.isTruncationWarning("Unexpected end of stream"));
        assertTrue(StrictImageReader.isTruncationWarning("unexpected EOF"));
        assertFalse(StrictImageReader.isTruncationWarning("Corrupt JPEG data: 4 extraneous bytes before marker 0xe0"));
        assertFalse(StrictImageReader.isTruncationWarning("Bogus APP14/Adobe segment, ignoring"));
        assertFalse(StrictImageReader.isTruncationWarning("Unknown Adobe color transform code 9"));
        assertFalse(StrictImageReader.isTruncationWarning(null));
    }

    /**
     * Given a written rendition that does not fully decode, when checked before publishing, then
     * the check fails and deletes it; a healthy rendition passes and is kept.
     */
    @Test
    public void assertDecodable_truncatedFileIsDeletedAndRejected_healthyFileIsKept() throws Exception {
        final File truncated = truncatedCopy("test.jpg");
        try {
            StrictImageReader.assertDecodable(truncated);
            fail("a truncated rendition must not be published");
        } catch (IOException expected) {
            assertFalse("the undecodable rendition must be deleted", truncated.exists());
        }

        final File healthy = healthyCopy("test.png");
        StrictImageReader.assertDecodable(healthy);
        assertTrue(healthy.exists());
    }

    /**
     * Given a truncated JPEG source, when EVERY legacy filter runs on it, then each one throws and
     * leaves neither a result file nor a temp file behind (nothing for the exporter to cache).
     */
    @Test
    public void legacyFilters_truncatedJpeg_throwAndLeaveNoFiles() throws Exception {
        for (final Map.Entry<String, Supplier<LegacyRun>> entry : legacyFilters().entrySet()) {
            final LegacyRun run = entry.getValue().get();
            final File source = truncatedCopy("test.jpg");
            try {
                run.filter.runFilter(source, run.params);
                fail(entry.getKey() + " must fail on a truncated JPEG");
            } catch (DotRuntimeException e) {
                assertNotNull(entry.getKey() + " must keep the cause", e.getCause());
            }
            final String[] leftOver = workDir.list((dir, name) -> !name.equals(source.getName()));
            assertEquals(entry.getKey() + " must leave no result or temp file: " + Arrays.toString(leftOver),
                    0, leftOver == null ? 0 : leftOver.length);
            assertTrue(source.delete());
        }
    }

    /**
     * Given a healthy source, when EVERY legacy filter runs on it, then each one still writes a
     * decodable result and leaves no temp file behind (no behavior change for healthy images).
     */
    @Test
    public void legacyFilters_healthySource_renderDecodableResult() throws Exception {
        for (final Map.Entry<String, Supplier<LegacyRun>> entry : legacyFilters().entrySet()) {
            final LegacyRun run = entry.getValue().get();
            final File source = PDF_SOURCE.equals(run.healthySource) ? onePagePdf() : healthyCopy(run.healthySource);

            final File result = run.filter.runFilter(source, run.params);

            assertTrue(entry.getKey() + " must write its result", result.exists());
            assertNotNull(entry.getKey() + " result must decode", StrictImageReader.read(result));
            assertNoTempFilesLeft();
        }
    }

    /** One legacy filter invocation: the filter, its request params and the healthy source to use. */
    private static final class LegacyRun {
        private final ImageFilter filter;
        private final Map<String, String[]> params;
        private final String healthySource;

        private LegacyRun(final ImageFilter filter, final String healthySource, final String... keyValues) {
            this.filter = filter;
            this.healthySource = healthySource;
            this.params = new HashMap<>();
            for (int i = 0; i < keyValues.length; i += 2) {
                params.put(keyValues[i], new String[]{keyValues[i + 1]});
            }
        }
    }

    /** Every legacy filter, with params that make it render (crop x/y non-zero: no focal-point lookup). */
    private static Map<String, Supplier<LegacyRun>> legacyFilters() {
        final Map<String, Supplier<LegacyRun>> filters = new LinkedHashMap<>();
        filters.put("crop", () -> new LegacyRun(new CropImageFilter(), "test.jpg",
                "crop_x", "1", "crop_y", "1", "crop_w", "50", "crop_h", "40"));
        filters.put("gif", () -> new LegacyRun(new GifImageFilter(), "test.jpg"));
        filters.put("exposure", () -> new LegacyRun(new ExposureImageFilter(), "test.jpg", "exposure_exp", "1.5"));
        filters.put("flip", () -> new LegacyRun(new FlipImageFilter(), "test.jpg", "flip_flip", "1"));
        filters.put("jpeg", () -> new LegacyRun(new JpegImageFilter(), "test.jpg", "jpeg_q", "80"));
        filters.put("png", () -> new LegacyRun(new PngImageFilter(), "test.jpg"));
        filters.put("gamma", () -> new LegacyRun(new GammaImageFilter(), "test.jpg", "gamma_g", "1.5"));
        filters.put("grayscale", () -> new LegacyRun(new GrayscaleImageFilter(), "test.jpg"));
        filters.put("rotate", () -> new LegacyRun(new RotateImageFilter(), "test.jpg", "rotate_a", "90"));
        filters.put("resizegif", () -> new LegacyRun(new ResizeGifImageFilter(), "test.gif",
                "resize_w", "50", "resize_h", "40"));
        filters.put("webp", () -> new LegacyRun(new WebPImageFilter(), "test.jpg", "webp_q", "80"));
        filters.put("hsb", () -> new LegacyRun(new HsbImageFilter(), "test.jpg", "hsb_h", "0.2"));
        filters.put("resize", () -> new LegacyRun(new ResizeImageFilter(), "test.jpg",
                "resize_w", "50", "resize_h", "40"));
        filters.put("subsample", () -> new LegacyRun(new SubSampleImageFilter(), "test.jpg",
                "subsample_w", "50", "subsample_h", "40"));
        filters.put("thumbnail", () -> new LegacyRun(new ThumbnailImageFilter(), "test.jpg",
                "thumbnail_w", "50", "thumbnail_h", "40"));
        filters.put("pdf", () -> new LegacyRun(new PDFImageFilter(), PDF_SOURCE, "pdf_dpi", "36"));
        return filters;
    }

    /** A one-page PDF, the healthy source of the legacy PDF filter. */
    private File onePagePdf() throws IOException {
        final File pdf = new File(workDir, generatedName(PDF_SOURCE));
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdf);
        }
        return pdf;
    }

    /** Healthy test.jpg with 4 junk bytes right after SOI: decodes fully, but the reader warns. */
    private File jpegWithExtraneousBytes() throws Exception {
        final byte[] jpeg = Files.readAllBytes(resource("test.jpg").toPath());
        final byte[] withJunk = new byte[jpeg.length + 4];
        System.arraycopy(jpeg, 0, withJunk, 0, 2);
        System.arraycopy(new byte[]{1, 2, 3, 4}, 0, withJunk, 2, 4);
        System.arraycopy(jpeg, 2, withJunk, 6, jpeg.length - 2);
        final File copy = new File(workDir, generatedName("extraneous.jpg"));
        Files.write(copy.toPath(), withJunk);
        return copy;
    }

    private void assertDecodeFails(final Runnable decode) {
        try {
            decode.run();
            fail("a truncated JPEG must not decode");
        } catch (DotRuntimeException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    /** Crop filter with a fixed focal point, so no contentlet metadata lookup is needed. */
    private CropImageFilter cropFilter(final Optional<Point> focalPoint) {
        return new CropImageFilter() {
            @Override
            protected Optional<Point> calcFocalPoint(final BufferedImage src,
                    final Map<String, String[]> parameters) {
                return focalPoint;
            }
        };
    }

    private Map<String, String[]> cropParams(final int width, final int height) {
        final Map<String, String[]> params = new HashMap<>();
        params.put("filter", new String[]{"Crop"});
        params.put("crop_w", new String[]{String.valueOf(width)});
        params.put("crop_h", new String[]{String.valueOf(height)});
        return params;
    }

    private File resource(final String name) throws URISyntaxException {
        return new File(getClass().getResource("/images/" + name).toURI());
    }

    private File healthyCopy(final String name) throws Exception {
        final byte[] bytes = Files.readAllBytes(resource(name).toPath());
        final File copy = new File(workDir, generatedName(name));
        Files.write(copy.toPath(), bytes);
        return copy;
    }

    private File truncatedCopy(final String name) throws Exception {
        final byte[] bytes = Files.readAllBytes(resource(name).toPath());
        final File copy = new File(workDir, generatedName(name));
        Files.write(copy.toPath(), Arrays.copyOf(bytes, (int) (bytes.length * TRUNCATED_FRACTION)));
        return copy;
    }

    /** Generated-file prefix keeps results next to the source instead of the dotGenerated folder. */
    private String generatedName(final String name) {
        return WebKeys.GENERATED_FILE + UUID.randomUUID() + "_" + name;
    }

    private void assertNoTempFilesLeft() {
        final String[] tmp = workDir.list((dir, fileName) -> fileName.contains(".tmp"));
        assertEquals("no temp rendition must be left behind", 0, tmp == null ? 0 : tmp.length);
    }

    private static void deleteRecursively(final File file) {
        if (file == null || !file.exists()) {
            return;
        }
        final File[] children = file.listFiles();
        if (children != null) {
            for (final File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
