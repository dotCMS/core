/**
 * 
 */
package com.dotmarketing.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * @author Jason Tesser
 * @since 1.6
 */
public class ZipUtil {

	  private static void zipDirectory(String dir2zip, ZipOutputStream zos, String zipPath)throws IOException, IllegalArgumentException { 
		File zipDir = new File(dir2zip);
		//create a new File object based on the directory we  have to zip get a listing of the directory content 
		String[] dirList = zipDir.list(); 
		byte[] readBuffer = new byte[2156]; 
		int bytesIn = 0; 
		//loop through dirList, and zip the files 
		for(int i=0; i<dirList.length; i++){
		    File f = new File(zipDir, dirList[i]); 
		    if(f.isDirectory()){ 
		    	//if the File object is a directory, call this function again to add its content recursively 
			    String filePath = f.getPath(); 
			    zipDirectory(filePath, zos, zipPath); 
			    continue; 
		    } 
			//if we reached here, the File object f was not a directory create a FileInputStream on top of f 
			FileInputStream fis = new FileInputStream(f); 
			//create a new zip entry 
			String path = f.getPath().substring(zipPath.length()+1);
			ZipEntry anEntry = new ZipEntry(path); 
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
	   * @param ZipOutputStream zos
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
	   public static void extract(ZipFile zipFile, File toDir) throws IOException{
		   if (! toDir.exists()){
		   		toDir.mkdirs();
		   }
		   Enumeration entries = zipFile.entries();
		   while (entries.hasMoreElements()) {
			   ZipEntry zipEntry = (ZipEntry) entries.nextElement();
			   if (zipEntry.isDirectory()) {
				   File dir = new File(toDir, zipEntry.getName());
				   if (! dir.exists()){ // make sure also empty directories get created!
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
	    public static void extract(ZipFile zipFile, ZipEntry zipEntry, File toDir) throws IOException {
	        File file = new File(toDir, zipEntry.getName());
	        File parentDir = file.getParentFile();
	        if (! parentDir.exists()){
	            parentDir.mkdirs();
	        }
	        
	        BufferedInputStream bis = null;
	        BufferedOutputStream bos = null;
	        try{
	            InputStream istr = zipFile.getInputStream(zipEntry);
	            bis = new BufferedInputStream(istr);
	            FileOutputStream fos = new FileOutputStream(file);
	            bos  = new BufferedOutputStream(fos);
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
	}
