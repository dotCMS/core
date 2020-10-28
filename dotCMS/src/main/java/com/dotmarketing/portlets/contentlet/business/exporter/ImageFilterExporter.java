package com.dotmarketing.portlets.contentlet.business.exporter;

import com.dotmarketing.image.filter.ImageFilter;
import com.dotmarketing.image.filter.ImageFilterApiImpl;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.util.Logger;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;


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
    public BinaryContentExporterData exportContent(File file, final Map<String, String[]> parameters)
                    throws BinaryContentExporterException {

        BinaryContentExporterData data;

        try {

            final Map<String,Class> filters = new ImageFilterApiImpl().resolveFilters(parameters);
            parameters.put("filter", filters.keySet().toArray(new String[filters.size()]));
            parameters.put("filters", filters.keySet().toArray(new String[filters.size()]));
            
            for (final Entry<String, Class> filter : filters.entrySet()) {
                try {

                    final ImageFilter imageFilter = (ImageFilter) filter.getValue().newInstance();
                    file = imageFilter.runFilter(file, parameters);
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
