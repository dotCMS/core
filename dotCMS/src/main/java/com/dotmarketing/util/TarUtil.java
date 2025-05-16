package com.dotmarketing.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
        String configValue = Config.getStringProperty(TAR_MAX_TOTAL_SIZE_KEY, null);
        return ArchiveUtil.parseSize(configValue, DEFAULT_MAX_TOTAL_SIZE);
    }
    
    /**
     * Gets the maximum size of a single extracted file (in bytes)
     * 
     * @return Maximum file size in bytes from config or default value
     */
    public static long getMaxFileSize() {
        String configValue = Config.getStringProperty(TAR_MAX_FILE_SIZE_KEY, null);
        return ArchiveUtil.parseSize(configValue, DEFAULT_MAX_FILE_SIZE);
    }
    
    /**
     * Gets the maximum number of entries to extract
     * 
     * @return Maximum number of entries from config or default value
     */
    public static int getMaxEntries() {
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
     * Adds a directory and its contents to a TAR archive with sanitized paths
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
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IOException("Invalid directory: " + directory.getAbsolutePath());
        }
        
        // Add directory entry itself if baseDirectory is specified
        if (baseDirectory != null && !baseDirectory.isEmpty()) {
            String sanitizedBaseDir = ArchiveUtil.sanitizePath(baseDirectory, convertHandlingMode(handlingMode));
            TarArchiveEntry dirEntry = new TarArchiveEntry(sanitizedBaseDir + "/");
            taos.putArchiveEntry(dirEntry);
            taos.closeArchiveEntry();
        }
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String relativePath = file.getAbsolutePath().substring(basePath.length());
                relativePath = relativePath.replace('\\', '/');
                if (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }
                
                if (baseDirectory != null && !baseDirectory.isEmpty()) {
                    relativePath = baseDirectory + "/" + relativePath;
                }
                
                if (file.isDirectory()) {
                    addDirectoryToTar(taos, file, basePath, relativePath, handlingMode);
                } else {
                    addFileToTar(taos, file, relativePath, handlingMode);
                }
            }
        }
    }
    
    /**
     * Adds a directory and its contents to a TAR archive with sanitized paths using the default handling mode
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
        
        // Sanitize the entry name
        String sanitizedName = ArchiveUtil.sanitizePath(entry.getName(), convertHandlingMode(handlingMode));
        if (!sanitizedName.equals(entry.getName())) {
            Logger.warn(TarUtil.class, "Potentially malicious tar entry renamed: " + 
                    entry.getName() + " -> " + sanitizedName);
        }
        
        // Create the full path to the target file/directory
        File targetFile = new File(outputDir, sanitizedName);
        
        // Check security using shared implementation
        if (!ArchiveUtil.checkSecurity(outputDir, targetFile, convertHandlingMode(handlingMode))) {
            return false;
        }
        
        // Create directories or extract files
        if (entry.isDirectory()) {
            if (!targetFile.exists() && !targetFile.mkdirs()) {
                Logger.error(TarUtil.class, "Failed to create directory: " + targetFile.getAbsolutePath());
                return false;
            }
        } else {
            // Ensure parent directory exists
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
            while ((entry = tarIn.getNextTarEntry()) != null) {
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
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
        }
        
        // Track total size and entry count for DoS protection
        AtomicLong totalSizeCounter = new AtomicLong(0);
        int entryCount = 0;
        
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(
                new GZIPInputStream(new BufferedInputStream(inputStream), GZIP_BUFFER_SIZE))) {
            
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextTarEntry()) != null) {
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
} 