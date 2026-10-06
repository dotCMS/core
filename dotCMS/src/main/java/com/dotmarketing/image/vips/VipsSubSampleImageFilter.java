package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import java.io.File;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.SubSampleImageFilter}. libvips
 * {@code thumbnail} is inherently a streaming, demand-driven shrink, so it achieves the same
 * low-memory downscale the legacy subsampler provides — without decompressing the whole image into
 * heap.
 */
public class VipsSubSampleImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {"w (int) specifies width", "h (int) specifies height"};
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final int width = intParam(parameters, "w", 0);
        final int height = intParam(parameters, "h", 0);
        VipsManager.run(arena -> {
            // No target given → re-encode the source rather than asking libvips for a 0-px thumbnail.
            if (width <= 0 && height <= 0) {
                VipsManager.load(arena, in).writeToFile(out.getAbsolutePath());
                return;
            }
            final VImage thumb = width > 0 && height > 0
                    ? VImage.thumbnail(arena, in.getAbsolutePath(), width, VipsOption.Int("height", height))
                    : VImage.thumbnail(arena, in.getAbsolutePath(), Math.max(width, height));
            thumb.writeToFile(out.getAbsolutePath());
        });
    }
}
