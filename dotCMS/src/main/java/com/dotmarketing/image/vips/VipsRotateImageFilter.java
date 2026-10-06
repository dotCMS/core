package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.RotateImageFilter}: rotates by an
 * arbitrary angle. libvips {@code rotate} resizes the canvas to the rotated bounding box, matching
 * the legacy {@code resize=true} behaviour. The {@code a} parameter is degrees clockwise.
 *
 * <p>The triangular corners exposed by a non-90° rotation are filled with the background colour.
 * By default this is transparent for images with an alpha channel and black otherwise (libvips
 * default). Pass {@code rotate_bg} as 9 digits (rrrgggbbb) to fill with a specific colour, e.g.
 * {@code rotate_bg=255255255} for white.</p>
 */
public class VipsRotateImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {
                "a (double) 0.00-359.99 degrees to rotate",
                "bg (int) optional 9-digit rgb background for the exposed corners"
        };
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final double angle = doubleParam(parameters, "a", 0d);
        final String[] bgParam = parameters.get(getPrefix() + "bg");
        final boolean hasBg = bgParam != null && bgParam[0] != null && bgParam[0].matches("\\d{9}");
        VipsManager.run(arena -> {
            final VImage src = VipsManager.load(arena, in);
            final VImage rotated = hasBg
                    ? src.rotate(angle, VipsOption.ArrayDouble("background", List.of(
                            (double) Integer.parseInt(bgParam[0].substring(0, 3)),
                            (double) Integer.parseInt(bgParam[0].substring(3, 6)),
                            (double) Integer.parseInt(bgParam[0].substring(6)))))
                    : src.rotate(angle);
            rotated.writeToFile(out.getAbsolutePath());
        });
    }
}
