package com.dotmarketing.portlets.contentlet.business.exporter;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Semaphore;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotmarketing.image.filter.ImageFilter;
import com.dotmarketing.image.filter.ImageFilterApiImpl;
import com.dotmarketing.image.filter.PDFImageFilter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
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
    
    private final int allowedRequests = Config.getIntProperty("IMAGE_GENERATION_SIMULTANEOUS_REQUESTS", 10);
    
    private final Semaphore semaphore  = new Semaphore(allowedRequests); 
    

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.dotmarketing.portlets.contentlet.business.BinaryContentExporter#exportContent(java.io.File,
     * java.util.Map)
     */
    public BinaryContentExporterData exportContent(File file, final Map<String, String[]> parameters)
                    throws BinaryContentExporterException {

        Class<ImageFilter> errorClass = ImageFilter.class;
        try {

            final Map<String,Class<ImageFilter>> filters = new ImageFilterApiImpl().resolveFilters(parameters);
            parameters.put("filter", filters.keySet().toArray(new String[filters.size()]));
            parameters.put("filters", filters.keySet().toArray(new String[filters.size()]));
            
            // run pdf filter first (if a pdf)
            if(!filters.isEmpty() && "pdf".equals(UtilMethods.getFileExtension(file.getName())) && !filters.containsKey("pdf")) {
                file = new PDFImageFilter().runFilter(file, parameters);
            }
            
            for (final Class<ImageFilter> filter : filters.values()) {
                errorClass=filter;
                file = runFilter(filter, file, parameters);
            }

            return new BinaryContentExporterData(file);
        } catch (Exception e) {

            Logger.warnAndDebug(errorClass,  e);
            throw new BinaryContentExporterException(e.getMessage(), e);
        }

    }
    
    
    private File runFilter(Class<ImageFilter> clazz, final File fileIn,final Map<String, String[]> parameters) throws Exception {
        
        boolean canRun=false;
        try {
            
            canRun = semaphore.tryAcquire();
            Logger.warn(getClass(), "Image permits/requests : " + allowedRequests + "/" + (allowedRequests-semaphore.availablePermits()));
            
            if(!canRun) {
                Logger.warn(getClass(), "Image permits exhausted : " + allowedRequests + "/" + (allowedRequests-semaphore.availablePermits()));
                
                Try.run(()->{HttpServletResponseThreadLocal.INSTANCE.getResponse().setHeader("cache-control", "max-age=0");});
                
                return fileIn;
                
            }
            final ImageFilter imageFilter =  clazz.getDeclaredConstructor().newInstance();
            return imageFilter.runFilter(fileIn, parameters);
        } 
        finally {
            if(canRun) {
                semaphore.release();
            }
        }
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
