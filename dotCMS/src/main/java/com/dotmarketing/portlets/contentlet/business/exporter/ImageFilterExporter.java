package com.dotmarketing.portlets.contentlet.business.exporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.image.ImageEngine;
import com.dotmarketing.image.focalpoint.FocalPoint;
import com.dotmarketing.image.focalpoint.FocalPointAPI;
import com.dotmarketing.image.focalpoint.FocalPointAPIImpl;
import com.dotcms.storage.binary.BinaryAssetReference;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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
    public BinaryContentExporterData exportContent(File file, final Map<String, String[]> suppliedParameters)
                    throws BinaryContentExporterException {
        if (s3Renditions()) {
            try (var lease = APILocator.getBinaryAssetStorageAPI().acquireCacheLease()) {
                return exportContentInternal(file, suppliedParameters);
            }
        }
        return exportContentInternal(file, suppliedParameters);
    }

    private BinaryContentExporterData exportContentInternal(File file, final Map<String, String[]> suppliedParameters)
                    throws BinaryContentExporterException {

        final Map<String, String[]> parameters = s3Renditions()
                ? new LinkedHashMap<>(suppliedParameters) : suppliedParameters;
        final String fileExtension = UtilMethods.getFileExtension(file.getName());
        if (UtilMethods.isVectorImage(fileExtension)) {
            Logger.info(this.getClass(), "Skipping vector image transformation for " + fileExtension);
            return new BinaryContentExporterData(file);
        }

        Class<? extends ImageFilter> errorClass = ImageFilter.class;
        try {

            final Map<String,Class<? extends ImageFilter>> filters = imageFilterAPI().resolveFilters(parameters);
            if (s3Renditions() && filters.isEmpty()) {
                return new BinaryContentExporterData(file);
            }
            if (s3Renditions() && filters.containsKey(ImageFilter.CROP)) {
                resolveCropFocalPoint(file, parameters);
            }
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
            if (!s3Renditions() && ConfigUtils.isDotGeneratedSharedCompleted()) {
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
            if (!s3Renditions() && ConfigUtils.isDotGeneratedSharedCompleted()) {
                publishToShared(file);
            }

            return new BinaryContentExporterData(file);
        } catch (Exception e) {

            Logger.warnAndDebug(errorClass,  e);
            throw new BinaryContentExporterException(e.getMessage(), e);
        }

    }
    
    
    /**
     * Runs one filter of the chain and returns the file to pass to the next filter.
     *
     * <p>With S3 asset storage on, the filter's predicted output is looked up first (local disk,
     * then S3) so a completed rendition is reused without running the filter. A predicted output
     * that this node has seen the filter never produce, because the filter returned its input
     * unchanged, is not looked up again, so a no-op filter does not contact S3 on later requests.
     * A newly produced output is uploaded after the image permit is released. If the upload
     * fails, the local output is still served and a warning is logged; the file stays local only,
     * and eviction keeps it because it has no durable copy.
     *
     * <p>With the flag off, this only runs the filter under the image permit, as before.
     *
     * @param imageFilter the filter to run
     * @param fileIn the input image, either the original or the previous filter's output
     * @param parameters the request's filter parameters
     * @return the filter's output, or {@code fileIn} when the filter is a no-op or no image
     *         permit is available
     * @throws DotDataException if the S3 lookup of the predicted output fails
     */
    private File runFilter(ImageFilter imageFilter, final File fileIn,final Map<String, String[]> parameters)
            throws DotDataException {
        final File predicted = s3Renditions() ? imageFilter.getResultsFile(fileIn, parameters) : null;
        if (predicted != null && UNPRODUCED_RESULTS.getIfPresent(predicted.getPath()) == null) {
            final File cached = cachedRendition(predicted);
            if (cached != null) {
                return cached;
            }
        }
        
        boolean canRun=false;
        final File result;
        try {
            
            canRun = semaphore.tryAcquire();
            Logger.debug(getClass(), "Image permits/requests : " + allowedRequests + "/" + (allowedRequests-semaphore.availablePermits()));
            
            if(!canRun) {
                Logger.warn(getClass(), "Image permits exhausted : " + allowedRequests + "/" + (allowedRequests-semaphore.availablePermits()));
                
                Try.run(()->HttpServletResponseThreadLocal.INSTANCE.getResponse().setHeader("cache-control", "max-age=0"));
                
                return fileIn;
                
            }

            result = imageFilter.runFilter(fileIn, parameters);
        } 
        finally {
            if(canRun) {
                semaphore.release();
            }
        }
        if (predicted != null) {
            if (result.equals(fileIn)) {
                UNPRODUCED_RESULTS.put(predicted.getPath(), Boolean.TRUE);
            } else {
                UNPRODUCED_RESULTS.invalidate(predicted.getPath());
                // Only the predicted path can be found by a later lookup, so only it is worth uploading.
                if (result.equals(predicted)) {
                    storeRendition(result);
                }
            }
        }
        return result;
    }

    /**
     * Uploads a newly produced rendition. Availability comes first: the rendition already exists
     * locally, so a failed upload is logged as a warning and the local file is served anyway.
     *
     * @param rendition the completed rendition under the local {@code dotGenerated} root
     */
    private void storeRendition(final File rendition) {
        try {
            APILocator.getBinaryAssetStorageAPI().storeGeneratedFile(rendition);
        } catch (final Exception e) {
            Logger.warnAndDebug(ImageFilterExporter.class, "Unable to store rendition " + rendition
                    + " in S3; serving the local copy, which stays local only: " + e.getMessage(), e);
        }
    }

    /**
     * Predicted filter outputs that a filter on this node returned its input for instead of
     * writing. The predicted path covers the filter, its parameters, the input's name and, when
     * the input has one, its revision key, so the same request is a no-op again and nothing is
     * ever written at that path. An entry is dropped if the filter later produces the output. The
     * cache is bounded because it only saves remote lookups; an evicted entry costs one lookup.
     */
    private static final Cache<String, Boolean> UNPRODUCED_RESULTS = Caffeine.newBuilder()
            .maximumSize(10_000).build();

    /** Renditions smaller than this are treated as missing/corrupt (matches the legacy guard). */
    private static final long MIN_VALID_FILE_LENGTH = 50L;

    /**
     * Returns the chain's final rendition when it already exists, so the request can skip the
     * filters.
     *
     * <p>With S3 asset storage on, the final rendition is looked up locally and then in S3. When a
     * filter in the chain is known on this node to return its input unchanged, the predicted final
     * path will never be written, so no lookup is made and the chain runs instead.
     *
     * @param clazzes the filter chain, in order
     * @param fileIn the input image
     * @param parameters the request's filter parameters
     * @return the existing final rendition, or empty when the chain must run
     * @throws DotDataException if the S3 lookup fails
     */
    private Optional<File> alreadyGenerated(final Collection<Class<? extends ImageFilter>> clazzes, final File fileIn,
                    final Map<String, String[]> parameters) throws DotDataException {

        if (s3Renditions()) {
            File predicted = fileIn;
            for (final Class<? extends ImageFilter> filter : clazzes) {
                predicted = Try.of(() -> filter.getDeclaredConstructor().newInstance())
                        .getOrElseThrow(DotRuntimeException::new).getResultsFile(predicted, parameters);
                if (UNPRODUCED_RESULTS.getIfPresent(predicted.getPath()) != null) {
                    return Optional.empty();
                }
            }
            return Optional.ofNullable(cachedRendition(predicted));
        }

        final File fileToReturn = finalResultFile(clazzes, fileIn, parameters);

        if (fileToReturn == null || ! fileToReturn.exists() ||  fileToReturn.length() < MIN_VALID_FILE_LENGTH) {
            return Optional.empty();
        }
        return Optional.of(fileToReturn);
    }

    /** Pin the source snapshot's focal point before computing any cache keys or rendering pixels. */
    void resolveCropFocalPoint(final File source, final Map<String, String[]> parameters)
            throws DotDataException {
        final FocalPointAPIImpl focalPoints = new FocalPointAPIImpl();
        Optional<FocalPoint> point = focalPoints.parseFocalPointFromParams(parameters);
        if (point.isEmpty()) {
            final String revision = BinaryAssetReference.keyOf(source);
            final Path root = Path.of(ConfigUtils.getAssetPath()).toAbsolutePath().normalize();
            final Path sourcePath = source.toPath().toAbsolutePath().normalize();
            final Path relative = root.relativize(sourcePath);
            final boolean legacy = sourcePath.startsWith(root) && relative.getNameCount() == 5
                    && relative.getName(0).toString().length() == 1
                    && relative.getName(1).toString().length() == 1
                    && relative.getName(2).toString().startsWith(
                            relative.getName(0).toString() + relative.getName(1));
            if (revision != null || legacy) {
                final String[] path = relative.toString().replace(File.separatorChar, '/').split("/");
                final Contentlet snapshot = new Contentlet();
                snapshot.setInode(path[2]);
                snapshot.getMap().put(path[3], source);
                final var metadata = APILocator.getFileMetadataAPI().getMetadata(snapshot, path[3]);
                if (metadata != null) {
                    point = focalPoints.parseFocalPoint((String) metadata.getCustomMeta().get(FocalPointAPI.FOCAL_POINT));
                }
            } else if (parameters.get("assetInodeOrIdentifier") != null && parameters.get("fieldVarName") != null) {
                point = focalPoints.readFocalPoint(parameters.get("assetInodeOrIdentifier")[0],
                        parameters.get("fieldVarName")[0]);
            }
        }
        // An empty value pins absence too, so later filters cannot read a different metadata value.
        parameters.put(ImageFilter.RESOLVED_CROP_FOCAL_POINT, new String[]{point.map(FocalPoint::toString).orElse("")});
    }

    private boolean s3Renditions() {
        return com.dotcms.storage.AssetStorageFeature.isEnabled();
    }

    private File cachedRendition(final File localFile) throws DotDataException {
        final File cached = APILocator.getBinaryAssetStorageAPI().getGeneratedFile(localFile);
        if (cached == null || !cached.isFile() || cached.length() < MIN_VALID_FILE_LENGTH) {
            return null;
        }
        return cached;
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
            // Match on a directory boundary so a sibling like ".../dotGenerated2/x" is not rebased
            // against base ".../dotGenerated".
            final String prefix = base.endsWith(File.separator) ? base : base + File.separator;
            if (!full.startsWith(prefix)) {
                return null;
            }
            return new File(sharedBase + full.substring(base.length()));
        } catch (IOException e) {
            Logger.warnAndDebug(ImageFilterExporter.class, "unable to map rendition to shared store: " + e.getMessage(), e);
            return null;
        }
    }

    /** Renditions currently being published, keyed by absolute shared path, to dedupe concurrent copies. */
    private static final Set<String> publishInFlight = ConcurrentHashMap.newKeySet();

    /**
     * Fire-and-forget copy of a finished rendition to the shared store on a virtual thread so the
     * serving request returns immediately. A no-op if the rendition is already published, or if another
     * thread is already publishing this exact target (which also bounds the spawned threads to one per
     * distinct rendition rather than one per request).
     */
    private void publishToShared(final File localFinal) {
        final File shared = toSharedFile(localFinal);
        if (shared == null || localFinal == null || !localFinal.exists()) {
            return;
        }
        if (shared.exists() && shared.length() >= MIN_VALID_FILE_LENGTH) {
            return;
        }
        final String key = shared.getAbsolutePath();
        if (!publishInFlight.add(key)) {
            return;
        }
        final File sharedTmpDir = new File(ConfigUtils.getAssetTempPath());
        Thread.ofVirtual().name("dotGenerated-share").start(() -> {
            try {
                copyToShared(localFinal, shared, sharedTmpDir);
            } finally {
                publishInFlight.remove(key);
            }
        });
    }

    /**
     * Stages {@code localFinal} into {@code sharedTmpDir} and atomically moves it onto {@code shared},
     * so concurrent readers never observe a partial file.
     *
     * <p>{@code sharedTmpDir} <b>must live on the same filesystem (the shared mount) as
     * {@code shared}</b>: the move is attempted as an {@link StandardCopyOption#ATOMIC_MOVE} and only
     * a same-filesystem rename can satisfy that. The caller passes {@link ConfigUtils#getAssetTempPath()}
     * (the shared {@code tmp_upload} dir under the assets root), which sits on the same mount as the
     * shared {@code dotGenerated} store. If the filesystem cannot do an atomic move, it falls back to a
     * non-atomic replace. The staged temp file is uniquely named (so two cluster instances cannot
     * collide) and is removed if the publish fails. Package-visible and synchronous for tests; the
     * virtual-thread hand-off lives in {@link #publishToShared(File)}.</p>
     */
    static void copyToShared(final File localFinal, final File shared, final File sharedTmpDir) {
        Path tmp = null;
        try {
            Files.createDirectories(shared.toPath().getParent());
            final Path tmpDir = Files.createDirectories(sharedTmpDir.toPath());
            tmp = Files.createTempFile(tmpDir, shared.getName() + "_", ".tmp");
            Files.copy(localFinal.toPath(), tmp, StandardCopyOption.REPLACE_EXISTING);
            moveIntoPlace(tmp, shared.toPath());
            tmp = null; // moved; nothing to clean up
            Logger.debug(ImageFilterExporter.class, "published rendition to shared store: " + shared);
        } catch (Exception e) {
            Logger.warnAndDebug(ImageFilterExporter.class,
                    "failed to publish rendition to shared store " + shared + ": " + e.getMessage(), e);
        } finally {
            if (tmp != null) {
                final Path orphan = tmp;
                Try.run(() -> Files.deleteIfExists(orphan));
            }
        }
    }

    /**
     * Moves {@code tmp} onto {@code target} atomically when the filesystem supports it, falling back to
     * a non-atomic replace otherwise.
     */
    private static void moveIntoPlace(final Path tmp, final Path target) throws IOException {
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
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
