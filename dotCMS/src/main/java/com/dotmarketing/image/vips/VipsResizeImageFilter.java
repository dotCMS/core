package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import app.photofox.vipsffm.enums.VipsSize;
import com.dotmarketing.image.filter.ResizeCalc;
import com.dotmarketing.util.UtilMethods;
import java.awt.Dimension;
import java.io.File;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.ResizeImageFilter}.
 *
 * <p>Target dimensions are computed with the exact same {@link ResizeCalc} the legacy filter uses, so
 * output geometry is byte-for-byte identical; only the resampling engine differs (libvips lanczos3
 * vs TwelveMonkeys). Animated GIFs are streamed through libvips with all pages preserved.</p>
 */
public class VipsResizeImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {
                "w (int) specifies width",
                "h (int) specifies height",
                "maxw (int) specifies maxWidth",
                "maxh (int) specifies maxHeight",
                "minw (int) specifies minWidth",
                "minh (int) specifies minHeight"
        };
    }

    /**
     * Mirror the legacy engine's format rule: a GIF source resizes to a GIF (preserving animation),
     * everything else to PNG. Without this, {@code runFilter} would inherit the PNG extension and
     * write an animated GIF's stacked page-strip as a single oversized PNG.
     */
    @Override
    public File getResultsFile(final File file, final Map<String, String[]> parameters) {
        if ("gif".equalsIgnoreCase(UtilMethods.getFileExtension(file.getName()))) {
            return getResultsFile(file, parameters, "gif");
        }
        return super.getResultsFile(file, parameters);
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final int w = intParam(parameters, "w", 0);
        final int h = intParam(parameters, "h", 0);
        final int mxw = intParam(parameters, "maxw", 0);
        final int mxh = intParam(parameters, "maxh", 0);
        final int mnw = intParam(parameters, "minw", 0);
        final int mnh = intParam(parameters, "minh", 0);

        final boolean animated = "gif".equalsIgnoreCase(UtilMethods.getFileExtension(in.getName()));

        VipsManager.run(arena -> {
            // Load (all pages for animated GIF) to read original dimensions.
            final VImage source = animated
                    ? VImage.newFromFile(arena, in.getAbsolutePath(), VipsOption.Int("n", -1))
                    : VImage.newFromFile(arena, in.getAbsolutePath());

            final int originalWidth = source.getWidth();
            // For animated images getHeight() returns total strip height; use page-height metadata.
            final int pageHeight = animated
                    ? VipsManager.metaInt(source, "page-height", source.getHeight())
                    : source.getHeight();

            final Dimension newSize = new ResizeCalc.Builder(new Dimension(originalWidth, pageHeight))
                    .desiredWidth(w)
                    .desiredHeight(h)
                    .maxWidth(mxw)
                    .maxHeight(mxh)
                    .minWidth(mnw)
                    .minHeight(mnh)
                    .build()
                    .getDim();

            if (newSize.width == originalWidth && newSize.height == pageHeight) {
                source.writeToFile(out.getAbsolutePath());
                return;
            }

            // thumbnailImage on an animated source resizes every page and preserves animation.
            final VImage resized = source.thumbnailImage(newSize.width,
                    VipsOption.Int("height", newSize.height),
                    VipsOption.Enum("size", VipsSize.SIZE_FORCE));
            resized.writeToFile(out.getAbsolutePath());
        });
    }
}
