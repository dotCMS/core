package com.dotmarketing.image.vips;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.util.Config;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Parity tests for the libvips image engine against the legacy AWT/TwelveMonkeys engine.
 *
 * <p><b>Parity is NOT pixel-identity</b> — libvips and jhlabs/TwelveMonkeys produce different bytes
 * for every operation. Parity here means:</p>
 * <ul>
 *   <li><b>Deterministic geometry exact</b>: output dimensions / crop rectangle match the legacy
 *       engine exactly (the resize math is the shared {@code ResizeCalc}).</li>
 *   <li><b>Encoding correct</b>: the right container is produced and decodes back to the expected
 *       size; alpha and animation are preserved where expected.</li>
 *   <li><b>Visual closeness within tolerance</b>: for resize, mean per-pixel luma delta against the
 *       legacy output stays under a generous threshold.</li>
 * </ul>
 *
 * <p>Tests are skipped (not failed) when native libvips is unavailable, so a developer machine
 * without it stays green. Set {@code DOT_TEST_REQUIRE_LIBVIPS=true} to invert that: the native
 * engine, the PDF delegate and the AVIF encoder then become hard assertions. That is how CI runs,
 * so a regression in the shipped native build cannot hide behind a skipped test. Run locally with
 * the libvips path wired in via {@code -Dvipsffm.libpath.args=...}.</p>
 */
public class VipsParityTest {

    /** Root for the truncated-input fixtures; JUnit deletes it, and everything under it, after the class. */
    @ClassRule
    public static final TemporaryFolder TRUNCATED_ROOT = new TemporaryFolder();

    /** Config key (env override {@code DOT_TEST_REQUIRE_LIBVIPS}) that turns skips into failures. */
    private static final String STRICT_PROPERTY = "TEST_REQUIRE_LIBVIPS";

    /**
     * Whether a missing native capability should fail instead of skipping.
     *
     * <p>Read through dotCMS {@code Config} rather than {@code System.getenv} so the override uses
     * the standard {@code DOT_} prefix: {@code DOT_TEST_REQUIRE_LIBVIPS=true}.</p>
     *
     * @return true when the native engine is required to be present and complete
     */
    private static boolean strict() {
        return Config.getBooleanProperty(STRICT_PROPERTY, false);
    }

    /**
     * Require a native capability, or skip when it is legitimately optional.
     *
     * <p>Outside strict mode a missing capability skips the test, keeping local runs green on hosts
     * with a partially delegated libvips. In strict mode it is a failure, so CI cannot pass while
     * the shipped native build is incomplete.</p>
     *
     * @param message description of the capability being required
     * @param supported whether the capability is available
     */
    private static void requireCapability(final String message, final boolean supported) {
        if (strict()) {
            assertTrue(message + " (TEST_REQUIRE_LIBVIPS is set, so this is a failure)", supported);
            return;
        }
        Assume.assumeTrue(message, supported);
    }

    /**
     * Gate the whole class on a usable native libvips.
     *
     * <p>In strict mode (CI) a missing libvips fails; otherwise the class is skipped so a host
     * without it does not report false failures.</p>
     */
    @BeforeClass
    public static void requireLibvips() {
        requireCapability("native libvips not available on this host", VipsManager.isAvailable());
    }

    private File image(final String name) {
        final URL url = getClass().getResource("/images/" + name);
        assertTrue("missing test image " + name, url != null);
        return new File(url.getFile());
    }

    private File tempOut(final String ext) throws Exception {
        final File f = File.createTempFile("vips-parity", "." + ext);
        f.delete();
        return f;
    }

