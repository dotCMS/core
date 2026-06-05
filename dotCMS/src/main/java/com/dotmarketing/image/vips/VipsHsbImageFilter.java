package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
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
        // Documented range is -1.0..1.0; clamp so out-of-range input can't produce negated bands.
        final double h = clamp(doubleParam(parameters, "h", 0d));
        final double s = clamp(doubleParam(parameters, "s", 0d));
        final double b = clamp(doubleParam(parameters, "b", 0d));
        VipsManager.run(arena -> {
            final VImage src = VipsManager.load(arena, in).colourspace(VipsInterpretation.INTERPRETATION_sRGB);
            // sRGB2HSV expects a 3-band image, so split off any alpha and re-attach it afterwards
            // (passing 3-element linear() coefficients to a 4-band image would otherwise throw).
            final boolean hasAlpha = src.hasAlpha();
            final int bands = VipsManager.metaInt(src, "bands", hasAlpha ? 4 : 3);
            final VImage colour = hasAlpha
                    ? src.extractBand(0, VipsOption.Int("n", bands - 1))
                    : src;
            final VImage alpha = hasAlpha ? src.extractBand(bands - 1) : null;

            final VImage hsv = colour.sRGB2HSV();
            // HSV bands are 0-255: shift H additively, scale S and V multiplicatively.
            final VImage adjusted = hsv.linear(
                    List.of(1.0, 1.0 + s, 1.0 + b),
                    List.of(h * 255.0, 0.0, 0.0));
            final VImage rgb = adjusted.HSV2sRGB();
            final VImage result = hasAlpha ? VImage.bandjoin(arena, List.of(rgb, alpha)) : rgb;
            result.writeToFile(out.getAbsolutePath());
        });
    }

    private static double clamp(final double v) {
        return Math.max(-1.0, Math.min(1.0, v));
    }
}
