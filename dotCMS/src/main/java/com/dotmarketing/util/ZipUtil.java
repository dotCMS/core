package com.dotmarketing.util;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for handling ZIP file operations safely.
 * All ZIP file operations should use methods from this class to ensure consistent
 * security measures against zip slip vulnerabilities.
 * 
 * @author Jason Tesser
 * @since 1.6
 */
public class ZipUtil {

	/**
	 * Default buffer size for I/O operations with ZIP files
	 */
	private static final int DEFAULT_BUFFER_SIZE = ArchiveUtil.DEFAULT_BUFFER_SIZE;

	/**
	 * Configuration keys for archive extraction limits
	 */
	public static final String ZIP_MAX_TOTAL_SIZE_KEY = "ZIP_MAX_TOTAL_SIZE";
	public static final String ZIP_MAX_FILE_SIZE_KEY = "ZIP_MAX_FILE_SIZE";
	public static final String ZIP_MAX_ENTRIES_KEY = "ZIP_MAX_ENTRIES";
	
	/**
	 * Default protection limits for archive extraction - use shared constants from ArchiveUtil
	 */
	private static final long DEFAULT_MAX_TOTAL_SIZE = ArchiveUtil.DEFAULT_MAX_TOTAL_SIZE;
	private static final long DEFAULT_MAX_FILE_SIZE = ArchiveUtil.DEFAULT_MAX_FILE_SIZE;
	private static final int DEFAULT_MAX_ENTRIES = ArchiveUtil.DEFAULT_MAX_ENTRIES;

	/**
	 * Defines how suspicious zip entries should be handled
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
	 * Sets the default handling mode for suspicious zip entries
	 * 
	 * @param handlingMode The handling mode to use for suspicious entries
	 */
	public static void setDefaultSuspiciousEntryHandling(SuspiciousEntryHandling handlingMode) {
		defaultHandlingMode.set(handlingMode);
	}
	
	/**
	 * Gets the current default handling mode for suspicious zip entries
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
		String sysProp = System.getProperty(ZIP_MAX_TOTAL_SIZE_KEY);
		if (sysProp != null && !sysProp.trim().isEmpty()) {
			return ArchiveUtil.parseSize(sysProp, DEFAULT_MAX_TOTAL_SIZE);
		}
		// Fall back to config
		String configValue = Config.getStringProperty(ZIP_MAX_TOTAL_SIZE_KEY, null);
		return ArchiveUtil.parseSize(configValue, DEFAULT_MAX_TOTAL_SIZE);
	}
	
	/**
	 * Gets the maximum size of a single extracted file (in bytes)
	 * 
	 * @return Maximum file size in bytes from config or default value
	 */
	public static long getMaxFileSize() {
		// First check if there's a system property (for unit tests)
		String sysProp = System.getProperty(ZIP_MAX_FILE_SIZE_KEY);
		if (sysProp != null && !sysProp.trim().isEmpty()) {
			return ArchiveUtil.parseSize(sysProp, DEFAULT_MAX_FILE_SIZE);
		}
		// Fall back to config
		String configValue = Config.getStringProperty(ZIP_MAX_FILE_SIZE_KEY, null);
		return ArchiveUtil.parseSize(configValue, DEFAULT_MAX_FILE_SIZE);
	}
	
	/**
	 * Gets the maximum number of entries to extract
	 * 
	 * @return Maximum number of entries from config or default value
	 */
	public static int getMaxEntries() {
		// First check if there's a system property (for unit tests)
		String sysProp = System.getProperty(ZIP_MAX_ENTRIES_KEY);
		if (sysProp != null && !sysProp.trim().isEmpty()) {
			try {
				return Integer.parseInt(sysProp);
			} catch (NumberFormatException e) {
				Logger.warn(ZipUtil.class, "Invalid ZIP_MAX_ENTRIES value: " + sysProp + ", using default: " + DEFAULT_MAX_ENTRIES);
			}
		}
		// Fall back to config
		return Config.getIntProperty(ZIP_MAX_ENTRIES_KEY, DEFAULT_MAX_ENTRIES);
	}
	
	/**
	 * Convert ZipUtil handling mode to ArchiveUtil handling mode
	 * 
	 * @param mode The ZipUtil handling mode
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
	 * Zips the contents of a directory and saves it in the provided ZipOutputStream.
	 * @param dir2zip Directory to zip
	 * @param zos ZipOutputStream to write to
	 * @param includeRootDir Whether to include the root directory in the entry paths
	 * @throws IOException If an error occurs during the zip process
	 * @throws IllegalArgumentException If the arguments are invalid
	 */
	public static void zipDirectory(String dir2zip, ZipOutputStream zos, boolean includeRootDir) throws IllegalArgumentException, IOException {
		File zipDir = new File(dir2zip);
		if(!zipDir.isDirectory()){
			throw new IllegalArgumentException("You must pass a directory");
		}
		String basePath = includeRootDir ? zipDir.getName() + "/" : "";
		addDirectoryToZip(zos, zipDir, basePath, includeRootDir);
	}

	/**
	 * Overload for backward compatibility (does not include root directory)
	 */
	public static void zipDirectory(String dir2zip, ZipOutputStream zos) throws IllegalArgumentException, IOException {
		zipDirectory(dir2zip, zos, false);
	}

