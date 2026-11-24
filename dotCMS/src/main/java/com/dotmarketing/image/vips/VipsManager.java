package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.Vips;
import app.photofox.vipsffm.VipsError;
import app.photofox.vipsffm.VipsHelper;
import app.photofox.vipsffm.VipsOption;
import app.photofox.vipsffm.enums.VipsFailOn;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.lang.foreign.Arena;

/**
 * Central entry point for the libvips (vips-ffm) backed image engine.
 *
 * <p>libvips work must run inside a {@link Vips#run} block so that the {@link Arena} that owns all
 * native allocations is scoped to a single thread and released deterministically when the block
 * exits. Every {@link com.dotmarketing.image.vips.VipsImageFilter} performs its full
 * load &rarr; transform &rarr; write cycle inside one {@link #run(VipsWork)} call so that no native
 * {@code VImage} ever escapes its arena.</p>
 *
 * <p>This engine is only active when {@code IMAGE_API_USE_LIBVIPS=true} <b>and</b> the native
 * {@code libvips}/{@code glib}/{@code gobject} libraries are resolvable on the host. Availability is
 * probed once and memoized; if libvips cannot be loaded the caller is expected to fall back to the
 * pure-JVM {@link com.dotmarketing.image.filter.ImageFilterApiImpl}.</p>
 */
public final class VipsManager {

    private VipsManager() {}

    /** Work to run against a libvips {@link Arena}. {@link VipsError} is unchecked. */
    @FunctionalInterface
    public interface VipsWork {
        void run(Arena arena);
    }

    public static final String USE_LIBVIPS = "IMAGE_API_USE_LIBVIPS";

    private static volatile Boolean available;

    /**
     * @return true if the libvips native libraries could be initialized in this JVM. Probed once
     *         and cached; safe to call on a hot path.
     */
    public static boolean isAvailable() {
        Boolean local = available;
        if (local == null) {
            synchronized (VipsManager.class) {
                local = available;
                if (local == null) {
                    // Deliberately a plain try/catch rather than io.vavr Try: when native libvips is
                    // absent the class initializer of app.photofox.vipsffm.Vips fails with an
                    // ExceptionInInitializerError (and NoClassDefFoundError thereafter). Both are
                    // LinkageErrors, which vavr classifies as fatal and rethrows instead of
                    // capturing as a Failure — so the probe would propagate rather than fall back.
                    try {
                        Vips.run(arena -> configureCache());
                        local = Boolean.TRUE;
                    } catch (Throwable t) {
                        Logger.warn(VipsManager.class,
                                "libvips not available, falling back to pure-JVM image engine: " + t);
                        local = Boolean.FALSE;
                    }
                    available = local;
                }
            }
        }
        return local;
    }

    /**
     * @return true if libvips is both enabled by config and physically available.
     */
    public static boolean isEnabled() {
        return Config.getBooleanProperty(USE_LIBVIPS, true) && isAvailable();
    }

