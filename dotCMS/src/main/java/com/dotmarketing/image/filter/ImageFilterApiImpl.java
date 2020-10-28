package com.dotmarketing.image.filter;

import com.liferay.util.StringPool;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    
    
    
    
    
    
    
    
    
}
