package com.dotcms.publisher.pusher;

import org.apache.commons.io.IOUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TarUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.util.FileUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public class PushUtils {

	
	
	/**
	 * Compress (tar.gz) the input files to the output file
	 *
	 * @param files The files to compress
	 * @param output The resulting output file (should end in .tar.gz)
	 * @param bundleRoot
	 * @throws IOException
	 */
	public static File compressFiles(Collection<File> files, File output, String bundleRoot)
		throws IOException
	{
		Logger.info(PushUtils.class, "Compressing "+files.size() + " to "+output.getAbsoluteFile());

		// Use TarUtil to create a secured TAR.GZ output stream
		try(TarArchiveOutputStream taos = TarUtil.createTarGzOutputStream(output)) {
			// Get to putting all the files in the compressed output file
			for (File f : files) {
				addFilesToCompression(taos, f, ".", bundleRoot);
			}
		}

		return output;
	}
	

	/**
	 * Tar and GZIPs a directory on the asset path
	 * @param directory
	 * @return
	 * @throws IOException
	 */
    public static File tarGzipDirectory(final File directory) throws IOException {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            throw new DotRuntimeException("Unable to compress directory:" + directory);
        }
        final String tempFileId = directory.getName() + UUIDGenerator.shorty();
        final File tempFile = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() +File.separator + tempFileId + ".tar.gz");
        final List<File> files = FileUtil.listFilesRecursively(directory);

        Logger.info(PushUtils.class, "Compressing " + files.size() + " to " + tempFile.getAbsoluteFile());

        // Use TarUtil to create a secured TAR.GZ output stream
        try (TarArchiveOutputStream tarArchiveOutputStream = TarUtil.createTarGzOutputStream(tempFile)) {
            // Get to putting all the files in the compressed output file
            for (final File file : files) {
                addFilesToCompression(tarArchiveOutputStream, file, ".", directory.getAbsolutePath());
            }
        }

        return tempFile;
    }

	/**
	 * Does the work of compression and going recursive for nested directories
	 * <p/>
	 *
	 *
	 * @param taos The archive
	 * @param file The file to add to the archive
	 * @param dir The directory that should serve as the parent directory in the archive
	 * @param bundleRoot The root directory path to check for directory traversal
	 * @throws IOException
	 */
	private static void addFilesToCompression(TarArchiveOutputStream taos, File file, String dir, String bundleRoot)
		throws IOException
	{
	    if(!file.getAbsolutePath().contains(bundleRoot)) {
	        throw new DotRuntimeException("Directory Traversal Warning: You can only tar files that are under the directory:" + bundleRoot + " found " + file.getAbsolutePath() );
	    }
	    
	    if(!file.isHidden()) {
	        // Create an entry for the file
	        if(!dir.equals(".")) {
	            if(File.separator.equals("\\")) {
	                dir = dir.replaceAll("\\\\", "/");
	            }
	            
	            // Use TarUtil to create a safe entry name
	            String entryName = dir + "/" + file.getName();
	            
	            if (file.isFile()) {
	                // Add the file to the archive using TarUtil for path sanitization
	                TarUtil.addFileToTar(taos, file, entryName);
	            } else if (file.isDirectory()) {
	                // Add directory entry
	                TarArchiveEntry dirEntry = TarUtil.createSafeTarEntry(entryName + "/");
	                taos.putArchiveEntry(dirEntry);
	                taos.closeArchiveEntry();
	                
	                // Recurse into the directory
	                for (File childFile : file.listFiles()) {
	                    addFilesToCompression(taos, childFile, file.getPath().substring(bundleRoot.length()), bundleRoot);
	                }
	            }
	        } else if (file.isDirectory()) {
	            // Root directory - just recurse into it
	            for (File childFile : file.listFiles()) {
	                addFilesToCompression(taos, childFile, file.getPath().substring(bundleRoot.length()), bundleRoot);
	            }
	        } else {
	            // Root file
	            TarUtil.addFileToTar(taos, file, file.getName());
	        }
	    }
	}
}
