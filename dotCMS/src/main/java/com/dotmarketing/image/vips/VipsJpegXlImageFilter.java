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
