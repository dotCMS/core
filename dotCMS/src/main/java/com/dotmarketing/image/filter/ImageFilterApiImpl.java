package com.dotmarketing.image.filter;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.StringPool;
import com.twelvemonkeys.image.ResampleOp;
import io.vavr.control.Try;

public class ImageFilterApiImpl implements ImageFilterAPI {




    /**
     * List of image filter classes accessible by a case-insensitive key
     */
    protected static final  Map<String, Class<? extends ImageFilter>> filterClasses = new ImmutableMap.Builder<String, Class<? extends ImageFilter>>()
                    .put(CROP, CropImageFilter.class)
                    .put(EXPOSURE, ExposureImageFilter.class)
                    .put(FLIP, FlipImageFilter.class)
                    .put(FOCAL_POINT, FocalPointImageFilter.class)
                    .put(GAMMA, GammaImageFilter.class)
                    .put(GIF, GifImageFilter.class)
                    .put(GRAY_SCALE, GrayscaleImageFilter.class)
                    .put(HSB, HsbImageFilter.class)
                    .put(JPEG, JpegImageFilter.class)
                    .put(JPG, JpegImageFilter.class)
                    .put(PDF, PDFImageFilter.class)
                    .put(PNG, PngImageFilter.class)
                    .put(RESIZE, ResizeImageFilter.class)
                    .put(SCALE, ScaleImageFilter.class)
                    .put(ROTATE, RotateImageFilter.class)
                    .put(THUMBNAIL, ThumbnailImageFilter.class)
                    .put(THUMB, ThumbnailImageFilter.class)
                    .put(WEBP, WebPImageFilter.class)
                    .build();

