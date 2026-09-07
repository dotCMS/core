package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.enums.VipsDirection;
import java.io.File;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.FlipImageFilter}: mirrors the image
 * horizontally when {@code flip_flip} is present.
 */
public class VipsFlipImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {"flip (present) mirror horizontally"};
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final boolean flip = parameters.get(getPrefix() + "flip") != null;
        final VipsDirection direction = flip ? VipsDirection.DIRECTION_HORIZONTAL : VipsDirection.DIRECTION_VERTICAL;
        VipsManager.run(arena -> {
            final VImage src = VipsManager.load(arena, in);
            // Match the legacy default: only act when explicitly flipped; otherwise re-encode as-is.
            final VImage result = flip ? src.flip(direction) : src;
            result.writeToFile(out.getAbsolutePath());
        });
    }
}
