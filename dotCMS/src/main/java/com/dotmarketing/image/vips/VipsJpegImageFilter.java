package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.JpegImageFilter}: encodes JPEG with a
 * quality factor and optional progressive (interlaced) output. Alpha is flattened onto white since
 * JPEG has no alpha channel.
 */
public class VipsJpegImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {"q (int) quality 0-100", "p (present) progressive/interlaced"};
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final int quality = intParam(parameters, "q", 85);
        final boolean progressive = parameters.get(getPrefix() + "p") != null;
        VipsManager.run(arena -> {
            VImage src = VipsManager.load(arena, in);
            if (src.hasAlpha()) {
                src = src.flatten(VipsOption.ArrayDouble("background", List.of(255.0, 255.0, 255.0)));
            }
            src.writeToFile(out.getAbsolutePath(),
                    VipsOption.Int("Q", quality),
                    VipsOption.Boolean("interlace", progressive));
        });
    }

    @Override
    public File getResultsFile(final File file, final Map<String, String[]> parameters) {
        return getResultsFile(file, parameters, "jpg");
    }
}
