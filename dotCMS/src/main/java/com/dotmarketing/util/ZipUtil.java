/**
 * 
 */
package com.dotmarketing.util;

import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @author Jason Tesser
 * @since 1.6
 */
public class ZipUtil {

	  private static void zipDirectory(String dir2zip, ZipOutputStream zos, String zipPath)throws IOException, IllegalArgumentException { 
		final File zipDir = new File(dir2zip);
		//create a new File object based on the directory we  have to zip get a listing of the directory content 
		final String[] dirList = zipDir.list();
		final byte[] readBuffer = new byte[2156];
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
			final ZipEntry anEntry = new ZipEntry(path);
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
	   * @param dir2zip
	   * @param zos
	   * @throws IOException
	   * @throws IllegalArgumentException
	   * @throws IOException
	   * @throws IllegalArgumentException
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
		   if (!toDir.exists()){
		   		toDir.mkdirs();
		   }
		   final Enumeration entries = zipFile.entries();
		   while (entries.hasMoreElements()) {
			   final ZipEntry zipEntry = (ZipEntry) entries.nextElement();
			   if (zipEntry.isDirectory()) {
				   final File dir = new File(toDir, zipEntry.getName());
				   if (checkSecurity(toDir,dir) && !dir.exists()){ // make sure also empty directories get created!
					   dir.mkdirs();
				   }
			   } else {
				   extract(zipFile, zipEntry, toDir);
			   }
	   		}
	   }	
	   
	    /**
	     * Extracts an entry of a zip file to a specified directory.
	     * @param zipFile the zip file to extract from
	     * @param zipEntry the entry of the zip file to extract
	     * @param toDir the target directory
	     * @throws java.io.IOException
	     */
	    private static void extract(final ZipFile zipFile, final ZipEntry zipEntry, final File toDir) throws IOException {
	        final File file = new File(toDir, zipEntry.getName());

            checkSecurity(toDir,file);

	        final File parentDir = file.getParentFile();
	        if (! parentDir.exists()){
	            parentDir.mkdirs();
	        }
	        
	        BufferedInputStream bis = null;
	        BufferedOutputStream bos = null;
	        try{
	            final InputStream istr = zipFile.getInputStream(zipEntry);
	            bis = new BufferedInputStream(istr);
	            final OutputStream os = Files.newOutputStream(file.toPath());
	            bos  = new BufferedOutputStream(os);
	            IOUtils.copy(bis, bos);
	        } finally {
	            if (bis !=  null){
	                bis.close();
	            }
	            if (bos != null){
	                bos.close();
	            }
	        }
	    }


	/**
	 * Check paths to determine if we're being attacked.
	 * @param parentDir
	 * @param newFile
	 * @return
	 * @throws IOException
	 */
	static boolean isNewFileDestinationSafe(final File parentDir, final File newFile)
			throws IOException {
		final String dirCanonicalPath = parentDir.getCanonicalPath();
		final String newFileCanonicalPath = newFile.getCanonicalPath();
		return newFileCanonicalPath.startsWith(dirCanonicalPath);
	}

	/**
	 * call to revise file destination is safe.
	 * If it isn't an exception is thrown
	 * @param parentDir
	 * @param newFile
	 * @return
	 * @throws IOException
	 */
	static boolean checkSecurity(final File parentDir, final File newFile) throws IOException {
		if (!isNewFileDestinationSafe(parentDir, newFile)) {
			//Log detailed info into the security logger
			parentDir.delete();
			SecurityLogger.logInfo(ZipUtil.class, String.format(
					"An attempt to unzip entry '%s' under an illegal destination has been made.",
					newFile.getCanonicalPath()));
			// and expose the minimum to the user
			throw new SecurityException("Illegal unzip attempt");
		}
		return true;
	}

}