	/**
	 * Adds a directory and all its contents to a ZIP archive.
	 * @param zipOut The ZIP output stream
	 * @param directory The directory to add
	 * @param baseEntryPath Base path within the ZIP for this directory (can be empty or null)
	 * @param includeRootDir Whether to include the root directory in the entry paths
	 * @throws IOException If an error occurs during the I/O operations
	 */
	public static void addDirectoryToZip(ZipOutputStream zipOut, File directory, String baseEntryPath, boolean includeRootDir) throws IOException {
		addDirectoryToZip(zipOut, directory, baseEntryPath, includeRootDir, defaultHandlingMode.get());
	}

	public static void addDirectoryToZip(ZipOutputStream zipOut, File directory, String baseEntryPath, boolean includeRootDir, SuspiciousEntryHandling handlingMode) throws IOException {
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("Source must be a directory");
		}
		String basePath = baseEntryPath == null ? "" : baseEntryPath;
		if (!basePath.isEmpty() && !basePath.endsWith("/")) {
			basePath += "/";
		}
		// Only add a directory entry for the root if includeRootDir is true and basePath is not empty
		if (includeRootDir && !basePath.isEmpty()) {
			ZipEntry dirEntry = createSafeZipEntry(basePath, handlingMode);
			zipOut.putNextEntry(dirEntry);
			zipOut.closeEntry();
		}
		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					String subDirPath = basePath + file.getName() + "/";
					addDirectoryToZip(zipOut, file, subDirPath, true, handlingMode);
				} else {
					String fileEntryPath = basePath + file.getName();
					addFileToZip(zipOut, file, fileEntryPath, handlingMode);
				}
			}
		}
	}

	/**
	 * Extracts a zip file to a specified directory.
	 * @param zipFile the zip file to extract
	 * @param toDir the target directory
	 * @throws java.io.IOException
	 */
	public static void extract(final ZipFile zipFile, final File toDir) throws IOException{
		// Delegate to the centralized safe extraction method
		safeExtractAll(zipFile, toDir);
	}	
	   
	/**
	 * Extracts an entry of a zip file to a specified directory.
	 * @param zipFile the zip file to extract from
	 * @param zipEntry the entry of the zip file to extract
	 * @param toDir the target directory
	 * @throws java.io.IOException
	 */
	private static void extract(final ZipFile zipFile, final ZipEntry zipEntry,
			final File toDir) throws IOException {
		// Delegate to the centralized safe extraction method for a single entry
		safeExtractEntry(zipFile, zipEntry, toDir);
	}
	
	/**
	 * Extracts an entry of a zip file to a specified directory.
	 * @param zipFile the zip file to extract from
	 * @param zipEntry the original entry from the zip file
	 * @param sanitizedEntry the sanitized entry with safe path
	 * @param toDir the target directory
	 * @throws java.io.IOException
	 */
	private static void extract(final ZipFile zipFile, final ZipEntry zipEntry, 
			final ZipEntry sanitizedEntry, final File toDir) throws IOException {
		// This method exists for backward compatibility
		// We'll use our new utility method instead of duplicating code
		safeExtractEntry(zipFile, zipEntry, toDir);
	}


	public static void extract(final InputStream zipFile, final String outputDirStr)
			throws IOException {
		// Delegate to the centralized safe extraction method
		safeExtract(zipFile, outputDirStr);
	}

	/**
	 * Check paths to determine if we're being attacked.
	 * @param parentDir Parent directory
	 * @param newFile File to check
	 * @return true if the file is within the parent directory, false otherwise
	 * @throws IOException If an error occurs during path resolution
	 */
	static boolean isNewFileDestinationSafe(final File parentDir, final File newFile)
			throws IOException {
		return ArchiveUtil.isNewFileDestinationSafe(parentDir, newFile);
	}

	/**
	 * Adds a file entry in the form of an Input Stream to the specified ZIP file.
	 *
	 * @param zip            The contents of the ZIP file represented as the {@link ZipOutputStream}
	 * @param entryName      The Zip's entry name, i.e.; the file name.
	 * @param inputStream    The contents of the entry as an {@link InputStream}
	 * @param flushZipStream If the contents of the {@link ZipOutputStream} need to be flushed as soon as an entry is
	 *                       added to the ZIP file, set this to {@code true}.
	 * @param handlingMode   How to handle suspicious entries
	 *
	 * @throws IOException An error occurred when adding an entry to the ZIP file, or handling streams.
	 */
	public static void addZipEntry(final ZipOutputStream zip, final String entryName, final InputStream inputStream,
								   final boolean flushZipStream, final SuspiciousEntryHandling handlingMode) throws IOException {
		Objects.requireNonNull(zip, "ZipOutputStream cannot be null");
		Objects.requireNonNull(entryName, "Entry name cannot be null");
		Objects.requireNonNull(inputStream, "InputStream cannot be null");

		// Check file size limit before adding the entry
		if (!ArchiveUtil.checkEntrySizeLimit(entryName, inputStream.available(), getMaxFileSize(), 
			convertHandlingMode(handlingMode))) {
			throw new SecurityException("Entry '" + entryName + "' exceeds maximum allowed file size");
		}

		// Create a safe zip entry
		ZipEntry entry = createSafeZipEntry(sanitizePath(entryName, handlingMode), handlingMode);
		zip.putNextEntry(entry);

		// Copy the input stream to the zip output stream
		IOUtils.copy(inputStream, zip);

		if (flushZipStream) {
			zip.flush();
		}

		zip.closeEntry();
	}
	
	/**
	 * Adds a file entry using the default handling mode for suspicious entries.
	 *
	 * @param zip            The contents of the ZIP file represented as the {@link ZipOutputStream}
	 * @param entryName      The Zip's entry name, i.e.; the file name.
	 * @param inputStream    The contents of the entry as an {@link InputStream}
	 * @param flushZipStream If the contents of the {@link ZipOutputStream} need to be flushed as soon as an entry is
	 *                       added to the ZIP file, set this to {@code true}.
	 *
	 * @throws IOException An error occurred when adding an entry to the ZIP file, or handling streams.
	 */
	public static void addZipEntry(final ZipOutputStream zip, final String entryName, final InputStream inputStream,
								   final boolean flushZipStream) throws IOException {
		addZipEntry(zip, entryName, inputStream, flushZipStream, defaultHandlingMode.get());
	}

	/**
	 * Sanitizes a path to prevent path traversal and zip slip vulnerabilities.
	 * Delegate to shared ArchiveUtil implementation.
	 * 
	 * @param entryName The path to sanitize
	 * @param handlingMode How to handle suspicious entries
	 * @param archivePath The path to the zip file containing this entry
	 * @return A sanitized path safe for zip file entries
	 * @throws SecurityException If the path contains malicious path traversal attempts
	 *                           and handling mode is ABORT
	 */
	public static String sanitizePath(String entryName, SuspiciousEntryHandling handlingMode, String archivePath) {
		return ArchiveUtil.sanitizePath(entryName, convertHandlingMode(handlingMode), archivePath);
	}

	/**
	 * Sanitizes a path to prevent path traversal and zip slip vulnerabilities.
	 * Delegate to shared ArchiveUtil implementation.
	 * 
	 * @param entryName The path to sanitize
	 * @param handlingMode How to handle suspicious entries
	 * @return A sanitized path safe for zip file entries
	 * @throws SecurityException If the path contains malicious path traversal attempts
	 *                           and handling mode is ABORT
	 * @deprecated Use {@link #sanitizePath(String, SuspiciousEntryHandling, String)} instead
	 */
	@Deprecated
	public static String sanitizePath(String entryName, SuspiciousEntryHandling handlingMode) {
		return sanitizePath(entryName, handlingMode, "unknown zip file");
	}

	/**
	 * Sanitizes a path to prevent path traversal and zip slip vulnerabilities using
	 * the default handling mode.
	 * 
	 * @param entryName The path to sanitize
	 * @return A sanitized path safe for zip file entries
	 * @throws SecurityException If the path contains malicious path traversal attempts
	 *                           and default handling mode is ABORT
	 */
	public static String sanitizePath(String entryName) {
		return sanitizePath(entryName, defaultHandlingMode.get(), "unknown zip file");
	}

	/**
	 * Creates a new ZipEntry with a sanitized path to prevent zip slip vulnerabilities.
	 * 
	 * @param entryName The original entry name
	 * @param handlingMode How to handle suspicious entries
	 * @return A ZipEntry with a sanitized path
	 */
	public static ZipEntry createSafeZipEntry(String entryName, SuspiciousEntryHandling handlingMode) {
		return new ZipEntry(sanitizePath(entryName, handlingMode));
	}
	
	/**
	 * Creates a new ZipEntry with a sanitized path using the default handling mode.
	 * 
	 * @param entryName The original entry name
	 * @return A ZipEntry with a sanitized path
	 */
	public static ZipEntry createSafeZipEntry(String entryName) {
		return createSafeZipEntry(entryName, defaultHandlingMode.get());
	}
	
	/**
	 * Safely extracts a single zip entry to the specified directory.
	 * Centralizes all security checks and path sanitization.
	 * 
	 * @param zipFile The zip file containing the entry
	 * @param zipEntry The entry to extract
	 * @param toDir The target directory
	 * @param handlingMode How to handle suspicious entries
	 * @return true if extraction was successful, false if skipped
	 * @throws IOException If an error occurs during extraction
	 * @throws SecurityException If a malicious entry is detected and handlingMode is ABORT
	 */
	public static boolean safeExtractEntry(ZipFile zipFile, ZipEntry zipEntry, File toDir, 
											 SuspiciousEntryHandling handlingMode) throws IOException {
		try {
			// Check for zip bombs - excessive compression ratio
			long size = zipEntry.getSize();
			long compressedSize = zipEntry.getCompressedSize();
			// Use shared implementation for size checks
			if (!ArchiveUtil.checkEntrySizeLimit(zipEntry.getName(), size, getMaxFileSize(), 
				convertHandlingMode(handlingMode))) {
				return false;
			}
			// If both sizes are available, check compression ratio
			if (size > 0 && compressedSize > 0) {
				long ratio = size / (compressedSize == 0 ? 1 : compressedSize);
				if (ratio > 100) { // Suspicious compression ratio
					Logger.warn(ZipUtil.class, String.format(
						"Suspicious compression ratio (%d:1) for entry: %s", ratio, zipEntry.getName()));
				}
			}
			// Sanitize the entry name
			String sanitizedName = sanitizePath(zipEntry.getName(), handlingMode);
			File outputFile = new File(toDir, sanitizedName);
			// Ensure all parent directories are directories
			ensureParentDirectoriesAreDirectories(outputFile);
			// Always create parent directory before writing a file
			File parent = outputFile.getParentFile();
			if (parent != null && !parent.exists()) {
				if (!parent.mkdirs()) {
					throw new IOException("Failed to create directory: " + parent);
				}
			}
			// Handle file/directory conflicts
			if (zipEntry.isDirectory()) {
				if (outputFile.exists() && outputFile.isFile()) {
					Logger.warn(ZipUtil.class, "Replacing file with directory during extraction: " + outputFile.getAbsolutePath());
					if (!outputFile.delete()) {
						throw new IOException("Failed to delete file to create directory: " + outputFile);
					}
				}
				if (checkSecurity(toDir, outputFile, handlingMode)) {
					if (!outputFile.exists() && !outputFile.mkdirs()) {
						throw new IOException("Failed to create directory: " + outputFile);
					}
				}
				else {
					return false;
				}
			} else {
				if (outputFile.exists() && outputFile.isDirectory()) {
					Logger.warn(ZipUtil.class, "Replacing directory with file during extraction: " + outputFile.getAbsolutePath());
					if (!deleteDirectory(outputFile)) {
						throw new IOException("Failed to delete directory to create file: " + outputFile);
					}
				}
				if (!checkSecurity(toDir, outputFile, handlingMode)) {
					return false;
				}
				try (InputStream is = zipFile.getInputStream(zipEntry);
					 OutputStream os = Files.newOutputStream(outputFile.toPath())) {
					byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
					long totalRead = 0;
					int read;
					while ((read = is.read(buffer)) != -1) {
						totalRead += read;
						if (totalRead > getMaxFileSize()) {
							os.close();
							outputFile.delete();
							String message = String.format(
								"Entry '%s' exceeded maximum allowed file size during extraction", 
								zipEntry.getName());
							if (handlingMode == SuspiciousEntryHandling.ABORT) {
								throw new SecurityException(message);
							}
							Logger.warn(ZipUtil.class, message);
							return false;
						}
						os.write(buffer, 0, read);
					}
				}
			}
			return true;
		} catch (SecurityException e) {
			if (handlingMode == SuspiciousEntryHandling.ABORT) {
				throw e;
			}
			Logger.warn(ZipUtil.class, "Skipped suspicious entry: " + zipEntry.getName() + " - " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * Safely extracts a zip entry using the default handling mode.
	 * 
	 * @param zipFile The zip file containing the entry
	 * @param zipEntry The entry to extract
	 * @param toDir The target directory
	 * @throws IOException If an error occurs during extraction
	 */
	public static void safeExtractEntry(ZipFile zipFile, ZipEntry zipEntry, File toDir) throws IOException {
		safeExtractEntry(zipFile, zipEntry, toDir, defaultHandlingMode.get());
	}
	
	/**
	 * Check paths to determine if we're being attacked.
	 * Delegates to shared implementation in ArchiveUtil.
	 * 
	 * @param parentDir Parent directory
	 * @param newFile File to check
	 * @param handlingMode How to handle suspicious entries
	 * @return true if the file is within the parent directory, false otherwise
	 * @throws IOException If an error occurs during path resolution
	 * @throws SecurityException If handlingMode is ABORT and the path is suspicious
	 */
	static boolean checkSecurity(final File parentDir, final File newFile, SuspiciousEntryHandling handlingMode) throws IOException {
		return ArchiveUtil.checkSecurity(parentDir, newFile, convertHandlingMode(handlingMode));
	}
	
	/**
	 * Check paths to determine if we're being attacked using the default handling mode.
	 * @param parentDir Parent directory
	 * @param newFile File to check
	 * @return true if the file is within the parent directory, false otherwise
	 * @throws IOException If an error occurs during path resolution
	 */
	static boolean checkSecurity(final File parentDir, final File newFile) throws IOException {
		return checkSecurity(parentDir, newFile, defaultHandlingMode.get());
	}

	/**
	 * Safely extracts an entire zip file to the specified directory.
	 * Centralizes all security checks and path sanitization.
	 * 
	 * @param zipFile The zip file to extract
	 * @param toDir The target directory
	 * @param handlingMode How to handle suspicious entries
	 * @throws IOException If an error occurs during extraction
	 */
	public static void safeExtractAll(ZipFile zipFile, File toDir, SuspiciousEntryHandling handlingMode) throws IOException {
		if (!toDir.exists()) {
			toDir.mkdirs();
		}
		
		// Apply DoS protections: track total size and entry count
		int entryCount = 0;
		int maxEntries = getMaxEntries();
		AtomicLong totalSizeCounter = new AtomicLong(0);
		
		Enumeration<?> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			// Check entry count limit using shared implementation
			if (!ArchiveUtil.checkEntryCountLimit(++entryCount, maxEntries, convertHandlingMode(handlingMode))) {
				throw new SecurityException("Zip file exceeds maximum allowed number of entries");
			}
			
			ZipEntry zipEntry = (ZipEntry) entries.nextElement();
			try {
				// Check file size limits 
				long entrySize = zipEntry.getSize();
				if (entrySize > 0) { // Some entries might not have size info
					// Check individual file size using shared implementation
					if (!ArchiveUtil.checkEntrySizeLimit(zipEntry.getName(), entrySize, getMaxFileSize(), 
						convertHandlingMode(handlingMode))) {
						throw new SecurityException("Entry '" + zipEntry.getName() + "' exceeds maximum allowed file size");
					}
					
					// Check total size using shared implementation
					if (!ArchiveUtil.checkTotalSizeLimit(zipEntry.getName(), entrySize, totalSizeCounter, 
						getMaxTotalSize(), convertHandlingMode(handlingMode))) {
						throw new SecurityException("Zip file exceeds maximum allowed total size");
					}
				}
				
				// Extract the entry
				if (safeExtractEntry(zipFile, zipEntry, toDir, handlingMode)) {
					// Update total size counter if extraction was successful and we have size info
					if (entrySize > 0) {
						totalSizeCounter.addAndGet(entrySize);
					}
				}
			} catch (SecurityException e) {
				if (handlingMode == SuspiciousEntryHandling.ABORT) {
					throw e;
				}
				Logger.warn(ZipUtil.class, "Skipped suspicious entry: " + zipEntry.getName() + " - " + e.getMessage());
			}
		}
	}
	
	/**
	 * Safely extracts an entire zip file using the default handling mode.
	 * 
	 * @param zipFile The zip file to extract
	 * @param toDir The target directory
	 * @throws IOException If an error occurs during extraction
	 */
	public static void safeExtractAll(ZipFile zipFile, File toDir) throws IOException {
		safeExtractAll(zipFile, toDir, defaultHandlingMode.get());
	}
	
	/**
	 * Safely extracts a zip input stream to the specified directory.
	 * Centralizes all security checks and path sanitization.
	 * 
	 * @param zipStream The zip input stream to extract
	 * @param outputDir The target directory
	 * @param handlingMode How to handle suspicious entries
	 * @throws IOException If an error occurs during extraction
	 * @throws SecurityException If a malicious zip entry is detected and handlingMode is ABORT
	 */
	public static void safeExtract(InputStream zipStream, String outputDirStr, SuspiciousEntryHandling handlingMode) throws IOException {
		Objects.requireNonNull(zipStream, "ZipInputStream cannot be null");
		Objects.requireNonNull(outputDirStr, "Output directory cannot be null");

		File outputDir = new File(outputDirStr);
		if (!outputDir.exists()) {
			if (!outputDir.mkdirs()) {
				throw new IOException("Failed to create output directory: " + outputDir);
			}
		}

		// Track total size and entry count
		AtomicLong totalSize = new AtomicLong(0);
		int entryCount = 0;

		try (ZipInputStream zis = new ZipInputStream(zipStream)) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				// Check entry count limit
				if (!ArchiveUtil.checkEntryCountLimit(++entryCount, getMaxEntries(), convertHandlingMode(handlingMode))) {
					throw new SecurityException("Zip file exceeds maximum allowed number of entries");
				}

				// Check file size limit
				if (!entry.isDirectory() && !ArchiveUtil.checkEntrySizeLimit(entry.getName(), entry.getSize(), 
					getMaxFileSize(), convertHandlingMode(handlingMode))) {
					throw new SecurityException("Entry '" + entry.getName() + "' exceeds maximum allowed file size");
				}

				// Check total size limit
				if (!entry.isDirectory() && !ArchiveUtil.checkTotalSizeLimit(entry.getName(), entry.getSize(), 
					totalSize, getMaxTotalSize(), convertHandlingMode(handlingMode))) {
					throw new SecurityException("Zip file exceeds maximum allowed total size");
				}

				// Sanitize the entry name
				String sanitizedPath = sanitizePath(entry.getName(), handlingMode);
				File outputFile = new File(outputDir, sanitizedPath);

				// Create parent directories if needed
				File parent = outputFile.getParentFile();
				if (parent != null && !parent.exists()) {
					if (!parent.mkdirs()) {
						throw new IOException("Failed to create directory: " + parent);
					}
				}

				// Extract the entry
				if (entry.isDirectory()) {
					if (!outputFile.exists() && !outputFile.mkdirs()) {
						throw new IOException("Failed to create directory: " + outputFile);
					}
				} else {
					long bytesExtracted = 0;
					try (OutputStream os = Files.newOutputStream(outputFile.toPath())) {
						byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
						int read;
						while ((read = zis.read(buffer)) != -1) {
							os.write(buffer, 0, read);
							bytesExtracted += read;
							if (bytesExtracted > getMaxFileSize()) {
								os.close();
								outputFile.delete();
								throw new SecurityException("Entry '" + entry.getName() + "' exceeded maximum allowed file size of " + getMaxFileSize() + " bytes during extraction");
							}
						}
					}
					long newTotal = totalSize.addAndGet(bytesExtracted);
					if (newTotal > getMaxTotalSize()) {
						outputFile.delete();
						throw new SecurityException("Extraction exceeded maximum total extraction size of " + getMaxTotalSize() + " bytes");
					}
				}

				zis.closeEntry();
			}
		}
	}
	
	/**
	 * Safely extracts a zip input stream using the default handling mode.
	 * 
	 * @param zipStream The zip input stream to extract
	 * @param outputDir The target directory
	 * @throws IOException If an error occurs during extraction
	 */
	public static void safeExtract(InputStream zipStream, String outputDir) throws IOException {
		safeExtract(zipStream, outputDir, defaultHandlingMode.get());
	}

	/**
	 * Creates a new ZipOutputStream with default settings and compression level
	 * 
	 * @param outputFile The file to write the zip to
	 * @return A configured ZipOutputStream
	 * @throws IOException If an error occurs creating the output stream
	 */
	public static ZipOutputStream createZipOutputStream(File outputFile) throws IOException {
		ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outputFile.toPath()));
		zos.setLevel(9); // Maximum compression
		return zos;
	}
	
	/**
	 * Creates a new ZipOutputStream with default settings and compression level
	 * 
	 * @param outputStream The output stream to write the zip to
	 * @return A configured ZipOutputStream
	 * @throws IOException If an error occurs creating the output stream
	 */
	public static ZipOutputStream createZipOutputStream(OutputStream outputStream) throws IOException {
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(outputStream));
		zos.setLevel(9); // Maximum compression
		return zos;
	}

	/**
	 * Adds a file to a ZIP archive using a safe entry name.
	 * This method handles all the I/O operations internally.
	 *
	 * @param zipOut The ZIP output stream
	 * @param file The file to add
	 * @param entryName Optional custom entry name (if null, file.getName() will be used)
	 * @param handlingMode How to handle suspicious entry names
	 * @throws IOException If an error occurs during the I/O operations
	 */
	public static void addFileToZip(ZipOutputStream zipOut, File file, String entryName, 
									SuspiciousEntryHandling handlingMode) throws IOException {
		if (file == null || !file.exists() || !file.isFile()) {
			throw new IOException("Invalid or non-existent file: " + 
								 (file != null ? file.getAbsolutePath() : "null"));
		}
		
		String nameToUse = entryName != null ? entryName : file.getName();
		ZipEntry entry = createSafeZipEntry(nameToUse, handlingMode);
		
		try (InputStream in = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
			zipOut.putNextEntry(entry);
			
			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			int len;
			while ((len = in.read(buffer)) > 0) {
				zipOut.write(buffer, 0, len);
			}
			zipOut.closeEntry();
		}
	}
	
	/**
	 * Adds a file to a ZIP archive using a safe entry name and the default handling mode.
	 *
	 * @param zipOut The ZIP output stream
	 * @param file The file to add
	 * @param entryName Optional custom entry name (if null, file.getName() will be used)
	 * @throws IOException If an error occurs during the I/O operations
	 */
	public static void addFileToZip(ZipOutputStream zipOut, File file, String entryName) throws IOException {
		addFileToZip(zipOut, file, entryName, defaultHandlingMode.get());
	}

	/**
	 * Adds a file to a ZIP archive using the file's name as the entry name.
	 *
	 * @param zipOut The ZIP output stream
	 * @param file The file to add
	 * @throws IOException If an error occurs during the I/O operations
	 */
	public static void addFileToZip(ZipOutputStream zipOut, File file) throws IOException {
		addFileToZip(zipOut, file, null, defaultHandlingMode.get());
	}
	
	/**
	 * Creates a ZIP file containing a single file.
	 *
	 * @param sourceFile The file to zip
	 * @param targetZipFile The ZIP file to create
	 * @param entryName Optional custom entry name (if null, sourceFile.getName() will be used)
	 * @param handlingMode How to handle suspicious entry names
	 * @return The created ZIP file
	 * @throws IOException If an error occurs during the ZIP operation
	 */
	public static File zipSingleFile(File sourceFile, File targetZipFile, String entryName, 
									SuspiciousEntryHandling handlingMode) throws IOException {
		try (ZipOutputStream zipOut = createZipOutputStream(targetZipFile)) {
			addFileToZip(zipOut, sourceFile, entryName, handlingMode);
		}
		return targetZipFile;
	}
	
	/**
	 * Creates a ZIP file containing a single file using the default handling mode.
	 *
	 * @param sourceFile The file to zip
	 * @param targetZipFile The ZIP file to create
	 * @param entryName Optional custom entry name (if null, sourceFile.getName() will be used)
	 * @return The created ZIP file
	 * @throws IOException If an error occurs during the ZIP operation
	 */
	public static File zipSingleFile(File sourceFile, File targetZipFile, String entryName) throws IOException {
		return zipSingleFile(sourceFile, targetZipFile, entryName, defaultHandlingMode.get());
	}
	
	/**
	 * Creates a ZIP file containing a single file using the file's name as the entry name.
	 *
	 * @param sourceFile The file to zip
	 * @param targetZipFile The ZIP file to create
	 * @return The created ZIP file
	 * @throws IOException If an error occurs during the ZIP operation
	 */
	public static File zipSingleFile(File sourceFile, File targetZipFile) throws IOException {
		return zipSingleFile(sourceFile, targetZipFile, null, defaultHandlingMode.get());
	}
	
	/**
	 * Creates a ZIP file containing multiple files and/or directories.
	 *
	 * @param sources Collection of files/directories to zip
	 * @param targetZipFile The ZIP file to create
	 * @param baseEntryPath Base path within the ZIP (can be empty or null)
	 * @param handlingMode How to handle suspicious entry names
	 * @return The created ZIP file
	 * @throws IOException If an error occurs during the ZIP operation
	 */
	public static File zipFiles(Collection<File> sources, File targetZipFile, String baseEntryPath, 
							   SuspiciousEntryHandling handlingMode) throws IOException {
		try (ZipOutputStream zipOut = createZipOutputStream(targetZipFile)) {
			for (File source : sources) {
				String entryPath = baseEntryPath == null ? "" : sanitizePath(baseEntryPath, handlingMode);
				if (!entryPath.isEmpty() && !entryPath.endsWith("/")) {
					entryPath += "/";
				}
				
				if (source.isDirectory()) {
					addDirectoryToZip(zipOut, source, entryPath + source.getName(), true, handlingMode);
				} else {
					addFileToZip(zipOut, source, entryPath + source.getName(), handlingMode);
				}
			}
		}
		return targetZipFile;
	}
	
	/**
	 * Creates a ZIP file containing multiple files and/or directories using the default handling mode.
	 *
	 * @param sources Collection of files/directories to zip
	 * @param targetZipFile The ZIP file to create
	 * @param baseEntryPath Base path within the ZIP (can be empty or null)
	 * @return The created ZIP file
	 * @throws IOException If an error occurs during the ZIP operation
	 */
	public static File zipFiles(Collection<File> sources, File targetZipFile, String baseEntryPath) throws IOException {
		return zipFiles(sources, targetZipFile, baseEntryPath, defaultHandlingMode.get());
	}
	
	/**
	 * Creates a ZIP file containing multiple files and/or directories with no base entry path.
	 *
	 * @param sources Collection of files/directories to zip
	 * @param targetZipFile The ZIP file to create
	 * @return The created ZIP file
	 * @throws IOException If an error occurs during the ZIP operation
	 */
	public static File zipFiles(Collection<File> sources, File targetZipFile) throws IOException {
		return zipFiles(sources, targetZipFile, null, defaultHandlingMode.get());
	}

	/**
	 * Creates a ZIP file containing files/directories with customized filtering, processing and naming.
	 *
	 * @param sources Collection of files/directories to zip
	 * @param targetZipFile The ZIP file to create
	 * @param baseEntryPath Base path within the ZIP (can be empty or null)
	 * @param filter Lambda that determines which files to include (null for all files)
	 * @param processor Lambda to process files before adding them (null for no processing)
	 * @param nameMapper Lambda to customize entry names (null for default naming)
	 * @param handlingMode How to handle suspicious entry names
	 * @return The created ZIP file
	 * @throws IOException If an error occurs during the ZIP operation
	 */
	public static File zipFilesWithCustomProcessing(
			Collection<File> sources, 
			File targetZipFile, 
			String baseEntryPath,
			ArchiveUtil.FileFilter filter, 
			ArchiveUtil.FileProcessor processor, 
			ArchiveUtil.EntryNameMapper nameMapper,
			SuspiciousEntryHandling handlingMode) throws IOException {
			
		try (ZipOutputStream zipOut = createZipOutputStream(targetZipFile)) {
			for (File source : sources) {
				// Skip if the filter rejects this file
				if (filter != null && !filter.accept(source)) {
					continue;
				}
				
				String basePath = baseEntryPath == null ? "" : sanitizePath(baseEntryPath, handlingMode);
				if (!basePath.isEmpty() && !basePath.endsWith("/")) {
					basePath += "/";
				}
				
				if (source.isDirectory()) {
					addDirectoryToZipWithCustomProcessing(zipOut, source, basePath, filter, processor, nameMapper, handlingMode);
				} else {
					addFileToZipWithCustomProcessing(zipOut, source, basePath, processor, nameMapper, handlingMode);
				}
			}
		}
		return targetZipFile;
	}
	
	/**
	 * Simplified version of zipFilesWithCustomProcessing with default handling mode.
	 * 
	 * @param sources Collection of files/directories to zip
	 * @param targetZipFile The ZIP file to create
	 * @param baseEntryPath Base path within the ZIP (can be empty or null)
	 * @param filter Lambda that determines which files to include (null for all files)
	 * @param processor Lambda to process files before adding them (null for no processing)
	 * @param nameMapper Lambda to customize entry names (null for default naming)
	 * @return The created ZIP file
	 * @throws IOException If an error occurs during the ZIP operation
	 */
	public static File zipFilesWithCustomProcessing(
			Collection<File> sources, 
			File targetZipFile, 
			String baseEntryPath,
			ArchiveUtil.FileFilter filter, 
			ArchiveUtil.FileProcessor processor, 
			ArchiveUtil.EntryNameMapper nameMapper) throws IOException {
		return zipFilesWithCustomProcessing(sources, targetZipFile, baseEntryPath, 
		    filter != null ? filter : ArchiveUtil.ACCEPT_ALL_FILTER,
		    processor != null ? processor : ArchiveUtil.NO_PROCESSING,
		    nameMapper != null ? nameMapper : ArchiveUtil.DEFAULT_NAME_MAPPER, 
		    defaultHandlingMode.get());
	}
	
	/**
	 * Adds a file to a ZIP with custom processing.
	 * 
	 * @param zipOut The ZIP output stream
	 * @param file The file to add
	 * @param basePath Base path within the archive
	 * @param processor Lambda to process the file (null for no processing)
	 * @param nameMapper Lambda to customize entry name (null for default naming)
	 * @param handlingMode How to handle suspicious entry names
	 * @throws IOException If an error occurs during the operation
	 */
	private static void addFileToZipWithCustomProcessing(
			ZipOutputStream zipOut, 
			File file, 
			String basePath,
			ArchiveUtil.FileProcessor processor, 
			ArchiveUtil.EntryNameMapper nameMapper,
			SuspiciousEntryHandling handlingMode) throws IOException {
			
		if (file == null || !file.exists() || !file.isFile()) {
			throw new IOException("Invalid or non-existent file: " + 
								 (file != null ? file.getAbsolutePath() : "null"));
		}
		
		// Determine entry name
		String entryName;
		if (nameMapper != null) {
			entryName = nameMapper.mapEntryName(file, basePath);
		} else {
			entryName = basePath + file.getName();
		}
		
		// Create safe entry
		ZipEntry entry = createSafeZipEntry(entryName, handlingMode);
		zipOut.putNextEntry(entry);
		
		// Process and write file contents
		try (InputStream in = processor != null ? 
				processor.process(file, entryName) : 
				new BufferedInputStream(Files.newInputStream(file.toPath()))) {
			
			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			int len;
			while ((len = in.read(buffer)) > 0) {
				zipOut.write(buffer, 0, len);
			}
		}
		
		zipOut.closeEntry();
	}
	
	/**
	 * Adds a directory to a ZIP with custom processing.
	 * 
	 * @param zipOut The ZIP output stream
	 * @param directory The directory to add
	 * @param basePath Base path within the archive
	 * @param filter Lambda that determines which files to include
	 * @param processor Lambda to process files before adding them
	 * @param nameMapper Lambda to customize entry names
	 * @param handlingMode How to handle suspicious entry names
	 * @throws IOException If an error occurs during the operation
	 */
	private static void addDirectoryToZipWithCustomProcessing(
			ZipOutputStream zipOut, 
			File directory, 
			String basePath,
			ArchiveUtil.FileFilter filter, 
			ArchiveUtil.FileProcessor processor, 
			ArchiveUtil.EntryNameMapper nameMapper,
			SuspiciousEntryHandling handlingMode) throws IOException {
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("Source must be a directory");
		}
		// Always add a directory entry for the current directory
		String dirEntryName = basePath.isEmpty() ? directory.getName() + "/" : basePath;
		if (!dirEntryName.endsWith("/")) {
			dirEntryName += "/";
		}
		ZipEntry dirEntry = createSafeZipEntry(dirEntryName, handlingMode);
		zipOut.putNextEntry(dirEntry);
		zipOut.closeEntry();
		// Process all files in the directory
		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					String newBasePath = dirEntryName + file.getName() + "/";
					addDirectoryToZipWithCustomProcessing(zipOut, file, newBasePath, filter, processor, nameMapper, handlingMode);
				} else if (filter == null || filter.accept(file)) {
					String entryName = nameMapper != null ? nameMapper.mapEntryName(file, dirEntryName) : dirEntryName + file.getName();
					addFileToZipWithCustomProcessing(zipOut, file, dirEntryName, processor, nameMapper, handlingMode);
				}
			}
		}
	}

	/**
	 * Example method showing how to create a ZIP file with customized file filtering
	 * and content processing. This is a practical utility that demonstrates the use
	 * of functional interfaces.
	 * 
	 * @param sourceDirectory Directory to zip
	 * @param targetZipFile Destination ZIP file
	 * @param includeExtensions File extensions to include (null or empty for all files)
	 * @param excludePatterns File name patterns to exclude (null or empty for no exclusions)
	 * @param maxFileSizeBytes Maximum size of individual files to include (0 for no limit)
	 * @return The created ZIP file
	 * @throws IOException If an error occurs during the ZIP operation
	 */
	public static File createFilteredZipFromDirectory(
			File sourceDirectory,
			File targetZipFile,
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
		return zipFilesWithCustomProcessing(
				ArchiveUtil.singleFileCollection(sourceDirectory),
				targetZipFile,
				null,  // No base path
				filter,
				processor,
				nameMapper);
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
			Logger.warn(ZipUtil.class, "Replacing file with directory during extraction: " + parent.getAbsolutePath());
			if (!parent.delete()) {
				throw new IOException("Failed to delete file to create directory: " + parent);
			}
			if (!parent.mkdirs()) {
				throw new IOException("Failed to create directory: " + parent);
			}
		}
	}
}
