package com.dotcms.storage.binary;

import com.dotcms.storage.AssetStorageFeature;
import com.dotmarketing.util.Config;
import com.dotmarketing.business.APILocator;
import java.util.function.Supplier;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Evicts binary files from this node's local FS cache when it exceeds a configurable size
 * threshold. {@code DotInitScheduler} runs it on a node-local thread. Only active when the S3 asset
 * storage flag is on, where the local filesystem is a cache layer backed by S3 as the durable store.
 *
 * <p>Eviction is least-recently-read: files are sorted by access time and the least recently read
 * are deleted first until total cache size is at or below the threshold. {@link BinaryAssetStorageAPIImpl}
 * sets the access time explicitly whenever it hands a cached file to a reader, so the order holds on
 * {@code noatime} mounts; it leaves the modification time alone because HTTP ETags are built from it.
 * Files read or written within the configured minimum age window are protected from eviction.</p>
 *
 * <p>The job only walks binary asset inode directories (single-char prefix
 * structure), not the entire asset root which contains metadata, thumbnails,
 * and other system files.</p>
 *
 * <p>Configuration properties:</p>
 * <ul>
 *     <li>{@code BINARY_CACHE_MAX_SIZE_MB} — Maximum cache size in MB (default: 5000 = 5GB)</li>
 *     <li>{@code BINARY_CACHE_EVICTION_MIN_AGE_MINUTES} — Minimum file age before eligible
 *         for eviction (default: 60 minutes)</li>
 *     <li>{@code BINARY_CACHE_EVICTION_CRON} — Cron expression for scheduling
 *         (no default — job only runs when explicitly configured)</li>
 * </ul>
 */
public class BinaryCacheEvictionJob implements Runnable {

    public static final String BINARY_CACHE_MAX_SIZE_MB_PROP = "BINARY_CACHE_MAX_SIZE_MB";
    public static final long BINARY_CACHE_MAX_SIZE_MB_DEFAULT = 5000; // 5GB

    public static final String BINARY_CACHE_EVICTION_MIN_AGE_MINUTES_PROP = "BINARY_CACHE_EVICTION_MIN_AGE_MINUTES";
    public static final int BINARY_CACHE_EVICTION_MIN_AGE_MINUTES_DEFAULT = 60; // 1 hour

    public static final String BINARY_CACHE_EVICTION_CRON_PROP = "BINARY_CACHE_EVICTION_CRON";

    private static final long BYTES_PER_MB = 1024L * 1024L;
    private static final int LARGE_CACHE_FILE_WARNING_THRESHOLD = 500_000;

    private final Supplier<BinaryAssetStorageAPI> binaryStorage;

    public BinaryCacheEvictionJob() {
        this(APILocator::getBinaryAssetStorageAPI);
    }

    BinaryCacheEvictionJob(final Supplier<BinaryAssetStorageAPI> binaryStorage) {
        this.binaryStorage = binaryStorage;
    }

    /**
     * Runs one eviction pass over this node's local cache. Does nothing with the flag off or while the
     * cache is within its size limit.
     */
    @Override
    public void run() {

        if (!AssetStorageFeature.isEnabled()) {
            return;
        }

        final long startTime = System.currentTimeMillis();
        final long thresholdBytes = Config.getLongProperty(
                BINARY_CACHE_MAX_SIZE_MB_PROP, BINARY_CACHE_MAX_SIZE_MB_DEFAULT) * BYTES_PER_MB;
        final int minAgeMinutes = Config.getIntProperty(
                BINARY_CACHE_EVICTION_MIN_AGE_MINUTES_PROP, BINARY_CACHE_EVICTION_MIN_AGE_MINUTES_DEFAULT);
        final long minAgeMillis = minAgeMinutes * 60L * 1000L;
        final long now = System.currentTimeMillis();

        // Collect binary asset files from inode directories only
        final File assetRoot = new File(ConfigUtils.getAssetPath());
        final List<FileInfo> files;
        try {
            files = collectBinaryAssetFiles(assetRoot);
            // Completed renditions share the cache budget; transient files are never candidates.
            final String generatedRoot = ConfigUtils.getDotGeneratedPath();
            if (generatedRoot != null) {
                files.addAll(collectBinaryAssetFiles(new File(generatedRoot), true));
            }
        } catch (final IOException e) {
            Logger.error(this, "BinaryCacheEvictionJob: failed to walk asset directory: " + e.getMessage(), e);
            return;
        }

        if (files.size() > LARGE_CACHE_FILE_WARNING_THRESHOLD) {
            Logger.warn(this, String.format(
                    "BinaryCacheEvictionJob: large cache (%d files) — consider increasing threshold",
                    files.size()));
        }

        long totalBytes = files.stream().mapToLong(f -> f.size).sum();
        final long thresholdMB = thresholdBytes / BYTES_PER_MB;

        Logger.info(this, String.format(
                "BinaryCacheEvictionJob starting: cacheSizeMB=%d thresholdMB=%d fileCount=%d",
                totalBytes / BYTES_PER_MB, thresholdMB, files.size()));

        if (totalBytes <= thresholdBytes) {
            Logger.info(this, String.format(
                    "BinaryCacheEvictionJob: cache size %dMB within limit %dMB — no eviction needed",
                    totalBytes / BYTES_PER_MB, thresholdMB));
            return;
        }

        // Least recently read first. Reads set the access time explicitly; writes leave it at creation.
        files.sort(Comparator.comparingLong(f -> f.lastRead));

        int evictedCount = 0;
        long freedBytes = 0;

        for (final FileInfo fileInfo : files) {
            if (totalBytes <= thresholdBytes) {
                break;
            }

            // Skip files read or written within the minimum age
            if ((now - fileInfo.lastRead) < minAgeMillis) {
                continue;
            }

            try {
                if (Files.size(fileInfo.path) != fileInfo.size
                        || Files.getLastModifiedTime(fileInfo.path).toMillis() != fileInfo.lastModified) {
                    continue;
                }
                if (binaryStorage.get().evictLocalFile(fileInfo.path.toFile())) {
                    totalBytes -= fileInfo.size;
                    freedBytes += fileInfo.size;
                    evictedCount++;
                }
            } catch (final Exception e) {
                Logger.warn(this, String.format(
                        "BinaryCacheEvictionJob: failed to delete %s: %s",
                        fileInfo.path, e.getMessage()));
            }
        }

        final long durationMs = System.currentTimeMillis() - startTime;
        Logger.info(this, String.format(
                "BinaryCacheEvictionJob complete: evicted=%d freedMB=%d remainingMB=%d thresholdMB=%d durationMs=%d",
                evictedCount, freedBytes / BYTES_PER_MB, totalBytes / BYTES_PER_MB,
                thresholdMB, durationMs));
    }

