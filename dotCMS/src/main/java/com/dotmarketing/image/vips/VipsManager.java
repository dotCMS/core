package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.Vips;
import app.photofox.vipsffm.VipsError;
import app.photofox.vipsffm.VipsHelper;
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
                    local = Try.of(() -> {
                        Vips.run(arena -> configureCache());
                        return Boolean.TRUE;
                    }).onFailure(e -> Logger.warn(VipsManager.class,
                            "libvips not available, falling back to pure-JVM image engine: " + e.getMessage()))
                            .getOrElse(Boolean.FALSE);
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
        return Config.getBooleanProperty(USE_LIBVIPS, false) && isAvailable();
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
            Vips.run(work::run);
        } catch (VipsError e) {
            throw new DotRuntimeException("libvips operation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Loads an image for full-frame (random access) operations such as crop, rotate and flip.
     */
    static VImage load(final Arena arena, final java.io.File file) {
        return VImage.newFromFile(arena, file.getAbsolutePath());
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
