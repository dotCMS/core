package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import java.io.File;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.RotateImageFilter}: rotates by an
 * arbitrary angle. libvips {@code rotate} resizes the canvas to the rotated bounding box, matching
 * the legacy {@code resize=true} behaviour. The {@code a} parameter is degrees clockwise.
 */
public class VipsRotateImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {"a (double) 0.00-359.99 degrees to rotate"};
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final double angle = doubleParam(parameters, "a", 0d);
        VipsManager.run(arena -> {
            final VImage src = VipsManager.load(arena, in);
            final VImage rotated = src.rotate(angle);
            rotated.writeToFile(out.getAbsolutePath());
        });
    }
}
