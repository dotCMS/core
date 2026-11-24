package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import app.photofox.vipsffm.enums.VipsSize;
import com.dotmarketing.image.filter.ImageFilterAPI;
import com.dotmarketing.image.filter.ResizeCalc;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import java.awt.Dimension;
import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.ResizeImageFilter}.
 *
 * <p>Target dimensions use the legacy {@link ResizeCalc}, with static images first normalised to
 * their displayed EXIF orientation. Animated GIFs are streamed through libvips with all pages
 * preserved.</p>
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

    /** Read display dimensions before looking up a rendition. */
    @Override
    public File prepare(final File file, final File metadataSource,
            final Map<String, String[]> parameters) {
        if (!metadataSource.isFile()) {
            return null;
        }
        final boolean gif = "gif".equalsIgnoreCase(UtilMethods.getFileExtension(file.getName()));
        final AtomicReference<Dimension> size = new AtomicReference<>();
        final AtomicBoolean upright = new AtomicBoolean();
        try {
            VipsManager.run(arena -> {
                final VImage image = VImage.newFromFile(arena, metadataSource.getAbsolutePath());
                final int orientation = gif ? 1 : VipsManager.metaInt(image, "orientation", 1);
                upright.set(orientation == 1);
                size.set(orientation >= 5 && orientation <= 8
                        ? new Dimension(image.getHeight(), image.getWidth())
                        : new Dimension(image.getWidth(), image.getHeight()));
            });
        } catch (Exception e) {
            if (!Config.getBooleanProperty("IMAGE_API_LIBVIPS_FALLBACK", true)) {
                throw e;
            }
            size.set(ImageFilterAPI.apiInstance.get().getWidthHeight(metadataSource));
        }
        final Dimension target = ResizeCalc.target(size.get(), getPrefix(), parameters);
        ResizeCalc.normalize(target, getPrefix(), parameters);
        return !gif && upright.get() && size.get().equals(target) ? file : getResultsFile(file, parameters);
    }

    /**
     * Resizes static images using upright dimensions so EXIF rotation cannot distort their aspect
     * ratio. Animated GIFs retain their page-strip geometry and animation metadata.
     *
     * @param in source image
     * @param out resized image
     * @param parameters prefixed resize or scale constraints
     */
    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final boolean animated = "gif".equalsIgnoreCase(UtilMethods.getFileExtension(in.getName()));

        VipsManager.run(arena -> {
            // Normalise static-image orientation before ResizeCalc and the no-resampling path.
            final VImage source = animated
                    ? VImage.newFromFile(arena, in.getAbsolutePath(), VipsOption.Int("n", -1))
                    : VImage.newFromFile(arena, in.getAbsolutePath()).autorot();

            final int originalWidth = source.getWidth();
            // For animated images getHeight() returns total strip height; use page-height metadata.
            final int pageHeight = animated
                    ? VipsManager.metaInt(source, "page-height", source.getHeight())
                    : source.getHeight();

            final Dimension newSize = ResizeCalc.target(new Dimension(originalWidth, pageHeight),
                    getPrefix(), parameters);

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
