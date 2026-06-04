package com.dotmarketing.image.vips;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.image.filter.ImageFilter;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.util.Map;

/**
 * Base class for all libvips-backed filters.
 *
 * <p>It reuses every piece of the legacy {@link ImageFilter} machinery — unique cache-file naming,
 * the {@code overwrite()} short-circuit, request-cost accounting and the URL parameter contract — so
 * a libvips filter is a true drop-in for its AWT counterpart. The only behavioural change is the
 * pixel work, which subclasses provide by overriding {@link #transform(File, File, Map)}.</p>
 *
 * <p>{@link #getFilterName()} is overridden to strip the leading {@code "vips"} from the simple class
 * name so that {@code VipsCropImageFilter} still answers to the canonical {@code "crop"} key and the
 * {@code crop_w} / {@code crop_h} URL parameters resolve unchanged.</p>
 */
public abstract class VipsImageFilter extends ImageFilter {

    /**
     * Performs the actual libvips transform, reading {@code in} and writing the finished image to
     * {@code out}. Package/subclass visible and free of cache-path concerns so it can be unit tested
     * directly against explicit temp files.
     */
    protected abstract void transform(File in, File out, Map<String, String[]> parameters);

    /**
     * The canonical filter name with the {@code vips} prefix removed, e.g. {@code VipsCropImageFilter
     * -> "crop"}. This keeps the parameter prefix ({@code crop_}) identical to the legacy engine.
     */
    @Override
    protected String getFilterName() {
        final String name = this.getClass().getSimpleName().replaceAll("ImageFilter", "").toLowerCase();
        return name.startsWith("vips") ? name.substring("vips".length()) : name;
    }

    /**
     * Standard run loop shared by every libvips filter: resolve the cache target, honour the
     * overwrite short-circuit, transform into a temp file and atomically rename into place.
     */
    @Override
    public File runFilter(final File file, final Map<String, String[]> parameters) {
        final File resultFile = getResultsFile(file, parameters);
        if (!overwrite(resultFile, parameters)) {
            return resultFile;
        }
        // libvips infers the encoder from the file suffix, so the temp file must carry the
        // result file's real extension (png/jpg/webp/gif), not a generic ".tmp".
        final String ext = com.dotmarketing.util.UtilMethods.getFileExtension(resultFile.getName());
        final File tempResultFile =
                new File(resultFile.getAbsoluteFile() + "_" + System.nanoTime() + "." + ext);
        try {
            transform(file, tempResultFile, parameters);
            if (!tempResultFile.renameTo(resultFile)) {
                throw new DotRuntimeException("unable to create file: " + resultFile);
            }
            return resultFile;
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), "libvips filter failed for " + file + ": " + e.getMessage(), e);
            io.vavr.control.Try.run(tempResultFile::delete);
            throw new DotRuntimeException("unable to convert file: " + file + " : " + e.getMessage(), e);
        }
    }

    /** Parse an int parameter under this filter's prefix, with a default. */
    protected int intParam(final Map<String, String[]> parameters, final String key, final int defaultValue) {
        final String[] value = parameters.get(getPrefix() + key);
        return value != null && value.length > 0
                ? io.vavr.control.Try.of(() -> Integer.parseInt(value[0])).getOrElse(defaultValue)
                : defaultValue;
    }

    /** Parse a double parameter under this filter's prefix, with a default. */
    protected double doubleParam(final Map<String, String[]> parameters, final String key, final double defaultValue) {
        final String[] value = parameters.get(getPrefix() + key);
        return value != null && value.length > 0
                ? io.vavr.control.Try.of(() -> Double.parseDouble(value[0])).getOrElse(defaultValue)
                : defaultValue;
    }
}
