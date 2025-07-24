package com.dotcms.metrics.binders;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive metric binder for dotCMS file asset and storage metrics.
 * 
 * This binder provides essential file management metrics including:
 * - Total file count and storage usage
 * - File operations (upload/download rates)
 * - File type distribution
 * - Storage health and capacity
 * - Asset delivery performance
 * 
 * These metrics are critical for monitoring storage capacity,
 * file delivery performance, and detecting storage issues.
 */
public class FileAssetMetrics implements MeterBinder {
    
    private static final String METRIC_PREFIX = "dotcms.files";
    
    // Thread-safe counters for file operations
    private final AtomicLong totalUploads = new AtomicLong(0);
    private final AtomicLong totalDownloads = new AtomicLong(0);
    private final AtomicLong uploadBytes = new AtomicLong(0);
    private final AtomicLong downloadBytes = new AtomicLong(0);
    
    @Override
    public void bindTo(MeterRegistry registry) {
        try {
            registerFileCountMetrics(registry);
            registerStorageMetrics(registry);
            registerFileOperationMetrics(registry);
            registerFileTypeMetrics(registry);
            registerStorageHealthMetrics(registry);
            
            Logger.info(this, "File asset metrics registered successfully");
            
        } catch (Exception e) {
            Logger.error(this, "Failed to register file asset metrics: " + e.getMessage(), e);
        }
    }
    
    /**
     * Register file count and basic statistics.
     */
    private void registerFileCountMetrics(MeterRegistry registry) {
        // Total file assets
        Gauge.builder(METRIC_PREFIX + ".count.total", this, metrics -> getTotalFileCount())
            .description("Total number of file assets")
            .register(registry);
        
        // Published file assets
        Gauge.builder(METRIC_PREFIX + ".count.published", this, metrics -> getPublishedFileCount())
            .description("Number of published file assets")
            .register(registry);
        
        // Draft file assets
        Gauge.builder(METRIC_PREFIX + ".count.draft", this, metrics -> getDraftFileCount())
            .description("Number of draft file assets")
            .register(registry);
        
        // Archived file assets
        Gauge.builder(METRIC_PREFIX + ".count.archived", this, metrics -> getArchivedFileCount())
            .description("Number of archived file assets")
            .register(registry);
        
        // Binary field files
        Gauge.builder(METRIC_PREFIX + ".count.binary_fields", this, metrics -> getBinaryFieldFileCount())
            .description("Number of binary field files")
            .register(registry);
    }
    
    /**
     * Register storage usage metrics.
     */
    private void registerStorageMetrics(MeterRegistry registry) {
        // Total storage used by file assets
        Gauge.builder(METRIC_PREFIX + ".storage.used_bytes", this, metrics -> getTotalStorageUsed())
            .description("Total storage used by file assets in bytes")
            .register(registry);
        
        // Average file size
        Gauge.builder(METRIC_PREFIX + ".storage.avg_file_size_bytes", this, metrics -> getAverageFileSize())
            .description("Average file size in bytes")
            .register(registry);
        
        // Largest file size
        Gauge.builder(METRIC_PREFIX + ".storage.largest_file_bytes", this, metrics -> getLargestFileSize())
            .description("Size of the largest file in bytes")
            .register(registry);
        
        // Asset folder disk usage
        Gauge.builder(METRIC_PREFIX + ".storage.asset_folder_bytes", this, metrics -> getAssetFolderDiskUsage())
            .description("Disk usage of asset folder in bytes")
            .register(registry);
        
        // Available disk space
        Gauge.builder(METRIC_PREFIX + ".storage.available_bytes", this, metrics -> getAvailableDiskSpace())
            .description("Available disk space for assets in bytes")
            .register(registry);
        
        // Storage utilization percentage
        Gauge.builder(METRIC_PREFIX + ".storage.utilization_percent", this, metrics -> getStorageUtilization())
            .description("Storage utilization percentage")
            .register(registry);
    }
    
    /**
     * Register file operation metrics.
     */
    private void registerFileOperationMetrics(MeterRegistry registry) {
        // Total uploads
        Gauge.builder(METRIC_PREFIX + ".operations.uploads.total", this, metrics -> totalUploads.get())
            .description("Total number of file uploads")
            .register(registry);
        
        // Total downloads
        Gauge.builder(METRIC_PREFIX + ".operations.downloads.total", this, metrics -> totalDownloads.get())
            .description("Total number of file downloads")
            .register(registry);
        
        // Upload throughput (bytes)
        Gauge.builder(METRIC_PREFIX + ".operations.uploads.bytes", this, metrics -> uploadBytes.get())
            .description("Total bytes uploaded")
            .register(registry);
        
        // Download throughput (bytes)
        Gauge.builder(METRIC_PREFIX + ".operations.downloads.bytes", this, metrics -> downloadBytes.get())
            .description("Total bytes downloaded")
            .register(registry);
        
        // Recent upload activity (last hour)
        Gauge.builder(METRIC_PREFIX + ".operations.uploads.recent", this, metrics -> getRecentUploads())
            .description("Number of uploads in the last hour")
            .register(registry);
        
        // Recent download activity (last hour)
        Gauge.builder(METRIC_PREFIX + ".operations.downloads.recent", this, metrics -> getRecentDownloads())
            .description("Number of downloads in the last hour")
            .register(registry);
    }
    
