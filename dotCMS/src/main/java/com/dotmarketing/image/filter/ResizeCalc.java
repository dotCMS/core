package com.dotmarketing.image.filter;

import com.dotmarketing.util.Config;
import java.awt.Dimension;
import java.util.Map;
import javax.annotation.Nonnull;

/** Computes a proportional rendition without allocating image pixels. */
public class ResizeCalc {

    private static final int DEFAULT_MAX_SIDE = 5_000;
    private static final long DEFAULT_MAX_PIXELS = 16_000_000L;
    private static final String[] SIZE_KEYS = {"w", "h", "maxw", "maxh", "minw", "minh"};

    private final int originalWidth;
    private final int originalHeight;
    private final int desiredWidth;
    private final int desiredHeight;
    private final int maxWidth;
    private final int maxHeight;
    private final int minWidth;
    private final int minHeight;

    private ResizeCalc(Builder builder) {
        this.originalWidth = builder.originalWidth;
        this.originalHeight = builder.originalHeight;
        this.desiredWidth = builder.desiredWidth;
        this.desiredHeight = builder.desiredHeight;
        this.maxWidth = builder.maxWidth;
        this.maxHeight = builder.maxHeight;
        this.minWidth = builder.minWidth;
        this.minHeight = builder.minHeight;
    }

    /** Resolve the six resize parameters using the same rules in both engines. */
    public static Dimension target(final Dimension source, final String prefix,
            final Map<String, String[]> parameters) {
        return new Builder(source)
                .desiredWidth(read(parameters, prefix + "w"))
                .desiredHeight(read(parameters, prefix + "h"))
                .maxWidth(read(parameters, prefix + "maxw"))
                .maxHeight(read(parameters, prefix + "maxh"))
                .minWidth(read(parameters, prefix + "minw"))
                .minHeight(read(parameters, prefix + "minh"))
                .build().getDim();
    }

    /** Put the actual size in the cache key, independent of how it was requested. */
    public static void normalize(final Dimension target, final String prefix,
            final Map<String, String[]> parameters) {
        for (final String key : SIZE_KEYS) {
            parameters.remove(prefix + key);
        }
        // An exact size without this marker might refer to a pre-limit rendition.
        parameters.put(prefix + "bounded", new String[]{"1"});
        parameters.put(prefix + "w", new String[]{Integer.toString(target.width)});
        parameters.put(prefix + "h", new String[]{Integer.toString(target.height)});
    }

    /** Parse a dimension; absent or invalid constraints are treated as unspecified. */
    private static int read(final Map<String, String[]> parameters, final String key) {
        final String[] value = parameters.get(key);
        if (value == null || value.length == 0) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value[0]));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /** Apply the tighter per-axis and total-pixel budgets to the requested size. */
    public Dimension getDim() {
        if (originalWidth < 1 || originalHeight < 1) {
            throw new IllegalArgumentException("Missing source dimensions");
        }
        if (desiredWidth > 0 && desiredHeight > 0) {
            return fit(desiredWidth, desiredHeight);
        }
        if (desiredWidth > 0) {
            return fit(desiredWidth, (double) desiredWidth * originalHeight / originalWidth);
        }
        if (desiredHeight > 0) {
            return fit((double) desiredHeight * originalWidth / originalHeight, desiredHeight);
        }

        if (maxWidth > 0 || maxHeight > 0) {
            if (maxWidth > 0 && maxWidth < originalWidth
                    && (maxHeight <= 0 || (long) maxWidth * originalHeight <= (long) maxHeight * originalWidth)) {
                return fit(maxWidth, (double) maxWidth * originalHeight / originalWidth);
            }
            if (maxHeight > 0 && maxHeight < originalHeight) {
                return fit((double) maxHeight * originalWidth / originalHeight, maxHeight);
            }
        } else {
            if (minWidth > originalWidth
                    && (long) minWidth * originalHeight >= (long) minHeight * originalWidth) {
                return fit(minWidth, (double) minWidth * originalHeight / originalWidth);
            }
            if (minHeight > originalHeight) {
                return fit((double) minHeight * originalWidth / originalHeight, minHeight);
            }
        }
        return fit(originalWidth, originalHeight);
    }

    /** Scale both axes together before converting to integer pixels. */
    private static Dimension fit(final double width, final double height) {
        final int legacyMax = positive("IMAGE_MAX_PIXEL_SIZE", DEFAULT_MAX_SIDE);
        final int widthLimit = Math.min(legacyMax, positive("image.resize.max.width", DEFAULT_MAX_SIDE));
        final int heightLimit = Math.min(legacyMax, positive("image.resize.max.height", DEFAULT_MAX_SIDE));
        final long configuredPixels = Config.getLongProperty("image.resize.max.pixels", DEFAULT_MAX_PIXELS);
        final long pixelLimit = configuredPixels > 0 ? configuredPixels : DEFAULT_MAX_PIXELS;

        // A very thin image needs at least one pixel on its short axis.
        final double scale = Math.min(1, Math.min(Math.min(Math.min(widthLimit, pixelLimit) / width,
                Math.min(heightLimit, pixelLimit) / height), Math.sqrt(pixelLimit / (width * height))));
        final int fittedWidth = Math.clamp((long) Math.nextUp(width * scale), 1, widthLimit);
        final int fittedHeight = Math.clamp((long) Math.nextUp(height * scale), 1, heightLimit);
        return new Dimension(fittedWidth, (int) Math.min(fittedHeight, pixelLimit / fittedWidth));
    }

    /** Non-positive configuration must not disable a dimension limit. */
    private static int positive(final String key, final int defaultValue) {
        final int value = Config.getIntProperty(key, defaultValue);
        return value > 0 ? value : defaultValue;
    }

    public static final class Builder {
        private int originalWidth;
        private int originalHeight;
        private int desiredWidth;
        private int desiredHeight;
        private int maxWidth;
        private int maxHeight;
        private int minWidth;
        private int minHeight;

        public Builder(int originalWidth, int originalHeight) {
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
        }

        public Builder(Dimension dim) {
            this.originalWidth = dim.width;
            this.originalHeight = dim.height;
        }


        public Builder desiredWidth(@Nonnull int desiredWidth) {
            this.desiredWidth = desiredWidth;
            return this;
        }

        public Builder desiredHeight(@Nonnull int desiredHeight) {
            this.desiredHeight = desiredHeight;
            return this;
        }

        public Builder maxWidth(@Nonnull int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public Builder maxHeight(@Nonnull int maxHeight) {
            this.maxHeight = maxHeight;
            return this;
        }
        public Builder minWidth(@Nonnull int minWidth) {
            this.minWidth = minWidth;
            return this;
        }

        public Builder minHeight(@Nonnull int minHeight) {
            this.minHeight = minHeight;
            return this;
        }
        public ResizeCalc build() {
            return new ResizeCalc(this);
        }
    }



}
