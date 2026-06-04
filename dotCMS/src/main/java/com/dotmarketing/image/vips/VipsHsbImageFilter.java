package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.enums.VipsInterpretation;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.HsbImageFilter}. Adjusts hue,
 * saturation and brightness by converting to HSV, applying a per-band linear adjustment, and
 * converting back to sRGB.
 *
 * <p>Note: this is a perceptual approximation of the legacy jhlabs HSBAdjustFilter. The hue shift is
 * additive (wrapping) and saturation/brightness are multiplicative scales, which is close but not
 * pixel-identical to jhlabs' HSL-based math.</p>
 */
public class VipsHsbImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {
                "h hue (double) between -1.0 and 1.0",
                "s saturation (double) between -1.0 and 1.0",
                "b brightness (double) between -1.0 and 1.0"
        };
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final double h = doubleParam(parameters, "h", 0d);
        final double s = doubleParam(parameters, "s", 0d);
        final double b = doubleParam(parameters, "b", 0d);
        VipsManager.run(arena -> {
            final VImage src = VipsManager.load(arena, in).colourspace(VipsInterpretation.INTERPRETATION_sRGB);
            final VImage hsv = src.sRGB2HSV();
            // HSV bands are 0-255: shift H additively, scale S and V multiplicatively.
            final VImage adjusted = hsv.linear(
                    List.of(1.0, 1.0 + s, 1.0 + b),
                    List.of(h * 255.0, 0.0, 0.0));
            final VImage result = adjusted.HSV2sRGB();
            result.writeToFile(out.getAbsolutePath());
        });
    }
}
