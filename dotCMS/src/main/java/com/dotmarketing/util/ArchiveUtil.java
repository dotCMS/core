package com.dotmarketing.util;

import com.dotcms.util.SizeUtil;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base utility class for archive operations in dotCMS.
 * Contains shared functionality used by both ZipUtil and TarUtil
 * to provide consistent security measures against archive-related vulnerabilities.
 */
public class ArchiveUtil {

    /**
     * Default buffer size for I/O operations with archive files
     */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Default protection limits for archive extraction
     */
    public static final long DEFAULT_MAX_TOTAL_SIZE = 5L * 1024L * 1024L * 1024L;  // 5GB max total extracted size
    public static final long DEFAULT_MAX_FILE_SIZE = 1L * 1024L * 1024L * 1024L;   // 1GB max single file size
    public static final int DEFAULT_MAX_ENTRIES = 10000;                         // 10,000 max entries

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
     * Sanitizes a path to prevent path traversal and archive slip vulnerabilities.
     * Removes leading slashes and resolves any relative paths to ensure they cannot
     * escape the target directory.
     * 
     * @param entryName The path to sanitize
     * @param handlingMode How to handle suspicious entries
     * @return A sanitized path safe for archive entries
     * @throws SecurityException If the path contains malicious path traversal attempts
     *                           and handling mode is ABORT
     */
    public static String sanitizePath(String entryName, SuspiciousEntryHandling handlingMode) {
        if (entryName == null) {
            throw new IllegalArgumentException("Entry name cannot be null");
        }
        
        // First check if the path is suspicious
        boolean isSuspicious = entryName.startsWith("/") || entryName.startsWith("\\") || 
            entryName.contains("../") || entryName.contains("..\\") || 
            entryName.contains("/..") || entryName.contains("\\..") ||
            entryName.contains(":/") || entryName.contains(":\\");
        
        // Remove any leading slashes that would make the path absolute
        String sanitized = entryName.replaceAll("^/+", "");
        
        // Split path into components
        String[] parts = sanitized.split("/");
        java.util.List<String> safePathParts = new java.util.ArrayList<>();
        
        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part)) {
                // Skip empty parts or current directory references
                continue;
            }
            if ("..".equals(part)) {
                // For tests, throw SecurityException if we detect path traversal attempts
                if (isSuspicious) {
                    SecurityLogger.logInfo(ArchiveUtil.class, String.format(
                            "Archive slip attack detected. Entry '%s' contains path traversal.",
                            entryName));
                    
                    if (handlingMode == SuspiciousEntryHandling.ABORT) {
                        throw new SecurityException("Illegal entry path: " + entryName);
                    }
                    
                    // If we're not aborting, skip this part but log the warning
                    Logger.warn(ArchiveUtil.class, "Skipping suspicious path component '..' in: " + entryName);
                    continue;
                }
                
                // Skip parent directory references and remove last path part if exists
                if (!safePathParts.isEmpty()) {
                    safePathParts.remove(safePathParts.size() - 1);
                }
                continue;
            }
            safePathParts.add(part);
        }
        
        // If the sanitized path is different from original, it might be malicious
        String result = String.join("/", safePathParts);
        if (isSuspicious && !result.equals(entryName)) {
            SecurityLogger.logInfo(ArchiveUtil.class, String.format(
                    "Archive slip attack detected. Entry '%s' was sanitized to '%s'.",
                    entryName, result));
                    
            if (handlingMode == SuspiciousEntryHandling.ABORT) {
                throw new SecurityException("Illegal entry path: " + entryName);
            }
            
            Logger.warn(ArchiveUtil.class, "Sanitized suspicious path: " + entryName + " to: " + result);
        }
        
        return result;
    }

    /**
     * Check paths to determine if we're being attacked.
     * 
     * @param parentDir Parent directory
     * @param newFile File to check
     * @param handlingMode How to handle suspicious entries
     * @return true if the file is within the parent directory, false otherwise
     * @throws IOException If an error occurs during path resolution
     * @throws SecurityException If handlingMode is ABORT and the path is suspicious
     */
    public static boolean checkSecurity(final File parentDir, final File newFile, SuspiciousEntryHandling handlingMode) throws IOException {
        if (!isNewFileDestinationSafe(parentDir, newFile)) {
            //Log detailed info into the security logger
            if(!parentDir.delete()){
                SecurityLogger.logInfo(ArchiveUtil.class, String.format(
                        "An attempt to extract entry '%s' under an illegal destination has been made. The injected directory [%s] couldn't get removed.",
                        parentDir.getCanonicalPath(), newFile.getCanonicalPath()));
            } else {
                SecurityLogger.logInfo(ArchiveUtil.class, String.format(
                        "An attempt to extract entry '%s' under an illegal destination has been made. The injected directory [%s] was successfully removed.",
                        parentDir.getCanonicalPath(), newFile.getCanonicalPath()));
            }
            
            if (handlingMode == SuspiciousEntryHandling.ABORT) {
                // and expose the minimum to the user
                throw new SecurityException("Illegal extraction attempt");
            }
            
            Logger.warn(ArchiveUtil.class, "Skipping entry with illegal destination: " + newFile.getPath());
            return false;
        }
        return true;
    }
    
    /**
     * Check paths to determine if we're being attacked.
     * 
     * @param parentDir Parent directory
     * @param newFile File to check
     * @return true if the file is within the parent directory, false otherwise
     * @throws IOException If an error occurs during path resolution
     */
    static boolean isNewFileDestinationSafe(final File parentDir, final File newFile) throws IOException {
        final String dirCanonicalPath = parentDir.getCanonicalPath();
        final String newFileCanonicalPath = newFile.getCanonicalPath();
        return newFileCanonicalPath.startsWith(dirCanonicalPath);
    }
    
    /**
     * Checks if an entry's size exceeds the maximum allowed size.
     * 
     * @param entryName Name of the entry being checked
     * @param entrySize Size of the entry in bytes
     * @param maxFileSize Maximum allowed size in bytes
     * @param handlingMode How to handle oversized entries
     * @return true if the entry is within size limits, false if it's oversized and should be skipped
     * @throws SecurityException If the entry is oversized and handlingMode is ABORT
     */
    public static boolean checkEntrySizeLimit(String entryName, long entrySize, long maxFileSize, 
                                              SuspiciousEntryHandling handlingMode) {
        if (entrySize > 0 && entrySize > maxFileSize) {
            String message = String.format(
                "Entry '%s' size (%d bytes) exceeds maximum allowed file size (%d bytes)", 
                entryName, entrySize, maxFileSize);
            Logger.error(ArchiveUtil.class, message);
            
            if (handlingMode == SuspiciousEntryHandling.ABORT) {
                throw new SecurityException(message);
            }
            Logger.warn(ArchiveUtil.class, "Skipped oversized entry: " + entryName);
            return false;
        }
        return true;
    }
    
    /**
     * Checks if adding an entry would exceed the total size limit.
     * 
     * @param entryName Name of the entry being checked
     * @param entrySize Size of the entry in bytes
     * @param totalSizeCounter Counter for tracking total extracted size
     * @param maxTotalSize Maximum allowed total size in bytes
     * @param handlingMode How to handle oversized entries
     * @return true if the entry can be added without exceeding the limit, false if it would exceed the limit
     * @throws SecurityException If the entry would exceed the limit and handlingMode is ABORT
     */
    public static boolean checkTotalSizeLimit(String entryName, long entrySize, AtomicLong totalSizeCounter, 
                                              long maxTotalSize, SuspiciousEntryHandling handlingMode) {
        if (entrySize > 0 && totalSizeCounter.get() + entrySize > maxTotalSize) {
            String message = String.format(
                "Extraction of entry '%s' would exceed maximum total extraction size of %d bytes", 
                entryName, maxTotalSize);
            Logger.error(ArchiveUtil.class, message);
            
            if (handlingMode == SuspiciousEntryHandling.ABORT) {
                throw new SecurityException(message);
            }
            return false;
        }
        return true;
    }
    
    /**
     * Checks if the entry count exceeds the maximum allowed.
     * 
     * @param entryCount Current entry count
     * @param maxEntries Maximum allowed entries
     * @param handlingMode How to handle entry count limit
     * @return true if the entry count is within limits, false if it exceeds the limit
     * @throws SecurityException If the entry count exceeds the limit and handlingMode is ABORT
     */
    public static boolean checkEntryCountLimit(int entryCount, int maxEntries, SuspiciousEntryHandling handlingMode) {
        if (entryCount > maxEntries) {
            String message = String.format("Maximum number of entries (%d) exceeded in archive extraction", maxEntries);
            Logger.warn(ArchiveUtil.class, "Stopping extraction after maximum entries limit reached");
            
            if (handlingMode == SuspiciousEntryHandling.ABORT) {
                throw new SecurityException(message);
            }
            return false;
        }
        return true;
    }
    
    /**
     * Parses a size string with potential units (KB, MB, GB) into bytes
     * using the existing dotCMS SizeUtil.
     * 
     * @param sizeStr The size string to parse (e.g., "5GB", "1024KB", "512")
     * @param defaultValue The default value to return if parsing fails
     * @return The size in bytes
     */
    public static long parseSize(String sizeStr, long defaultValue) {
        try {
            return SizeUtil.convertToBytes(sizeStr);
        } catch (Exception e) {
            Logger.warn(ArchiveUtil.class, "Error parsing size value: " + sizeStr + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
} 