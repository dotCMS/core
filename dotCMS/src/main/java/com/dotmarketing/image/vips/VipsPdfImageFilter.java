package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import java.io.File;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.PDFImageFilter}: renders a PDF page to a
 * raster image. Requires the host libvips to be built with the poppler (or pdfium) delegate.
 *
 * <p>{@code page} is 1-based (matching the legacy filter) and {@code dpi} controls render resolution.
 * libvips renders directly at the requested DPI rather than scaling a 72-dpi base.</p>
 */
public class VipsPdfImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {"page (int) 1-based page number", "dpi (float) render resolution"};
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final int page = Math.max(1, intParam(parameters, "page", 1));
        final double dpi = doubleParam(parameters, "dpi", 72d);
        VipsManager.run(arena -> {
            final VImage src = VImage.newFromFile(arena, in.getAbsolutePath(),
                    VipsOption.Int("page", page - 1),
                    VipsOption.Double("dpi", dpi));
            src.writeToFile(out.getAbsolutePath());
        });
    }
}