    private Map<String, String[]> params(final String... kv) {
        final Map<String, String[]> p = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            p.put(kv[i], new String[] {kv[i + 1]});
        }
        return p;
    }

    private Dimension dims(final File f) throws Exception {
        final BufferedImage bi = ImageIO.read(f);
        assertTrue("output did not decode: " + f, bi != null);
        return new Dimension(bi.getWidth(), bi.getHeight());
    }

    // ---- API-level parity (mirrors ImageFilterAPIImplTest expectations) --------------------------

    @Test
    public void widthHeight_matches_known_dimensions() {
        final VipsImageFilterApiImpl api = new VipsImageFilterApiImpl();
        Dimension d = api.getWidthHeight(image("10by10000.png"));
        assertEquals(10000, d.width);
        assertEquals(10, d.height);

        d = api.getWidthHeight(image("test.webp"));
        assertEquals(1024, d.width);
        assertEquals(772, d.height);
    }

    @Test
    public void svg_dimensions_render_natively() {
        final VipsImageFilterApiImpl api = new VipsImageFilterApiImpl();
        final Dimension d = api.getWidthHeight(image("test.svg"));
        assertTrue("svg width > 0", d.width > 0);
        assertTrue("svg height > 0", d.height > 0);
    }

    @Test
    public void resizeImage_returns_exact_dimensions() {
        final VipsImageFilterApiImpl api = new VipsImageFilterApiImpl();
        final BufferedImage out = api.resizeImage(image("10by10000.png"), 1000, 10);
        assertEquals(1000, out.getWidth());
        assertEquals(10, out.getHeight());
    }

    @Test
    public void intelligentResize_caps_at_max_size() {
        final VipsImageFilterApiImpl api = new VipsImageFilterApiImpl();
        final BufferedImage out = api.intelligentResize(image("10by10000.png"), 7000, 10);
        assertEquals("width clamped to IMAGE_MAX_PIXEL_SIZE", 5000, out.getWidth());
        assertEquals(10, out.getHeight());
    }

    // ---- Filter-level parity (geometry exact + encoding correct) ---------------------------------

    @Test
    public void resize_filter_geometry_and_visual_parity() throws Exception {
        final File in = image("test.jpg");
        final File out = tempOut("png");
        new VipsResizeImageFilter().transform(in, out, params("resize_w", "200", "resize_h", "150"));

        final Dimension d = dims(out);
        assertEquals(200, d.width);
        assertEquals(150, d.height);

        // visual closeness vs legacy engine output at the same size
        final BufferedImage legacy =
                com.dotmarketing.image.filter.ImageFilterAPI.apiInstance.apply().resizeImage(in, 200, 150);
        final double delta = meanLumaDelta(ImageIO.read(out), legacy);
        assertTrue("resize mean luma delta too high: " + delta, delta < 12.0);
    }

    @Test
    public void crop_filter_extracts_exact_region() throws Exception {
        final File in = image("test.jpg");
        final File out = tempOut("png");
        new VipsCropImageFilter().transform(in, out, params("crop_x", "10", "crop_y", "10",
                "crop_w", "120", "crop_h", "90"));
        final Dimension d = dims(out);
        assertEquals(120, d.width);
        assertEquals(90, d.height);
    }

    @Test
    public void thumbnail_filter_produces_exact_canvas() throws Exception {
        final File in = image("test.jpg");
        final File out = tempOut("png");
        new VipsThumbnailImageFilter().transform(in, out, params("thumbnail_w", "100", "thumbnail_h", "80"));
        final Dimension d = dims(out);
        assertEquals(100, d.width);
        assertEquals(80, d.height);
    }

    @Test
    public void rotate_filter_changes_bounding_box() throws Exception {
        final File in = image("test.jpg");
        final File out = tempOut("png");
        new VipsRotateImageFilter().transform(in, out, params("rotate_a", "45"));
        final Dimension d = dims(out);
        final Dimension orig = new VipsImageFilterApiImpl().getWidthHeight(in);
        assertTrue("45deg rotate grows bounding box", d.width > orig.width || d.height > orig.height);
    }

    @Test
    public void flip_filter_preserves_dimensions() throws Exception {
        final File in = image("test.jpg");
        final File out = tempOut("png");
        final Dimension orig = new VipsImageFilterApiImpl().getWidthHeight(in);
        new VipsFlipImageFilter().transform(in, out, params("flip_flip", "true"));
        final Dimension d = dims(out);
        assertEquals(orig.width, d.width);
        assertEquals(orig.height, d.height);
    }

    @Test
    public void gamma_grayscale_exposure_hsb_produce_valid_images() throws Exception {
        final File in = image("test.jpg");
        for (final String op : new String[] {"gamma", "grayscale", "exposure", "hsb"}) {
            final File out = tempOut("png");
            switch (op) {
                case "gamma":
                    new VipsGammaImageFilter().transform(in, out, params("gamma_g", "2.2"));
                    break;
                case "grayscale":
                    new VipsGrayscaleImageFilter().transform(in, out, params());
                    break;
                case "exposure":
                    new VipsExposureImageFilter().transform(in, out, params("exposure_exp", "1.5"));
                    break;
                default:
                    new VipsHsbImageFilter().transform(in, out, params("hsb_h", "0.1", "hsb_s", "0.2", "hsb_b", "0.1"));
            }
            assertTrue(op + " output exists", out.exists() && out.length() > 50);
            assertTrue(op + " output decodes", ImageIO.read(out) != null);
        }
    }

    @Test
    public void hsb_handles_rgba_input_without_crashing() throws Exception {
        // Build a genuine RGBA (alpha) PNG so the HSV band-split path is exercised.
        final File dir = java.nio.file.Files.createTempDirectory("vips-rgba").toFile();
        final File in = new File(dir, "rgba.png");
        final BufferedImage argb = new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 40; x++) {
                argb.setRGB(x, y, ((x * 6) << 24) | (0x3366cc));
            }
        }
        ImageIO.write(argb, "png", in);

        final File out = tempOut("png");
        new VipsHsbImageFilter().transform(in, out, params("hsb_h", "0.1", "hsb_s", "0.3", "hsb_b", "0.1"));
        final BufferedImage result = ImageIO.read(out);
        assertTrue("rgba hsb output decodes", result != null);
        assertEquals(40, result.getWidth());
        assertEquals(30, result.getHeight());
        assertTrue("alpha preserved", result.getColorModel().hasAlpha());
    }

    @Test
    public void pdf_renders_page_to_png() throws Exception {
        requireCapability("host libvips lacks the PDF (poppler) delegate", pdfSupported());
        // Write a minimal one-page PDF directly (no fixture needed).
        final File dir = java.nio.file.Files.createTempDirectory("vips-pdf").toFile();
        final File in = new File(dir, "dotGenerated_doc.pdf");
        java.nio.file.Files.write(in.toPath(), MINI_PDF.getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        // runFilter must produce a .png (not .pdf) result, like the legacy PDF filter.
        final File result = new VipsPdfImageFilter().runFilter(in, params("pdf_dpi", "100"));
        assertTrue("pdf result is png, not pdf: " + result.getName(), result.getName().endsWith(".png"));
        final BufferedImage img = ImageIO.read(result);
        assertTrue("pdf render decodes", img != null && img.getWidth() > 0 && img.getHeight() > 0);
    }

    private boolean pdfSupported() {
        try {
            final File dir = java.nio.file.Files.createTempDirectory("vips-pdfprobe").toFile();
            final File in = new File(dir, "probe.pdf");
            java.nio.file.Files.write(in.toPath(), MINI_PDF.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            final File out = tempOut("png");
            new VipsPdfImageFilter().transform(in, out, params());
            return out.exists() && out.length() > 50;
        } catch (Exception e) {
            return false;
        }
    }

    private static final String MINI_PDF =
            "%PDF-1.1\n"
            + "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
            + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
            + "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 100]/Contents 4 0 R>>endobj\n"
            + "4 0 obj<</Length 44>>stream\nBT /F1 24 Tf 20 40 Td (Hi PDF) Tj ET\nendstream endobj\n"
            + "trailer<</Root 1 0 R>>\n%%EOF\n";

    // ---- Format encoders -------------------------------------------------------------------------

    @Test
    public void jpeg_png_webp_gif_encoders_produce_valid_files() throws Exception {
        final File in = image("test.png");
        final Dimension orig = new VipsImageFilterApiImpl().getWidthHeight(in);

        final File jpg = tempOut("jpg");
        new VipsJpegImageFilter().transform(in, jpg, params("jpeg_q", "80"));
        assertEquals(orig, dims(jpg));

        final File png = tempOut("png");
        new VipsPngImageFilter().transform(in, png, params());
        assertEquals(orig, dims(png));

        final File webp = tempOut("webp");
        new VipsWebPImageFilter().transform(in, webp, params("webp_q", "80"));
        assertTrue("webp written", webp.exists() && webp.length() > 50);

        final File gif = tempOut("gif");
        new VipsGifImageFilter().transform(in, gif, params());
        assertEquals(orig, dims(gif));
    }

    @Test
    public void animated_gif_resize_preserves_animation() throws Exception {
        // The bundled test.gif fixture is single-frame, so build a genuinely animated input:
        // otherwise this assertion cannot tell "animation preserved" from "flattened to one frame".
        final File in = animatedGif(3);
        final int sourceFrames = gifFrameCount(in);
        assertTrue("fixture is animated (frames=" + sourceFrames + ")", sourceFrames > 1);

        final File out = tempOut("gif");
        new VipsResizeImageFilter().transform(in, out, params("resize_w", "50", "resize_h", "50"));
        assertTrue("animated gif resize output exists", out.exists() && out.length() > 50);
        assertTrue("gif decodes", ImageIO.read(out) != null);
        assertEquals("animation frame count preserved", sourceFrames, gifFrameCount(out));
    }

    /**
     * Build a small genuinely-animated GIF.
     *
     * <p>The bundled {@code test.gif} fixture has a single frame, so tests that claim to check
     * animation preservation need their own input rather than a static resource.</p>
     *
     * @param frames number of frames to write
     * @return the written GIF
     * @throws Exception if the file cannot be written
     */
    private File animatedGif(final int frames) throws Exception {
        final File out = tempOut("gif");
        final ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            try {
                for (int i = 0; i < frames; i++) {
                    final BufferedImage frame = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
                    final int colour = 0x333333 * (i + 1);
                    for (int y = 0; y < frame.getHeight(); y++) {
                        for (int x = 0; x < frame.getWidth(); x++) {
                            frame.setRGB(x, y, colour);
                        }
                    }
                    writer.writeToSequence(new IIOImage(frame, null, null), null);
                }
            } finally {
                writer.endWriteSequence();
                writer.dispose();
            }
        }
        return out;
    }

    /**
     * Count the frames in a GIF, to tell a preserved animation from a flattened single frame.
     *
     * @param gif file to inspect
     * @return number of frames, or 0 when no GIF reader is registered
     * @throws Exception if the file cannot be read
     */
    private int gifFrameCount(final File gif) throws Exception {
        try (ImageInputStream stream = ImageIO.createImageInputStream(gif)) {
            final ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(stream);
            try {
                return reader.getNumImages(true);
            } finally {
                reader.dispose();
            }
        }
    }

    // ---- Production path: runFilter (output-extension determination + rename) -------------------

    /** Stage an input named with the GENERATED_FILE prefix so getResultsFile writes alongside it. */
    private File staged(final String src, final String stagedExt) throws Exception {
        final File dir = java.nio.file.Files.createTempDirectory("vips-runfilter").toFile();
        final File in = new File(dir, "dotGenerated_src." + stagedExt);
        java.nio.file.Files.copy(image(src).toPath(), in.toPath());
        return in;
    }

    @Test
    public void runFilter_resize_writes_png_with_exact_dims() throws Exception {
        final File in = staged("test.jpg", "jpg");
        final File out = new VipsResizeImageFilter().runFilter(in, params("resize_w", "200", "resize_h", "150"));
        assertTrue("result is png", out.getName().endsWith(".png"));
        assertEquals(new Dimension(200, 150), dims(out));
    }

    @Test
    public void runFilter_resize_of_gif_stays_gif_and_keeps_animation() throws Exception {
        // Production path, on a genuinely animated input (test.gif is single-frame).
        final File dir = java.nio.file.Files.createTempDirectory("vips-runfilter").toFile();
        final File in = new File(dir, "dotGenerated_src.gif");
        java.nio.file.Files.copy(animatedGif(3).toPath(), in.toPath());
        final int sourceFrames = gifFrameCount(in);
        assertTrue("fixture is animated (frames=" + sourceFrames + ")", sourceFrames > 1);

        final File out = new VipsResizeImageFilter().runFilter(in, params("resize_w", "50", "resize_h", "50"));
        assertTrue("result keeps .gif extension (not png)", out.getName().endsWith(".gif"));
        // page height (single frame) is the resized height, not the stacked strip
        final Dimension d = new VipsImageFilterApiImpl().getWidthHeight(out);
        assertEquals(50, d.height);
        assertTrue("gif decodes", ImageIO.read(out) != null);
        assertEquals("animation frame count preserved through runFilter", sourceFrames, gifFrameCount(out));
    }

    // ---- New capability: content-aware smart crop (no legacy equivalent) -----------------------

    /** AVIF encode requires libheif built with an AV1 encoder (e.g. libheif-plugin-aomenc). */
    private boolean avifEncodeSupported() {
        try {
            final File probeIn = image("test.png");
            final File probeOut = tempOut("avif");
            new VipsAvifImageFilter().transform(probeIn, probeOut, params("avif_q", "50"));
            return probeOut.exists() && probeOut.length() > 50;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    public void avif_encoder_produces_valid_avif() throws Exception {
        requireCapability("host libvips lacks an AVIF/AV1 encoder (libheif-plugin-aomenc)",
                avifEncodeSupported());
        final File in = image("test.png");
        final File out = tempOut("avif");
        new VipsAvifImageFilter().transform(in, out, params("avif_q", "50"));
        assertTrue("avif written", out.exists() && out.length() > 50);
        // AVIF magic: ISO-BMFF 'ftyp' box with an 'avif'/'avis' brand at offset 8.
        final byte[] head = new byte[12];
        try (java.io.InputStream is = new java.io.FileInputStream(out)) {
            assertEquals(12, is.read(head));
        }
        final String brand = new String(head, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
        assertTrue("expected avif brand, got '" + brand + "'", brand.startsWith("avi"));
    }

    /**
     * Decode an AVIF back through the engine, covering the libheif decode path independently of the
     * encoder that {@link #avif_encoder_produces_valid_avif()} exercises.
     *
     * @throws Exception if the fixture or output cannot be read
     */
    @Test
    public void avif_decode_reads_back_encoded_output() throws Exception {
        requireCapability("host libvips lacks an AVIF/AV1 encoder (libheif-plugin-aomenc)",
                avifEncodeSupported());
        final File in = image("test.png");
        final File encoded = tempOut("avif");
        new VipsAvifImageFilter().transform(in, encoded, params("avif_q", "50"));

        // Read the geometry back through heifload and compare with the source: a write-only bug
        // (valid container, undecodable payload) would otherwise go unnoticed.
        final Dimension decoded = new VipsImageFilterApiImpl().getWidthHeight(encoded);
        final Dimension original = new VipsImageFilterApiImpl().getWidthHeight(in);
        assertEquals("AVIF decodes back to the source geometry", original, decoded);
    }

    @Test
    public void smartcrop_produces_exact_box_from_salient_region() throws Exception {
        final File in = image("test.jpg");
        final File out = tempOut("png");
        new VipsSmartCropImageFilter().transform(in, out,
                params("smartcrop_w", "120", "smartcrop_h", "120", "smartcrop_mode", "attention"));
        final Dimension d = dims(out);
        assertEquals(120, d.width);
        assertEquals(120, d.height);
    }

    @Test
    public void smartcrop_clamps_oversized_request_instead_of_failing() throws Exception {
        final File in = image("test.jpg");
        final Dimension src = new VipsImageFilterApiImpl().getWidthHeight(in);
        final File out = tempOut("png");
        // Request a height far larger than the source — must clamp, not throw "bad extract area".
        new VipsSmartCropImageFilter().transform(in, out,
                params("smartcrop_w", "120", "smartcrop_h", String.valueOf(src.height * 10)));
        final Dimension d = dims(out);
        assertEquals(120, d.width);
        assertTrue("height clamped to source", d.height <= src.height);
    }

    // ---- Resilience: fall back to the legacy engine when a libvips op fails --------------------

    /** A libvips PNG filter whose pixel work always fails, to exercise the fallback path. */
    static class FailingVipsPngFilter extends VipsPngImageFilter {
        @Override
        protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
            throw new RuntimeException("forced libvips failure for test");
        }
        @Override
        protected String getFilterName() {
            return "png";
        }
    }

    @Test
    public void failed_vips_op_falls_back_to_legacy_engine() throws Exception {
        // Name the input with the GENERATED_FILE prefix so getResultsFile writes alongside it
        // (no DB / generated-path config needed in a unit test).
        final File dir = java.nio.file.Files.createTempDirectory("vips-fallback").toFile();
        final File in = new File(dir, "dotGenerated_fallback_src.png");
        java.nio.file.Files.copy(image("test.png").toPath(), in.toPath());

        final File result = new FailingVipsPngFilter().runFilter(in, new HashMap<>());

        assertTrue("fallback produced a result file", result.exists() && result.length() > 50);
        assertTrue("fallback output is a valid image", ImageIO.read(result) != null);
    }

    // ---- Truncated input: a failed libvips read must not publish a black/grey rendition --------

    private static final String FALLBACK_PROPERTY = "IMAGE_API_LIBVIPS_FALLBACK";

    /**
     * Stage a copy of a bundled image cut to {@code keepFraction} of its bytes, named with the
     * GENERATED_FILE prefix so {@code getResultsFile} writes alongside it in its own directory.
     *
     * @param src bundled image name
     * @param ext extension of the staged copy
     * @param keepFraction fraction of the original bytes to keep (0-1)
     * @return the truncated copy
     * @throws Exception if the copy cannot be written
     */
    private File truncated(final String src, final String ext, final double keepFraction) throws Exception {
        final File dir = TRUNCATED_ROOT.newFolder();
        final File in = new File(dir, "dotGenerated_truncated." + ext);
        final byte[] bytes = java.nio.file.Files.readAllBytes(image(src).toPath());
        java.nio.file.Files.write(in.toPath(),
                java.util.Arrays.copyOf(bytes, (int) (bytes.length * keepFraction)));
        return in;
    }

    /**
     * @param input a staged input file
     * @return every file next to {@code input} other than itself, i.e. any rendition left behind
     */
    private java.util.List<File> renditionsBesides(final File input) {
        final File[] files = input.getParentFile().listFiles();
        final java.util.List<File> others = new java.util.ArrayList<>();
        if (files != null) {
            for (final File f : files) {
                if (!f.equals(input)) {
                    others.add(f);
                }
            }
        }
        return others;
    }

    /**
     * Resize a truncated image with {@code IMAGE_API_LIBVIPS_FALLBACK=false} and assert it fails
     * loudly and leaves no rendition behind.
     *
     * @param in truncated input staged by {@link #truncated}
     */
    private void assertTruncatedFailsWithoutFallback(final File in) {
        final String previous = Config.getStringProperty(FALLBACK_PROPERTY, null);
        Config.setProperty(FALLBACK_PROPERTY, false);
        try {
            new VipsResizeImageFilter().runFilter(in, params("resize_w", "200", "resize_h", "150"));
            org.junit.Assert.fail("truncated " + in.getName() + " must not render through libvips");
        } catch (com.dotmarketing.exception.DotRuntimeException expected) {
            assertTrue("no rendition may be cached: " + renditionsBesides(in), renditionsBesides(in).isEmpty());
        } finally {
            Config.setProperty(FALLBACK_PROPERTY, previous);
        }
    }

    /**
     * Resize a truncated image with {@code IMAGE_API_LIBVIPS_FALLBACK=true}: libvips recognises the
     * format but cannot decode it, so the input is damaged and the legacy engine is skipped. The
     * libvips error is raised directly (it is the cause, not a legacy decode error) and nothing is cached.
     *
     * @param in truncated input staged by {@link #truncated}
     */
    private void assertTruncatedSkipsLegacyFallback(final File in) {
        final String previous = Config.getStringProperty(FALLBACK_PROPERTY, null);
        Config.setProperty(FALLBACK_PROPERTY, true);
        try {
            new VipsResizeImageFilter().runFilter(in, params("resize_w", "200", "resize_h", "150"));
            org.junit.Assert.fail("truncated " + in.getName() + " must not render, with or without fallback");
        } catch (com.dotmarketing.exception.DotRuntimeException expected) {
            assertTrue("the libvips error must be raised, not a legacy one: " + expected.getCause(),
                    expected.getCause() != null && String.valueOf(expected.getCause().getMessage())
                            .startsWith("libvips operation failed"));
            assertTrue("no rendition may be cached: " + renditionsBesides(in), renditionsBesides(in).isEmpty());
        } finally {
            Config.setProperty(FALLBACK_PROPERTY, previous);
        }
    }

    /**
     * Assert the libvips resize transform itself rejects a truncated input.
     *
     * @param in truncated input staged by {@link #truncated}
     * @throws Exception if the temp output cannot be created
     */
    private void assertTransformRejects(final File in) throws Exception {
        final File out = tempOut("png");
        try {
            new VipsResizeImageFilter().transform(in, out, params("resize_w", "200", "resize_h", "150"));
            org.junit.Assert.fail("libvips must fail on truncated " + in.getName() + " (fail_on=error)");
        } catch (RuntimeException expected) {
            // VipsError / DotRuntimeException: the read failed loudly instead of filling rows
        }
    }

    @Test
    public void transform_of_truncated_jpeg_fails_instead_of_filling_grey_rows() throws Exception {
        assertTransformRejects(truncated("test.jpg", "jpg", 0.35));
    }

    @Test
    public void transform_of_truncated_png_fails_instead_of_filling_black_rows() throws Exception {
        assertTransformRejects(truncated("test.png", "png", 0.35));
    }

    @Test
    public void runFilter_truncated_jpeg_without_fallback_throws_and_caches_nothing() throws Exception {
        assertTruncatedFailsWithoutFallback(truncated("test.jpg", "jpg", 0.35));
    }

    @Test
    public void runFilter_truncated_png_without_fallback_throws_and_caches_nothing() throws Exception {
        assertTruncatedFailsWithoutFallback(truncated("test.png", "png", 0.35));
    }

    @Test
    public void runFilter_truncated_jpeg_with_fallback_skips_legacy_engine() throws Exception {
        assertTruncatedSkipsLegacyFallback(truncated("test.jpg", "jpg", 0.35));
    }

    @Test
    public void runFilter_truncated_png_with_fallback_skips_legacy_engine() throws Exception {
        assertTruncatedSkipsLegacyFallback(truncated("test.png", "png", 0.35));
    }

    @Test
    public void runFilter_renders_once_truncated_source_is_healthy_again() throws Exception {
        final File in = truncated("test.jpg", "jpg", 0.35);
        assertTruncatedFailsWithoutFallback(in);

        // the source is repaired: the same request must now render instead of serving a bad cache
        java.nio.file.Files.copy(image("test.jpg").toPath(), in.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        final File out = new VipsResizeImageFilter().runFilter(in, params("resize_w", "200", "resize_h", "150"));
        assertEquals(new Dimension(200, 150), dims(out));
    }

    /**
     * Stages bytes as a generated input in a fresh folder, so any rendition lands next to it.
     *
     * @param bytes the file content
     * @param ext the file extension
     * @return the staged input
     * @throws Exception if the file cannot be written
     */
    private File staged(final byte[] bytes, final String ext) throws Exception {
        final File in = new File(TRUNCATED_ROOT.newFolder(), "dotGenerated_staged." + ext);
        java.nio.file.Files.write(in.toPath(), bytes);
        return in;
    }

    /**
     * Resizes the input with the legacy fallback off and asserts it renders at the requested size,
     * so the result can only come from libvips.
     *
     * @param in the staged input
     * @throws Exception if the result cannot be read
     */
    private void assertRendersWithoutFallback(final File in) throws Exception {
        final String previous = Config.getStringProperty(FALLBACK_PROPERTY, null);
        Config.setProperty(FALLBACK_PROPERTY, false);
        try {
            final File out = new VipsResizeImageFilter().runFilter(in, params("resize_w", "200", "resize_h", "150"));
            assertEquals(new Dimension(200, 150), dims(out));
        } finally {
            Config.setProperty(FALLBACK_PROPERTY, previous);
        }
    }

    /**
     * Given a JPEG that holds all its image data but lacks its final EOI marker (common in camera and
     * pipeline output; libjpeg reports it like a truncated file), when resized by libvips, then it
     * renders: the marker is restored on a temporary copy and the transform is retried.
     */
    @Test
    public void runFilter_complete_jpeg_without_eoi_marker_renders() throws Exception {
        final byte[] bytes = java.nio.file.Files.readAllBytes(image("test.jpg").toPath());
        assertRendersWithoutFallback(staged(java.util.Arrays.copyOf(bytes, bytes.length - 2), "jpg"));
    }

    /**
     * Given a PNG whose first IDAT chunk has a wrong checksum but complete data (browsers and the
     * previous engine render it), when resized by libvips, then it renders: {@code fail_on=truncated}
     * rejects short reads only, not checksum errors. The fixture is written with several IDAT chunks,
     * which is what makes libvips check the first one ({@code fail_on=error} rejects it).
     */
    @Test
    public void runFilter_png_with_bad_idat_checksum_renders() throws Exception {
        final BufferedImage noise = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < noise.getHeight(); y++) {
            for (int x = 0; x < noise.getWidth(); x++) {
                noise.setRGB(x, y, (x * 7919 + y * 104729) & 0xFFFFFF);
            }
        }
        final java.io.ByteArrayOutputStream png = new java.io.ByteArrayOutputStream();
        ImageIO.write(noise, "png", png);
        final byte[] bytes = png.toByteArray();
        final int idat = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1).indexOf("IDAT");
        assertTrue("fixture has an IDAT chunk", idat > 4);
        final int length = java.nio.ByteBuffer.wrap(bytes, idat - 4, 4).getInt();
        final int crcLastByte = idat + 4 + length + 3;
        bytes[crcLastByte] = (byte) (bytes[crcLastByte] ^ 0xFF);
        assertRendersWithoutFallback(staged(bytes, "png"));
    }

    @Test
    public void legacy_fallback_map_covers_every_filter() {
        for (final String name : new String[] {"crop", "resize", "scale", "thumbnail", "rotate", "flip",
                "gamma", "grayscale", "hsb", "exposure", "subsample", "jpeg", "png", "webp", "gif", "pdf"}) {
            assertTrue("missing legacy fallback for " + name, VipsLegacyFilters.forName(name).isPresent());
        }
    }

    /** Mean absolute luma difference (0-255) between two equally-sized images. */
    private double meanLumaDelta(final BufferedImage a, final BufferedImage b) {
        final int w = Math.min(a.getWidth(), b.getWidth());
        final int h = Math.min(a.getHeight(), b.getHeight());
        double sum = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sum += Math.abs(luma(a.getRGB(x, y)) - luma(b.getRGB(x, y)));
            }
        }
        return sum / (w * h);
    }

    private double luma(final int rgb) {
        final int r = (rgb >> 16) & 0xff;
        final int g = (rgb >> 8) & 0xff;
        final int b = rgb & 0xff;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }
}
