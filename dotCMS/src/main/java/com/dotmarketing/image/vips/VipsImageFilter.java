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
            io.vavr.control.Try.run(tempResultFile::delete);
            final java.util.Optional<File> repaired = VipsManager.withMissingJpegEoiRepaired(file);
            if (repaired.isPresent()) {
                return retryWithRepairedInput(file, repaired.get(), tempResultFile, resultFile, parameters, e);
            }
            return fallbackOrThrow(file, parameters, e, true);
        }
    }

    /**
     * Runs the transform again on a repaired copy of the input: a JPEG that is complete but was
     * missing its final EOI marker. The result keeps the name derived from the original input.
     *
     * @param file the original input
     * @param repaired the repaired temporary copy, deleted here
     * @param tempResultFile the staging file for the result
     * @param resultFile the rendition to publish
     * @param parameters the filter parameters
     * @param original the failure of the first attempt, reported if the retry also fails
     * @return the published rendition
     */
    private File retryWithRepairedInput(final File file, final File repaired, final File tempResultFile,
            final File resultFile, final Map<String, String[]> parameters, final Exception original) {
        try {
            Logger.warn(this.getClass(), "libvips filter '" + getFilterName() + "': " + file
                    + " is a complete JPEG without its end marker; rendering it with the marker restored");
            transform(repaired, tempResultFile, parameters);
            if (!tempResultFile.renameTo(resultFile)) {
                throw new DotRuntimeException("unable to create file: " + resultFile);
            }
            return resultFile;
        } catch (Exception retryFailure) {
            io.vavr.control.Try.run(tempResultFile::delete);
            // the input was verified as a complete JPEG, so this is not damaged data: keep the usual
            // legacy fallback (the legacy reader accepts a lone missing EOI marker)
            return fallbackOrThrow(file, parameters, retryFailure, false);
        } finally {
            io.vavr.control.Try.run(() -> java.nio.file.Files.deleteIfExists(repaired.toPath()));
        }
    }

    /**
     * When a libvips operation fails because of the engine (a delegate such as PDF/AVIF not built into
     * the host libvips, an operation bug), degrade to the equivalent legacy filter rather than serving
     * an error. This is enabled by default and can be disabled with {@code IMAGE_API_LIBVIPS_FALLBACK=false}.
     *
     * <p>No fallback when the input itself is damaged: libvips recognises its format but cannot decode
     * it ({@code fail_on=truncated}). The legacy engine would decode the same bad bytes into the JVM heap
     * only to fail again, so the error is raised directly.</p>
     *
     * @param file the input the failed filter was reading
     * @param parameters the filter parameters
     * @param cause the libvips failure
     * @param checkInput whether to check if the input is damaged before falling back; {@code false} when
     *                   the input is already known to be complete (a repaired JPEG)
     * @return the legacy filter's rendition
     */
    private File fallbackOrThrow(final File file, final Map<String, String[]> parameters, final Exception cause,
            final boolean checkInput) {
        final boolean fallbackEnabled =
                com.dotmarketing.util.Config.getBooleanProperty("IMAGE_API_LIBVIPS_FALLBACK", true);
        if (fallbackEnabled && checkInput && isDamagedInput(file)) {
            Logger.warnAndDebug(this.getClass(), "libvips filter '" + getFilterName() + "' failed for " + file
                    + "; the input does not decode, so the legacy engine is skipped: " + cause.getMessage(), cause);
            throw new DotRuntimeException("unable to convert file: " + file + " : " + cause.getMessage(), cause);
        }
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

    /**
     * Tells whether a failed filter's input is damaged rather than unsupported: libvips has a loader
     * for it but cannot decode every pixel. Runs only after a failure, entirely in native memory.
     *
     * @param file the input the failed filter was reading
     * @return true when the input is in a format libvips reads and does not decode fully
     */
    private static boolean isDamagedInput(final File file) {
        return VipsManager.hasLoader(file) && !VipsManager.decodesFully(file);
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
