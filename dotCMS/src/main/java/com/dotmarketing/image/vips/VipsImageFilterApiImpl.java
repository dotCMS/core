package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import app.photofox.vipsffm.enums.VipsSize;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.image.filter.FocalPointImageFilter;
import com.dotmarketing.image.filter.ImageFilter;
import com.dotmarketing.image.filter.ImageFilterAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

/**
 * libvips (vips-ffm) backed implementation of {@link ImageFilterAPI}.
 *
 * <p>This is a drop-in alternative to {@link com.dotmarketing.image.filter.ImageFilterApiImpl},
 * selected at runtime by the {@code IMAGE_API_USE_LIBVIPS} feature flag (see {@link VipsManager}).
 * It preserves the exact URL parameter contract — the same filter keys ({@code crop}, {@code resize},
 * …) and prefixed parameters ({@code crop_w}, {@code resize_w}, …) — by mapping each key to a
 * libvips filter that subclasses the legacy {@link ImageFilter} base.</p>
 *
 * <p>The {@code focalpoint} filter is engine-agnostic (it only writes metadata) and is reused from
 * the legacy package unchanged.</p>
 */
public class VipsImageFilterApiImpl implements ImageFilterAPI {

    /**
     * Anything w or h greater than this pixel size will be shrunk down to this (OOM guard parity).
     */
    private static final int MAX_SIZE =
            Try.of(() -> Config.getIntProperty("IMAGE_MAX_PIXEL_SIZE", 5000)).getOrElse(5000);

    private static final String FILTER = "filter";
    private static final String FILTERS = "filters";

    /** Canonical filter key -> libvips filter class. Mirrors {@code ImageFilterApiImpl.filterClasses}. */
    protected static final Map<String, Class<? extends ImageFilter>> filterClasses =
            new ImmutableMap.Builder<String, Class<? extends ImageFilter>>()
                    .put(CROP, VipsCropImageFilter.class)
                    .put(EXPOSURE, VipsExposureImageFilter.class)
                    .put(FLIP, VipsFlipImageFilter.class)
                    .put(FOCAL_POINT, FocalPointImageFilter.class)
                    .put(GAMMA, VipsGammaImageFilter.class)
                    .put(GIF, VipsGifImageFilter.class)
                    .put(GRAY_SCALE, VipsGrayscaleImageFilter.class)
                    .put(HSB, VipsHsbImageFilter.class)
                    .put(JPEG, VipsJpegImageFilter.class)
                    .put(JPG, VipsJpegImageFilter.class)
                    .put(PDF, VipsPdfImageFilter.class)
                    .put(PNG, VipsPngImageFilter.class)
                    .put(RESIZE, VipsResizeImageFilter.class)
                    .put(SCALE, VipsScaleImageFilter.class)
                    .put(THUMBNAIL, VipsThumbnailImageFilter.class)
                    .put(THUMB, VipsThumbnailImageFilter.class)
                    .put(WEBP, VipsWebPImageFilter.class)
                    .build();

    public VipsImageFilterApiImpl() {
        // Probe availability eagerly so misconfiguration surfaces at construction, not first request.
        if (!VipsManager.isAvailable()) {
            Logger.warn(this, "VipsImageFilterApiImpl constructed but native libvips is not available.");
        }
    }

    @Override
    public Map<String, Class<? extends ImageFilter>> resolveFilters(final Map<String, String[]> parameters) {
        final List<String> filters = new ArrayList<>();
        if (parameters.containsKey(FILTER)) {
            filters.addAll(Arrays.asList(parameters.get(FILTER)[0].toLowerCase().split(StringPool.COMMA)));
        } else if (parameters.get(FILTERS) != null) {
            filters.addAll(Arrays.asList(parameters.get(FILTERS)[0].toLowerCase().split(StringPool.COMMA)));
        }
        parameters.forEach((key, value) -> {
            if (key.contains(StringPool.UNDERLINE)) {
                final String filter = key.substring(0, key.indexOf(StringPool.UNDERLINE));
                if (!filters.contains(filter)) {
                    filters.add(filter);
                }
            }
        });

        final Map<String, Class<? extends ImageFilter>> classes = new LinkedHashMap<>();
        filters.forEach(s -> {
            final String filter = s.toLowerCase();
            if (!classes.containsKey(filter) && filterClasses.containsKey(filter)) {
                classes.put(filter, filterClasses.get(filter));
            }
        });
        return classes;
    }

