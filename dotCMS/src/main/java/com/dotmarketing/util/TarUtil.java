package com.dotmarketing.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.Objects;

/**
 * Utility class for handling TAR file operations safely.
 * All TAR file operations should use methods from this class to ensure consistent
 * security measures against path traversal vulnerabilities.
 * 
 * @see ZipUtil for similar approach with ZIP files
 */
public class TarUtil {

    /**
     * Default buffer size for I/O operations with TAR files (8KB)
     */
    private static final int DEFAULT_BUFFER_SIZE = ArchiveUtil.DEFAULT_BUFFER_SIZE;
    private static final int GZIP_BUFFER_SIZE = 65536;
    
    /**
     * Configuration keys for archive extraction limits
     */
    public static final String TAR_MAX_TOTAL_SIZE_KEY = "TAR_MAX_TOTAL_SIZE";
    public static final String TAR_MAX_FILE_SIZE_KEY = "TAR_MAX_FILE_SIZE";
    public static final String TAR_MAX_ENTRIES_KEY = "TAR_MAX_ENTRIES";
    
    /**
     * Default protection limits for archive extraction - use shared constants
     */
    private static final long DEFAULT_MAX_TOTAL_SIZE = ArchiveUtil.DEFAULT_MAX_TOTAL_SIZE;
    private static final long DEFAULT_MAX_FILE_SIZE = ArchiveUtil.DEFAULT_MAX_FILE_SIZE;
    private static final int DEFAULT_MAX_ENTRIES = ArchiveUtil.DEFAULT_MAX_ENTRIES;

    /**
     * Defines how suspicious entries should be handled
     */
    public enum SuspiciousEntryHandling {
        /**
         * Throw a SecurityException immediately when a suspicious entry is found and stop processing
         */
        ABORT,
        
        /**
         * Log a warning and skip suspicious entries but continue processing other entries
         */
        SKIP_AND_CONTINUE
    }
    
    /**
     * Default handling mode for suspicious entries - using ThreadLocal for thread safety
     */
    private static ThreadLocal<SuspiciousEntryHandling> defaultHandlingMode = 
            ThreadLocal.withInitial(() -> SuspiciousEntryHandling.ABORT);
    
    /**
     * Sets the default handling mode for suspicious entries for the current thread
     * 
     * @param handlingMode The handling mode to use for suspicious entries
     */
    public static void setDefaultSuspiciousEntryHandling(SuspiciousEntryHandling handlingMode) {
        defaultHandlingMode.set(handlingMode);
    }
    
    /**
     * Gets the current default handling mode for suspicious entries for the current thread
     * 
     * @return The current handling mode
     */
    public static SuspiciousEntryHandling getDefaultSuspiciousEntryHandling() {
        return defaultHandlingMode.get();
    }
    
