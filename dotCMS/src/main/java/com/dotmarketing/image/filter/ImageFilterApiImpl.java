package com.dotmarketing.image.filter;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import com.twelvemonkeys.image.ResampleOp;
import io.vavr.control.Try;
import jersey.repackaged.com.google.common.collect.ImmutableMap;

public class ImageFilterApiImpl implements ImageFilterAPI {

    public File getImageFileFromUri(final String uri) {
        return null;
    }

    public String getVariableFromPath(final String path) {
        return null;
    }

    /**
     * List of image filter classes accessable by a case insensitive key
     */
    protected static Map<String, Class> filterClasses = new HashMap<String, Class>(Stream.of(
                    new SimpleEntry<>(CROP, CropImageFilter.class),
                    new SimpleEntry<>(EXPOSURE, ExposureImageFilter.class),
                    new SimpleEntry<>(FLIP, FlipImageFilter.class),
                    new SimpleEntry<>(FOCAL_POINT, FocalPointImageFilter.class),
                    new SimpleEntry<>(GAMMA, GammaImageFilter.class), new SimpleEntry<>(GIF, GifImageFilter.class),
                    new SimpleEntry<>(GRAY_SCALE, GrayscaleImageFilter.class),
                    new SimpleEntry<>(HSB, HsbImageFilter.class), new SimpleEntry<>(JPEG, JpegImageFilter.class),
                    new SimpleEntry<>(JPG, JpegImageFilter.class), new SimpleEntry<>(PDF, PDFImageFilter.class),
                    new SimpleEntry<>(PNG, PngImageFilter.class), new SimpleEntry<>(RESIZE, ResizeImageFilter.class),
                    new SimpleEntry<>(ROTATE, RotateImageFilter.class),
                    new SimpleEntry<>(THUMBNAIL, ThumbnailImageFilter.class),
                    new SimpleEntry<>(THUMB, ThumbnailImageFilter.class),
                    new SimpleEntry<>(WEBP, WebPImageFilter.class))
                    .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));

    /**
     * Anything w or h greater than this pixel size will be shrunk down to this
     */
    private final static int maxSize =
                    Try.of(() -> Config.getIntProperty("IMAGE_MAX_PIXEL_SIZE", 5000)).getOrElse(5000);
    public final static int DEFAULT_RESAMPLE_OPT =
                    Try.of(() -> Config.getIntProperty("IMAGE_DEFAULT_RESAMPLE_OPT", ResampleOp.FILTER_TRIANGLE))
                                    .getOrElse(ResampleOp.FILTER_TRIANGLE);

    @Override
    public Map<String, Class> resolveFilters(final Map<String, String[]> parameters) {

        final List<String> filters = new ArrayList<>();

        if (parameters.containsKey("filter")) {
            filters.addAll(Arrays.asList(parameters.get("filter")[0].toLowerCase().split(StringPool.COMMA)));
        } else if (parameters.get("filters") != null) {
            filters.addAll(Arrays.asList(parameters.get("filters")[0].toLowerCase().split(StringPool.COMMA)));
        }

        parameters.entrySet().forEach(k -> {
            if (k.getKey().contains(StringPool.UNDERLINE)) {
                final String filter = k.getKey().substring(0, k.getKey().indexOf(StringPool.UNDERLINE));
                if (!filters.contains(filter)) {
                    filters.add(filter);
                }
            }
        });

        final Map<String, Class> classes = new LinkedHashMap<>();
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
                Try.run(() -> reader.dispose());

            }
        } catch (Exception e) {
            throw new DotRuntimeException("error:" + imageFile.getName() + " : " + e, e);
        }

    }

    /**
     * gets the reader based on both the input stream and the file extension
     * 
     * @param imageFile
     * @param inputStream
     * @return
     */
    ImageReader getReader(File imageFile, ImageInputStream inputStream) {
        Set<ImageReader> readers = new LinkedHashSet<>();

        ImageIO.getImageReaders(inputStream).forEachRemaining(readers::add);
        ImageIO.getImageReadersBySuffix(UtilMethods.getFileExtension(imageFile.getName()))
                        .forEachRemaining(readers::add);
        if(readers.size()>1) {
            readers.removeIf(r -> r instanceof net.sf.javavp8decoder.imageio.WebPImageReader);
        }
        if (readers.isEmpty()) {
            throw new DotRuntimeException("Unable to find reader for image:" + imageFile);
        }
        return readers.stream().findFirst().get();

    }

    @Override
    public BufferedImage resizeImage(final BufferedImage srcImage, int width, int height) {

        return this.resizeImage(srcImage, width, height, DEFAULT_RESAMPLE_OPT);

    }

    @Override
    public BufferedImage resizeImage(final BufferedImage srcImage, int width, int height, int resampleOption) {

        width = Math.min(maxSize, width);
        height = Math.min(maxSize, height);
        resampleOption = (resampleOption < 0) ? 0 : (resampleOption > 15) ? 15 : resampleOption;

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

        
        width = Math.min(maxSize, width);
        height = Math.min(maxSize, height);
        
        // resample huge images to a maxSize (prevents OOM)
        if ((originalSize.width > maxSize || originalSize.height  > maxSize)) {
            final Map<String, String[]> params = ImmutableMap.of(
                            "subsample_w", new String[] {String.valueOf(maxSize)},
                            "subsample_h", new String[] {String.valueOf(maxSize)},
                            "subsample_hash", new String[] {hash},
                            "filter", new String[] {"subsample"}
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
                Try.run(() -> reader.dispose());
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
