package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import java.io.File;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.GifImageFilter}: re-encodes as GIF,
 * preserving all frames of an animated source ({@code n=-1}).
 */
public class VipsGifImageFilter extends VipsImageFilter {

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        VipsManager.run(arena -> {
            final VImage src = VImage.newFromFile(arena, in.getAbsolutePath(), VipsOption.Int("n", -1));
            src.writeToFile(out.getAbsolutePath());
        });
    }

    @Override
    public File getResultsFile(final File file, final Map<String, String[]> parameters) {
        return getResultsFile(file, parameters, "gif");
    }
}
