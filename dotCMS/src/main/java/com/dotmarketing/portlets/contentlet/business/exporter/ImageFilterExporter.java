package com.dotmarketing.portlets.contentlet.business.exporter;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.dotmarketing.image.filter.CropImageFilter;
import com.dotmarketing.image.filter.ExposureImageFilter;
import com.dotmarketing.image.filter.FlipImageFilter;
import com.dotmarketing.image.filter.FocalPointImageFilter;
import com.dotmarketing.image.filter.GammaImageFilter;
import com.dotmarketing.image.filter.GifImageFilter;
import com.dotmarketing.image.filter.HsbImageFilter;
import com.dotmarketing.image.filter.ImageFilter;
import com.dotmarketing.image.filter.ImageFilterApiImpl;
import com.dotmarketing.image.filter.JpegImageFilter;
import com.dotmarketing.image.filter.PDFImageFilter;
import com.dotmarketing.image.filter.PngImageFilter;
import com.dotmarketing.image.filter.ResizeImageFilter;
import com.dotmarketing.image.filter.RotateImageFilter;
import com.dotmarketing.image.filter.ScaleImageFilter;
import com.dotmarketing.image.filter.ThumbnailImageFilter;
import com.dotmarketing.image.filter.WebPImageFilter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;


/**
 * 
 * A exporter that can take 1 or more filters in a chain
 * 
 * the chain is provided by the "filter=" parameter You can chain filters so that you resize then
 * crop to produce the resulting image
 * 
 * 
 */

public class ImageFilterExporter implements BinaryContentExporter {



    /*
     * (non-Javadoc)
     * 
     * @see
     * com.dotmarketing.portlets.contentlet.business.BinaryContentExporter#exportContent(java.io.File,
     * java.util.Map)
     */
    public BinaryContentExporterData exportContent(File file, Map<String, String[]> parameters)
                    throws BinaryContentExporterException {


        BinaryContentExporterData data;

        try {

            Map<String,Class> filters = new ImageFilterApiImpl().resolveFilters(parameters);
            
            parameters.put("filter", filters.keySet().toArray(new String[filters.size()]));
            parameters.put("filters", filters.keySet().toArray(new String[filters.size()]));
            
            for (Entry<String, Class> filter : filters.entrySet()) {
                try {
                    ImageFilter i = (ImageFilter) filter.getValue().newInstance();
                    file = i.runFilter(file, parameters);
                } catch (Exception e) {
                    Logger.warnAndDebug(ImageFilterExporter.class,
                                    "Exception in " + filter + " :" + e.getMessage() + e.getStackTrace()[0], e);
                }
            }


            data = new BinaryContentExporterData(file);

        } catch (Exception e) {
            Logger.warnAndDebug(ImageFilterExporter.class,  e);
            throw new BinaryContentExporterException(e.getMessage(), e);
        }

        return data;
    }

    
    
    

    public String getName() {
        return "Image Filter Exporter";
    }

    public String getPathMapping() {
        return "image";
    }

    public String getDescription() {
        return "Specify filters to run a source image through";
    }

}
