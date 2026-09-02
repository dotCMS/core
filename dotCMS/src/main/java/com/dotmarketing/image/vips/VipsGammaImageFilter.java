package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import java.io.File;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.GammaImageFilter}. Applies a gamma
 * curve via libvips {@code gamma}; the {@code g} parameter is the exponent.
 */
public class VipsGammaImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {"g (double) between 0 and 3.0"};
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final double g = doubleParam(parameters, "g", 1d);
        VipsManager.run(arena -> {
            final VImage src = VipsManager.load(arena, in);
            final VImage result = g > 0 ? src.gamma(VipsOption.Double("exponent", g)) : src.gamma();
            result.writeToFile(out.getAbsolutePath());
        });
    }
}
