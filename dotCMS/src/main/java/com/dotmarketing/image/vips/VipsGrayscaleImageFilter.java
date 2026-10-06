package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.enums.VipsInterpretation;
import java.io.File;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.GrayscaleImageFilter}: converts to the
 * single-band {@code B_W} colourspace.
 */
public class VipsGrayscaleImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {"none"};
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        VipsManager.run(arena -> {
            final VImage src = VipsManager.load(arena, in);
            final VImage gray = src.colourspace(VipsInterpretation.INTERPRETATION_B_W);
            gray.writeToFile(out.getAbsolutePath());
        });
    }
}
