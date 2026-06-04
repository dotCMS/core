package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.Vips;
import app.photofox.vipsffm.VipsError;
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
                        Vips.run(arena -> {});
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