    /**
     * Anything w or h greater than this pixel size will be shrunk down to this
     */
    private static final int MAX_SIZE =
                    Try.of(() -> Config.getIntProperty("IMAGE_MAX_PIXEL_SIZE", 5000)).getOrElse(5000);
    public static final int DEFAULT_RESAMPLE_OPT =
                    Try.of(() -> Config.getIntProperty("IMAGE_DEFAULT_RESAMPLE_OPT", ResampleOp.FILTER_TRIANGLE))
                                    .getOrElse(ResampleOp.FILTER_TRIANGLE);
    public static final String FILTER = "filter";
    public static final String FILTERS = "filters";

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
                classes.put(s.toLowerCase(), filterClasses.get(filter));
            }
        });

        return classes;
    }

    @Override
    public Dimension getWidthHeight(final File imageFile) {

        try (ImageInputStream inputStream = ImageIO.createImageInputStream(imageFile)) {
            final ImageReader reader = getReader(imageFile, inputStream);
            try {
                reader.setInput(inputStream, true, true);
                return new Dimension(reader.getWidth(0), reader.getHeight(0));
            } finally {
                Try.run(reader::dispose);

            }
        } catch (Exception e) {
            throw new DotRuntimeException("error:" + imageFile.getName() + " : " + e, e);
        }

    }

    /**
     * gets the reader based on both the input stream and the file extension
     * 
     * @param imageFile the image file
     * @param inputStream the input stream
     * @return the reader
     */
    ImageReader getReader(File imageFile, ImageInputStream inputStream) {
        Set<ImageReader> readers = new LinkedHashSet<>();

        ImageIO.getImageReaders(inputStream).forEachRemaining(readers::add);
        ImageIO.getImageReadersBySuffix(UtilMethods.getFileExtension(imageFile.getName()))
                        .forEachRemaining(readers::add);
        if(readers.size()>1) {
            // We remove the Luciad based webp-imageio reader if there are more than one reader should choose twelve monkeys
            readers.removeIf(r ->
                    r.getOriginatingProvider().getVendorName().equals("Luciad")
            );
        }
        return readers.stream().findFirst().orElseThrow(()->new DotRuntimeException("Unable to find reader for image:" + imageFile));

    }

    @Override
    public BufferedImage resizeImage(final BufferedImage srcImage, int width, int height) {

        return this.resizeImage(srcImage, width, height, DEFAULT_RESAMPLE_OPT);

    }

    @Override
    public BufferedImage resizeImage(final BufferedImage srcImage, int width, int height, int resampleOption) {

        width = Math.min(MAX_SIZE, width);
        height = Math.min(MAX_SIZE, height);
        resampleOption = Math.max(resampleOption, 0);
        resampleOption = Math.min(resampleOption, 15);

        BufferedImageOp resampler = new ResampleOp(width, height, resampleOption);
        return resampler.filter(srcImage, null);

    }

    @Override
    public BufferedImage resizeImage(final File imageFile, final int width, final int height) {
        return resizeImage(imageFile, width, height, DEFAULT_RESAMPLE_OPT);
    }

    @Override
    public BufferedImage resizeImage(final File imageFile, final int width, final int height, int resampleOption) {
        final Dimension sourceSize = getWidthHeight(imageFile);

        try (ImageInputStream inputStream = ImageIO.createImageInputStream(imageFile)) {
            final ImageReader reader = getReader(imageFile, inputStream);
            try {
                reader.setInput(inputStream, true, true);
                if (sourceSize.getWidth() == width && sourceSize.getHeight() == height) {
                    return reader.read(0);
                }

                return this.resizeImage(reader.read(0), width, height, DEFAULT_RESAMPLE_OPT);

            } finally {
                reader.dispose();
            }

        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public BufferedImage intelligentResize(File incomingImage, int width, int height) {
        
        return intelligentResize(incomingImage,  width,  height, DEFAULT_RESAMPLE_OPT);
        
    }
    
    
    
    @Override
    public BufferedImage intelligentResize(File incomingImage, int width, int height, int resampleOption) {

        
        final String hash = DigestUtils.sha256Hex(incomingImage.getAbsolutePath());
        Dimension originalSize = getWidthHeight(incomingImage);

        
        width = Math.min(MAX_SIZE, width);
        height = Math.min(MAX_SIZE, height);
        
        // resample huge images to a maxSize (prevents OOM)
        if ((originalSize.width > MAX_SIZE || originalSize.height  > MAX_SIZE)) {
            final Map<String, String[]> params = Map.of(
                            "subsample_w", new String[] {String.valueOf(MAX_SIZE)},
                            "subsample_h", new String[] {String.valueOf(MAX_SIZE)},
                            "subsample_hash", new String[] {hash},
                    FILTER, new String[] {"subsample"}
            );
            incomingImage = new SubSampleImageFilter().runFilter(incomingImage, params);
        }
        
        return this.resizeImage(incomingImage, width, height, resampleOption);

    }

    @Override
    public BufferedImage subsampleImage(final File image, final int width, final int height) {

        try (ImageInputStream inputStream = ImageIO.createImageInputStream(image)) {
            final ImageReader reader = getReader(image, inputStream);

            try {
                return this.subsampleImage(inputStream, reader, width, height);
            } finally {
                Try.run(reader::dispose);
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    BufferedImage subsampleImage(final ImageInputStream inputStream, final ImageReader reader, final int width,
                    final int height) throws IOException {

        final ImageReadParam imageReaderParams = reader.getDefaultReadParam();

        reader.setInput(inputStream, true, true);
        final Dimension sourceSize = new Dimension(reader.getWidth(0), reader.getHeight(0));
        final Dimension targetSize = new Dimension(width, height);
        final int subsampling = (int) scaleSubsamplingMaintainAspectRatio(sourceSize, targetSize);

        imageReaderParams.setSourceSubsampling(subsampling, subsampling, 0, 0);

        return reader.read(0, imageReaderParams);

    }

    private long scaleSubsamplingMaintainAspectRatio(final Dimension sourceSize, final Dimension targetSize) {

        if (sourceSize.getWidth() > targetSize.getWidth()) {
            return (long) Math.floor(sourceSize.getWidth() / targetSize.getWidth());
        } else if (sourceSize.getHeight() > targetSize.getHeight()) {
            return (long) Math.floor(sourceSize.getHeight() / targetSize.getHeight());
        }

        return 1;
    }

}
