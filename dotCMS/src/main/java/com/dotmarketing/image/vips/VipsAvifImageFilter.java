package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import java.io.File;
import java.util.Map;

/**
 * AVIF encoder — a modern format the legacy engine cannot produce. Encodes via libvips' libheif
 * delegate (AV1), typically 20-50% smaller than WebP/JPEG at equal quality.
 *
 * <p>URL contract: {@code filter=avif&avif_q=50}. {@code q} is 0-100 quality; {@code lossless=true}
 * switches to lossless. Requires the host libvips to be built with libheif (Ubuntu 24.04's
 * {@code libvips42} is); if absent the request fails (no legacy fallback — AVIF is libvips-only).</p>
 */
public class VipsAvifImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {
                "q (int) 0-100 quality",
                "lossless (present) lossless encode",
                "effort (int) 0-9 encoder effort/speed tradeoff"
        };
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final int quality = intParam(parameters, "q", 50);
        final boolean lossless = parameters.get(getPrefix() + "lossless") != null;
        final int effort = intParam(parameters, "effort", 4);
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
        return getResultsFile(file, parameters, "avif");
    }
}
