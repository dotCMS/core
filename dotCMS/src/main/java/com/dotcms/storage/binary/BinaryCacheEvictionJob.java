package com.dotcms.storage.binary;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Quartz job that evicts the oldest binary files from the local FS cache when
 * it exceeds a configurable size threshold. Only active in BINARY_CHAIN mode
 * where the local filesystem is a cache layer backed by S3 as the durable store.
 *
 * <p>Eviction uses a least-recently-modified (LRM) strategy: files are sorted by
 * {@code lastModified} timestamp and the oldest files are deleted first until
 * total cache size is at or below the threshold. Files modified within the
 * configured minimum age window are protected from eviction.</p>
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
public class BinaryCacheEvictionJob implements StatefulJob {

    public static final String BINARY_CACHE_MAX_SIZE_MB_PROP = "BINARY_CACHE_MAX_SIZE_MB";
    public static final long BINARY_CACHE_MAX_SIZE_MB_DEFAULT = 5000; // 5GB

    public static final String BINARY_CACHE_EVICTION_MIN_AGE_MINUTES_PROP = "BINARY_CACHE_EVICTION_MIN_AGE_MINUTES";
    public static final int BINARY_CACHE_EVICTION_MIN_AGE_MINUTES_DEFAULT = 60; // 1 hour

    public static final String BINARY_CACHE_EVICTION_CRON_PROP = "BINARY_CACHE_EVICTION_CRON";

    private static final long BYTES_PER_MB = 1024L * 1024L;
    private static final int LARGE_CACHE_FILE_WARNING_THRESHOLD = 500_000;

    public BinaryCacheEvictionJob() {
        // Empty constructor required by Quartz
    }

    @Override
    public void execute(final JobExecutionContext ctx) throws JobExecutionException {

        // Gate: only run in BINARY_CHAIN mode
        final String storageType = Config.getStringProperty(
                BinaryAssetStorageAPIImpl.BINARY_ASSET_STORAGE_TYPE_PROP, "FILE_SYSTEM");
        if (!"BINARY_CHAIN".equalsIgnoreCase(storageType)) {
            Logger.info(this, "BinaryCacheEvictionJob skipped — not in BINARY_CHAIN mode");
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

        // Sort by lastModified ascending (oldest first)
        files.sort(Comparator.comparingLong(f -> f.lastModified));

        int evictedCount = 0;
        long freedBytes = 0;

        for (final FileInfo fileInfo : files) {
            if (totalBytes <= thresholdBytes) {
                break;
            }

            // Skip files newer than min age
            if ((now - fileInfo.lastModified) < minAgeMillis) {
                continue;
            }

            try {
                if (Files.deleteIfExists(fileInfo.path)) {
                    totalBytes -= fileInfo.size;
                    freedBytes += fileInfo.size;
                    evictedCount++;
                }
            } catch (final IOException e) {
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

        final List<FileInfo> files = new ArrayList<>();

        if (!assetRoot.exists() || !assetRoot.isDirectory()) {
            return files;
        }

        // Level 1: single-char prefix directories (inode first char)
        final File[] level1Dirs = assetRoot.listFiles(
                f -> f.isDirectory() && f.getName().length() == 1);
        if (level1Dirs == null) {
            return files;
        }

        for (final File l1 : level1Dirs) {
            // Level 2: single-char prefix directories (inode second char)
            final File[] level2Dirs = l1.listFiles(
                    f -> f.isDirectory() && f.getName().length() == 1);
            if (level2Dirs == null) {
                continue;
            }

            for (final File l2 : level2Dirs) {
                // Walk 3 levels deep: inode/field/file
                try (final Stream<Path> walk = Files.walk(l2.toPath(), 3)) {
                    walk.filter(Files::isRegularFile)
                            .forEach(path -> {
                                try {
                                    files.add(new FileInfo(
                                            path,
                                            Files.size(path),
                                            Files.getLastModifiedTime(path).toMillis()));
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

        FileInfo(final Path path, final long size, final long lastModified) {
            this.path = path;
            this.size = size;
            this.lastModified = lastModified;
        }
    }

}