    @Override
    public Dimension getWidthHeight(final File imageFile) {
        if (imageFile == null) {
            throw new DotRuntimeException("imageFile is null");
        }
        final AtomicReference<Dimension> dim = new AtomicReference<>();
        try {
            VipsManager.run(arena -> {
                // libvips reads only the header here; SVG is rendered natively via librsvg.
                final VImage img = VImage.newFromFile(arena, imageFile.getAbsolutePath());
                final boolean animated = VipsManager.metaInt(img, "n-pages", 1) > 1;
                final int height = animated
                        ? VipsManager.metaInt(img, "page-height", img.getHeight())
                        : img.getHeight();
                dim.set(new Dimension(img.getWidth(), height));
            });
            return dim.get();
        } catch (Exception e) {
            // Degrade to the legacy reader if libvips cannot open this file (e.g. missing delegate).
            if (Config.getBooleanProperty("IMAGE_API_LIBVIPS_FALLBACK", true)) {
                Logger.warnAndDebug(this.getClass(), "libvips getWidthHeight failed for " + imageFile.getName()
                        + "; falling back to legacy engine: " + e.getMessage(), e);
                return ImageFilterAPI.apiInstance.apply().getWidthHeight(imageFile);
            }
            throw e;
        }
    }

    @Override
    public BufferedImage resizeImage(final File imageFile, final int width, final int height) {
        return resizeImage(imageFile, width, height, DEFAULT_RESAMPLE_OPT_PLACEHOLDER);
    }

    @Override
    public BufferedImage resizeImage(final File imageFile, final int width, final int height, final int resampleOption) {
        final int w = Math.min(MAX_SIZE, width);
        final int h = Math.min(MAX_SIZE, height);
        final AtomicReference<BufferedImage> ref = new AtomicReference<>();
        VipsManager.run(arena -> {
            final VImage resized = VImage.thumbnail(arena, imageFile.getAbsolutePath(), w,
                    VipsOption.Int("height", h),
                    VipsOption.Enum("size", VipsSize.SIZE_FORCE));
            ref.set(toBufferedImage(resized));
        });
        return ref.get();
    }

    @Override
    public BufferedImage resizeImage(final BufferedImage srcImage, final int width, final int height) {
        return resizeImage(srcImage, width, height, DEFAULT_RESAMPLE_OPT_PLACEHOLDER);
    }

    @Override
    public BufferedImage resizeImage(final BufferedImage srcImage, final int width, final int height,
            final int resampleOption) {
        final File temp = writeTempPng(srcImage);
        try {
            return resizeImage(temp, width, height, resampleOption);
        } finally {
            Try.run(temp::delete);
        }
    }

    @Override
    public BufferedImage subsampleImage(final File image, final int width, final int height) {
        final AtomicReference<BufferedImage> ref = new AtomicReference<>();
        VipsManager.run(arena -> {
            final VImage thumb = width > 0 && height > 0
                    ? VImage.thumbnail(arena, image.getAbsolutePath(), width, VipsOption.Int("height", height))
                    : VImage.thumbnail(arena, image.getAbsolutePath(), Math.max(width, height));
            ref.set(toBufferedImage(thumb));
        });
        return ref.get();
    }

    @Override
    public BufferedImage intelligentResize(final File incomingImage, final int width, final int height) {
        return intelligentResize(incomingImage, width, height, DEFAULT_RESAMPLE_OPT_PLACEHOLDER);
    }

    @Override
    public BufferedImage intelligentResize(final File incomingImage, final int width, final int height,
            final int resampleOption) {
        // libvips thumbnail streams the source, so there is no OOM risk on huge inputs and no need
        // for a separate subsample pre-pass — we only clamp to MAX_SIZE for parity.
        return resizeImage(incomingImage, Math.min(MAX_SIZE, width), Math.min(MAX_SIZE, height), resampleOption);
    }

    /** Convert a libvips image to a {@link BufferedImage} via an in-memory PNG round-trip. */
    private BufferedImage toBufferedImage(final VImage image) {
        try {
            final File temp = File.createTempFile("vips-bridge", ".png");
            try {
                image.writeToFile(temp.getAbsolutePath());
                return ImageIO.read(temp);
            } finally {
                Try.run(temp::delete);
            }
        } catch (Exception e) {
            throw new DotRuntimeException("unable to convert libvips image to BufferedImage: " + e.getMessage(), e);
        }
    }

    /** Persist a {@link BufferedImage} to a temp PNG so libvips can load it. */
    private File writeTempPng(final BufferedImage image) {
        try {
            final File temp = File.createTempFile("vips-in", ".png");
            ImageIO.write(image, "png", temp);
            return temp;
        } catch (Exception e) {
            throw new DotRuntimeException("unable to stage BufferedImage for libvips: " + e.getMessage(), e);
        }
    }

    // The resample-option int is a TwelveMonkeys concept; libvips uses its own lanczos3 kernel, so
    // the value is accepted for API compatibility but not mapped. 0 is a safe sentinel.
    private static final int DEFAULT_RESAMPLE_OPT_PLACEHOLDER = 0;
}