    /**
     * Applies optional libvips operation-cache tuning, once, when the engine first initializes.
     *
     * <p>The operation cache memoizes operation <i>results</i> across calls. For dotCMS — which serves
     * mostly unique transforms and keeps its own on-disk rendition cache ({@code dotGenerated}) — the
     * in-process cache rarely hits, so the libvips defaults are kept unless explicitly overridden.
     * Every knob defaults to "leave libvips' default"; set one to change it:</p>
     * <ul>
     *   <li>{@code IMAGE_LIBVIPS_CACHE_DISABLED=true} — turn the operation cache off entirely.</li>
     *   <li>{@code IMAGE_LIBVIPS_CACHE_MAX_MEM} — max cache memory in <b>bytes</b> (e.g. 268435456 = 256MB).</li>
     *   <li>{@code IMAGE_LIBVIPS_CACHE_MAX} — max number of cached operations.</li>
     *   <li>{@code IMAGE_LIBVIPS_CACHE_MAX_FILES} — max cached open files.</li>
     * </ul>
     * <p>Note: this is <b>not</b> a per-operation memory cap — a single operation's working buffers
     * are bounded by the image, not by this cache, and are freed when its arena closes.</p>
     */
    private static void configureCache() {
        // Disabled by default: dotCMS serves mostly unique transforms and keeps its own on-disk
        // rendition cache, so the in-process op-result cache rarely hits and mainly pins memory.
        // Set IMAGE_LIBVIPS_CACHE_DISABLED=false to re-enable libvips' default cache.
        if (Config.getBooleanProperty("IMAGE_LIBVIPS_CACHE_DISABLED", true)) {
            Try.run(Vips::disableOperationCache)
                    .onFailure(e -> Logger.warn(VipsManager.class, "unable to disable libvips op cache: " + e.getMessage()));
            Logger.info(VipsManager.class, "libvips operation cache disabled (default)");
            return;
        }
        final long maxMem = Config.getLongProperty("IMAGE_LIBVIPS_CACHE_MAX_MEM", -1L);
        if (maxMem >= 0) {
            Try.run(() -> VipsHelper.cache_set_max_mem(maxMem))
                    .onFailure(e -> Logger.warn(VipsManager.class, "unable to set libvips cache max mem: " + e.getMessage()));
            Logger.info(VipsManager.class, "libvips operation cache max mem set to " + maxMem + " bytes");
        }
        final int maxOps = Config.getIntProperty("IMAGE_LIBVIPS_CACHE_MAX", -1);
        if (maxOps >= 0) {
            Try.run(() -> VipsHelper.cache_set_max(maxOps))
                    .onFailure(e -> Logger.warn(VipsManager.class, "unable to set libvips cache max ops: " + e.getMessage()));
        }
        final int maxFiles = Config.getIntProperty("IMAGE_LIBVIPS_CACHE_MAX_FILES", -1);
        if (maxFiles >= 0) {
            Try.run(() -> VipsHelper.cache_set_max_files(maxFiles))
                    .onFailure(e -> Logger.warn(VipsManager.class, "unable to set libvips cache max files: " + e.getMessage()));
        }
    }

