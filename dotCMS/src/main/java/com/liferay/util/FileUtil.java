/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util;

import com.dotcms.publisher.pusher.PushUtils;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.apache.commons.io.filefilter.TrueFileFilter;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * <a href="FileUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.13 $
 *
 */
public class FileUtil {
	//http://jira.dotmarketing.net/browse/DOTCMS-2178
	final static long KILO_BYTE = 1024;
	final static long MEGA_BYTE = 1024*1024;
	final static long GIGA_BYTE = 1024*1024*1024;
	final static long TERA_BYTE = 1024*1024*1024*1024;

	public static void copyDirectory(
			String sourceDirName, String destinationDirName, boolean hardLinks) throws IOException {

			copyDirectory(new File(sourceDirName), new File(destinationDirName), hardLinks);
		}

	public static void copyDirectory(
		String sourceDirName, String destinationDirName) throws IOException {

		copyDirectory(new File(sourceDirName), new File(destinationDirName));
	}

	public static void copyDirectory(
			String sourceDirName, String destinationDirName, FileFilter filter) throws IOException {

			copyDirectory(new File(sourceDirName), new File(destinationDirName), true, filter);
		}
	
	
	
	
	public static void copyDirectory(File source, File destination, boolean hardLinks) throws IOException {
	    copyDirectory(source,destination,hardLinks,null);
	}
	
	public static void copyDirectory(File source, File destination, boolean hardLinks, FileFilter filter) throws IOException {
		if (source.exists() && source.isDirectory()) {
			if (!destination.exists()) {
				destination.mkdirs();
			}

			File[] fileArray = filter!=null ? source.listFiles(filter) : source.listFiles();

			for (int i = 0; i < fileArray.length; i++) {
			    if(fileArray[i].getName().endsWith("xml")) {
			        String name=fileArray[i].getName();
			        Logger.info(FileUtil.class, "copy "+name);
			    }
			    
				if (fileArray[i].isDirectory()) {
					copyDirectory(
						fileArray[i],
						new File(destination.getPath() + File.separator
							+ fileArray[i].getName()), hardLinks, filter);
				}
				else {
					copyFile(
						fileArray[i],
						new File(destination.getPath() + File.separator
							+ fileArray[i].getName()), hardLinks);
				}
			}
		}
	}

	public static void copyDirectory(File source, File destination) throws IOException {
		copyDirectory(source, destination, Config.getBooleanProperty("CONTENT_VERSION_HARD_LINK", true));
	}
	

	public static void copyFile(
		String sourceFileName, String destinationFileName) throws IOException {

		copyFile(new File(sourceFileName), new File(destinationFileName));
	}

	public static void copyFile(File source, File destination) throws IOException {
		copyFile(source, destination, Config.getBooleanProperty("CONTENT_VERSION_HARD_LINK", true));
	}
	
	private static void validateEmptyFile(File source) throws IOException{
		final String metaDataPath = "metaData" + File.separator + "content";
        final String languagePropertyPath = "messages" + File.separator + "cms_language";
        
        if (source.length() == 0) {
            Logger.warn(FileUtil.class, source.getAbsolutePath() + " is empty");
            if (!Config.getBooleanProperty("CONTENT_ALLOW_ZERO_LENGTH_FILES", true) && !(source.getAbsolutePath()
                .endsWith(metaDataPath) || source.getAbsolutePath().contains(languagePropertyPath))) {
                throw new IOException("Source file is 0 length, failing " + source);
            }
        }
	}

    public static void copyFile(File source, File destination, boolean hardLinks) throws IOException {
        copyFile(source, destination, hardLinks, true);
    }