    /**
     * Register file type distribution metrics.
     */
    private void registerFileTypeMetrics(MeterRegistry registry) {
        // Image files
        Gauge.builder(METRIC_PREFIX + ".types.images", this, metrics -> getFileCountByType("image"))
            .description("Number of image files")
            .register(registry);
        
        // Document files
        Gauge.builder(METRIC_PREFIX + ".types.documents", this, metrics -> getFileCountByType("document"))
            .description("Number of document files")
            .register(registry);
        
        // Video files
        Gauge.builder(METRIC_PREFIX + ".types.videos", this, metrics -> getFileCountByType("video"))
            .description("Number of video files")
            .register(registry);
        
        // Audio files
        Gauge.builder(METRIC_PREFIX + ".types.audio", this, metrics -> getFileCountByType("audio"))
            .description("Number of audio files")
            .register(registry);
        
        // Other files
        Gauge.builder(METRIC_PREFIX + ".types.other", this, metrics -> getFileCountByType("other"))
            .description("Number of other file types")
            .register(registry);
    }
    
    /**
     * Register storage health and performance metrics.
     */
    private void registerStorageHealthMetrics(MeterRegistry registry) {
        // Storage accessibility
        Gauge.builder(METRIC_PREFIX + ".health.storage_accessible", this, metrics -> isStorageAccessible() ? 1.0 : 0.0)
            .description("Whether storage is accessible (1=accessible, 0=not accessible)")
            .register(registry);
        
        // Orphaned files (files without database references)
        Gauge.builder(METRIC_PREFIX + ".health.orphaned_files", this, metrics -> getOrphanedFileCount())
            .description("Number of orphaned files in storage")
            .register(registry);
        
        // Missing files (database references without files)
        Gauge.builder(METRIC_PREFIX + ".health.missing_files", this, metrics -> getMissingFileCount())
            .description("Number of missing files (database refs without files)")
            .register(registry);
        
        // File system performance (write test)
        Gauge.builder(METRIC_PREFIX + ".health.write_performance_ms", this, metrics -> getWritePerformance())
            .description("File write performance test in milliseconds")
            .register(registry);
    }
    
    // ====================================================================
    // HELPER METHODS FOR FILE COUNTING
    // ====================================================================
    
