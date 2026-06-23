package com.dotmarketing.portlets.contentlet.business.exporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.image.ImageEngine;
import com.dotmarketing.image.filter.ImageFilter;
import com.dotmarketing.image.filter.ImageFilterAPI;
import com.dotmarketing.image.filter.PDFImageFilter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
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
        if (UtilMethods.isVectorImage(fileExtension)) {
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

            //short circuit if we already have it generated locally
            if (tempFile.isPresent()) {
                return new BinaryContentExporterData(tempFile.get());
            }

            // SHARED_COMPLETED: if another instance (or this instance, pre-restart) already published
            // this exact rendition to the shared store, serve it directly and skip regeneration.
            if (ConfigUtils.isDotGeneratedSharedCompleted()) {
                final File shared = toSharedFile(finalResultFile(filters.values(), file, parameters));
                if (shared != null && shared.exists() && shared.length() >= MIN_VALID_FILE_LENGTH) {
                    return new BinaryContentExporterData(shared);
                }
            }

            for (final Class<? extends ImageFilter> filter : filters.values()) {
                errorClass=filter;
                final ImageFilter imageFilter =  filter.getDeclaredConstructor().newInstance();

                file = runFilter(imageFilter, file, parameters);
            }

            // The final rendition is ready locally under dotsecure; publish it to the shared store on a
            // background virtual thread so the request returns immediately and no other instance — nor a
            // restart of this one — has to regenerate it.
            if (ConfigUtils.isDotGeneratedSharedCompleted()) {
                publishToShared(file);
            }

            return new BinaryContentExporterData(file);
        } catch (Exception e) {

            Logger.warnAndDebug(errorClass,  e);
            throw new BinaryContentExporterException(e.getMessage(), e);
        }

    }
    
    
    private File runFilter(ImageFilter imageFilter, final File fileIn,final Map<String, String[]> parameters)  {
        
        boolean canRun=false;
        try {
            
            canRun = semaphore.tryAcquire();
            Logger.debug(getClass(), "Image permits/requests : " + allowedRequests + "/" + (allowedRequests-semaphore.availablePermits()));
            
            if(!canRun) {
                Logger.warn(getClass(), "Image permits exhausted : " + allowedRequests + "/" + (allowedRequests-semaphore.availablePermits()));
                
                Try.run(()->HttpServletResponseThreadLocal.INSTANCE.getResponse().setHeader("cache-control", "max-age=0"));
                
                return fileIn;
                
            }

            return imageFilter.runFilter(fileIn, parameters);
        } 
        finally {
            if(canRun) {
                semaphore.release();
            }
        }
    }

    /** Renditions smaller than this are treated as missing/corrupt (matches the legacy guard). */
    private static final long MIN_VALID_FILE_LENGTH = 50L;

    private Optional<File> alreadyGenerated(final Collection<Class<? extends ImageFilter>> clazzes, final File fileIn,
                    final Map<String, String[]> parameters)  {

        final File fileToReturn = finalResultFile(clazzes, fileIn, parameters);

        if (fileToReturn == null || ! fileToReturn.exists() ||  fileToReturn.length() < MIN_VALID_FILE_LENGTH) {
            return Optional.empty();
        }
        return Optional.of(fileToReturn);
    }

    /**
     * Resolves the final cache file for a filter chain WITHOUT running any pixel work or checking
     * existence — it walks {@link ImageFilter#getResultsFile} through every filter exactly as the
     * generation loop would, returning the path the last filter would write to.
     */
    private File finalResultFile(final Collection<Class<? extends ImageFilter>> clazzes, final File fileIn,
                    final Map<String, String[]> parameters) {
        File fileToReturn = fileIn;
        for (final Class<? extends ImageFilter> filter : clazzes) {
            final ImageFilter imageFilter =
                    Try.of(() -> filter.getDeclaredConstructor().newInstance()).getOrElseThrow(DotRuntimeException::new);
            fileToReturn = imageFilter.getResultsFile(fileToReturn, parameters);
        }
        return fileToReturn;
    }

    /**
     * Maps a locally-generated rendition (under {@link ConfigUtils#getDotGeneratedPath()}, i.e.
     * dotsecure) to its counterpart in the shared store ({@link ConfigUtils#getDotGeneratedSharedPath()}),
     * preserving the {@code inode[0]/inode[1]/hashedName} layout. Returns {@code null} when the file is
     * not under the local dotGenerated root (e.g. a re-transform of an already-generated file).
     */
    private File toSharedFile(final File localFinal) {
        return toSharedFile(localFinal, ConfigUtils.getDotGeneratedPath(), ConfigUtils.getDotGeneratedSharedPath());
    }

    /**
     * Pure path mapping (package-visible for tests): rebases {@code localFinal} from {@code localBase}
     * onto {@code sharedBase}, preserving the trailing {@code inode[0]/inode[1]/hashedName} segment.
     * Returns {@code null} when {@code localFinal} is not under {@code localBase}.
     */
    static File toSharedFile(final File localFinal, final String localBase, final String sharedBase) {
        if (localFinal == null) {
            return null;
        }
        try {
            final String base = new File(localBase).getCanonicalPath();
            final String full = localFinal.getCanonicalPath();
            if (!full.startsWith(base)) {
                return null;
            }
            return new File(sharedBase + full.substring(base.length()));
        } catch (IOException e) {
            Logger.warnAndDebug(ImageFilterExporter.class, "unable to map rendition to shared store: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fire-and-forget copy of a finished rendition to the shared store on a virtual thread so the
     * serving request returns immediately. If another instance already published it, this is a no-op.
     */
    private void publishToShared(final File localFinal) {
        final File shared = toSharedFile(localFinal);
        if (shared == null || localFinal == null || !localFinal.exists()) {
            return;
        }
        if (shared.exists() && shared.length() >= MIN_VALID_FILE_LENGTH) {
            return;
        }
        Thread.ofVirtual().name("dotGenerated-share")
                .start(() -> copyToShared(localFinal, shared, new File(ConfigUtils.getAssetTempPath())));
    }

    /**
     * Stages {@code localFinal} into {@code sharedTmpDir} and atomically moves it onto {@code shared},
     * so concurrent readers never observe a partial file.
     *
     * <p>{@code sharedTmpDir} <b>must live on the same filesystem (the shared mount) as
     * {@code shared}</b> — otherwise {@link Files#move} degrades to a cross-filesystem copy+delete and
     * loses atomicity. The caller passes {@link ConfigUtils#getAssetTempPath()} (the shared
     * {@code tmp_upload} dir under the assets root), which sits on the same mount as the shared
     * {@code dotGenerated} store. Package-visible and synchronous for tests; the virtual-thread
     * hand-off lives in {@link #publishToShared(File)}.</p>
     */
    static void copyToShared(final File localFinal, final File shared, final File sharedTmpDir) {
        try {
            final File parent = shared.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!sharedTmpDir.exists()) {
                sharedTmpDir.mkdirs();
            }
            final File tmp = new File(sharedTmpDir, shared.getName() + "_" + System.nanoTime() + ".tmp");
            Files.copy(localFinal.toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp.toPath(), shared.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Logger.debug(ImageFilterExporter.class, "published rendition to shared store: " + shared);
        } catch (Exception e) {
            Logger.warnAndDebug(ImageFilterExporter.class,
                    "failed to publish rendition to shared store " + shared + ": " + e.getMessage(), e);
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