    /**
     * Gets the maximum total size of all extracted files (in bytes)
     * 
     * @return Maximum total size in bytes from config or default value
     */
    public static long getMaxTotalSize() {
        // First check if there's a system property (for unit tests)
        String sysProp = System.getProperty(TAR_MAX_TOTAL_SIZE_KEY);
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            return ArchiveUtil.parseSize(sysProp, DEFAULT_MAX_TOTAL_SIZE);
        }
        // Fall back to config
        String configValue = Config.getStringProperty(TAR_MAX_TOTAL_SIZE_KEY, null);
        return ArchiveUtil.parseSize(configValue, DEFAULT_MAX_TOTAL_SIZE);
    }
    
    /**
     * Gets the maximum size of a single extracted file (in bytes)
     * 
     * @return Maximum file size in bytes from config or default value
     */
    public static long getMaxFileSize() {
        // First check if there's a system property (for unit tests)
        String sysProp = System.getProperty(TAR_MAX_FILE_SIZE_KEY);
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            return ArchiveUtil.parseSize(sysProp, DEFAULT_MAX_FILE_SIZE);
        }
        // Fall back to config
        String configValue = Config.getStringProperty(TAR_MAX_FILE_SIZE_KEY, null);
        return ArchiveUtil.parseSize(configValue, DEFAULT_MAX_FILE_SIZE);
    }
    
    /**
     * Gets the maximum number of entries to extract
     * 
     * @return Maximum number of entries from config or default value
     */
    public static int getMaxEntries() {
        // First check if there's a system property (for unit tests)
        String sysProp = System.getProperty(TAR_MAX_ENTRIES_KEY);
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            try {
                return Integer.parseInt(sysProp);
            } catch (NumberFormatException e) {
                Logger.warn(TarUtil.class, "Invalid TAR_MAX_ENTRIES value: " + sysProp + ", using default: " + DEFAULT_MAX_ENTRIES);
            }
        }
        // Fall back to config
        return Config.getIntProperty(TAR_MAX_ENTRIES_KEY, DEFAULT_MAX_ENTRIES);
    }

    /**
     * Converts TarUtil handling mode to ArchiveUtil handling mode
     * 
     * @param mode The TarUtil handling mode
     * @return The equivalent ArchiveUtil handling mode
     */
    private static ArchiveUtil.SuspiciousEntryHandling convertHandlingMode(SuspiciousEntryHandling mode) {
        if (mode == SuspiciousEntryHandling.ABORT) {
            return ArchiveUtil.SuspiciousEntryHandling.ABORT;
        } else {
            return ArchiveUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE;
        }
    }

    /**
     * Creates a TAR archive output stream with GZIP compression
     * 
     * @param outputFile The file to write to
     * @return A configured TarArchiveOutputStream
     * @throws IOException If an error occurs while creating the output stream
     */
    public static TarArchiveOutputStream createTarGzOutputStream(File outputFile) throws IOException {
        final TarArchiveOutputStream taos = new TarArchiveOutputStream(
                new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)), GZIP_BUFFER_SIZE));
        
        // Configure for large files and long filenames
        taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
        taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        
        return taos;
    }
    
    /**
     * Creates a TAR archive output stream with GZIP compression
     * 
     * @param outputStream The output stream to write to
     * @return A configured TarArchiveOutputStream
     * @throws IOException If an error occurs while creating the output stream
     */
    public static TarArchiveOutputStream createTarGzOutputStream(OutputStream outputStream) throws IOException {
        final TarArchiveOutputStream taos = new TarArchiveOutputStream(
                new GZIPOutputStream(outputStream, GZIP_BUFFER_SIZE));
        
        // Configure for large files and long filenames
        taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
        taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        
        return taos;
    }
    
    /**
     * Creates a new TarArchiveEntry with a sanitized path to prevent path traversal vulnerabilities
     * 
     * @param path The path for the entry
     * @param handlingMode How to handle suspicious paths
     * @return A TarArchiveEntry with a sanitized path
     * @throws SecurityException If the path is suspicious and handlingMode is ABORT
     */
    public static TarArchiveEntry createSafeTarEntry(String path, SuspiciousEntryHandling handlingMode) {
        return new TarArchiveEntry(ArchiveUtil.sanitizePath(path, convertHandlingMode(handlingMode)));
    }
    
    /**
     * Creates a new TarArchiveEntry with a sanitized path using the default handling mode
     * 
     * @param path The path for the entry
     * @return A TarArchiveEntry with a sanitized path
     * @throws SecurityException If the path is suspicious and default handling mode is ABORT
     */
    public static TarArchiveEntry createSafeTarEntry(String path) {
        return createSafeTarEntry(path, defaultHandlingMode.get());
    }
    
    /**
     * Creates a new TarArchiveEntry for a file with a sanitized path
     * 
     * @param file The file to create an entry for
     * @param relativePath The relative path within the archive
     * @param handlingMode How to handle suspicious paths
     * @return A TarArchiveEntry with a sanitized path
     * @throws SecurityException If the path is suspicious and handlingMode is ABORT
     */
    public static TarArchiveEntry createSafeTarEntry(File file, String relativePath, SuspiciousEntryHandling handlingMode) {
        String safePath = ArchiveUtil.sanitizePath(relativePath, convertHandlingMode(handlingMode));
        return new TarArchiveEntry(file, safePath);
    }
    
    /**
     * Creates a new TarArchiveEntry for a file with a sanitized path using the default handling mode
     * 
     * @param file The file to create an entry for
     * @param relativePath The relative path within the archive
     * @return A TarArchiveEntry with a sanitized path
     * @throws SecurityException If the path is suspicious and default handling mode is ABORT
     */
    public static TarArchiveEntry createSafeTarEntry(File file, String relativePath) {
        return createSafeTarEntry(file, relativePath, defaultHandlingMode.get());
    }
    
    /**
     * Adds a file to a TAR archive with a sanitized path
     * 
     * @param taos The TAR archive output stream
     * @param file The file to add
     * @param relativePath The relative path within the archive
     * @param handlingMode How to handle suspicious paths
     * @throws IOException If an error occurs during the add operation
     */
    public static void addFileToTar(TarArchiveOutputStream taos, File file, String relativePath, 
                                   SuspiciousEntryHandling handlingMode) throws IOException {
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }
        
        TarArchiveEntry entry = createSafeTarEntry(file, relativePath, handlingMode);
        taos.putArchiveEntry(entry);
        
        if (file.isFile()) {
            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                IOUtils.copy(is, taos);
            }
        }
        
        taos.closeArchiveEntry();
    }
    
    /**
     * Adds a file to a TAR archive with a sanitized path using the default handling mode
     * 
     * @param taos The TAR archive output stream
     * @param file The file to add
     * @param relativePath The relative path within the archive
     * @throws IOException If an error occurs during the add operation
     */
    public static void addFileToTar(TarArchiveOutputStream taos, File file, String relativePath) throws IOException {
        addFileToTar(taos, file, relativePath, defaultHandlingMode.get());
    }
    
    /**
     * Adds byte array content to a TAR archive with a sanitized path
     * 
     * @param taos The TAR archive output stream
     * @param bytes The content to add
     * @param entryPath The path for the entry
     * @param handlingMode How to handle suspicious paths
     * @throws IOException If an error occurs during the add operation
     */
    public static void addBytesToTar(TarArchiveOutputStream taos, byte[] bytes, String entryPath,
                                   SuspiciousEntryHandling handlingMode) throws IOException {
        String sanitizedPath = ArchiveUtil.sanitizePath(entryPath, convertHandlingMode(handlingMode));
        TarArchiveEntry entry = new TarArchiveEntry(sanitizedPath);
        entry.setSize(bytes.length);
        
        taos.putArchiveEntry(entry);
        
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            IOUtils.copy(is, taos);
        }
        
        taos.closeArchiveEntry();
    }
    
    /**
     * Adds byte array content to a TAR archive with a sanitized path using the default handling mode
     * 
     * @param taos The TAR archive output stream
     * @param bytes The content to add
     * @param entryPath The path for the entry
     * @throws IOException If an error occurs during the add operation
     */
    public static void addBytesToTar(TarArchiveOutputStream taos, byte[] bytes, String entryPath) throws IOException {
        addBytesToTar(taos, bytes, entryPath, defaultHandlingMode.get());
    }
    
    /**
     * Adds a directory to a TAR archive with sanitized paths using the default handling mode.
     * This method provides a simplified API to add a directory to an existing archive.
     * 
     * @param taos The TAR archive output stream
     * @param directory The directory to add
     * @param basePath The base path of the directory (for path calculation)
     * @param baseDirectory The base directory name in the archive
     * @throws IOException If an error occurs during the add operation
     */
    public static void addDirectoryToTar(TarArchiveOutputStream taos, File directory, String basePath,
                                       String baseDirectory) throws IOException {
        addDirectoryToTar(taos, directory, basePath, baseDirectory, defaultHandlingMode.get());
    }
    
    /**
     * Adds a directory to a TAR archive with sanitized paths.
     * This version leverages the functional interfaces for more maintainable code.
     * 
     * @param taos The TAR archive output stream
     * @param directory The directory to add
     * @param basePath The base path of the directory (for path calculation)
     * @param baseDirectory The base directory name in the archive
     * @param handlingMode How to handle suspicious paths
     * @throws IOException If an error occurs during the add operation
     */
    public static void addDirectoryToTar(TarArchiveOutputStream taos, File directory, String basePath,
                                       String baseDirectory, SuspiciousEntryHandling handlingMode) throws IOException {
        Objects.requireNonNull(taos, "TAR output stream cannot be null");
        Objects.requireNonNull(directory, "Directory cannot be null");
        
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Source must be a directory");
        }
        
        // Add directory entry if base path is not empty
        if (baseDirectory != null && !baseDirectory.isEmpty()) {
            String sanitizedBaseDir = ArchiveUtil.sanitizePath(baseDirectory, convertHandlingMode(handlingMode));
            if (!sanitizedBaseDir.endsWith("/")) {
                sanitizedBaseDir += "/";
            }
            TarArchiveEntry dirEntry = createSafeTarEntry(sanitizedBaseDir, handlingMode);
            taos.putArchiveEntry(dirEntry);
            taos.closeArchiveEntry();
        }
        
        // Process children
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                // Calculate relative path from base path
                String relativePath = file.getAbsolutePath().substring(basePath.length() + 1);
                String entryPath = baseDirectory != null && !baseDirectory.isEmpty() ? 
                    baseDirectory + "/" + relativePath : relativePath;
                
                // Sanitize the entry path
                String sanitizedEntryPath = ArchiveUtil.sanitizePath(entryPath, convertHandlingMode(handlingMode));
                
                if (file.isDirectory()) {
                    // Add directory entry
                    String dirEntryPath = sanitizedEntryPath.endsWith("/") ? sanitizedEntryPath : sanitizedEntryPath + "/";
                    TarArchiveEntry dirEntry = createSafeTarEntry(dirEntryPath, handlingMode);
                    taos.putArchiveEntry(dirEntry);
                    taos.closeArchiveEntry();
                    
                    // Recursively process subdirectory
                    addDirectoryToTar(taos, file, basePath, sanitizedEntryPath, handlingMode);
                } else {
                    // Add file
                    TarArchiveEntry fileEntry = createSafeTarEntry(file, sanitizedEntryPath, handlingMode);
                    taos.putArchiveEntry(fileEntry);
                    try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                        IOUtils.copy(is, taos);
                    }
                    taos.closeArchiveEntry();
                }
            }
        }
    }
    
    /**
     * Safely extracts a TAR entry to the specified directory
     * 
     * @param tarIn The TAR archive input stream
     * @param entry The entry to extract
     * @param outputDir The target directory for extraction
     * @param handlingMode How to handle suspicious paths
     * @param totalSizeCounter Counter for tracking total extracted size
     * @return true if extraction was successful, false if skipped
     * @throws IOException If an error occurs during extraction
     */
    private static boolean safeExtractEntry(TarArchiveInputStream tarIn, TarArchiveEntry entry, 
                                         File outputDir, SuspiciousEntryHandling handlingMode,
                                         AtomicLong totalSizeCounter) throws IOException {
        // Skip symbolic links and other special files for security
        if (entry.isLink() || entry.isSymbolicLink() || !entry.isFile() && !entry.isDirectory()) {
            Logger.warn(TarUtil.class, "Skipping special entry: " + entry.getName());
            return false;
        }
        // Check file size using shared implementation
        if (!ArchiveUtil.checkEntrySizeLimit(entry.getName(), entry.getSize(), getMaxFileSize(), 
            convertHandlingMode(handlingMode))) {
            return false;
        }
        // Check total size using shared implementation
        if (!ArchiveUtil.checkTotalSizeLimit(entry.getName(), entry.getSize(), totalSizeCounter, 
            getMaxTotalSize(), convertHandlingMode(handlingMode))) {
            return false;
        }
        
        try {
            // Sanitize the entry name
            String sanitizedName = ArchiveUtil.sanitizePath(entry.getName(), convertHandlingMode(handlingMode), "extract");
            if (!sanitizedName.equals(entry.getName())) {
                Logger.warn(TarUtil.class, "Potentially malicious tar entry renamed: " + 
                        entry.getName() + " -> " + sanitizedName);
            }
            
            // Create the full path to the target file/directory
            File targetFile = new File(outputDir, sanitizedName);
            
            // Ensure all parent directories are directories
            ensureParentDirectoriesAreDirectories(targetFile);
            
            // Check security using shared implementation
            if (!ArchiveUtil.checkSecurity(outputDir, targetFile, convertHandlingMode(handlingMode))) {
                return false;
            }
            
            // Handle directory/file conflicts
            if (targetFile.exists()) {
                if (entry.isDirectory() && targetFile.isFile()) {
                    Logger.warn(TarUtil.class, "Replacing file with directory during extraction: " + targetFile.getAbsolutePath());
                    if (!targetFile.delete()) {
                        throw new IOException("Failed to delete file to create directory: " + targetFile);
                    }
                } else if (!entry.isDirectory() && targetFile.isDirectory()) {
                    Logger.warn(TarUtil.class, "Replacing directory with file during extraction: " + targetFile.getAbsolutePath());
                    if (!deleteDirectory(targetFile)) {
                        throw new IOException("Failed to delete directory to create file: " + targetFile);
                    }
                }
            }
            
            // Create directories or extract files
            if (entry.isDirectory()) {
                if (!targetFile.exists() && !targetFile.mkdirs()) {
                    Logger.error(TarUtil.class, "Failed to create directory: " + targetFile.getAbsolutePath());
                    return false;
                }
            } else {
                // Ensure parent directory exists (double-check, since we already tried to create them above)
                File parent = targetFile.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    Logger.error(TarUtil.class, "Failed to create parent directory: " + parent.getAbsolutePath());
                    return false;
                }
                
                // Extract file
                try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                    int bytesRead;
                    long totalRead = 0;
                    while ((bytesRead = tarIn.read(buffer)) != -1) {
                        totalRead += bytesRead;
                        // Check if we exceed max file size during extraction
                        if (totalRead > getMaxFileSize()) {
                            out.close();
                            targetFile.delete();
                            String message = String.format(
                                "Entry '%s' exceeded maximum allowed file size during extraction", 
                                entry.getName());
                            if (handlingMode == SuspiciousEntryHandling.ABORT) {
                                throw new SecurityException(message);
                            }
                            Logger.warn(TarUtil.class, message);
                            return false;
                        }
                        out.write(buffer, 0, bytesRead);
                    }
                    // Update total size counter
                    totalSizeCounter.addAndGet(totalRead);
                }
                // Set executable bit for executable files
                if ((entry.getMode() & 0100) != 0) {
                    targetFile.setExecutable(true, false);
                }
            }
            // Set last modified time if available
            long modTime = entry.getModTime().getTime();
            if (modTime > 0) {
                targetFile.setLastModified(modTime);
            }
            return true;
        } catch (IOException e) {
            Logger.error(TarUtil.class, "Error extracting entry " + entry.getName() + ": " + e.getMessage(), e);
            throw e;
        }
    }
    
    // Helper to recursively delete a directory
    private static boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteDirectory(child)) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }

    // Helper to ensure all parent directories are directories
    private static void ensureParentDirectoriesAreDirectories(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent == null) return;
        
        ensureParentDirectoriesAreDirectories(parent);
        
        if (parent.exists() && !parent.isDirectory()) {
            Logger.warn(TarUtil.class, "Replacing file with directory during extraction: " + parent.getAbsolutePath());
            if (!parent.delete()) {
                throw new IOException("Failed to delete file to create directory: " + parent);
            }
            if (!parent.mkdirs()) {
                throw new IOException("Failed to create directory: " + parent);
            }
        } else if (!parent.exists()) {
            // Create directory if it doesn't exist
            Logger.debug(TarUtil.class, "Creating parent directory during extraction: " + parent.getAbsolutePath());
            if (!parent.mkdirs()) {
                throw new IOException("Failed to create parent directory: " + parent);
            }
        }
    }
    
    /**
     * Safely extracts a TAR entry to the specified directory using the default handling mode
     * 
     * @param tarIn The TAR archive input stream
     * @param entry The entry to extract
     * @param outputDir The target directory for extraction
     * @return true if extraction was successful, false if skipped
     * @throws IOException If an error occurs during extraction
     */
    public static boolean safeExtractEntry(TarArchiveInputStream tarIn, TarArchiveEntry entry, File outputDir) throws IOException {
        return safeExtractEntry(tarIn, entry, outputDir, defaultHandlingMode.get(), new AtomicLong(0));
    }
    
    /**
     * Safely extracts a TAR archive with GZIP compression to the specified directory
     * 
     * @param inputFile The TAR.GZ file to extract
     * @param outputDir The target directory for extraction
     * @param handlingMode How to handle suspicious paths
     * @throws IOException If an error occurs during extraction
     */
    public static void safeExtractTarGz(File inputFile, File outputDir, SuspiciousEntryHandling handlingMode) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
        }
        
        // Track total size and entry count for DoS protection
        AtomicLong totalSizeCounter = new AtomicLong(0);
        int entryCount = 0;
        
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(
                new GZIPInputStream(new BufferedInputStream(new FileInputStream(inputFile)), GZIP_BUFFER_SIZE))) {
            
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                // Check entry count limit using shared implementation
                if (!ArchiveUtil.checkEntryCountLimit(++entryCount, getMaxEntries(), convertHandlingMode(handlingMode))) {
                    break;
                }
                
                // Extract entry
                try {
                    if (!safeExtractEntry(tarIn, entry, outputDir, handlingMode, totalSizeCounter)) {
                        Logger.warn(TarUtil.class, "Skipped entry: " + entry.getName());
                    }
                } catch (SecurityException e) {
                    if (handlingMode == SuspiciousEntryHandling.ABORT) {
                        throw e;
                    }
                    Logger.warn(TarUtil.class, "Skipped suspicious entry: " + entry.getName() + " - " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Safely extracts a TAR archive with GZIP compression to the specified directory
     * using the default handling mode
     * 
     * @param inputFile The TAR.GZ file to extract
     * @param outputDir The target directory for extraction
     * @throws IOException If an error occurs during extraction
     */
    public static void safeExtractTarGz(File inputFile, File outputDir) throws IOException {
        safeExtractTarGz(inputFile, outputDir, defaultHandlingMode.get());
    }
    
    /**
     * Safely extracts a TAR.GZ input stream to the specified directory
     * 
     * @param inputStream The TAR.GZ input stream
     * @param outputDir The target directory for extraction
     * @param handlingMode How to handle suspicious paths
     * @throws IOException If an error occurs during extraction
     */
    public static void safeExtractTarGz(InputStream inputStream, File outputDir, SuspiciousEntryHandling handlingMode) throws IOException {
        Objects.requireNonNull(inputStream, "TAR.GZ stream cannot be null");
        Objects.requireNonNull(outputDir, "Output directory cannot be null");
        
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
        }
        
        try (GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(inputStream);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
            
            TarArchiveEntry entry;
            AtomicLong totalSize = new AtomicLong(0);
            int entryCount = 0;
            
            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                entryCount++;
                
                // Check entry count limit
                if (!ArchiveUtil.checkEntryCountLimit(entryCount, getMaxEntries(), convertHandlingMode(handlingMode))) {
                    break;
                }
                
                // Check file size limit
                if (entry.isFile() && !ArchiveUtil.checkEntrySizeLimit(entry.getName(), entry.getSize(), 
                    getMaxFileSize(), convertHandlingMode(handlingMode))) {
                    continue;
                }
                
                // Check total size limit
                if (entry.isFile() && !ArchiveUtil.checkTotalSizeLimit(entry.getName(), entry.getSize(), 
                    totalSize, getMaxTotalSize(), convertHandlingMode(handlingMode))) {
                    continue;
                }
                
                // Sanitize entry name
                String entryName = ArchiveUtil.sanitizePath(entry.getName(), convertHandlingMode(handlingMode));
                File outputFile = new File(outputDir, entryName);
                
                // Create parent directories if needed
                File parent = outputFile.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Failed to create parent directory: " + parent.getAbsolutePath());
                }
                
                // Extract entry
                if (entry.isDirectory()) {
                    if (!outputFile.exists() && !outputFile.mkdirs()) {
                        throw new IOException("Failed to create directory: " + outputFile.getAbsolutePath());
                    }
                } else if (entry.isFile()) {
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        IOUtils.copy(tarIn, fos);
                        // Update total size after successful extraction
                        totalSize.addAndGet(entry.getSize());
                    }
                }
            }
        }
    }
    
    /**
     * Safely extracts a TAR.GZ input stream to the specified directory
     * using the default handling mode
     * 
     * @param inputStream The TAR.GZ input stream
     * @param outputDir The target directory for extraction
     * @throws IOException If an error occurs during extraction
     */
    public static void safeExtractTarGz(InputStream inputStream, File outputDir) throws IOException {
        safeExtractTarGz(inputStream, outputDir, defaultHandlingMode.get());
    }

    /**
     * Creates a TAR.GZ file containing a single file.
     *
     * @param sourceFile The file to archive
     * @param targetTarGzFile The TAR.GZ file to create
     * @param entryName Optional custom entry name (if null, sourceFile.getName() will be used)
     * @param handlingMode How to handle suspicious entry names
     * @return The created TAR.GZ file
     * @throws IOException If an error occurs during the archiving operation
     */
    public static File tarGzSingleFile(File sourceFile, File targetTarGzFile, String entryName,
                                     SuspiciousEntryHandling handlingMode) throws IOException {
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
            throw new IOException("Invalid or non-existent file: " +
                                 (sourceFile != null ? sourceFile.getAbsolutePath() : "null"));
        }
        
        try (TarArchiveOutputStream taos = createTarGzOutputStream(targetTarGzFile)) {
            String nameToUse = entryName != null ? entryName : sourceFile.getName();
            addFileToTar(taos, sourceFile, nameToUse, handlingMode);
        }
        
        return targetTarGzFile;
    }
    
    /**
     * Creates a TAR.GZ file containing a single file using the default handling mode.
     *
     * @param sourceFile The file to archive
     * @param targetTarGzFile The TAR.GZ file to create
     * @param entryName Optional custom entry name (if null, sourceFile.getName() will be used)
     * @return The created TAR.GZ file
     * @throws IOException If an error occurs during the archiving operation
     */
    public static File tarGzSingleFile(File sourceFile, File targetTarGzFile, String entryName) throws IOException {
        return tarGzSingleFile(sourceFile, targetTarGzFile, entryName, defaultHandlingMode.get());
    }
    
    /**
     * Creates a TAR.GZ file containing a single file using the file's name as the entry name.
     *
     * @param sourceFile The file to archive
     * @param targetTarGzFile The TAR.GZ file to create
     * @return The created TAR.GZ file
     * @throws IOException If an error occurs during the archiving operation
     */
    public static File tarGzSingleFile(File sourceFile, File targetTarGzFile) throws IOException {
        return tarGzSingleFile(sourceFile, targetTarGzFile, null, defaultHandlingMode.get());
    }
    
    /**
     * Creates a TAR.GZ file containing multiple files and/or directories.
     *
     * @param sources Collection of files/directories to archive
     * @param targetTarGzFile The TAR.GZ file to create
     * @param baseEntryPath Base path within the archive (can be empty or null)
     * @param handlingMode How to handle suspicious entry names
     * @return The created TAR.GZ file
     * @throws IOException If an error occurs during the archiving operation
     */
    public static File tarGzFiles(Collection<File> sources, File targetTarGzFile, String baseEntryPath,
                               SuspiciousEntryHandling handlingMode) throws IOException {
        try (TarArchiveOutputStream taos = createTarGzOutputStream(targetTarGzFile)) {
            for (File source : sources) {
                if (source.isDirectory()) {
                    // For directories, use the existing addDirectoryToTar method
                    String basePath = source.getAbsolutePath();
                    String baseDir = baseEntryPath != null ? 
                        ArchiveUtil.sanitizePath(baseEntryPath, convertHandlingMode(handlingMode)) : "";
                    
                    if (!baseDir.isEmpty() && !baseDir.endsWith("/")) {
                        baseDir += "/";
                    }
                    
                    // Add the directory name if we have a base path
                    if (!baseDir.isEmpty()) {
                        baseDir += source.getName();
                    } else {
                        baseDir = source.getName();
                    }
                    
                    addDirectoryToTar(taos, source, basePath, baseDir, handlingMode);
                } else {
                    // For files, determine the entry name
                    String entryName = source.getName();
                    if (baseEntryPath != null && !baseEntryPath.isEmpty()) {
                        String baseDir = ArchiveUtil.sanitizePath(baseEntryPath, convertHandlingMode(handlingMode));
                        if (!baseDir.endsWith("/")) {
                            baseDir += "/";
                        }
                        entryName = baseDir + entryName;
                    }
                    
                    addFileToTar(taos, source, entryName, handlingMode);
                }
            }
        }
        
        return targetTarGzFile;
    }
    
    /**
     * Creates a TAR.GZ file containing multiple files and/or directories using the default handling mode.
     *
     * @param sources Collection of files/directories to archive
     * @param targetTarGzFile The TAR.GZ file to create
     * @param baseEntryPath Base path within the archive (can be empty or null)
     * @return The created TAR.GZ file
     * @throws IOException If an error occurs during the archiving operation
     */
    public static File tarGzFiles(Collection<File> sources, File targetTarGzFile, String baseEntryPath) throws IOException {
        return tarGzFiles(sources, targetTarGzFile, baseEntryPath, defaultHandlingMode.get());
    }
    
    /**
     * Creates a TAR.GZ file containing multiple files and/or directories with no base entry path.
     *
     * @param sources Collection of files/directories to archive
     * @param targetTarGzFile The TAR.GZ file to create
     * @return The created TAR.GZ file
     * @throws IOException If an error occurs during the archiving operation
     */
    public static File tarGzFiles(Collection<File> sources, File targetTarGzFile) throws IOException {
        return tarGzFiles(sources, targetTarGzFile, null, defaultHandlingMode.get());
    }
    
    /**
     * Compresses a directory to a TAR.GZ file more easily than using multiple methods.
     * This is a convenience method that encapsulates all the necessary I/O operations.
     *
     * @param directory The directory to compress
     * @param targetTarGzFile The TAR.GZ file to create
     * @param includeBaseDirName Whether to include the base directory name in the archive paths
     * @return The created TAR.GZ file
     * @throws IOException If an error occurs during the archiving operation
     */
    public static File compressDirectoryToTarGz(File directory, File targetTarGzFile, boolean includeBaseDirName) throws IOException {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            throw new IOException("Invalid or non-existent directory: " +
                                 (directory != null ? directory.getAbsolutePath() : "null"));
        }
        
        try (TarArchiveOutputStream taos = createTarGzOutputStream(targetTarGzFile)) {
            String basePath = directory.getAbsolutePath();
            String baseDir = includeBaseDirName ? directory.getName() : "";
            
            addDirectoryToTar(taos, directory, basePath, baseDir);
        }
        
        return targetTarGzFile;
    }

    /**
     * Creates a TAR.GZ file with custom file filtering and processing.
     * 
     * @param sources Collection of files/directories to archive
     * @param targetTarGzFile The TAR.GZ file to create
     * @param baseEntryPath Base path within the archive (can be empty or null)
     * @param filter Lambda that determines which files to include (null for all files)
     * @param processor Lambda to process files before adding them (null for no processing)
     * @param nameMapper Lambda to customize entry names (null for default naming)
     * @param handlingMode How to handle suspicious paths
     * @return The created TAR.GZ file
     * @throws IOException If an error occurs during the operation
     */
    public static File tarGzFilesWithCustomProcessing(
            Collection<File> sources, 
            File targetTarGzFile, 
            String baseEntryPath,
            ArchiveUtil.FileFilter filter, 
            ArchiveUtil.FileProcessor processor, 
            ArchiveUtil.EntryNameMapper nameMapper,
            SuspiciousEntryHandling handlingMode) throws IOException {
        
        try (TarArchiveOutputStream taos = createTarGzOutputStream(targetTarGzFile)) {
            // Process each source file/directory
            for (File source : sources) {
                // Skip if filter rejects this file
                if (filter != null && !filter.accept(source)) {
                    continue;
                }
                
                // Process base path
                String basePath = baseEntryPath == null ? "" : 
                    ArchiveUtil.sanitizePath(baseEntryPath, convertHandlingMode(handlingMode));
                if (!basePath.isEmpty() && !basePath.endsWith("/")) {
                    basePath += "/";
                }
                
                if (source.isDirectory()) {
                    // For directories, recursively process all files
                    addDirectoryToTarWithCustomProcessing(taos, source, source.getAbsolutePath(), basePath, 
                        filter, processor, nameMapper, handlingMode);
                } else {
                    // For files, process with custom processing
                    addFileToTarWithCustomProcessing(taos, source, basePath, processor, nameMapper, handlingMode);
                }
            }
        }
        
        return targetTarGzFile;
    }
    
    private static void addDirectoryToTarWithCustomProcessing(
            TarArchiveOutputStream taos, 
            File directory, 
            String dirBasePath,
            String entryBasePath,
            ArchiveUtil.FileFilter filter, 
            ArchiveUtil.FileProcessor processor, 
            ArchiveUtil.EntryNameMapper nameMapper,
            SuspiciousEntryHandling handlingMode) throws IOException {
            
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Source must be a directory");
        }

        // Add directory entry if base path is not empty
        if (entryBasePath != null && !entryBasePath.isEmpty()) {
            String dirEntryName = entryBasePath.endsWith("/") ? entryBasePath : entryBasePath + "/";
            TarArchiveEntry dirEntry = createSafeTarEntry(dirEntryName, handlingMode);
            taos.putArchiveEntry(dirEntry);
            taos.closeArchiveEntry();
        }

        // Process all files in the directory
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Recursively process subdirectories
                    String newEntryPath = entryBasePath == null ? file.getName() : 
                        entryBasePath + (entryBasePath.endsWith("/") ? "" : "/") + file.getName();
                    addDirectoryToTarWithCustomProcessing(taos, file, dirBasePath, newEntryPath, 
                        filter, processor, nameMapper, handlingMode);
                } else if (filter == null || filter.accept(file)) {
                    // Process regular files that pass the filter
                    String fileEntryPath = entryBasePath == null ? file.getName() :
                        entryBasePath + (entryBasePath.endsWith("/") ? "" : "/") + file.getName();
                    addFileToTarWithCustomProcessing(taos, file, fileEntryPath, processor, nameMapper, handlingMode);
                }
            }
        }
    }
    
    /**
     * Creates a TAR.GZ file with custom file filtering and processing using default handling mode.
     * 
     * @param sources Collection of files/directories to archive
     * @param targetTarGzFile The TAR.GZ file to create
     * @param baseEntryPath Base path within the archive (can be empty or null)
     * @param filter Lambda that determines which files to include (null for all files)
     * @param processor Lambda to process files before adding them (null for no processing)
     * @param nameMapper Lambda to customize entry names (null for default naming)
     * @return The created TAR.GZ file
     * @throws IOException If an error occurs during the operation
     */
    public static File tarGzFilesWithCustomProcessing(
            Collection<File> sources, 
            File targetTarGzFile, 
            String baseEntryPath,
            ArchiveUtil.FileFilter filter, 
            ArchiveUtil.FileProcessor processor, 
            ArchiveUtil.EntryNameMapper nameMapper) throws IOException {
        return tarGzFilesWithCustomProcessing(sources, targetTarGzFile, baseEntryPath, 
            filter != null ? filter : ArchiveUtil.ACCEPT_ALL_FILTER,
            processor != null ? processor : ArchiveUtil.NO_PROCESSING,
            nameMapper != null ? nameMapper : ArchiveUtil.DEFAULT_NAME_MAPPER,
            defaultHandlingMode.get());
    }
    
    /**
     * Compresses a directory to a TAR.GZ file with custom file processing.
     * 
     * @param directory The directory to compress
     * @param targetTarGzFile The TAR.GZ file to create
     * @param includeBaseDirName Whether to include the base directory name in paths
     * @param filter Lambda that determines which files to include (null for all files)
     * @param processor Lambda to process files before adding them (null for no processing)
     * @param nameMapper Lambda to customize entry names (null for default naming)
     * @param handlingMode How to handle suspicious paths
     * @return The created TAR.GZ file
     * @throws IOException If an error occurs during the operation
     */
    public static File compressDirectoryToTarGzWithCustomProcessing(
            File directory, 
            File targetTarGzFile, 
            boolean includeBaseDirName,
            ArchiveUtil.FileFilter filter, 
            ArchiveUtil.FileProcessor processor, 
            ArchiveUtil.EntryNameMapper nameMapper,
            SuspiciousEntryHandling handlingMode) throws IOException {
        
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            throw new IOException("Invalid or non-existent directory: " +
                                 (directory != null ? directory.getAbsolutePath() : "null"));
        }
        
        try (TarArchiveOutputStream taos = createTarGzOutputStream(targetTarGzFile)) {
            String basePath = directory.getAbsolutePath();
            String baseDir = includeBaseDirName ? directory.getName() : "";
            
            addDirectoryToTarWithCustomProcessing(taos, directory, basePath, baseDir, 
                                                filter, processor, nameMapper, handlingMode);
        }
        
        return targetTarGzFile;
    }
    
    /**
     * Compresses a directory to a TAR.GZ file with custom file processing using default handling mode.
     * 
     * @param directory The directory to compress
     * @param targetTarGzFile The TAR.GZ file to create
     * @param includeBaseDirName Whether to include the base directory name in paths
     * @param filter Lambda that determines which files to include (null for all files)
     * @param processor Lambda to process files before adding them (null for no processing)
     * @param nameMapper Lambda to customize entry names (null for default naming)
     * @return The created TAR.GZ file
     * @throws IOException If an error occurs during the operation
     */
    public static File compressDirectoryToTarGzWithCustomProcessing(
            File directory, 
            File targetTarGzFile, 
            boolean includeBaseDirName,
            ArchiveUtil.FileFilter filter, 
            ArchiveUtil.FileProcessor processor, 
            ArchiveUtil.EntryNameMapper nameMapper) throws IOException {
        return compressDirectoryToTarGzWithCustomProcessing(directory, targetTarGzFile, includeBaseDirName, 
                                                         filter, processor, nameMapper, defaultHandlingMode.get());
    }
    
    /**
     * Adds a file to a TAR archive with custom processing.
     * 
     * @param taos The TAR archive output stream
     * @param file The file to add
     * @param basePath Base path within the archive
     * @param processor Lambda to process the file (null for no processing)
     * @param nameMapper Lambda to customize entry name (null for default naming)
     * @param handlingMode How to handle suspicious entries
     * @throws IOException If an error occurs during the operation
     */
    private static void addFileToTarWithCustomProcessing(
            TarArchiveOutputStream taos, 
            File file, 
            String basePath,
            ArchiveUtil.FileProcessor processor, 
            ArchiveUtil.EntryNameMapper nameMapper,
            SuspiciousEntryHandling handlingMode) throws IOException {
        
        Objects.requireNonNull(taos, "TAR output stream cannot be null");
        Objects.requireNonNull(file, "File cannot be null");
        
        if (!file.isFile()) {
            throw new IllegalArgumentException("Source must be a file");
        }
        
        // Map entry name if mapper is provided
        String entryPath = basePath;
        if (nameMapper != null) {
            entryPath = nameMapper.mapEntryName(file, basePath);
            if (entryPath == null || entryPath.isEmpty()) {
                Logger.warn(TarUtil.class, "Entry name mapper returned invalid path for: " + file.getAbsolutePath());
                return;
            }
        }
        
        // Sanitize entry path
        String sanitizedPath = ArchiveUtil.sanitizePath(entryPath, convertHandlingMode(handlingMode));
        
        // Create entry
        TarArchiveEntry entry = createSafeTarEntry(file, sanitizedPath, handlingMode);
        
        // Process file if processor is provided
        try (InputStream inputStream = processor != null ? 
                processor.process(file, sanitizedPath) : 
                new FileInputStream(file)) {
            
            // Set entry size and add to archive
            if (processor != null) {
                // For processed files, we need to buffer to determine size
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(inputStream, baos);
                byte[] processedData = baos.toByteArray();
                entry.setSize(processedData.length);
                taos.putArchiveEntry(entry);
                
                // Write the processed data
                try (ByteArrayInputStream bais = new ByteArrayInputStream(processedData)) {
                    IOUtils.copy(bais, taos);
                }
            } else {
                // For unprocessed files, use file size directly
                entry.setSize(file.length());
                taos.putArchiveEntry(entry);
                IOUtils.copy(inputStream, taos);
            }
        }
        
        taos.closeArchiveEntry();
    }

    /**
     * Example method showing how to create a TAR.GZ file with customized file filtering
     * and content processing. This is a practical utility that demonstrates the use
     * of functional interfaces.
     * 
     * @param sourceDirectory Directory to archive
     * @param targetTarGzFile Destination TAR.GZ file
     * @param includeExtensions File extensions to include (null or empty for all files)
     * @param excludePatterns File name patterns to exclude (null or empty for no exclusions)
     * @param maxFileSizeBytes Maximum size of individual files to include (0 for no limit)
     * @return The created TAR.GZ file
     * @throws IOException If an error occurs during the archiving operation
     */
    public static File createFilteredTarGzFromDirectory(
            File sourceDirectory,
            File targetTarGzFile,
            String[] includeExtensions,
            String[] excludePatterns,
            long maxFileSizeBytes) throws IOException {
        
        // Create composite filter using ArchiveUtil's functional helpers
        ArchiveUtil.FileFilter filter = ArchiveUtil.ACCEPT_ALL_FILTER;
        
        // Add extension filter if specified
        if (includeExtensions != null && includeExtensions.length > 0) {
            filter = ArchiveUtil.and(filter, ArchiveUtil.byExtension(includeExtensions));
        }
        
        // Add exclusion pattern filter if specified
        if (excludePatterns != null && excludePatterns.length > 0) {
            for (String pattern : excludePatterns) {
                filter = ArchiveUtil.and(filter, ArchiveUtil.not(ArchiveUtil.byNamePattern(pattern)));
            }
        }
        
        // Create size filter if specified
        if (maxFileSizeBytes > 0) {
            filter = ArchiveUtil.and(filter, file -> !file.isFile() || file.length() <= maxFileSizeBytes);
        }
        
        // Create processor for files (demonstration of content transformation)
        ArchiveUtil.FileProcessor processor = (file, entryName) -> {
            // Just pass through the file in this example,
            // but you could transform content here if needed
            return java.nio.file.Files.newInputStream(file.toPath());
        };
        
        // Default naming strategy
        ArchiveUtil.EntryNameMapper nameMapper = ArchiveUtil.DEFAULT_NAME_MAPPER;
        
        // Call the custom processing method with our filter, processor, and mapper
        return tarGzFilesWithCustomProcessing(
                ArchiveUtil.singleFileCollection(sourceDirectory),
                targetTarGzFile,
                null,  // No base path
                filter,
                processor,
                nameMapper);
    }

    /**
     * Sanitizes a path to prevent path traversal and tar slip vulnerabilities.
     * Delegate to shared ArchiveUtil implementation.
     * 
     * @param entryName The path to sanitize
     * @param handlingMode How to handle suspicious entries
     * @param archivePath The path to the tar file containing this entry
     * @return A sanitized path safe for tar file entries
     * @throws SecurityException If the path contains malicious path traversal attempts
     *                           and handling mode is ABORT
     */
    public static String sanitizePath(String entryName, SuspiciousEntryHandling handlingMode, String archivePath) {
        return ArchiveUtil.sanitizePath(entryName, convertHandlingMode(handlingMode), archivePath);
    }

    /**
     * Sanitizes a path to prevent path traversal and tar slip vulnerabilities.
     * Delegate to shared ArchiveUtil implementation.
     * 
     * @param entryName The path to sanitize
     * @param handlingMode How to handle suspicious entries
     * @return A sanitized path safe for tar file entries
     * @throws SecurityException If the path contains malicious path traversal attempts
     *                           and handling mode is ABORT
     * @deprecated Use {@link #sanitizePath(String, SuspiciousEntryHandling, String)} instead
     */
    @Deprecated
    public static String sanitizePath(String entryName, SuspiciousEntryHandling handlingMode) {
        return sanitizePath(entryName, handlingMode, "unknown tar file");
    }

    /**
     * Sanitizes a path to prevent path traversal and tar slip vulnerabilities using
     * the default handling mode.
     * 
     * @param entryName The path to sanitize
     * @return A sanitized path safe for tar file entries
     * @throws SecurityException If the path contains malicious path traversal attempts
     *                           and default handling mode is ABORT
     */
    public static String sanitizePath(String entryName) {
        return sanitizePath(entryName, defaultHandlingMode.get(), "unknown tar file");
    }
} 