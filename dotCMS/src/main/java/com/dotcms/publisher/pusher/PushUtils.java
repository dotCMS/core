package com.dotcms.publisher.pusher;

import java.io.*;
import java.nio.file.Files;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

import com.dotcms.repackage.org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import com.dotcms.repackage.org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;

import com.dotmarketing.util.Logger;

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
	               // Create the output stream for the output file

		// try-with-resources handles close of streams
		try(OutputStream fos = Files.newOutputStream(output.toPath());
			// Wrap the output file stream in streams that will tar and gzip everything
			TarArchiveOutputStream taos = new TarArchiveOutputStream(
				new GZIPOutputStream(new BufferedOutputStream(fos))) ) {

			// TAR originally didn't support long file names, so enable the support for it
			taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

			// Get to putting all the files in the compressed output file
			for (File f : files) {
				addFilesToCompression(taos, f, ".", bundleRoot);
			}
		}

		return output;
	}
	
	

	/**
	 * Does the work of compression and going recursive for nested directories
	 * <p/>
	 *
	 *
	 * @param taos The archive
	 * @param file The file to add to the archive
	        * @param dir The directory that should serve as the parent directory in the archivew
	 * @throws IOException
	 */
	private static void addFilesToCompression(TarArchiveOutputStream taos, File file, String dir, String bundleRoot)
		throws IOException
	{
	    	if(!file.isHidden()) {
	    		// Create an entry for the file
	    		if(!dir.equals("."))
	    			if(File.separator.equals("\\")){
	    				dir = dir.replaceAll("\\\\", "/");
	    			}
	    			taos.putArchiveEntry(new TarArchiveEntry(file, dir + "/" + file.getName()));
				if (file.isFile()) {
			        // Add the file to the archive
					try(BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
						IOUtils.copy(bis, taos);
						taos.closeArchiveEntry();
					}
				} else if (file.isDirectory()) {
					//Logger.info(this.getClass(),file.getPath().substring(bundleRoot.length()));
			         // close the archive entry
					if(!dir.equals("."))
						taos.closeArchiveEntry();
			         // go through all the files in the directory and using recursion, add them to the archive
					for (File childFile : file.listFiles()) {
						addFilesToCompression(taos, childFile, file.getPath().substring(bundleRoot.length()), bundleRoot);
					}
				}
	    	}
	    
	}
	
	
	
}