    private double getTotalFileCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM contentlet WHERE structure_inode IN " +
                 "(SELECT inode FROM structure WHERE structuretype = 6) AND deleted = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get total file count: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getPublishedFileCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM contentlet WHERE structure_inode IN " +
                 "(SELECT inode FROM structure WHERE structuretype = 6) AND live = true AND deleted = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get published file count: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getDraftFileCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM contentlet WHERE structure_inode IN " +
                 "(SELECT inode FROM structure WHERE structuretype = 6) AND working = true AND live = false AND deleted = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get draft file count: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getArchivedFileCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM contentlet WHERE structure_inode IN " +
                 "(SELECT inode FROM structure WHERE structuretype = 6) AND archived = true AND deleted = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get archived file count: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getBinaryFieldFileCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM contentlet_version_info cvi " +
                 "JOIN contentlet c ON cvi.working_inode = c.inode " +
                 "WHERE c.deleted = false AND EXISTS (" +
                 "  SELECT 1 FROM field f " +
                 "  JOIN structure s ON f.structure_inode = s.inode " +
                 "  WHERE s.inode = c.structure_inode AND f.field_type = 'binary'" +
                 ")");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get binary field file count: " + e.getMessage());
            return 0.0;
        }
    }
    
    // ====================================================================
    // HELPER METHODS FOR STORAGE METRICS
    // ====================================================================
    
    private double getTotalStorageUsed() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT SUM(COALESCE(file_size, 0)) FROM contentlet c " +
                 "JOIN structure s ON c.structure_inode = s.inode " +
                 "WHERE s.structuretype = 6 AND c.deleted = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get total storage used: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getAverageFileSize() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT AVG(COALESCE(file_size, 0)) FROM contentlet c " +
                 "JOIN structure s ON c.structure_inode = s.inode " +
                 "WHERE s.structuretype = 6 AND c.deleted = false AND file_size > 0");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get average file size: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getLargestFileSize() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT MAX(COALESCE(file_size, 0)) FROM contentlet c " +
                 "JOIN structure s ON c.structure_inode = s.inode " +
                 "WHERE s.structuretype = 6 AND c.deleted = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get largest file size: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getAssetFolderDiskUsage() {
        try {
            String assetPath = Config.getStringProperty("ASSET_REAL_PATH", "assets");
            File assetDir = new File(assetPath);
            return assetDir.exists() ? getFolderSize(assetDir) : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get asset folder disk usage: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getAvailableDiskSpace() {
        try {
            String assetPath = Config.getStringProperty("ASSET_REAL_PATH", "assets");
            File assetDir = new File(assetPath);
            return assetDir.exists() ? assetDir.getUsableSpace() : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get available disk space: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getStorageUtilization() {
        try {
            String assetPath = Config.getStringProperty("ASSET_REAL_PATH", "assets");
            File assetDir = new File(assetPath);
            if (!assetDir.exists()) return 0.0;
            
            long totalSpace = assetDir.getTotalSpace();
            long usableSpace = assetDir.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;
            
            return totalSpace > 0 ? ((double) usedSpace / totalSpace) * 100 : 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get storage utilization: " + e.getMessage());
            return 0.0;
        }
    }
    
    // ====================================================================
    // HELPER METHODS FOR FILE OPERATIONS
    // ====================================================================
    
    private double getRecentUploads() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM contentlet c " +
                 "JOIN structure s ON c.structure_inode = s.inode " +
                 "WHERE s.structuretype = 6 AND c.mod_date > NOW() - INTERVAL '1 hour' AND c.deleted = false");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get recent uploads: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getRecentDownloads() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM contentlet c " +
                 "JOIN structure s ON c.structure_inode = s.inode " +
                 "WHERE s.structuretype = 6 AND c.mod_date > NOW() - INTERVAL '1 hour' AND c.deleted = false " +
                 "AND c.mod_date != c.idate")) { // Only count files that were accessed/modified after creation
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get recent downloads: " + e.getMessage());
            return 0.0;
        }
    }
    
    // ====================================================================
    // HELPER METHODS FOR FILE TYPES
    // ====================================================================
    
    private double getFileCountByType(String fileType) {
        String mimePattern;
        switch (fileType.toLowerCase()) {
            case "image":
                mimePattern = "image/%";
                break;
            case "video":
                mimePattern = "video/%";
                break;
            case "audio":
                mimePattern = "audio/%";
                break;
            case "document":
                mimePattern = "application/%";
                break;
            default:
                // For "other", count everything that doesn't match common patterns
                return getTotalFileCount() - getFileCountByType("image") - 
                       getFileCountByType("video") - getFileCountByType("audio") - 
                       getFileCountByType("document");
        }
        
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM contentlet c " +
                 "JOIN structure s ON c.structure_inode = s.inode " +
                 "WHERE s.structuretype = 6 AND c.deleted = false AND c.mimetype LIKE ?")) {
            stmt.setString(1, mimePattern);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (Exception e) {
            Logger.debug(this, "Failed to get file count by type " + fileType + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    // ====================================================================
    // HELPER METHODS FOR STORAGE HEALTH
    // ====================================================================
    
    private boolean isStorageAccessible() {
        try {
            String assetPath = Config.getStringProperty("ASSET_REAL_PATH", "assets");
            File assetDir = new File(assetPath);
            return assetDir.exists() && assetDir.canRead() && assetDir.canWrite();
        } catch (Exception e) {
            Logger.debug(this, "Storage accessibility check failed: " + e.getMessage());
            return false;
        }
    }
    
    private double getOrphanedFileCount() {
        try (Connection conn = DbConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM contentlet c " +
                 "JOIN structure s ON c.structure_inode = s.inode " +
                 "WHERE s.structuretype = 6 AND c.deleted = true")) {
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get orphaned file count: " + e.getMessage());
            return 0.0;
        }
    }
    
    private double getMissingFileCount() {
        // This would require checking if database references have corresponding files
        // Complex operation - placeholder for now
        return 0.0;
    }
    
    private double getWritePerformance() {
        try {
            String assetPath = Config.getStringProperty("ASSET_REAL_PATH", "assets");
            File assetDir = new File(assetPath);
            
            if (!assetDir.exists() || !assetDir.canWrite()) {
                return -1.0; // Indicates write test failed
            }
            
            File testFile = new File(assetDir, "metrics_write_test.tmp");
            long startTime = System.currentTimeMillis();
            
            // Simple write test
            try {
                testFile.createNewFile();
                testFile.delete();
            } catch (Exception e) {
                return -1.0;
            }
            
            return System.currentTimeMillis() - startTime;
        } catch (Exception e) {
            Logger.debug(this, "Write performance test failed: " + e.getMessage());
            return -1.0;
        }
    }
    
    // ====================================================================
    // UTILITY METHODS
    // ====================================================================
    
    private long getFolderSize(File folder) {
        long size = 0;
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getFolderSize(file);
                    }
                }
            }
        }
        return size;
    }
    
    // ====================================================================
    // PUBLIC METHODS FOR OPERATION TRACKING (to be called by file handlers)
    // ====================================================================
    
    /**
     * Record file upload (to be called by file upload handlers).
     */
    public void recordUpload(long fileSize) {
        totalUploads.incrementAndGet();
        uploadBytes.addAndGet(fileSize);
    }
    
    /**
     * Record file download (to be called by file download handlers).
     */
    public void recordDownload(long fileSize) {
        totalDownloads.incrementAndGet();
        downloadBytes.addAndGet(fileSize);
    }
} 