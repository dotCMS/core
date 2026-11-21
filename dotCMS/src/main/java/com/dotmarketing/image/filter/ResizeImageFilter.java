package com.dotmarketing.image.filter;

import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import javax.imageio.ImageIO;

public class ResizeImageFilter extends ImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[]{"w (int) specifies width", "h (int) specifies height",
                "i (int) interpolation", "maxw (int) specifies maxWidth",
                "maxh (int) specifies maxHeight", "minw (int) specifies minWidth",
                "minh (int) specifies minHeight"};
    }

    /** Resolve a width-only or height-only request using source metadata, not decoded pixels. */
    @Override
    public File prepare(final File file, final File metadataSource,
            final Map<String, String[]> parameters) {
        if (!metadataSource.isFile()) {
            return null;
        }
        final Dimension source = ImageFilterAPI.apiInstance.get().getWidthHeight(metadataSource);
        final Dimension target = ResizeCalc.target(source, getPrefix(), parameters);
        ResizeCalc.normalize(target, getPrefix(), parameters);
        return source.equals(target) && !isGif(file) ? file : getResultsFile(file, parameters);
    }

    /** Generate the bounded rendition, using the GIF encoder when animation must be preserved. */
    @Override
    public File runFilter(final File file, final Map<String, String[]> parameters) {
        final Dimension original = ImageFilterAPI.apiInstance.get().getWidthHeight(file);
        final Dimension target = ResizeCalc.target(original, getPrefix(), parameters);
        if (original.equals(target) && !isGif(file)) {
            return file;
        }
        final File result = getResultsFile(file, parameters);
        if (!overwrite(result, parameters)) {
            return result;
        }

        if (isGif(file)) {
            return new ResizeGifImageFilter().resize(file, result, target, getPrefix(), parameters);
        }

        final int resample = Try.of(() -> Integer.parseInt(parameters.get(getPrefix() + "ro")[0]))
                .getOrElse(ImageFilterApiImpl.DEFAULT_RESAMPLE_OPT);
        final File tmp = new File(result.getAbsolutePath() + "_" + System.nanoTime() + ".tmp");
        try {
            final BufferedImage image = ImageFilterAPI.apiInstance.get()
                    .intelligentResize(file, target.width, target.height, resample);
            try {
                ImageIO.write(image, FILE_EXT, tmp);
            } finally {
                image.flush();
            }
            if (tmp.renameTo(result)) {
                return result;
            }
            throw new DotRuntimeException("unable to create file: " + result);
        } catch (Exception e) {
            throw new DotRuntimeException("unable to convert file: " + file, e);
        } finally {
            tmp.delete();
        }
    }

    /** Only actual GIFs use the animated writer. */
    private static boolean isGif(final File file) {
        return "gif".equalsIgnoreCase(com.dotmarketing.util.UtilMethods.getFileExtension(file.getName()));
    }

    /** Use the same rendition path in the cache lookup and in the GIF writer. */
    @Override
    public File getResultsFile(final File file, final Map<String, String[]> parameters) {
        return getResultsFile(file, parameters, isGif(file) ? "gif" : FILE_EXT);
    }
}
