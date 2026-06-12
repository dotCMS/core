package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import java.io.File;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.WebPImageFilter}: encodes WebP with a
 * quality factor, switching to lossless at {@code q >= 100}.
 */
public class VipsWebPImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {"q (int) between 0-100 specifies quality (100 = lossless)"};
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final int quality = intParam(parameters, "q", 85);
        final boolean lossless = quality >= 100;
        VipsManager.run(arena -> {
            final VImage src = VipsManager.load(arena, in);
            src.writeToFile(out.getAbsolutePath(),
                    VipsOption.Int("Q", quality),
                    VipsOption.Boolean("lossless", lossless));
        });
    }

    @Override
    public File getResultsFile(final File file, final Map<String, String[]> parameters) {
        return getResultsFile(file, parameters, "webp");
    }
}