	public static void copyFile(File source, File destination, boolean hardLinks, boolean validateEmptyFile) throws IOException {
		
        
        if (!source.exists()) {
            throw new IOException("Source file does not exist" + source);
        }

        if(source.getAbsolutePath().equalsIgnoreCase(destination.getAbsolutePath())) {
        	return;
		}
        
        if (validateEmptyFile){
            validateEmptyFile(source);
        }

        if (hardLinks && !Config.getBooleanProperty("CONTENT_VERSION_HARD_LINK", true)) {
            hardLinks = false;
        }

        if ((destination.getParentFile() != null) &&
            (!destination.getParentFile().exists())) {

            destination.getParentFile().mkdirs();
        }

        if (hardLinks) {

            // I think we need to be sure to unlink first
            if (destination.exists()) {
                Path destinationPath = Paths.get(destination.getAbsolutePath());
                //"If the file is a symbolic link then the symbolic link itself, not the final target of the link, is deleted."
                Files.delete(destinationPath);
            }

            Path newLink = Paths.get(destination.getAbsolutePath());
            Path existingFile = Paths.get(source.getAbsolutePath());

            try {

                Files.createLink(newLink, existingFile);
                // setting this means we will try again if we cannot hard link
                if (!destination.exists() ||
                        (validateEmptyFile && destination.length() == 0)) {
                    hardLinks = false;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Can't create hardLink. source: " + source.getAbsolutePath());
                    sb.append(", destination: " + destination.getAbsolutePath());
                    Logger.warn(FileUtil.class, sb.toString());
                }
            }  catch (FileAlreadyExistsException e1) {
                StringBuilder sb = new StringBuilder();
                sb.append("Source File: " + source.getAbsolutePath());
                sb.append("already exists on the destination: " + destination.getAbsolutePath());
                Logger.debug(FileUtil.class, sb.toString());
            } catch (IOException e2 ){
                hardLinks = false; // setting to false will execute the fallback
                StringBuilder sb = new StringBuilder();
                sb.append("Could not created the hard link, will try copy for source: " + source);
                sb.append(", destination: " + destination + ". Error message: " + e2.getMessage());
                Logger.debug(FileUtil.class, sb.toString());
            } 
        }

        if (!hardLinks) {
            try (final ReadableByteChannel inputChannel = Channels.newChannel(Files.newInputStream(source.toPath()));
                    final WritableByteChannel outputChannel = Channels.newChannel(Files.newOutputStream(destination.toPath()))){
                FileUtil.fastCopyUsingNio(inputChannel, outputChannel);
            }
        }

    }

	public static void copyFileLazy(String source, String destination)
		throws IOException {

		String oldContent = null;
		try {
			oldContent = FileUtil.read(source);
		}
		catch (FileNotFoundException fnfe) {
			return;
		}

		String newContent = null;
		try {
			newContent = FileUtil.read(destination);
		}
		catch (FileNotFoundException fnfe) {
		}

		if (oldContent == null || !oldContent.equals(newContent)) {
			FileUtil.copyFile(source, destination);
		}
	}

	public static void deltree(String directory) {
		deltree(new File(directory));
	}

	/**
   * This method takes a directory and will delete all files older than
   * the passed in Date.  It will only delete directories older than the
   * passed in Date if they are empty. It will not delete the directory passed in
	 * @param directory
	 * @param olderThan
	 */
  public static void cleanTree(File directory, Date olderThan) {
    cleanTree(directory, olderThan.getTime()); 
  }
  
  /**
   * This method takes a directory and will delete all files older than
   * the passed in time in millis.  It will only delete directories older than the
   * passed in time if they are empty.  It will not delete the directory passed in
   * @param directory
   * @param deleteOlderTime
   */
	 public static void cleanTree(final File directory, final long deleteOlderTime) {

	    if (directory==null || !directory.exists() ) {
	      Logger.info(FileUtil.class, "cleanTree Directory " + directory + " not found exiting");
	      return;
	    }
	    if(!directory.isDirectory()) {
	      directory.delete();
	      return;
	    }

	    final List<File> allOldFiles = com.liferay.util.FileUtil.listFilesRecursively(directory, new FileFilter() {
	      @Override
	      public boolean accept(File pathname) {
	        return pathname.lastModified() < deleteOlderTime;
	      }
	    });

	    // delete old files
      allOldFiles.stream().filter(f -> f.isFile()).forEach(f->f.delete());
      
      //delete old directories (only empy directories will be deleted)
	    allOldFiles.stream().filter(f -> f.exists() && f.isDirectory()).sorted(new Comparator<File>() {
        @Override
        public int compare(final File a, final File b) {
          return a.getAbsolutePath().length() - b.getAbsolutePath().length();
        }
	    }).forEach(f->f.delete());


	  }
	
	
	
	
	
