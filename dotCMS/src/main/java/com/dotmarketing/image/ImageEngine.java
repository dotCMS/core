package com.dotmarketing.image;

import com.dotmarketing.image.filter.ImageFilterAPI;



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


    public static ImageFilterAPI resolve() {
        return ImageFilterAPI.apiInstance.apply();
    }
}
