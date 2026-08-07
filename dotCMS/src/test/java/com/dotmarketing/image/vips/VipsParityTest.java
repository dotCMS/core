package com.dotmarketing.image.vips;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

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
 * <p>Tests are skipped (not failed) when native libvips is unavailable, so CI without libvips stays
 * green. Run locally with the libvips path wired in via {@code -Dvipsffm.libpath.args=...}.</p>
 */
public class VipsParityTest {

    @BeforeClass
    public static void requireLibvips() {
        Assume.assumeTrue("native libvips not available on this host", VipsManager.isAvailable());
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
        Assume.assumeTrue("host libvips lacks the PDF (poppler) delegate", pdfSupported());
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
        final File in = image("test.gif");
        final File out = tempOut("gif");
        new VipsResizeImageFilter().transform(in, out, params("resize_w", "50", "resize_h", "50"));
        assertTrue("animated gif resize output exists", out.exists() && out.length() > 50);
        assertTrue("gif decodes", ImageIO.read(out) != null);
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
        final File in = staged("test.gif", "gif");
        final File out = new VipsResizeImageFilter().runFilter(in, params("resize_w", "50", "resize_h", "50"));
        assertTrue("result keeps .gif extension (not png)", out.getName().endsWith(".gif"));
        // page height (single frame) is the resized height, not the stacked strip
        final Dimension d = new VipsImageFilterApiImpl().getWidthHeight(out);
        assertEquals(50, d.height);
        assertTrue("gif decodes", ImageIO.read(out) != null);
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
        Assume.assumeTrue("host libvips lacks an AVIF/AV1 encoder (libheif-plugin-aomenc)",
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

    /** JPEG XL encode requires the host libvips to be built with the libjxl delegate. */
    private boolean jxlEncodeSupported() {
        try {
            final File probeIn = image("test.png");
            final File probeOut = tempOut("jxl");
            new VipsJpegXlImageFilter().transform(probeIn, probeOut, params("jxl_q", "75"));
            return probeOut.exists() && probeOut.length() > 50;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    public void jxl_encoder_produces_valid_jxl() throws Exception {
        Assume.assumeTrue("host libvips lacks a JPEG XL encoder (libjxl)", jxlEncodeSupported());
        final File in = image("test.png");
        final File out = tempOut("jxl");
        new VipsJpegXlImageFilter().transform(in, out, params("jxl_q", "75"));
        assertTrue("jxl written", out.exists() && out.length() > 50);
        // JPEG XL signatures: the raw codestream starts with FF 0A; the ISO-BMFF container starts
        // with a 'JXL ' box (00 00 00 0C 4A 58 4C 20).
        final byte[] head = new byte[12];
        try (java.io.InputStream is = new java.io.FileInputStream(out)) {
            assertEquals(12, is.read(head));
        }
        final boolean rawCodestream = (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0x0A;
        final boolean container = (head[4] & 0xFF) == 0x4A && (head[5] & 0xFF) == 0x58
                && (head[6] & 0xFF) == 0x4C && (head[7] & 0xFF) == 0x20;
        assertTrue("expected a JPEG XL signature", rawCodestream || container);

        // Prove the jxl_* params are actually read (not silently ignored via a wrong prefix): lossless
        // must produce a distinctly larger file than low-quality lossy. If the prefix regressed both
        // would fall back to the same default and this assertion would fail.
        final File lossy = tempOut("jxl");
        final File lossless = tempOut("jxl");
        new VipsJpegXlImageFilter().transform(in, lossy, params("jxl_q", "10"));
        new VipsJpegXlImageFilter().transform(in, lossless, params("jxl_lossless", "true"));
        assertTrue("jxl_lossless must be honored (lossless > lossy q10)",
                lossless.length() > lossy.length() * 2);
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
