package com.dotmarketing.image.filter;

import com.dotcms.cost.RequestCost;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.StringPool;
import com.twelvemonkeys.image.ResampleOp;
import io.vavr.control.Try;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;

public class ImageFilterApiImpl implements ImageFilterAPI {


    ImageFilterApiImpl(){
        ImageIO.scanForPlugins();
        deregisterProviders();
    }




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

    @RequestCost
    @Override
    public Dimension getWidthHeight(final File imageFile) {

        if (imageFile == null) {
            throw new DotRuntimeException("imageFile is null");
        }

        if (imageFile.getName().toLowerCase().endsWith(".svg")) {
            try {
                return getWidthHeightofSVG(imageFile);
            } catch (Exception e){
                //If invoking the getWidthHeightofSVG method fails, we will try to get dimensions using the inputStream
                Logger.debug(this, "Error getting dimensions of SVG file: " + imageFile.getName(), e);
            }
        }


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

    @VisibleForTesting
    Dimension getWidthHeightofSVG(@Nonnull final File imageFile) {
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(
                XMLResourceDescriptor.getXMLParserClassName());

        try (InputStream is = Files.newInputStream(imageFile.toPath())) {
            Document document = factory.createDocument(imageFile.toURI().toString(), is);

            String viewBox = document.getDocumentElement().getAttribute("viewBox");
            String[] viewBoxValues = viewBox.split(" ");

            float x = Float.parseFloat(viewBoxValues[2]);
            float y = Float.parseFloat(viewBoxValues[3]);


            return new Dimension(Math.round(x), Math.round(y));

        } catch (Exception e) {
            throw new DotRuntimeException("unable to parse svg file: " + imageFile.getAbsolutePath() + " : " + e.getMessage(), e);
        }
    }

    private <T> T lookupProviderByName(final ServiceRegistry registry, final String providerClassName) {
        try {
            return (T) registry.getServiceProviderByClass(Class.forName(providerClassName));
        }
        catch (ClassNotFoundException ignore) {
            return null;
        }
    }

    private String[] providersToIgnore = Config.getStringArrayProperty("IMAGE_READER_SPIS_TO_DEREGISTER", new String[]{"net.sf.javavp8decoder.imageio.WebPImageReaderSpi"});


    private void deregisterProviders()  {

        IIORegistry registry = IIORegistry.getDefaultInstance();

        for(String providerClazz: providersToIgnore) {
            ImageReaderSpi  provider= lookupProviderByName(registry, providerClazz);
            registry.deregisterServiceProvider(provider);
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


        // remove old VP8 ImageReader as there are cases where it is broken
        readers.removeIf(r->r.getClass().equals(net.sf.javavp8decoder.imageio.WebPImageReader.class));

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

    @RequestCost
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

                return this.resizeImage(reader.read(0), width, height, resampleOption);

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
