package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import java.io.File;
import java.util.Map;

/**
 * JPEG XL encoder — a modern format the legacy engine cannot produce. Encodes via libvips' libjxl
 * delegate, typically smaller than JPEG at equal quality with support for lossless re-compression of
 * existing JPEGs.
 *
 * <p>URL contract: {@code filter=jxl&jxl_q=75}. {@code q} is 0-100 quality; {@code lossless=true}
 * switches to mathematically lossless encoding; {@code effort} (1-9) trades encode speed for size.
 * Requires the host libvips to be built with libjxl; if absent the request fails (no legacy
 * fallback — JPEG XL is libvips-only).</p>
 */
public class VipsJpegXlImageFilter extends VipsImageFilter {

    /**
     * Force the parameter prefix to {@code jxl_} (the documented contract). Without this override the
     * default derives the name from the class — {@code VipsJpegXlImageFilter -> "jpegxl"} — so the
     * documented {@code jxl_q}/{@code jxl_lossless}/{@code jxl_effort} params would be silently ignored.
     * The {@code jpegxl} request alias still resolves the filter (registered in
     * {@code VipsImageFilterApiImpl}); only the param prefix is pinned here, so it is {@code jxl_} for
     * both {@code filter=jxl} and {@code filter=jpegxl}.
     */
    @Override
    protected String getFilterName() {
        return "jxl";
    }

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {
                "q (int) 0-100 quality",
                "lossless (present) lossless encode",
                "effort (int) 1-9 encoder effort/speed tradeoff"
        };
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final int quality = intParam(parameters, "q", 75);
        final boolean lossless = parameters.get(getPrefix() + "lossless") != null;
        final int effort = intParam(parameters, "effort", 7);
        VipsManager.run(arena -> {
            final VImage src = VipsManager.load(arena, in);
            src.writeToFile(out.getAbsolutePath(),
                    VipsOption.Int("Q", quality),
                    VipsOption.Boolean("lossless", lossless),
                    VipsOption.Int("effort", effort));
        });
    }

    @Override
    public File getResultsFile(final File file, final Map<String, String[]> parameters) {
        return getResultsFile(file, parameters, "jxl");
    }
}