    /**
     * Runs the supplied work inside a libvips arena, translating native errors into
     * {@link DotRuntimeException} so callers see a consistent dotCMS exception type.
     */
    public static void run(final VipsWork work) {
        try {
            // libvips keeps one process-wide error buffer that is never reset between operations; clear
            // it so a failure reports this operation's error, not messages left by unrelated earlier work
            Try.run(VipsHelper::error_clear);
            Vips.run(work::run);
        } catch (VipsError e) {
            throw new DotRuntimeException("libvips operation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Load option that makes libvips fail on truncated input (a short file, or a read that stops
     * partway) instead of silently filling the missing scanlines with black/grey. Pass it to every
     * {@code VImage.newFromFile} and {@code VImage.thumbnail} call so a bad read reaches the error
     * path rather than the cache.
     *
     * <p>{@code truncated}, not {@code error}: {@code error} also rejects images browsers and the
     * previous engine render fine, e.g. a PNG with a bad IDAT checksum ({@code pngload: invalid chunk
     * checksum}). A JPEG that is complete but lacks its final EOI marker is still reported as
     * truncated by libjpeg; {@link #withMissingJpegEoiRepaired(java.io.File)} recovers that case.</p>
     */
    static final VipsOption FAIL_ON_TRUNCATED = VipsOption.Enum("fail_on", VipsFailOn.FAIL_ON_TRUNCATED);

    /**
     * Loads an image for full-frame (random access) operations such as crop, rotate and flip.
     */
    static VImage load(final Arena arena, final java.io.File file) {
        return VImage.newFromFile(arena, file.getAbsolutePath(), FAIL_ON_TRUNCATED);
    }

    /** Extensions whose libvips loader may hold several frames, so a full check decodes all of them. */
    private static final java.util.Set<String> MULTI_PAGE_EXTENSIONS = java.util.Set.of("gif", "webp");

    /**
     * Tells whether libvips has a loader for this file, judged from its content. When it does, a
     * failure to read the file is about the file's data (truncated or corrupt), not about an
     * unsupported format or a delegate (PDF, AVIF...) missing from this libvips build.
     *
     * @param file the image file
     * @return true when libvips is enabled and recognises the file's format
     */
    public static boolean hasLoader(final java.io.File file) {
        if (!isEnabled()) {
            return false;
        }
        final String[] loader = new String[1];
        return Try.run(() -> run(arena -> loader[0] = VipsHelper.foreign_find_load(arena, file.getAbsolutePath())))
                .map(ignored -> loader[0] != null && !loader[0].isEmpty())
                .getOrElse(false);
    }

    /**
     * Decodes every pixel of the file in libvips with {@code fail_on=truncated}, all frames for GIF and
     * WebP. The work stays in native memory: {@code avg()} walks the pixels and returns one number, so
     * nothing is copied into the JVM heap.
     *
     * <p>Known limit: a GIF cut off after its first complete frame still decodes. libvips' bundled
     * libnsgif turns "end of data" into success once one frame is complete ({@code nsgif_data_scan}),
     * so the file loads with fewer frames and no error at any {@code fail_on} level.</p>
     *
     * @param file the image file
     * @return true when libvips is enabled and decodes the whole image without error
     */
    public static boolean decodesFully(final java.io.File file) {
        if (!isEnabled()) {
            return false;
        }
        final String ext = com.dotmarketing.util.UtilMethods.getFileExtension(file.getName());
        final boolean multiPage = ext != null && MULTI_PAGE_EXTENSIONS.contains(ext.toLowerCase(java.util.Locale.ROOT));
        return Try.run(() -> run(arena -> (multiPage
                ? VImage.newFromFile(arena, file.getAbsolutePath(), VipsOption.Int("n", -1), FAIL_ON_TRUNCATED)
                : load(arena, file)).avg())).isSuccess();
    }

    /** Strictest load: any decoder warning fails. Used only to vet a repaired JPEG. */
    private static final VipsOption FAIL_ON_WARNING = VipsOption.Enum("fail_on", VipsFailOn.FAIL_ON_WARNING);

    /**
     * Recovers a JPEG that is complete but lacks its final EOI marker ({@code FFD9}), which libjpeg
     * reports exactly like a truncated file. A copy with the marker appended is decoded with
     * {@code fail_on=warning}: a complete image then decodes with no warning at all, while a file that
     * is really cut short still warns "premature end of data segment" and is rejected. (With the
     * marker appended, a cut-short file passes {@code fail_on=truncated} and {@code error}, so the
     * strict level is required here.) Only called after a load failed.
     *
     * @param file an input libvips could not load
     * @return a temporary repaired copy (the caller deletes it), or empty when the file is not a JPEG
     *         missing only its EOI marker
     */
    public static java.util.Optional<java.io.File> withMissingJpegEoiRepaired(final java.io.File file) {
        if (!isEnabled() || !endsWithoutJpegEoi(file)) {
            return java.util.Optional.empty();
        }
        final java.io.File repaired = Try.of(() -> java.io.File.createTempFile("dot-eoi-repaired_", ".jpg")).getOrNull();
        if (repaired == null) {
            return java.util.Optional.empty();
        }
        final boolean staged = Try.run(() -> {
            java.nio.file.Files.copy(file.toPath(), repaired.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            java.nio.file.Files.write(repaired.toPath(), new byte[]{(byte) 0xFF, (byte) 0xD9},
                    java.nio.file.StandardOpenOption.APPEND);
        }).isSuccess();
        if (!staged) {
            // never leave a partial copy behind when the copy or the append fails
            Try.run(() -> java.nio.file.Files.deleteIfExists(repaired.toPath()));
            return java.util.Optional.empty();
        }
        final boolean complete = Try.run(() -> run(arena ->
                VImage.newFromFile(arena, repaired.getAbsolutePath(), FAIL_ON_WARNING).avg())).isSuccess();
        if (complete) {
            return java.util.Optional.of(repaired);
        }
        Try.run(() -> java.nio.file.Files.deleteIfExists(repaired.toPath()));
        return java.util.Optional.empty();
    }

    /**
     * Tells whether a file starts like a JPEG (SOI marker) but does not end with the EOI marker.
     *
     * @param file the input file
     * @return true for a JPEG whose last two bytes are not {@code FF D9}
     */
    private static boolean endsWithoutJpegEoi(final java.io.File file) {
        return Try.of(() -> {
            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
                final long length = raf.length();
                if (length < 4) {
                    return false;
                }
                final boolean startsAsJpeg = raf.read() == 0xFF && raf.read() == 0xD8;
                raf.seek(length - 2);
                final boolean endsWithEoi = raf.read() == 0xFF && raf.read() == 0xD9;
                return startsAsJpeg && !endsWithEoi;
            }
        }).getOrElse(false);
    }

    /**
     * Null-safe read of an integer metadata field. libvips {@code getInt} returns {@code null} (not
     * an exception) when a field such as {@code n-pages} or {@code page-height} is absent, so callers
     * must guard against it.
     */
    static int metaInt(final VImage image, final String field, final int defaultValue) {
        try {
            final Integer value = image.getInt(field);
            return value == null ? defaultValue : value;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
