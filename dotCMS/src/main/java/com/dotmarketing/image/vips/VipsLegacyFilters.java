package com.dotmarketing.image.vips;

import com.dotmarketing.image.filter.CropImageFilter;
import com.dotmarketing.image.filter.ExposureImageFilter;
import com.dotmarketing.image.filter.FlipImageFilter;
import com.dotmarketing.image.filter.GammaImageFilter;
import com.dotmarketing.image.filter.GifImageFilter;
import com.dotmarketing.image.filter.GrayscaleImageFilter;
import com.dotmarketing.image.filter.HsbImageFilter;
import com.dotmarketing.image.filter.ImageFilter;
import com.dotmarketing.image.filter.JpegImageFilter;
import com.dotmarketing.image.filter.PDFImageFilter;
import com.dotmarketing.image.filter.PngImageFilter;
import com.dotmarketing.image.filter.ResizeImageFilter;
import com.dotmarketing.image.filter.RotateImageFilter;
import com.dotmarketing.image.filter.ScaleImageFilter;
import com.dotmarketing.image.filter.SubSampleImageFilter;
import com.dotmarketing.image.filter.ThumbnailImageFilter;
import com.dotmarketing.image.filter.WebPImageFilter;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Maps each canonical filter name to its legacy (pure-JVM) {@link ImageFilter}, so a libvips filter
 * can degrade gracefully to the original engine when the native operation fails (e.g. a corrupt
 * image, or a libvips build missing the PDF/AVIF delegate). The legacy filter honours the exact same
 * URL parameter contract, so the fallback is transparent to callers.
 */
final class VipsLegacyFilters {

    private VipsLegacyFilters() {}

    private static final Map<String, Supplier<ImageFilter>> BY_NAME =
            Map.ofEntries(
                    Map.entry("crop", CropImageFilter::new),
                    Map.entry("resize", ResizeImageFilter::new),
                    Map.entry("scale", ScaleImageFilter::new),
                    Map.entry("thumbnail", ThumbnailImageFilter::new),
                    Map.entry("rotate", RotateImageFilter::new),
                    Map.entry("flip", FlipImageFilter::new),
                    Map.entry("gamma", GammaImageFilter::new),
                    Map.entry("grayscale", GrayscaleImageFilter::new),
                    Map.entry("hsb", HsbImageFilter::new),
                    Map.entry("exposure", ExposureImageFilter::new),
                    Map.entry("subsample", SubSampleImageFilter::new),
                    Map.entry("jpeg", JpegImageFilter::new),
                    Map.entry("png", PngImageFilter::new),
                    Map.entry("webp", WebPImageFilter::new),
                    Map.entry("gif", GifImageFilter::new),
                    Map.entry("pdf", PDFImageFilter::new));

    static Optional<ImageFilter> forName(final String canonicalName) {
        final Supplier<ImageFilter> supplier = BY_NAME.get(canonicalName);
        return supplier == null ? Optional.empty() : Optional.of(supplier.get());
    }
}