    /**
     * Collects binary asset files from the inode directory structure.
     * Only walks directories matching the binary asset pattern:
     * {@code {assetRoot}/{char}/{char}/{inode}/{field}/{file}}.
     *
     * <p>Single-character directories at the first two levels are inode prefix
     * directories. Multi-character directories (tmp_upload, bundles, etc.)
     * are skipped.</p>
     *
     * @param assetRoot the asset root directory
     * @return list of FileInfo for all binary asset files found
     * @throws IOException if the directory walk fails
     */
    List<FileInfo> collectBinaryAssetFiles(final File assetRoot) throws IOException {
        return collectBinaryAssetFiles(assetRoot, false);
    }

    /**
     * Collects evictable files under the asset root, or with {@code generated} set, completed
     * renditions under the dotGenerated root.
     *
     * @param assetRoot the root directory to walk
     * @param generated whether the root is the dotGenerated root
     * @return list of FileInfo for all evictable files found
     * @throws IOException if the directory walk fails
     */
    private List<FileInfo> collectBinaryAssetFiles(final File assetRoot, final boolean generated) throws IOException {
        final boolean s3 = AssetStorageFeature.isEnabled();
        final List<FileInfo> files = new ArrayList<>();

        if (!assetRoot.exists() || !assetRoot.isDirectory()) {
            return files;
        }

        // Level 1: single-char prefix directories (inode first char)
        final File[] level1Dirs = assetRoot.listFiles(
                f -> f.isDirectory() && f.getName().length() == 1
                        && (!s3 || !Files.isSymbolicLink(f.toPath())));
        if (level1Dirs == null) {
            return files;
        }

        for (final File l1 : level1Dirs) {
            // Level 2: single-char prefix directories (inode second char)
            final File[] level2Dirs = l1.listFiles(
                    f -> f.isDirectory() && f.getName().length() == 1
                        && (!s3 || !Files.isSymbolicLink(f.toPath())));
            if (level2Dirs == null) {
                continue;
            }

            for (final File l2 : level2Dirs) {
                // Immutable revisions add two levels: inode/field/.revisions/revision/file.
                final int depth = AssetStorageFeature.isEnabled() ? 5 : 3;
                try (final Stream<Path> walk = Files.walk(l2.toPath(), depth)) {
                    walk.filter(path -> !Files.isSymbolicLink(path) && Files.isRegularFile(path))
                            .filter(path -> !s3 || BinaryAssetStorageAPIImpl.isEvictableAssetPath(
                                    assetRoot.toPath().relativize(path), generated))
                            .forEach(path -> {
                                try {
                                    final BasicFileAttributes attributes =
                                            Files.readAttributes(path, BasicFileAttributes.class);
                                    files.add(new FileInfo(
                                            path,
                                            attributes.size(),
                                            attributes.lastModifiedTime().toMillis(),
                                            attributes.lastAccessTime().toMillis()));
                                } catch (final IOException e) {
                                    Logger.warn(this, "Failed to stat file: " + path);
                                }
                            });
                }
            }
        }

        return files;
    }

    /**
     * Holds file metadata for eviction sorting and size tracking.
     */
    static class FileInfo {

        final Path path;
        final long size;
        final long lastModified;
        final long lastRead;

        /**
         * @param path         the file
         * @param size         the file size in bytes
         * @param lastModified the modification time, used to detect a change before eviction
         * @param lastRead     the access time, which orders eviction and applies the minimum age
         */
        FileInfo(final Path path, final long size, final long lastModified, final long lastRead) {
            this.path = path;
            this.size = size;
            this.lastModified = lastModified;
            this.lastRead = lastRead;
        }
    }

}