	public static void deltree(File directory, boolean deleteTopDir) {
		if (directory.exists() && directory.isDirectory()) {
			File[] fileArray = directory.listFiles();

			for (int i = 0; i < fileArray.length; i++) {
				if (fileArray[i].isDirectory()) {
					deltree(fileArray[i]);
				}
				else {
					fileArray[i].delete();
				}
			}
			if(deleteTopDir)
				directory.delete();
		}else{
			if(directory.exists()){
				directory.delete();
			}
		}
	}

	public static void deltree(File directory) {
		deltree(directory, true);
	}

	public static byte[] getBytes(File file) throws IOException {
		if (file == null || !file.exists()) {
			return null;
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		final InputStream in = Files.newInputStream(file.toPath());

		byte buffer[] = new byte[2048];

		int c;

		while (( c = in.read(buffer) ) != -1) {
			out.write(buffer, 0, c);
		}

		in.close();
		out.close();

		return out.toByteArray();
	}

	public static boolean isWindows(){
		return File.separatorChar == '\\';
		
	}
	
	/*
	 * This will return a path whether the file exists or not
	 * (Websphere returns a null if the file does not exist, which throws a lot of NPEs)
	 * NOTE: relativePath has to start with "/"
	 */
	public static String getRealPath(String relativePath){
		ServletContext context = Config.CONTEXT;
		if(context ==null){
			Logger.fatal(FileUtil.class, "Config.CONTEXT not initialized with a servlet context, dying");
			throw new DotStateException("Config.CONTEXT not initialized with a servlet context, dying");
		}

		//Fallback for wrong use of the File.separator using the context.getRealPath
		if (relativePath.contains("\\")) {
			relativePath = replaceSeparator(relativePath);
		}

        //Relative path has to start with "/"
        if(!relativePath.startsWith("/")){
            relativePath = File.separatorChar + relativePath;
        }

		String ret = Config.CONTEXT.getRealPath(relativePath);	
		if(ret !=null) {
            return ret;
        }

		//String base = Config.CONTEXT.getRealPath("/");
		String base = Config.CONTEXT_PATH;
		base = (base.lastIndexOf(File.separatorChar) == base.length()-1) ? base.substring(0, base.lastIndexOf(File.separatorChar)) : base;
		relativePath = relativePath.replace('/', File.separatorChar);
		
		return base + relativePath;
	}
	
	
	
	public static String getPath(String fullFileName) {
		int pos = fullFileName.lastIndexOf("/");

		if (pos == -1) {
			pos = fullFileName.lastIndexOf("\\");
		}

		String shortFileName = fullFileName.substring(0, pos);

		if (Validator.isNull(shortFileName)) {
			return "/";
		}

		return shortFileName;
	}

	public static String getShortFileName(String fullFileName) {
		int pos = fullFileName.lastIndexOf("/");

		if (pos == -1) {
			pos = fullFileName.lastIndexOf("\\");
		}

		String shortFileName =
			fullFileName.substring(pos + 1, fullFileName.length());

		return shortFileName;
	}

	public static boolean exists(String fileName) {
		File file = new File(fileName);

		return file.exists();
	}

	public static String[] listDirs(String fileName) throws IOException {
		return listDirs(new File(fileName));
	}

	public static String[] listDirs(File file) throws IOException {
		List dirs = new ArrayList();

		File[] fileArray = file.listFiles();

		for (int i = 0; i < fileArray.length; i++) {
			if (fileArray[i].isDirectory()) {
				dirs.add(fileArray[i].getName());
			}
		}

		return (String[])dirs.toArray(new String[0]);
	}

	public static String[] listFiles(String fileName) throws IOException {
		return listFiles(new File(fileName));
	}

	public static String[] listFiles(String fileName, Boolean includeSubDirs) throws IOException {
		return listFiles(new File(fileName), includeSubDirs);
	}

	public static boolean containsParentFolder(File file, File[] folders) {
		for (File folder : Arrays.asList(folders)) {

			if(file.getParent().equalsIgnoreCase(folder.getPath())) {
				return true;
			}
		}

		return false;
	}

	public static String[] listFiles(File dir) throws IOException {
		return listFiles(dir, false);
	}

	public static File[] listFileHandles(String fileName, Boolean includeSubDirs) throws IOException {
		return listFileHandles(new File(fileName), includeSubDirs);
	}
	
	public static File[] listFileHandles(File dir, Boolean includeSubDirs) throws IOException {
		
	    if(!dir.exists() || ! dir.isDirectory()){
	    	return new File[0];
	    }
		
		
		FileFilter fileFilter = new FileFilter() {
	        public boolean accept(File file) {
	            return file.isDirectory();
	        }
	    };

		File[] subFolders = dir.listFiles(fileFilter);
	
		List<File> files = new ArrayList<File>();
	
		List<File> fileArray = new ArrayList<File>(FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, includeSubDirs ? TrueFileFilter.INSTANCE : null));
	
		for (File file : fileArray) {
			if(file.isFile()) {
				if(includeSubDirs && containsParentFolder(file, subFolders)) {
					files.add(file);
				} else {
					files.add(file);
				}
			}
		}
	
		return (File[])files.toArray(new File[0]);
	}
	
