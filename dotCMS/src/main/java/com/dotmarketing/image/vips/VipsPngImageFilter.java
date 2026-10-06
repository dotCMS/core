package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import java.io.File;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.PngImageFilter}: re-encodes the image
 * as PNG, preserving any alpha channel.
 */
public class VipsPngImageFilter extends VipsImageFilter {

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        VipsManager.run(arena -> {
            final VImage src = VipsManager.load(arena, in);
            src.writeToFile(out.getAbsolutePath());
        });
    }
}
