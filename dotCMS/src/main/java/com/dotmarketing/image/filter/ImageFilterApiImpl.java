package com.dotmarketing.image.filter;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

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
                    new SimpleEntry<>(GAMMA, GammaImageFilter.class),
                    new SimpleEntry<>(GIF, GifImageFilter.class),
                    new SimpleEntry<>(GRAY_SCALE, GrayscaleImageFilter.class),
                    new SimpleEntry<>(HSB, HsbImageFilter.class),
                    new SimpleEntry<>(JPEG, JpegImageFilter.class),
                    new SimpleEntry<>(JPG, JpegImageFilter.class),
                    new SimpleEntry<>(PDF, PDFImageFilter.class),
                    new SimpleEntry<>(PNG, PngImageFilter.class),
                    new SimpleEntry<>(RESIZE, ResizeImageFilter.class),
                    new SimpleEntry<>(ROTATE, RotateImageFilter.class),
                    new SimpleEntry<>(SCALE, ScaleImageFilter.class),
                    new SimpleEntry<>(THUMBNAIL, ThumbnailImageFilter.class),
                    new SimpleEntry<>(THUMB, ThumbnailImageFilter.class),
                    new SimpleEntry<>(WEBP, WebPImageFilter.class))
                    .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));



    @Override
    public Map<String,Class> resolveFilters(final Map<String, String[]> parameters){
        
        final List<String> filters = new ArrayList<>();

        if (parameters.containsKey("filter")) {
            filters.addAll(Arrays.asList(parameters.get("filter")[0].toLowerCase().split(StringPool.COMMA)));
        } else if (parameters.get("filters") != null) {
            filters.addAll(Arrays.asList(parameters.get("filters")[0].toLowerCase().split(StringPool.COMMA)));
        }
        
        parameters.entrySet().forEach(k-> {
            if(k.getKey().contains(StringPool.UNDERLINE)) {
                final String filter = k.getKey().substring(0, k.getKey().indexOf(StringPool.UNDERLINE));
                if(!filters.contains(filter)) {
                    filters.add(filter);
                }
            }
        });
        
        final Map<String,Class> classes = new LinkedHashMap<>();
        filters.forEach(s->{
            final String filter = s.toLowerCase();
            if(!classes.containsKey(filter) && filterClasses.containsKey(filter)) {
                classes.put(s.toLowerCase(), filterClasses.get(filter));
            }
        });
        
        return classes;
    }
    
    @Override
    public Dimension getWidthHeight(final File image) {
        
        try (ImageInputStream iis = ImageIO.createImageInputStream(image)){
            // Find all image readers that recognize the image format
            Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
            if (!iter.hasNext()) {
                Logger.info(getClass(), "no reader found for :" + image);
                return new Dimension(0,0);
            }
            // Use the first reader
            ImageReader reader = (ImageReader)iter.next();

            reader.setInput(iis);
            int w = reader.getWidth(0);
            int h = reader.getHeight(0);
            
            Dimension dim = new Dimension(w,h);
            reader.dispose();
            return dim;
        }
        catch(Exception e) {
            Logger.warnAndDebug(getClass(), e.getMessage() + " : " + image,e);
            return new Dimension(0,0);
            
        }
        
    }
    
    @Override
    public BufferedImage subsampleImage(final File image, final int width, final int height)  {
        try (ImageInputStream subsampleImage = ImageIO.createImageInputStream(image)){

            // Find all image readers that recognize the image format
            Iterator<ImageReader> iter = ImageIO.getImageReaders(subsampleImage);
            // Use the first reader
            ImageReader reader = (ImageReader)iter.next();
            
            
            return this.subsampleImage(subsampleImage,reader,width,height );
        }catch(Exception e) {
            throw new DotRuntimeException(e);
        }
        

    }
    
    
    @Override
    public BufferedImage subsampleImage(final ImageInputStream inputStream, final ImageReader reader, final int width, final int height) throws IOException {

        final ImageReadParam imageReaderParams = reader.getDefaultReadParam();

        reader.setInput(inputStream, true, true);
        final Dimension sourceSize = new Dimension(reader.getWidth(0), reader.getHeight(0));
        final Dimension targetSize = new Dimension(width, height);
        final int subsampling = (int) scaleSubsamplingMaintainAspectRatio(sourceSize, targetSize);
        

        
        imageReaderParams.setSourceSubsampling(subsampling, subsampling, 0, 0);


        return  reader.read(0, imageReaderParams);

    }

    private long scaleSubsamplingMaintainAspectRatio(final Dimension sourceSize, final Dimension targetSize) {

        if (sourceSize.getWidth() > targetSize.getWidth()) {
            return Math.round(sourceSize.getWidth() / targetSize.getWidth());
        }
        else if (sourceSize.getHeight() > targetSize.getHeight()) {
            return Math.round(sourceSize.getHeight() / targetSize.getHeight());
        }

        return 1;
    }
    
    
    
    
}
