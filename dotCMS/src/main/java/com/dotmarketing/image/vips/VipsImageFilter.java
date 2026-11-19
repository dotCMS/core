package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.image.filter.ImageFilter;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.util.Map;
import java.util.Set;

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

    /** Rendition extensions whose libvips loader accepts {@code n}, i.e. may hold several frames. */
    private static final Set<String> MULTI_PAGE_EXTENSIONS = Set.of("gif", "webp");

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
            assertFullyDecodable(tempResultFile);
            if (!tempResultFile.renameTo(resultFile)) {
                throw new DotRuntimeException("unable to create file: " + resultFile);
            }
            return resultFile;
        } catch (Exception e) {
            io.vavr.control.Try.run(tempResultFile::delete);
            return fallbackOrThrow(file, parameters, e);
        }
    }

    /**
     * Re-reads a freshly written rendition and decodes every pixel before it is published to the
     * cache, so a partial or corrupt write never reaches {@code dotGenerated}. A header-only open is
     * not enough: {@code avg()} forces libvips to walk every scanline, and {@code fail_on=error} turns
     * any decode error or truncation into a {@link DotRuntimeException}.
     *
     * <p>Multi-page formats (animated GIF/WebP) are loaded with {@code n=-1} so every frame is
     * decoded: a truncated write loses the last frames first, which a frame-0 check would miss.</p>
     *
     * @param rendition the written temp file about to be renamed into place
     */
    private static void assertFullyDecodable(final File rendition) {
        if (isMultiPageFormat(rendition)) {
            VipsManager.run(arena -> VImage.newFromFile(arena, rendition.getAbsolutePath(),
                    VipsOption.Int("n", -1), VipsManager.FAIL_ON_ERROR).avg());
        } else {
            VipsManager.run(arena -> VipsManager.load(arena, rendition).avg());
        }
    }

    /**
     * @param rendition a written rendition
     * @return true when its extension names a format whose libvips loader accepts {@code n} (pages)
     */
    private static boolean isMultiPageFormat(final File rendition) {
        final String ext = com.dotmarketing.util.UtilMethods.getFileExtension(rendition.getName());
        return MULTI_PAGE_EXTENSIONS.contains(ext == null ? "" : ext.toLowerCase());
    }

    /**
     * When a libvips operation fails (corrupt image, a delegate such as PDF/AVIF not built into the
     * host libvips, etc.) degrade to the equivalent legacy filter rather than serving an error. This
     * is enabled by default and can be disabled with {@code IMAGE_API_LIBVIPS_FALLBACK=false}.
     */
    private File fallbackOrThrow(final File file, final Map<String, String[]> parameters, final Exception cause) {
        final boolean fallbackEnabled =
                com.dotmarketing.util.Config.getBooleanProperty("IMAGE_API_LIBVIPS_FALLBACK", true);
        if (fallbackEnabled) {
            final java.util.Optional<com.dotmarketing.image.filter.ImageFilter> legacy =
                    VipsLegacyFilters.forName(getFilterName());
            if (legacy.isPresent()) {
                Logger.warnAndDebug(this.getClass(),
                        "libvips filter '" + getFilterName() + "' failed for " + file
                                + "; falling back to legacy engine: " + cause.getMessage(), cause);
                return legacy.get().runFilter(file, parameters);
            }
        }
        Logger.warnAndDebug(this.getClass(), "libvips filter failed for " + file + ": " + cause.getMessage(), cause);
        throw new DotRuntimeException("unable to convert file: " + file + " : " + cause.getMessage(), cause);
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