	public static String[] listFiles(File dir, Boolean includeSubDirs) throws IOException {
		 FileFilter fileFilter = new FileFilter() {
		        public boolean accept(File file) {
		            return file.isDirectory();
		        }
		    };
		File[] subFolders = dir.listFiles(fileFilter);

		List<String> files = new ArrayList<String>();

		List<File> fileArray = new ArrayList<File>(FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, includeSubDirs ? TrueFileFilter.INSTANCE : null));

		for (File file : fileArray) {
			if(file.isFile()) {
				if(includeSubDirs && containsParentFolder(file, subFolders)) {
					files.add(file.getParentFile().getName() + File.separator + file.getName());
				} else {
					files.add(file.getName());
				}
			}
		}

		return (String[])files.toArray(new String[0]);
	}

	public static void mkdirs(String pathName) {
		File file = new File(pathName);
		file.mkdirs();
	}

	public static boolean move(
		String sourceFileName, String destinationFileName) throws IOException {

		return move(new File(sourceFileName), new File(destinationFileName));
	}

	public static boolean move(File source, File destination) throws IOException {
		return move(source, destination, true);
	}
	/**
	 * This method was created as way to avoid the error when you upload a file via Finder GIT-#9334
	 * 
	 * @param source
	 * @param destination
	 * @param validateEmptyFile if is false it won't check if the file is empty
	 * @return
	 * @throws IOException
	 */
	public static boolean move(File source, File destination, boolean validateEmptyFile) throws IOException {
		if (!source.exists()) {
			return false;
		}
		
		if(validateEmptyFile){
			validateEmptyFile(source);
		}
		//If both files exists and are equals no need to move it.
        //Confirms that destination exists.
        if(destination.exists()) {
            try (//Creates InputStream for both files.
                    InputStream inputSource = Files.newInputStream(source.toPath());
                    InputStream inputDestination = Files.newInputStream(destination.toPath())){

                //Both files checked.
                if(DigestUtils.md5Hex(inputSource).equals(DigestUtils.md5Hex(inputDestination))){

                    //If both files are the same but the destination is a soft link do nothing...., we don't want to remove the original file
                    if ( source.toPath().toRealPath().equals(destination.toPath().toRealPath())
                            || Files.isSymbolicLink(destination.toPath()) ) {
                        return true;
                    }

                    return source.delete();
                }
            } catch (Exception e) {
                //In case of error, no worries. Continued with the same logic of move.
                Logger.debug(FileUtil.class, "MD5 Checksum failed, continue with standard move");
            }
        }

		destination.delete();

		boolean success = source.renameTo(destination);
		
		// if the rename fails, copy

		if (!success) {
			copyFile(source, destination);
			success = source.delete();
		}
		return success;
	}

	public static String read(String fileName) throws IOException {
		return read(new File(fileName));
	}

	public static String read(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));

		StringBuffer sb = new StringBuffer();
		String line = null;

		while ((line = br.readLine()) != null) {
			sb.append(line).append('\n');
		}

		br.close();

		return sb.toString().trim();
	}

	public static File[] sortFiles(File[] files) {
		Arrays.sort(files, new FileComparator());

		List directoryList = new ArrayList();
		List fileList = new ArrayList();

		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				directoryList.add(files[i]);
			}
			else {
				fileList.add(files[i]);
			}
		}

		directoryList.addAll(fileList);

		return (File[])directoryList.toArray(new File[0]);
	}

	public static String replaceSeparator(String fileName) {
		return StringUtil.replace(fileName, '\\', "/");
	}

	public static List toList(Reader reader) {
		List list = new ArrayList();

		try {
			BufferedReader br = new BufferedReader(reader);

			StringBuffer sb = new StringBuffer();
			String line = null;

			while ((line = br.readLine()) != null) {
				list.add(line);
			}

			br.close();
		}
		catch (IOException ioe) {
		}

		return list;
	}

	public static List toList(String fileName) {
		try {
			return toList(new FileReader(fileName));
		}
		catch (IOException ioe) {
			return new ArrayList();
		}
	}

	public static Properties toProperties(InputStream is) {
		Properties props = new Properties();

		try {
			props.load(is);
		}
		catch (IOException ioe) {
		}

		return props;
	}

	public static Properties toProperties(String fileName) {
		try (final InputStream is = Files.newInputStream(Paths.get(fileName))){
            return toProperties(is);
		}
		catch (IOException ioe) {
			return new Properties();
		}
	}

	public static void write(File file, String s) throws IOException {
		if (file.getParent() != null) {
			mkdirs(file.getParent());
		}

		BufferedWriter bw = new BufferedWriter(new FileWriter(file));

		bw.flush();
		bw.write(s);

		bw.close();
	}

	public static void write(String fileName, String s) throws IOException {
		write(new File(fileName), s);
	}

	public static void write(String pathName, String fileName, String s)
		throws IOException {

		write(new File(pathName, fileName), s);
	}
     //	http://jira.dotmarketing.net/browse/DOTCMS-2178
	public static String getsize(File fileName){
		String finalVal;
		long filesize = fileName.length();
		BigDecimal size = new BigDecimal(filesize);
		BigDecimal byteVal = null;
		BigDecimal changedByteVal = null;
		finalVal = "";
		if(filesize <= 0){
			finalVal = "";
		}else if(filesize< MEGA_BYTE){
			byteVal = new BigDecimal(KILO_BYTE);
			if(size!=null){
				changedByteVal = size.divide(byteVal,MathContext.UNLIMITED);
				finalVal = Long.toString(Math.round(Math.ceil(changedByteVal.doubleValue())))+" KB";
			}
		}else if(filesize< GIGA_BYTE ){
			byteVal = new BigDecimal(MEGA_BYTE);
			if(size!=null){
				changedByteVal = size.divide(byteVal,MathContext.UNLIMITED);
				finalVal = Long.toString(Math.round(Math.ceil(changedByteVal.doubleValue())))+" MB";
			}
		}else if(filesize< TERA_BYTE){
			byteVal = new BigDecimal(GIGA_BYTE);
			if(size!=null){
				changedByteVal = size.divide(byteVal,MathContext.UNLIMITED);
				finalVal = Long.toString(Math.round(Math.ceil(changedByteVal.doubleValue())))+" GB";
			}
		} else{
			byteVal = new BigDecimal(TERA_BYTE);
			if(size!=null){
				changedByteVal = size.divide(byteVal,MathContext.UNLIMITED);
				finalVal = Long.toString(Math.round(Math.ceil(changedByteVal.doubleValue())))+" TB";
			}
		}
		return finalVal;
	}
	
    /**
     * Method to do a fast copy from a readable byte channel to a writable byte
     * channel using java NIO.
     * 
     * @param src
     *            Reading from
     * @param dest
     *            Writing to
     * @throws IOException
     */
    public static void fastCopyUsingNio(final ReadableByteChannel src, final WritableByteChannel dest)
            throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(5 * 1024);

        while (src.read(buffer) != -1) {
            buffer.flip();
            dest.write(buffer);
            buffer.compact();
        }

        buffer.flip();

        while (buffer.hasRemaining()) {
            dest.write(buffer);
        }
    }

	/**
	  * Recursively walk a directory tree and return a List of all
	  * Files found; the List is sorted using File.compareTo().
	  *
	  * @param aStartingDir is a valid directory, which can be read.
	  */
	  static public List<File> listFilesRecursively(File aStartingDir)  {
		    return listFilesRecursively(aStartingDir, null);
	  }



	/**
	  * Recursively walk a directory tree and return a List of all
	  * Files found; the List is sorted using File.compareTo().
	  *
	  * @param aStartingDir is a valid directory, which can be read.
	  */
	  static public List<File> listFilesRecursively(File aStartingDir, FileFilter filter)  {
		    validateDirectory(aStartingDir);
		    List<File> result = getFileListingNoSort(aStartingDir, filter);
		    Collections.sort(result);
		    return result;
	  }


	  // PRIVATE //
	  static private List<File> getFileListingNoSort(File aStartingDir, FileFilter filter) {
	    List<File> result = new ArrayList<File>();

	    File[] filesAndDirs = null;
	    if(filter !=null){
	    	filesAndDirs = aStartingDir.listFiles(filter);
	    }
	    else{
	    	filesAndDirs = aStartingDir.listFiles();
	    }
	    List<File> filesDirs = Arrays.asList(filesAndDirs);
	    for(File file : filesDirs) {
	      result.add(file); //always add, even if directory
	      if ( ! file.isFile() ) {
	        //must be a directory
	        //recursive call!
	        List<File> deeperList = getFileListingNoSort(file, filter);
	        result.addAll(deeperList);
	      }
	    }
	    return result;
	  }

	  /**
	   * Directory is valid if it exists, does not represent a file, and can be read.
	   */
	   static private void validateDirectory (File aDirectory)  {
	     if (aDirectory == null) {
	       throw new DotStateException("Directory should not be null.");
	     }
	     if (!aDirectory.exists()) {
	       throw new DotStateException("Directory does not exist: " + aDirectory);
	     }
	     if (!aDirectory.isDirectory()) {
	       throw new DotStateException("Is not a directory: " + aDirectory);
	     }
	     if (!aDirectory.canRead()) {
	       throw new DotStateException("Directory cannot be read: " + aDirectory);
	     }
	   }

	/**
	 * @return File (directory) created in file system from the parameter path.
	 */
	static public File mkDirsIfNeeded(final String path) {
		File folder = new File(path);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		return folder;
	} //mkDirsIfNeeded.

	/**
	 * Creates an input stream without compression
	 * @param path {@link File}
	 * @return InputStream
	 * @throws IOException
	 */
	public static InputStream createInputStream(final File path) throws IOException {

		return createInputStream(path.toPath(), StreamCompressorType.NONE);
	}

	/**
	 * Creates an input stream without compression
	 * @param path {@link Path}
	 * @return InputStream
	 * @throws IOException
	 */
	public static InputStream createInputStream(final Path path) throws IOException {

		return createInputStream(path, StreamCompressorType.NONE);
	}

	/**
	 * Create an input stream and a compression based on the type argument such as "gzip", "bzip2"
	 * @param path {@link File}
	 * @param type {@link String}
	 * @return InputStream
	 * @throws IOException
	 */
	public static InputStream createInputStream(final File path, final String type) throws IOException {

		return createInputStream(path.toPath(), type);
	}

	/**
	 * Create an input stream and a compression based on the type argument such as "gzip", "bzip2"
	 * @param path {@link Path}
	 * @param type {@link String}
	 * @return InputStream
	 * @throws IOException
	 */
	public static InputStream createInputStream(final Path path, final String type) throws IOException {

		final StreamCompressorType streamCompressorType = "gzip".equalsIgnoreCase(type)?
				StreamCompressorType.GZIP:
				"bzip2".equalsIgnoreCase(type)?
						StreamCompressorType.BZIP2:StreamCompressorType.NONE;

		return createInputStream(path, streamCompressorType);

	}

	/**
	 * Create an input stream and a compression based on the type argument such as StreamCompressorType.GZIP, StreamCompressorType.BZIP2
	 * @param path {@link Path}
	 * @param type {@link StreamCompressorType}
	 * @return InputStream
	 * @throws IOException
	 */
	public static InputStream createInputStream(final Path path, final StreamCompressorType type) throws IOException {

		return wrapCompressedInputStream(Files.newInputStream(path), type);
	}

	/**
	 * Wraps the original input stream into a compress indicated on type {@link StreamCompressorType}
	 * Does not wrap anything if the type is not supported
	 * @param stream {@link InputStream}
	 * @param type   {@link StreamCompressorType}
	 * @return InputStream
	 * @throws IOException
	 */
	public static InputStream wrapCompressedInputStream (final InputStream stream, final StreamCompressorType type) throws IOException {

		switch (type) {
			case GZIP:
				return new GZIPInputStream(stream);
			case BZIP2:
				return new BZip2CompressorInputStream(stream);
			default:
				return stream;
		}
	}

	/**
	 * Creates an output stream without compression
	 * @param path {@link File}
	 * @return InputStream
	 * @throws IOException
	 */
	public static OutputStream createOutputStream(final File path) throws IOException {

		return createOutputStream(path.toPath(), StreamCompressorType.NONE);
	}

	/**
	 * Creates an output stream without compression
	 * @param path {@link Path}
	 * @return InputStream
	 * @throws IOException
	 */
	public static OutputStream createOutputStream(final Path path) throws IOException {

		return createOutputStream(path, StreamCompressorType.NONE);
	}

	/**
	 * Create an output stream and a compression based on the type argument such as "gzip", "bzip2"
	 * @param path {@link File}
	 * @param type {@link String}
	 * @return InputStream
	 * @throws IOException
	 */
	public static OutputStream createOutputStream(final File path, final String type) throws IOException {

		return createOutputStream(path.toPath(), type);
	}

	/**
	 * Create an output stream and a compression based on the type argument such as "gzip", "bzip2"
	 * @param path {@link Path}
	 * @param type {@link String}
	 * @return InputStream
	 * @throws IOException
	 */
	public static OutputStream createOutputStream(final Path path, final String type) throws IOException {

		final StreamCompressorType streamCompressorType = "gzip".equalsIgnoreCase(type)?
				StreamCompressorType.GZIP:
				"bzip2".equalsIgnoreCase(type)?
						StreamCompressorType.BZIP2:StreamCompressorType.NONE;

		return createOutputStream(path, streamCompressorType);

	}

	public static OutputStream createOutputStream(final Path path, final StreamCompressorType type) throws IOException {

		switch (type) {
			case GZIP:
				return new GZIPOutputStream(Files.newOutputStream(path));
			case BZIP2:
				return new BZip2CompressorOutputStream(Files.newOutputStream(path));
			default:
				return Files.newOutputStream(path);
		}
	}

	public static enum StreamCompressorType {

		GZIP, BZIP2, NONE;
	}
	
	/**
	 * Convienience Method to access .tar.gz functionality
	 * @param directory
	 * @return
	 * @throws IOException
	 */
    public static File tarGzipDirectory(final File directory) throws IOException {
	    return PushUtils.tarGzipDirectory(directory);
	    
	}
	
	
	

}
