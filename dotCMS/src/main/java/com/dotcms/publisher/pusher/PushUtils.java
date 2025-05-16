package com.dotcms.publisher.pusher;

import org.apache.commons.io.IOUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ArchiveUtil;
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
	 * @param bundleRoot The root directory path to check for directory traversal
	 * @throws IOException If an error occurs during the operation
	 * @return The compressed file
	 */
	public static File compressFiles(Collection<File> files, File output, String bundleRoot)
		throws IOException
	{
		Logger.info(PushUtils.class, "Compressing "+files.size() + " to "+output.getAbsoluteFile());
		
		// First verify all files are within the bundleRoot directory for security
		File bundleRootDir = new File(bundleRoot);
		for (File file : files) {
			if (!ArchiveUtil.validateFileWithinDirectory(bundleRootDir, file, 
					ArchiveUtil.SuspiciousEntryHandling.ABORT)) {
				throw new DotRuntimeException(
					"Directory Traversal Warning: You can only tar files that are under the directory:" + 
					bundleRoot + " found " + file.getAbsolutePath());
			}
		}
		
		// Now use the high-level TarUtil method to create the archive
		// This takes care of all the I/O operations internally
		return TarUtil.tarGzFiles(files, output, null);
	}
	

	/**
	 * Tar and GZIPs a directory on the asset path
	 * 
	 * @param directory The directory to compress
	 * @return The compressed tar.gz file
	 * @throws IOException If an error occurs during the operation
	 */
    public static File tarGzipDirectory(final File directory) throws IOException {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            throw new DotRuntimeException("Unable to compress directory:" + directory);
        }
        
        final String tempFileId = directory.getName() + UUIDGenerator.shorty();
        final File tempFile = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + 
                                      File.separator + tempFileId + ".tar.gz");
        
        Logger.info(PushUtils.class, "Compressing directory " + directory.getAbsolutePath() + 
                                    " to " + tempFile.getAbsoluteFile());
        
        // Use TarUtil's high-level method to compress the directory
        // This takes care of all the I/O operations internally
        // We set includeBaseDirName to false to maintain backwards compatibility
        return TarUtil.compressDirectoryToTarGz(directory, tempFile, false);
    }
}
