package com.dotmarketing.image.filter;

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


    public File getImageFileFromUri(String uri) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getVariableFromPath(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * List of image filter classes accessable by a case insensitive key
     */
    protected static Map<String, Class> filterClasses = new HashMap<String, Class>(Stream.of(
                    new SimpleEntry<>("crop", CropImageFilter.class), 
                    new SimpleEntry<>("exposure", ExposureImageFilter.class),
                    new SimpleEntry<>("flip", FlipImageFilter.class),
                    new SimpleEntry<>("focalpoint", FocalPointImageFilter.class),
                    new SimpleEntry<>("gamma", GammaImageFilter.class), 
                    new SimpleEntry<>("gif", GifImageFilter.class),
                    new SimpleEntry<>("grayscale", GrayscaleImageFilter.class), 
                    new SimpleEntry<>("hsb", HsbImageFilter.class), 
                    new SimpleEntry<>("jpeg", JpegImageFilter.class),
                    new SimpleEntry<>("jpg", JpegImageFilter.class), 
                    new SimpleEntry<>("pdf", PDFImageFilter.class),
                    new SimpleEntry<>("png", PngImageFilter.class), 
                    new SimpleEntry<>("resize", ResizeImageFilter.class),
                    new SimpleEntry<>("rotate", RotateImageFilter.class), 
                    new SimpleEntry<>("scale", ScaleImageFilter.class),
                    new SimpleEntry<>("thumbnail", ThumbnailImageFilter.class),
                    new SimpleEntry<>("thumb", ThumbnailImageFilter.class), 
                    new SimpleEntry<>("webp", WebPImageFilter.class))
                    .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())));



    /**
     * returns the filters that have been specified in the filter
     * parameter or in the the arguements passed in
     * @param parameters
     * @return
     */
    @Override
    public Map<String,Class> resolveFilters(Map<String, String[]> parameters){
        
        List<String> filters= new ArrayList<>();

        if (parameters.containsKey("filter")) {
            filters.addAll(Arrays.asList(parameters.get("filter")[0].toLowerCase().split(",")));
        } else if (parameters.get("filters") != null) {
            filters.addAll(Arrays.asList(parameters.get("filters")[0].toLowerCase().split(",")));
        }
        
        parameters.entrySet().forEach(k-> {
            if(k.getKey().contains("_")) {
                final String f = k.getKey().substring(0,k.getKey().indexOf("_"));
                if(!filters.contains(f)) {
                    filters.add(f);
                }
            }
        });
        
        Map<String,Class> classes = new LinkedHashMap<String, Class>();
        filters.forEach(s->{
            String filter = s.toLowerCase();
            if(!classes.containsKey(filter) && filterClasses.containsKey(filter)) {
                classes.put(s.toLowerCase(), filterClasses.get(filter));
            }
            
            
        });
        
        return classes;
    }
    
    
    
    
    
    
    
    
    
}
