package com.dotmarketing.image;

import com.dotmarketing.image.filter.ImageFilterAPI;
import com.dotmarketing.image.vips.VipsImageFilterApiImpl;
import com.dotmarketing.image.vips.VipsManager;
import io.vavr.Function0;

/**
 * Single selection point for the active {@link ImageFilterAPI} implementation.
 *
 * <p>Returns the libvips engine when the {@code IMAGE_API_USE_LIBVIPS} feature flag is on and native
 * libvips is available, otherwise the pure-JVM engine. Every call-site that wants to honour the flag
 * — the image exporter, metadata generation, the velocity binary view tool — should resolve through
 * here rather than referencing {@link ImageFilterAPI#apiInstance} directly (which is always the
 * legacy engine).</p>
 */
public final class ImageEngine {

    private ImageEngine() {}

    private static final Function0<VipsImageFilterApiImpl> VIPS =
            Function0.of(VipsImageFilterApiImpl::new).memoized();

    /**
     * @return the libvips engine if enabled and available, else the legacy pure-JVM engine.
     */
    public static ImageFilterAPI resolve() {
        return VipsManager.isEnabled() ? VIPS.apply() : ImageFilterAPI.apiInstance.apply();
    }
}
