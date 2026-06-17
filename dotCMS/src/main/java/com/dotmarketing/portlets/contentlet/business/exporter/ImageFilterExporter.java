package com.dotmarketing.portlets.contentlet.business.exporter;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.image.ImageEngine;
import com.dotmarketing.image.filter.ImageFilter;
import com.dotmarketing.image.filter.ImageFilterAPI;
import com.dotmarketing.image.filter.PDFImageFilter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

/**
 *
 * An exporter that can take 1 or more filters in a chain
 * <p>
 * the chain is provided by the "filter=" parameter You can chain filters so that you resize then
 * crop to produce the resulting image
 * <p>
 *
 */

public class ImageFilterExporter implements BinaryContentExporter {

    private final int allowedRequests = Config.getIntProperty("IMAGE_GENERATION_SIMULTANEOUS_REQUESTS", 10);

    private final Semaphore semaphore  = new Semaphore(allowedRequests);

    private static final Set<String> VECTOR_EXTENSIONS = ImmutableSet.of("svg", "eps", "ai", "dxf");


    /**
     * Selects the image engine per the {@code IMAGE_API_USE_LIBVIPS} feature flag. The choice only
     * affects which {@link ImageFilter} subclasses {@code resolveFilters} returns — the URL parameter
     * contract is identical for both engines.
     */
    // package-visible for tests that pin the feature-flag selection behaviour
    ImageFilterAPI imageFilterAPI() {
        return ImageEngine.resolve();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.dotmarketing.portlets.contentlet.business.BinaryContentExporter#exportContent(java.io.File,
     * java.util.Map)
     */
    public BinaryContentExporterData exportContent(File file, final Map<String, String[]> parameters)
            throws BinaryContentExporterException {

        final String fileExtension = UtilMethods.getFileExtension(file.getName());
        if (VECTOR_EXTENSIONS.contains(fileExtension)) {
            Logger.info(this.getClass(), "Skipping vector image transformation for " + fileExtension);
            return new BinaryContentExporterData(file);
        }

        Class<? extends ImageFilter> errorClass = ImageFilter.class;
        try {

            final Map<String,Class<? extends ImageFilter>> filters = imageFilterAPI().resolveFilters(parameters);
            parameters.put("filter", filters.keySet().toArray(new String[0]));
            parameters.put("filters", filters.keySet().toArray(new String[0]));

            // run pdf filter first (if a pdf)
            if(!filters.isEmpty() && "pdf".equals(fileExtension) && !filters.containsKey("pdf")) {
                file = runFilter(new PDFImageFilter(), file, parameters);
            }

            Optional<File> tempFile = alreadyGenerated(filters.values(), file, parameters);

            //short circuit if we already have it generated
            if (tempFile.isPresent()) {
                return new BinaryContentExporterData(tempFile.get());
            }


            for (final Class<? extends ImageFilter> filter : filters.values()) {
                errorClass=filter;
                final ImageFilter imageFilter =  filter.getDeclaredConstructor().newInstance();

                file = runFilter(imageFilter, file, parameters);
            }

            return new BinaryContentExporterData(file);
        } catch (Exception e) {

            Logger.warnAndDebug(errorClass,  e);
            throw new BinaryContentExporterException(e.getMessage(), e);
        }

    }

    public class ImageNotReadyException extends Exception {
        ImageNotReadyException(String message) {
            super(message);
        }
    }

    private File runFilter(ImageFilter imageFilter, final File fileIn,final Map<String, String[]> parameters)
            throws ImageNotReadyException {

        boolean canRun=false;
        try {

            canRun = semaphore.tryAcquire();
            Logger.debug(getClass(), "Image permits/requests : " + allowedRequests + "/" + (allowedRequests-semaphore.availablePermits()));

            if(!canRun) {
                Logger.warn(getClass(), "Image permits exhausted : " + allowedRequests + "/" + (allowedRequests-semaphore.availablePermits()));

                throw new ImageNotReadyException("Image permits exhausted");

            }

            return imageFilter.runFilter(fileIn, parameters);
        }
        finally {
            if(canRun) {
                semaphore.release();
            }
        }
    }

    private Optional<File> alreadyGenerated(final Collection<Class<? extends ImageFilter>> clazzes, final File fileIn,
            final Map<String, String[]> parameters)  {

        File fileToReturn = fileIn;

        for (final Class<? extends ImageFilter> filter : clazzes) {
            final ImageFilter imageFilter =  Try.of(()-> filter.getDeclaredConstructor().newInstance()).getOrElseThrow(DotRuntimeException::new);
            fileToReturn  = imageFilter.getResultsFile(fileToReturn, parameters);


        }

        if (fileToReturn == null || ! fileToReturn.exists() ||  fileToReturn.length() < 50) {
            return Optional.empty();
        }
        return Optional.of(fileToReturn);
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
