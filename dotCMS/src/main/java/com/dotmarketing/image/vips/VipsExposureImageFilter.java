package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.ExposureImageFilter}.
 *
 * <p>The legacy jhlabs exposure transfer is the nonlinear curve {@code 1 - exp(-x * exposure)}. This
 * implementation approximates it with a linear gain (multiplicative exposure) in sRGB space, which is
 * visually close for moderate values and avoids a per-pixel LUT. A future pass can build the exact
 * curve with an {@code identity} LUT + {@code math(EXP)} if byte-level fidelity is required.</p>
 */
public class VipsExposureImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {"exp (double) between 0 and 5.0"};
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final double exposure = doubleParam(parameters, "exp", 1d);
        final double gain = exposure <= 0 ? 1.0 : exposure;
        VipsManager.run(arena -> {
            final VImage src = VipsManager.load(arena, in);
            final VImage result = src.linear(List.of(gain), List.of(0.0),
                    app.photofox.vipsffm.VipsOption.Boolean("uchar", true));
            result.writeToFile(out.getAbsolutePath());
        });
    }
}
