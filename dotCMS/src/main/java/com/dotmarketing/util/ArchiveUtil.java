package com.dotmarketing.util;

import com.dotcms.util.SizeUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
     * Functional interfaces for common archive operations
     */
    @FunctionalInterface
    public interface FileFilter {
        /**
         * Determines whether a file should be included in the archive.
         * 
         * @param file The file to check
         * @return true if the file should be included, false if it should be skipped
         */
        boolean accept(File file);
    }
    
    @FunctionalInterface
    public interface FileProcessor {
        /**
         * Processes a file before it's added to the archive.
         * This can be used to transform file contents or perform other operations.
         * 
         * @param file The file to process
         * @param entryName The proposed entry name in the archive
         * @return A processed input stream containing the data to archive
         * @throws IOException If an error occurs during processing
         */
        InputStream process(File file, String entryName) throws IOException;
    }
    
    @FunctionalInterface
    public interface EntryNameMapper {
        /**
         * Maps a file to its corresponding entry name in the archive.
         * This allows custom naming logic for archive entries.
         * 
         * @param file The file being added to the archive
         * @param basePath The base path within the archive (if any)
         * @return The entry name to use in the archive
         */
        String mapEntryName(File file, String basePath);
    }
    
    /**
     * Common file filter that accepts all files
     */
    public static final FileFilter ACCEPT_ALL_FILTER = file -> true;
    
    /**
     * Common file filter that accepts only regular files (not directories)
     */
    public static final FileFilter REGULAR_FILES_ONLY = File::isFile;
    
    /**
     * Common file filter that accepts only directories
     */
    public static final FileFilter DIRECTORIES_ONLY = File::isDirectory;
    
    /**
     * Common entry name mapper that uses the file's name without any path manipulation
     */
    public static final EntryNameMapper DEFAULT_NAME_MAPPER = (file, basePath) -> {
        String path = basePath == null ? "" : basePath;
        if (!path.isEmpty() && !path.endsWith("/")) {
            path += "/";
        }
        return path + file.getName();
    };
    
    /**
     * Common file processor that returns the file as is without processing
     */
    public static final FileProcessor NO_PROCESSING = (file, entryName) -> java.nio.file.Files.newInputStream(file.toPath());
    
    /**
     * Set to track paths that have already logged leading slash warnings
     */
    private static final java.util.Set<String> loggedLeadingSlashPaths = new java.util.concurrent.ConcurrentHashMap<String, Boolean>().newKeySet();

    /**
     * Sanitizes a path to prevent path traversal and archive slip vulnerabilities.
     * Removes leading slashes and resolves any relative paths to ensure they cannot
     * escape the target directory.
     * 
     * @param entryName The path to sanitize
     * @param handlingMode How to handle suspicious entries
     * @param archivePath The path to the archive file (zip or tar.gz) containing this entry
     * @return A sanitized path safe for archive entries
     * @throws SecurityException If the path contains malicious path traversal attempts
     *                           and handling mode is ABORT
     */
    public static String sanitizePath(String entryName, SuspiciousEntryHandling handlingMode, String archivePath) {
        if (entryName == null) {
            throw new IllegalArgumentException("Entry name cannot be null");
        }
        
        // First check if the path is suspicious
        boolean isSuspicious = entryName.contains("../") || entryName.contains("..\\") || 
            entryName.contains("/..") || entryName.contains("\\..") ||
            entryName.contains(":/") || entryName.contains(":\\");
        
        // Check for leading slashes
        boolean hasLeadingSlash = entryName.startsWith("/") || entryName.startsWith("\\");
        
        // Remove any leading slashes that would make the path absolute
        String sanitized = entryName.replaceAll("^/+", "");
        
        try {
            // Create a temporary file to resolve the path
            File tempFile = new File(sanitized);
            String canonicalPath = tempFile.getCanonicalPath();
            
            // If the path was modified during canonicalization, it might be suspicious
            if (!canonicalPath.equals(sanitized)) {
                SecurityLogger.logInfo(ArchiveUtil.class, String.format(
                        "Path in '%s' was canonicalized: '%s' -> '%s'",
                        archivePath, entryName, canonicalPath));
                
                if (handlingMode == SuspiciousEntryHandling.ABORT) {
                    throw new SecurityException("Illegal entry path in " + archivePath + ": " + entryName);
                }
                
                Logger.warn(ArchiveUtil.class, "Path was canonicalized in " + archivePath + ": " + entryName + " -> " + canonicalPath);
            }
            
            // Convert back to forward slashes and remove any leading/trailing slashes
            String result = canonicalPath.replace('\\', '/').replaceAll("^/+|/+$", "");
            
            // Log warning for leading slashes only once per path
            if (hasLeadingSlash && loggedLeadingSlashPaths.add(entryName)) {
                Logger.warn(ArchiveUtil.class, "Path contains leading slash in " + archivePath + ": " + entryName + " -> " + result);
            }
            
            return result;
        } catch (IOException e) {
            // If we can't resolve the canonical path, fall back to the old method
            Logger.warn(ArchiveUtil.class, "Failed to resolve canonical path for " + entryName + " in " + archivePath + ", using fallback method");
            
            // Split path into components and normalize
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
                                "Possible archive slip attack detected in '%s'. Entry '%s' contains path traversal.",
                                archivePath, entryName));
                        
                        if (handlingMode == SuspiciousEntryHandling.ABORT) {
                            throw new SecurityException("Illegal entry path in " + archivePath + ": " + entryName);
                        }
                        
                        // If we're not aborting, skip this part but log the warning
                        Logger.warn(ArchiveUtil.class, "Skipping suspicious path component '..' in " + archivePath + ": " + entryName);
                        continue;
                    }
                    
                    // For legitimate ".." references, remove the last path part if it exists
                    if (!safePathParts.isEmpty()) {
                        safePathParts.remove(safePathParts.size() - 1);
                    }
                    continue;
                }
                safePathParts.add(part);
            }
            
            // Join the normalized path parts
            String result = String.join("/", safePathParts);
            
            // If the path was modified during sanitization, log it
            if (!result.equals(entryName)) {
                if (isSuspicious) {
                    SecurityLogger.logInfo(ArchiveUtil.class, String.format(
                            "Possible archive slip attack detected in '%s'. Entry '%s' was sanitized to '%s'.",
                            archivePath, entryName, result));
                            
                    if (handlingMode == SuspiciousEntryHandling.ABORT) {
                        throw new SecurityException("Illegal entry path in " + archivePath + ": " + entryName);
                    }
                    
                    Logger.warn(ArchiveUtil.class, "Sanitized suspicious path in " + archivePath + ": " + entryName + " to: " + result);
                } else {
                    Logger.debug(ArchiveUtil.class, "Normalized path in " + archivePath + ": " + entryName + " to: " + result);
                }
            }
            
            return result;
        }
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
     * @deprecated Use {@link #sanitizePath(String, SuspiciousEntryHandling, String)} instead
     */
    @Deprecated
    public static String sanitizePath(String entryName, SuspiciousEntryHandling handlingMode) {
        return sanitizePath(entryName, handlingMode, "unknown archive");
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
        return validateFileWithinDirectory(parentDir, newFile, handlingMode);
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
        try {
            return validateFileWithinDirectory(parentDir, newFile, SuspiciousEntryHandling.SKIP_AND_CONTINUE);
        } catch (SecurityException e) {
            // This should never happen with SKIP_AND_CONTINUE mode, but added for compatibility
            return false;
        }
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

    /**
     * Validates that a file path is safely contained within a target directory.
     * 
     * @param targetDirectory The directory that should contain the path
     * @param filePath The path to validate
     * @param handlingMode How to handle invalid paths
     * @return true if the path is within the target directory, false otherwise
     * @throws SecurityException If the path isn't safe and handling mode is ABORT
     */
    public static boolean validatePathWithinDirectory(File targetDirectory, 
                                                      String filePath, 
                                                      SuspiciousEntryHandling handlingMode) {
        try {
            // Ensure target directory exists
            if (!targetDirectory.exists()) {
                if (!targetDirectory.mkdirs()) {
                    Logger.error(ArchiveUtil.class, "Failed to create target directory: " + targetDirectory);
                    return false;
                }
            }
            
            // Quick check for common path traversal patterns before any further processing
            boolean isSuspicious = filePath.contains("../") || filePath.contains("..\\") || 
                                   filePath.startsWith("/") || filePath.startsWith("\\") ||
                                   filePath.contains(":/") || filePath.contains(":\\");
                
            if (isSuspicious) {
                String message = String.format(
                    "Suspicious path detected: '%s'", filePath);
                
                SecurityLogger.logInfo(ArchiveUtil.class, message);
                
                if (handlingMode == SuspiciousEntryHandling.ABORT) {
                    throw new SecurityException(message);
                }
                
                Logger.warn(ArchiveUtil.class, "Path contains suspicious patterns: " + filePath);
                return false;
            }
            
            // Convert to absolute canonical paths for comparison
            String targetDirPath = targetDirectory.getCanonicalPath();
            
            // Sanitize the file path to remove any path traversal attempts
            String sanitizedPath = sanitizePath(filePath, handlingMode);
            
            // Create a file representing the combined path
            File fullPath = new File(targetDirectory, sanitizedPath);
            
            // Ensure parent directories exist
            File parentDir = fullPath.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    Logger.warn(ArchiveUtil.class, "Failed to create parent directories for: " + fullPath);
                }
            }
            
            String canonicalPath = fullPath.getCanonicalPath();
            
            // Verify the file's canonical path starts with the target directory's canonical path
            boolean isSafe = canonicalPath.startsWith(targetDirPath);
            
            if (!isSafe) {
                String message = String.format(
                    "Path traversal attempt detected. Path '%s' would escape target directory '%s'",
                    filePath, targetDirPath);
                
                SecurityLogger.logInfo(ArchiveUtil.class, message);
                
                if (handlingMode == SuspiciousEntryHandling.ABORT) {
                    throw new SecurityException(message);
                }
                
                Logger.warn(ArchiveUtil.class, "Invalid path blocked: " + filePath);
                return false;
            }
            
            return true;
        } catch (IOException e) {
            // If we can't resolve canonical paths, assume it's unsafe
            Logger.error(ArchiveUtil.class, "Error validating path security: " + e.getMessage(), e);
            
            if (handlingMode == SuspiciousEntryHandling.ABORT) {
                throw new SecurityException("Unable to verify path safety: " + e.getMessage());
            }
            
            return false;
        }
    }
    
    /**
     * Validates that a file is safely contained within a target directory.
     * 
     * @param targetDirectory The directory that should contain the file
     * @param file The file to validate
     * @param handlingMode How to handle invalid paths
     * @return true if the file is within the target directory, false otherwise
     * @throws SecurityException If the file isn't safe and handling mode is ABORT
     */
    public static boolean validateFileWithinDirectory(File targetDirectory, 
                                                     File file, 
                                                     SuspiciousEntryHandling handlingMode) {
        try {
            // Convert to absolute canonical paths for comparison
            String targetDirPath = targetDirectory.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            
            // Verify the file's canonical path starts with the target directory's canonical path
            boolean isSafe = filePath.startsWith(targetDirPath);
            
            if (!isSafe) {
                String message = String.format(
                    "Path traversal attempt detected. File '%s' is outside target directory '%s'",
                    filePath, targetDirPath);
                
                SecurityLogger.logInfo(ArchiveUtil.class, message);
                
                if (handlingMode == SuspiciousEntryHandling.ABORT) {
                    throw new SecurityException(message);
                }
                
                Logger.warn(ArchiveUtil.class, "Invalid file path blocked: " + filePath);
                return false;
            }
            
            return true;
        } catch (IOException e) {
            // If we can't resolve canonical paths, assume it's unsafe
            Logger.error(ArchiveUtil.class, "Error validating file security: " + e.getMessage(), e);
            
            if (handlingMode == SuspiciousEntryHandling.ABORT) {
                throw new SecurityException("Unable to verify file safety: " + e.getMessage());
            }
            
            return false;
        }
    }
    
    /**
     * Normalizes a base path for archive entries.
     * Ensures the path ends with a slash if it's not empty.
     * 
     * @param basePath The base path to normalize
     * @return A normalized base path ending with a slash if not empty
     */
    public static String normalizeBasePath(String basePath) {
        if (basePath == null || basePath.isEmpty()) {
            return "";
        }
        
        if (!basePath.endsWith("/")) {
            return basePath + "/";
        }
        
        return basePath;
    }
    
    /**
     * Creates parent directories for a file if they don't exist.
     * 
     * @param file The file whose parent directories should be created
     * @return true if parent directories exist or were created successfully, false otherwise
     */
    public static boolean createParentDirectories(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            return parent.mkdirs();
        }
        return true;
    }
    
    /**
     * Creates a filter that combines multiple filters with AND logic.
     * A file must be accepted by all filters to be included.
     * 
     * @param filters The filters to combine
     * @return A filter that applies all the provided filters
     */
    public static FileFilter and(FileFilter... filters) {
        return file -> {
            for (FileFilter filter : filters) {
                if (filter != null && !filter.accept(file)) {
                    return false;
                }
            }
            return true;
        };
    }
    
    /**
     * Creates a filter that combines multiple filters with OR logic.
     * A file will be included if any filter accepts it.
     * 
     * @param filters The filters to combine
     * @return A filter that applies any of the provided filters
     */
    public static FileFilter or(FileFilter... filters) {
        return file -> {
            for (FileFilter filter : filters) {
                if (filter != null && filter.accept(file)) {
                    return true;
                }
            }
            return filters.length == 0; // If no filters, accept all
        };
    }
    
    /**
     * Creates a filter that inverts another filter's logic.
     * 
     * @param filter The filter to invert
     * @return A filter that returns the opposite of the provided filter
     */
    public static FileFilter not(FileFilter filter) {
        return file -> filter == null || !filter.accept(file);
    }
    
    /**
     * Creates a filter that accepts files with specific extensions.
     * 
     * @param extensions The file extensions to accept (without the dot)
     * @return A filter that accepts files with the specified extensions
     */
    public static FileFilter byExtension(String... extensions) {
        if (extensions == null || extensions.length == 0) {
            return ACCEPT_ALL_FILTER;
        }
        
        // Convert extensions to lowercase for case-insensitive comparison
        final String[] lowerExtensions = Arrays.stream(extensions)
            .map(ext -> ext.toLowerCase())
            .toArray(String[]::new);
        
        return file -> {
            if (!file.isFile()) {
                return false;
            }
            
            String fileName = file.getName().toLowerCase();
            // Check if file name ends with any of the extensions
            return Arrays.stream(lowerExtensions)
                .anyMatch(ext -> {
                    // Handle multiple extensions (e.g. .txt.sh)
                    String[] fileExts = fileName.split("\\.");
                    if (fileExts.length > 1) {
                        // Check if any of the file's extensions match
                        for (int i = 1; i < fileExts.length; i++) {
                            if (fileExts[i].equals(ext)) {
                                return true;
                            }
                        }
                    }
                    // Also check the last extension
                    return fileName.endsWith("." + ext);
                });
        };
    }
    
    /**
     * Creates a filter that accepts files based on a name pattern.
     * 
     * @param pattern The regex pattern to match file names
     * @return A filter that accepts files with names matching the pattern
     */
    public static FileFilter byNamePattern(String pattern) {
        return file -> file.getName().matches(pattern);
    }
    
    /**
     * Creates an EntryNameMapper that prefixes entry names with a path.
     * 
     * @param prefix The prefix to add to entry names
     * @return An EntryNameMapper that adds the prefix to entry names
     */
    public static EntryNameMapper withPrefix(String prefix) {
        final String normalizedPrefix = normalizeBasePath(prefix);
        return (file, basePath) -> normalizedPrefix + DEFAULT_NAME_MAPPER.mapEntryName(file, basePath);
    }
    
    /**
     * Creates an EntryNameMapper that replaces the file extension.
     * 
     * @param newExtension The new extension to use (without the dot)
     * @return An EntryNameMapper that changes file extensions
     */
    public static EntryNameMapper withExtension(String newExtension) {
        return (file, basePath) -> {
            String name = file.getName();
            int dotIndex = name.lastIndexOf('.');
            String newName;
            
            if (dotIndex < 0 || dotIndex == name.length() - 1) {
                // No extension or dot at the end, add the new one
                newName = name + (name.endsWith(".") ? "" : ".") + newExtension;
            } else {
                // Replace the existing extension with the new one
                newName = name.substring(0, dotIndex) + "." + newExtension;
            }
            
            // Apply the base path if provided
            String path = basePath == null ? "" : basePath;
            if (!path.isEmpty() && !path.endsWith("/")) {
                path += "/";
            }
            
            return path + newName;
        };
    }
    
    /**
     * Creates a collection containing a single file for archive methods that expect collections.
     * 
     * @param file The file to include in the collection
     * @return A collection containing only the specified file
     */
    public static Collection<File> singleFileCollection(File file) {
        return Arrays.asList(file);
    }
} 