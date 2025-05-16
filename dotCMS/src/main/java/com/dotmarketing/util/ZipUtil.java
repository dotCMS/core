package com.dotmarketing.util;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
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
		String configValue = Config.getStringProperty(ZIP_MAX_TOTAL_SIZE_KEY, null);
		return ArchiveUtil.parseSize(configValue, DEFAULT_MAX_TOTAL_SIZE);
	}
	
	/**
	 * Gets the maximum size of a single extracted file (in bytes)
	 * 
	 * @return Maximum file size in bytes from config or default value
	 */
	public static long getMaxFileSize() {
		String configValue = Config.getStringProperty(ZIP_MAX_FILE_SIZE_KEY, null);
		return ArchiveUtil.parseSize(configValue, DEFAULT_MAX_FILE_SIZE);
	}
	
	/**
	 * Gets the maximum number of entries to extract
	 * 
	 * @return Maximum number of entries from config or default value
	 */
	public static int getMaxEntries() {
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
	 * Zips the contents of a directory and saves it in the provided ZipOutputStream
	 * 
	 * @param dir2zip Directory to zip
	 * @param zos ZipOutputStream to write to
	 * @param zipPath Base path used for entry names
	 * @throws IOException If an error occurs during the zip process
	 * @throws IllegalArgumentException If the arguments are invalid
	 */
	private static void zipDirectory(String dir2zip, ZipOutputStream zos, String zipPath)throws IOException, IllegalArgumentException { 
		final File zipDir = new File(dir2zip);
		//create a new File object based on the directory we  have to zip get a listing of the directory content 
		final String[] dirList = zipDir.list();
		final byte[] readBuffer = new byte[DEFAULT_BUFFER_SIZE];
		int bytesIn = 0;
		//loop through dirList, and zip the files 
		for(int i=0; i<dirList.length; i++){
		    final File f = new File(zipDir, dirList[i]);
		    if(f.isDirectory()){ 
		    	//if the File object is a directory, call this function again to add its content recursively 
			    String filePath = f.getPath(); 
			    zipDirectory(filePath, zos, zipPath); 
			    continue; 
		    } 
			//if we reached here, the File object f was not a directory create a InputStream on top of f
			final InputStream fis = Files.newInputStream(f.toPath());
			//create a new zip entry 
			final String path = f.getPath().substring(zipPath.length()+1);
			// Sanitize the path to prevent zip slip vulnerabilities
			final String sanitizedPath = sanitizePath(path);
			final ZipEntry anEntry = new ZipEntry(sanitizedPath);
			//place the zip entry in the ZipOutputStream object 
			zos.putNextEntry(anEntry); 
			//now write the content of the file to the ZipOutputStream 
	        while((bytesIn = fis.read(readBuffer)) != -1){ 
	            zos.write(readBuffer, 0, bytesIn); 
	        } 
	        fis.close(); 
		   }
	  }

	/**
	 * Zip the contents of the directory, and save it in the zipfile
	 * @param dir2zip Directory to zip
	 * @param zos ZipOutputStream to write to
	 * @throws IOException If an error occurs during the zip process
	 * @throws IllegalArgumentException If the arguments are invalid
	 */
	public static void zipDirectory(String dir2zip, ZipOutputStream zos) throws IllegalArgumentException, IOException{
		  File zipDir = new File(dir2zip);
		  if(!zipDir.isDirectory()){
			  throw new IllegalArgumentException("You must pass a directory");
		  }
		  String zipPath = zipDir.getPath();
		  zipDirectory(dir2zip, zos, zipPath);
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
		if (inputStream != null) {
			// Sanitize the entry name to prevent zip slip vulnerabilities
			final String sanitizedEntryName = sanitizePath(entryName, handlingMode);
			final ZipEntry zipEntry = new ZipEntry(sanitizedEntryName);
			zip.putNextEntry(zipEntry);
			// Write data into zip
			IOUtils.copy(inputStream, zip);
			zip.closeEntry();
			inputStream.close();
			if (flushZipStream) {
				zip.flush();
			}
		}
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
	 * @return A sanitized path safe for zip file entries
	 * @throws SecurityException If the path contains malicious path traversal attempts
	 *                           and handling mode is ABORT
	 */
	public static String sanitizePath(String entryName, SuspiciousEntryHandling handlingMode) {
		return ArchiveUtil.sanitizePath(entryName, convertHandlingMode(handlingMode));
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
		return sanitizePath(entryName, defaultHandlingMode.get());
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
			
			// Only directories end with '/'
			if (zipEntry.isDirectory()) {
				String sanitizedName = sanitizePath(zipEntry.getName(), handlingMode);
				File newDir = new File(toDir, sanitizedName);
				// Ensure path traversal protection
				if (checkSecurity(toDir, newDir, handlingMode)) {
					newDir.mkdirs();
				} else {
					return false;
				}
			} else {
				// Not a directory, extract as a file
				String sanitizedName = sanitizePath(zipEntry.getName(), handlingMode);
				File outputFile = new File(toDir, sanitizedName);
				
				// Create parent directory if it doesn't exist
				File parent = outputFile.getParentFile();
				if (parent != null && !parent.exists()) {
					parent.mkdirs();
				}
				
				// Ensure path traversal protection
				if (!checkSecurity(toDir, outputFile, handlingMode)) {
					return false;
				}
				
				// Extract the file with proper resource management
				try (InputStream is = zipFile.getInputStream(zipEntry);
					 OutputStream os = Files.newOutputStream(outputFile.toPath())) {
					
					// Use controlled buffer copy instead of IOUtils.copy to better handle DoS attacks
					byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
					long totalRead = 0;
					int read;
					
					while ((read = is.read(buffer)) != -1) {
						totalRead += read;
						
						// Additional safeguard during extraction
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
				break;
			}
			
			ZipEntry zipEntry = (ZipEntry) entries.nextElement();
			try {
				// Check file size limits 
				long entrySize = zipEntry.getSize();
				if (entrySize > 0) { // Some entries might not have size info
					// Check individual file size using shared implementation
					if (!ArchiveUtil.checkEntrySizeLimit(zipEntry.getName(), entrySize, getMaxFileSize(), 
						convertHandlingMode(handlingMode))) {
						continue; // Skip this entry
					}
					
					// Check total size using shared implementation
					if (!ArchiveUtil.checkTotalSizeLimit(zipEntry.getName(), entrySize, totalSizeCounter, 
						getMaxTotalSize(), convertHandlingMode(handlingMode))) {
						break; // Stop extraction
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
		File outputDir = new File(outputDirStr);
		outputDir.mkdirs();
		
		// Apply DoS protections: track total size and entry count
		int entryCount = 0;
		int maxEntries = getMaxEntries();
		AtomicLong totalSizeCounter = new AtomicLong(0);
		
		ZipEntry ze = null;
		try (ZipInputStream zin = new ZipInputStream(zipStream)) {
			while ((ze = zin.getNextEntry()) != null) {
				// Check entry count limit using shared implementation
				if (!ArchiveUtil.checkEntryCountLimit(++entryCount, maxEntries, convertHandlingMode(handlingMode))) {
					break;
				}
				
				try {
					// Check file size limits
					long entrySize = ze.getSize();
					if (entrySize > 0) { // Some entries might not have size info
						// Check individual file size using shared implementation
						if (!ArchiveUtil.checkEntrySizeLimit(ze.getName(), entrySize, getMaxFileSize(), 
							convertHandlingMode(handlingMode))) {
							// Skip this entry and move to the next one
							continue;
						}
						
						// Check total size using shared implementation
						if (!ArchiveUtil.checkTotalSizeLimit(ze.getName(), entrySize, totalSizeCounter, 
							getMaxTotalSize(), convertHandlingMode(handlingMode))) {
							break; // Stop extraction 
						}
					}
					
					// Sanitize the entry name
					String sanitizedName = sanitizePath(ze.getName(), handlingMode);
					if (!sanitizedName.equals(ze.getName())) {
						Logger.warn(ZipUtil.class, "Potentially malicious zip entry renamed: " + 
								ze.getName() + " -> " + sanitizedName);
					}
					
					Logger.info(ZipUtil.class, "Unzipping " + sanitizedName);
					File newFile = new File(outputDir + File.separator + sanitizedName);
					
					if (newFile.getCanonicalPath().startsWith(outputDir.getCanonicalPath())) {
						// Create parent directories if needed
						File parentDir = newFile.getParentFile();
						if (!parentDir.exists()) {
							parentDir.mkdirs();
						}
						
						// Extract the file with size tracking
						try (OutputStream os = Files.newOutputStream(newFile.toPath())) {
							// Use a counting output stream to track actual bytes written
							final long[] counter = {0};
							byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
							int bytesRead;
							while ((bytesRead = zin.read(buffer)) != -1) {
								os.write(buffer, 0, bytesRead);
								counter[0] += bytesRead;
								
								// Check for DoS during streaming if entry size is not known
								if (entrySize <= 0 && counter[0] > getMaxFileSize()) {
									os.close();
									newFile.delete();
									String message = String.format(
										"Entry '%s' exceeded maximum allowed file size during extraction", 
										ze.getName());
									
									if (handlingMode == SuspiciousEntryHandling.ABORT) {
										throw new SecurityException(message);
									}
									
									Logger.warn(ZipUtil.class, message);
									break;
								}
							}
							
							// Update the total size counter with actual bytes written
							totalSizeCounter.addAndGet(counter[0]);
						}
					} else {
						// If we get here, we've detected a path traversal attempt
						SecurityLogger.logInfo(ZipUtil.class, String.format(
								"Zip slip attack detected. Entry '%s' attempts to extract outside target directory.",
								ze.getName()));
						
						if (handlingMode == SuspiciousEntryHandling.ABORT) {
							throw new SecurityException("Illegal unzip attempt");
						}
						
						Logger.warn(ZipUtil.class, "Skipping entry with illegal destination: " + ze.getName());
					}
				} catch (SecurityException e) {
					if (handlingMode == SuspiciousEntryHandling.ABORT) {
						throw e;
					}
					Logger.warn(ZipUtil.class, "Skipped suspicious entry: " + ze.getName() + " - " + e.getMessage());
				}
			}
		} catch (IOException e) {
			String errorMsg = String.format("Error while unzipping Data in file '%s': %s",
					null != ze ? ze.getName() : "", e.getMessage());
			Logger.error(ZipUtil.class, errorMsg, e);
			throw e;
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
}
