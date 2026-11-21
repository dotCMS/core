package com.dotmarketing.portlets.contentlet.business.exporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
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
    
    private static final int DEFAULT_GENERATION_LIMIT = 2;
    private static final Semaphore semaphore = new Semaphore(Math.max(1,
            Config.getIntProperty("IMAGE_GENERATION_SIMULTANEOUS_REQUESTS", DEFAULT_GENERATION_LIMIT)));
    private static final long MIN_VALID_FILE_LENGTH = 50L;

    /** Resolve the active engine without changing the URL parameter contract. */
    ImageFilterAPI imageFilterAPI() {
        return ImageEngine.resolve();
    }

    /** Serve a cached rendition or reserve capacity for the entire filter chain. */
    @Override
    public BinaryContentExporterData exportContent(File file, final Map<String, String[]> input)
            throws BinaryContentExporterException {
        final String extension = UtilMethods.getFileExtension(file.getName());
        if (UtilMethods.isVectorImage(extension)) {
            return new BinaryContentExporterData(file);
        }

        try {
            final Map<String, String[]> parameters = new LinkedHashMap<>(input);
            final Map<String, Class<? extends ImageFilter>> resolved = imageFilterAPI().resolveFilters(parameters);
            if (resolved.isEmpty()) {
                return new BinaryContentExporterData(file);
            }
            final List<ImageFilter> filters = new ArrayList<>();
            if ("pdf".equalsIgnoreCase(extension) && !resolved.containsKey("pdf")) {
                filters.add(new PDFImageFilter());
            }
            for (final Class<? extends ImageFilter> type : resolved.values()) {
                filters.add(type.getDeclaredConstructor().newInstance());
            }
            parameters.put("filter", resolved.keySet().toArray(String[]::new));
            parameters.put("filters", parameters.get("filter"));
            final boolean shared = ConfigUtils.isDotGeneratedSharedCompleted();
            final File cached = cachedResult(filters, file, new LinkedHashMap<>(parameters), shared);
            if (cached != null) {
                return new BinaryContentExporterData(cached);
            }
            if (!semaphore.tryAcquire()) {
                throw new ImageGenerationBusyException();
            }

            try {
                for (final ImageFilter filter : filters) {
                    if (filter.prepare(file, file, parameters) == null) {
                        throw new DotRuntimeException("Missing input for " + filter.getClass().getSimpleName());
                    }
                    file = filter.runFilter(file, parameters);
                }
                if (shared) {
                    publishToShared(file);
                }
                return new BinaryContentExporterData(file);
            } finally {
                semaphore.release();
            }
        } catch (ImageGenerationBusyException e) {
            throw e;
        } catch (Exception e) {
            Logger.warnAndDebug(getClass(), e);
            throw new BinaryContentExporterException(e.getMessage(), e);
        }
    }

    /** Resolve normalized cache paths without rendering missing intermediate images. */
    private File cachedResult(final List<ImageFilter> filters, final File source,
            final Map<String, String[]> parameters, final boolean shared) {
        File path = source;
        for (final ImageFilter filter : filters) {
            final File sharedInput = shared && !path.equals(source) && !valid(path)
                    ? toSharedFile(path) : null;
            path = filter.prepare(path, valid(sharedInput) ? sharedInput : path, parameters);
            // shortcut: a cold node can miss a shared final result; cache intermediate metadata if needed.
            if (path == null) {
                return null;
            }
        }
        if (valid(path)) {
            return path;
        }
        final File sharedResult = shared ? toSharedFile(path) : null;
        return valid(sharedResult) ? sharedResult : null;
    }

    /** Ignore absent, empty, or incomplete cache files. */
    private static boolean valid(final File file) {
        return file != null && file.isFile() && file.length() >= MIN_VALID_FILE_LENGTH;
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
