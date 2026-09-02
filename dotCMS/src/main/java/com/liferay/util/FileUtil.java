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
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.util.Collection;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import javax.servlet.ServletContext;
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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 * <a href="FileUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.13 $
 *
 */
@SuppressWarnings("javasecurity:S2083")
public class FileUtil {

	public static final long KILO_BYTE = 1024L;
	public static final long MEGA_BYTE = 1024L *1024;
	public static final long GIGA_BYTE = 1024L *1024*1024;
	public static final long TERA_BYTE = 1024L *1024*1024*1024;
	private static final String CONTENT_VERSION_HARD_LINK = "CONTENT_VERSION_HARD_LINK";
	private static final String CONTENT_ALLOW_ZERO_LENGTH_FILES = "CONTENT_ALLOW_ZERO_LENGTH_FILES";

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

	public static void copyDirectory(File source, File destination, boolean hardLinks,
			FileFilter filter) throws IOException {
		if (source.exists() && source.isDirectory()) {
			if (!destination.exists()) {
				final boolean mkdirs = destination.mkdirs();
				if (!mkdirs) {
					Logger.error(FileUtil.class,
							String.format(" Failed to make destination Dir [%s]", destination));
				}
			}

			File[] fileArray = filter != null ? source.listFiles(filter) : source.listFiles();
			if (null == fileArray) {
				Logger.warn(FileUtil.class,String.format(" No files were returned from [%s] applying filter [%s]. ",source, filter));
				return;
			}
			internalCopy(destination, hardLinks, filter, fileArray);
		}
	}

	/**
	 * List of files copy
	 * @param destination
	 * @param hardLinks
	 * @param filter
	 * @param fileArray
	 * @throws IOException
	 */
	private static void internalCopy(File destination, boolean hardLinks, FileFilter filter,
			File[] fileArray) throws IOException {
		for (File file : fileArray) {
			if (file.getName().endsWith("xml")) {
				Logger.info(FileUtil.class, "copy " + file.getName());
			}
			if (file.isDirectory()) {
				copyDirectory(
						file,
						new File(destination.getPath() + File.separator
								+ file.getName()), hardLinks, filter);
			} else {
				copyFile(
						file,
						new File(destination.getPath() + File.separator
								+ file.getName()), hardLinks);
			}
		}
	}

	public static void copyDirectory(File source, File destination) throws IOException {
		copyDirectory(source, destination, Config.getBooleanProperty(CONTENT_VERSION_HARD_LINK, true));
	}


	public static void copyFile(
			String sourceFileName, String destinationFileName) throws IOException {

		copyFile(new File(sourceFileName), new File(destinationFileName));
	}

	public static void copyFile(File source, File destination) throws IOException {
		copyFile(source, destination, Config.getBooleanProperty(CONTENT_VERSION_HARD_LINK, true));
	}

	public static void validateEmptyFile(File source) throws IOException{
		final String metaDataPath = "metaData" + File.separator + "content";
		final String languagePropertyPath = "messages" + File.separator + "cms_language";

		if (source.length() == 0) {
			Logger.warn(FileUtil.class, source.getAbsolutePath() + " is empty");
			if (!Config.getBooleanProperty(CONTENT_ALLOW_ZERO_LENGTH_FILES, true) && !(source.getAbsolutePath()
					.endsWith(metaDataPath) || source.getAbsolutePath().contains(languagePropertyPath))) {
				throw new IOException("Source file is 0 length, failing " + source);
			}
		}
	}

	public static void copyFile(File source, File destination, boolean hardLinks) throws IOException {
		copyFile(source, destination, hardLinks, true);
	}

	public static void copyFile(File source, File destination, boolean hardLinks, boolean validateEmptyFile) throws IOException {


		if(source==null || destination==null){
			// Prevent NPE, throw expected error
			throw new IOException("Source or Destination file is null ");
		}

		if (!source.exists()) {
			throw new IOException("Source file does not exist " + source);
		}

		if(source.getAbsolutePath().equalsIgnoreCase(destination.getAbsolutePath())) {
			return;
		}

		if (validateEmptyFile){
			validateEmptyFile(source);
		}

		if (hardLinks && !Config.getBooleanProperty(CONTENT_VERSION_HARD_LINK, true)) {
			hardLinks = false;
		}

		if ((destination.getParentFile() != null) &&
				(!destination.getParentFile().exists())) {

			final boolean mkdirs = destination.getParentFile().mkdirs();
			if(!mkdirs){
				Logger.error(FileUtil.class,String.format(" Failed to make destination parent Dir [%s]", destination));
			}
		}

		if (hardLinks) {
			hardLinks = handleHardLinks(source, destination, validateEmptyFile);
		}

		if (!hardLinks) {
			copyFile(source, Files.newOutputStream(destination.toPath()));
		}

	}

	/**
	 * Creates Hardlinks when possible
	 * @param source
	 * @param destination
	 * @param validateEmptyFile
	 * @return
	 * @throws IOException
	 */
	private static boolean handleHardLinks(File source, File destination, boolean validateEmptyFile) throws IOException {
		boolean hardLinks = true;
		// I think we need to be sure to unlink first
		if (destination.exists()) {
			Path destinationPath = Paths.get(destination.getAbsolutePath());
			//"If the file is a symbolic link then the symbolic link itself, not the final target of the link, is deleted."
			try (Stream<Path> fileStream = Files.walk(destinationPath)) {
				fileStream.sorted(Comparator.reverseOrder())
						.map(Path::toFile)
						.forEach(File::delete);
			}
		}

		Path newLink = Paths.get(destination.getAbsolutePath());
		Path existingFile = Paths.get(source.getAbsolutePath());

		try {
			Files.createLink(newLink, existingFile);
			// setting this means we will try again if we cannot hard link
			if (!destination.exists() || (validateEmptyFile && destination.length() == 0)) {
				hardLinks = false;
				Logger.warn(FileUtil.class, String.format("Can't create hardLink. source: [%s] , destination: [%s].", source.getAbsolutePath(), destination.getAbsolutePath() ));
			}
		}  catch (FileAlreadyExistsException e1) {
			Logger.debug(FileUtil.class, String.format("Source File: [%s] already exists on the destination: [%s]",
					source.getAbsolutePath(), destination.getAbsolutePath()));
		} catch (IOException e2 ){
			hardLinks = false; // setting to false will execute the fallback
			Logger.debug(FileUtil.class, String.format("Could not created the hard link, will try copy for source: [%s], destination:[%s]. Error message: [%s]",
					source.getAbsolutePath(), destination.getAbsolutePath(),e2.getMessage()));
		}
		return hardLinks;
	}

	public static void copyFile(final File source, final OutputStream destination) throws IOException {
		try (final ReadableByteChannel inputChannel = Channels.newChannel(Files.newInputStream(source.toPath()));
				final WritableByteChannel outputChannel = Channels.newChannel(destination)){
			FileUtil.fastCopyUsingNio(inputChannel, outputChannel);
		} catch(IOException e) {
			Logger.error(FileUtil.class, e);
			throw e;
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
		catch (FileNotFoundException ignored) {
			//Ignored
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
			internalDelete(directory);
			return;
		}

		final List<File> allOldFiles = com.liferay.util.FileUtil.listFilesRecursively(directory,
				pathname -> pathname.lastModified() < deleteOlderTime);

		// delete old files
		allOldFiles.stream().filter(File::isFile).forEach(FileUtil::internalDelete);

		//delete old directories (only empty directories will be deleted)
		allOldFiles.stream().filter(f -> f.exists() && f.isDirectory()).sorted(
				(a, b) -> a.getAbsolutePath().length() - b.getAbsolutePath().length()).forEach(
				FileUtil::internalDelete);


	}

	/**
	 * NIO File Delete with proper logging
	 * @param f
	 * @return
	 */
	private static boolean internalDelete(File f) {
		try {
			Files.deleteIfExists(f.toPath());
			return true;
		} catch (IOException e) {
			Logger.debug(FileUtil.class, String.format("Fail to delete file/dir [%s]", f), e);
		}
		return false;
	}


	public static void deltree(File directory, boolean deleteTopDir) {
		if (directory.exists() && directory.isDirectory()) {
			File[] fileArray = directory.listFiles();

			if (null != fileArray) {
				for (File file : fileArray) {
					if (file.isDirectory()) {
						deltree(file);
					} else {
						internalDelete(file);
					}
				}
			}
			if (deleteTopDir) {
				internalDelete(directory);
			}
		}else{
			if(directory.exists()){
				internalDelete(directory);
			}
		}
	}

	public static void deltree(File directory) {
		deltree(directory, true);
	}

	public static byte[] getBytes(File file) throws IOException {
		if (file == null || !file.exists()) {
			return new byte[0];
		}

		try(
				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				final InputStream in = Files.newInputStream(file.toPath())
		) {

			byte[] buffer = new byte[2048];

			int c;

			while ((c = in.read(buffer)) != -1) {
				out.write(buffer, 0, c);
			}

			return out.toByteArray();
		}
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
		// This is for UNIT TESTS
		if(context ==null){
		    String tmpPath = System.getProperty("java.io.tmpdir");
		    relativePath = relativePath.replace("/", File.separator);
		    relativePath = relativePath.startsWith(File.separator) ? relativePath : File.separator + relativePath;
		    Logger.error(FileUtil.class, "Config.CONTEXT not initialized returning a tmp path: " + tmpPath + relativePath);
		    return tmpPath + relativePath;

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

		String base = Config.CONTEXT.getRealPath("/");
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

		return fullFileName.substring(pos + 1);
	}

	public static boolean exists(String fileName) {
		File file = new File(fileName);

		return file.exists();
	}

	public static String[] listDirs(String fileName) {
		return listDirs(new File(fileName));
	}

	/**
	 * Iterates the directory by using the pathFilter and passing the paths found to the pathConsumer
	 * @param directoryPath String base directory path
	 * @param pathFilter Predicate<Path> filter to be applied to the paths found
	 * @param pathConsumer Consumer<Path> consumer to be applied to the paths found
	 * @throws IOException
	 */
	public static void walk(final String directoryPath,
							final Predicate<Path> pathFilter,
							final Consumer<Path> pathConsumer) throws IOException {

		try (Stream<Path> walk = Files.walk(Paths.get(directoryPath))) {

			walk.filter(pathFilter).forEach(pathConsumer);
		}
	}

	public static String[] listDirs(File file) {
		List<String> dirs = new ArrayList<>();

		File[] fileArray = file.listFiles();
		if (null != fileArray) {
			for (File value : fileArray) {
				if (value.isDirectory()) {
					dirs.add(value.getName());
				}
			}
		}

		return dirs.toArray(new String[0]);
	}

	public static String[] listFiles(String fileName) {
		return listFiles(new File(fileName));
	}

	public static String[] listFiles(String fileName, boolean includeSubDirs) {
		return listFiles(new File(fileName), includeSubDirs);
	}

	public static boolean containsParentFolder(File file, File[] folders) {
		for (File folder : folders) {

			if(file.getParent().equalsIgnoreCase(folder.getPath())) {
				return true;
			}
		}

		return false;
	}

	public static String[] listFiles(File dir) {
		return listFiles(dir, false);
	}

    /**
     * Lists the file handles from the specified directory or file. If the input
     * is a directory, the method retrieves file handles from the directory, and
     * optionally its subdirectories, based on the provided parameters.
     *
     * @param fileName the name or path of the file or directory to list file handles from
     * @param includeSubDirs a flag indicating whether subdirectories should also be included
     *                       when processing a directory
     * @return an array of {@code File} objects representing the file handles found
     *         in the specified location
     * @deprecated Use a more modern method for handling files, as getFilesByPattern(File directory, String pattern)
     */
    @Deprecated
	public static File[] listFileHandles(String fileName, Boolean includeSubDirs) {
		return listFileHandles(new File(fileName), includeSubDirs);
	}

    /**
     * Lists the file handles from the specified directory or file. If the input
     * is a directory, the method retrieves file handles from the directory, and
     * optionally its subdirectories, based on the provided parameters.
     *
     * @param dir the name or path of the file or directory to list file handles from
     * @param includeSubDirs a flag indicating whether subdirectories should also be included
     *                       when processing a directory
     * @return an array of {@code File} objects representing the file handles found
     *         in the specified location
     * @deprecated Use a more modern method for handling files, as getFilesByPattern(File directory, String pattern)
     */
    @Deprecated
	public static File[] listFileHandles(File dir, boolean includeSubDirs) {

		if(!dir.exists() || ! dir.isDirectory()){
			return new File[0];
		}


		FileFilter fileFilter = File::isDirectory;

		File[] subFolders = dir.listFiles(fileFilter);

		List<File> files = new ArrayList<>();

		List<File> fileArray = new ArrayList<>(FileUtils.listFiles(dir, TrueFileFilter.INSTANCE,
				includeSubDirs ? TrueFileFilter.INSTANCE : null));

		for (File file : fileArray) {
			if(file.isFile()) {
				if(includeSubDirs && null != subFolders && containsParentFolder(file, subFolders)) {
					files.add(file.getParentFile());
				} else {
					files.add(file);
				}
			}
		}

		return files.toArray(new File[0]);
	}

	public static String[] listFiles(File dir, boolean includeSubDirs) {
		FileFilter fileFilter = File::isDirectory;
		File[] subFolders = dir.listFiles(fileFilter);

		List<String> files = new ArrayList<>();

		List<File> fileArray = new ArrayList<>(FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, includeSubDirs ? TrueFileFilter.INSTANCE : null));

		for (File file : fileArray) {
			if(file.isFile()) {
				if(includeSubDirs && null != subFolders && containsParentFolder(file, subFolders)) {
					files.add(file.getParentFile().getName() + File.separator + file.getName());
				} else {
					files.add(file.getName());
				}
			}
		}

		return files.toArray(new String[0]);
	}

    /**
     * Retrieves a collection of files from the specified directory that match a given pattern.
     * The method searches recursively within the directory and retrieves all files,
     * matching the default pattern.
     *
     * @param directory the directory to search for files; must be a valid directory
     * @return a collection of files from the specified directory that match the pattern
     */
    public static Collection<File> getFilesByPattern(File directory, String pattern) {
        return FileUtils.listFiles(
                directory,
                new WildcardFileFilter(pattern),  // e.g., "*.txt"
                TrueFileFilter.INSTANCE
        );
    }
	public static void mkdirs(String pathName) {
		Path path = Paths.get(pathName);
		try {
			Files.createDirectories(path);
		} catch (IOException e) {
			Logger.error(FileUtil.class, String.format("Fail to makeDir [%s]", path), e);
		}
	}

	/**
	 * This method was created as way to avoid the error when you upload a file via Finder GIT-#9334
	 * Both params are expected to be files
	 * @param source Source file
	 * @param destination Empty destination file (Not a directory)
	 * @return bool result that indicates whether the operation succeeded or failed
	 */
	public static boolean move(File source, File destination) throws IOException {
		return move(source, destination, true);
	}
	/**
	 * This method was created as way to avoid the error when you upload a file via Finder GIT-#9334
	 *
	 * @param source Source file
	 * @param destination Empty destination file (Not a directory)
	 * @param validateEmptyFile if is false it won't check if the file is empty
	 * @return bool result that indicates whether the operation succeeded or failed
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

					return internalDelete(source);
				}
			} catch (Exception e) {
				//In case of error, no worries. Continued with the same logic of move.
				Logger.debug(FileUtil.class, "MD5 Checksum failed, continue with standard move");
			}
		}

		final boolean delete = internalDelete(destination);
		if(!delete){
			Logger.warn(FileUtil.class,String.format(" Fail to remove destination dir [%s]",destination));
		}

		boolean success = source.renameTo(destination);

		// if to rename fails, copy

		if (!success) {
			copyFile(source, destination);
			success = internalDelete(source);
		}
		return success;
	}

	public static String read(String fileName) throws IOException {
		return read(new File(fileName));
	}

	public static String read(File file) throws IOException {
		try(BufferedReader br = new BufferedReader(new FileReader(file))) {

			StringBuilder sb = new StringBuilder();
			String line = null;

			while ((line = br.readLine()) != null) {
				sb.append(line).append('\n');
			}

			return sb.toString().trim();
		}

	}

	public static File[] sortFiles(File[] files) {
		Arrays.sort(files, new FileComparator());

		List<File> directoryList = new ArrayList<>();
		List<File>fileList = new ArrayList<>();

		for (File file : files) {
			if (file.isDirectory()) {
				directoryList.add(file);
			} else {
				fileList.add(file);
			}
		}

		directoryList.addAll(fileList);

		return directoryList.toArray(new File[0]);
	}

	public static String replaceSeparator(String fileName) {
		return StringUtil.replace(fileName, '\\', "/");
	}

	public static List<String> toList(Reader reader) {
		List<String> list = new ArrayList<>();

		try {
			BufferedReader br = new BufferedReader(reader);

			String line = null;

			while ((line = br.readLine()) != null) {
				list.add(line);
			}

			br.close();
		}
		catch (IOException ignored) {
			//Ignored
		}

		return list;
	}

	public static List<String> toList(String fileName) {
		try {
			return toList(new FileReader(fileName));
		}
		catch (IOException ioe) {
			return new ArrayList<>();
		}
	}

	/**
	 * Convert the input stream to a string
	 * @param in InputStream
	 * @return String
	 * @throws IOException
	 */
	public static String toString (final InputStream in) throws IOException {
		return IOUtils.toString(in, StandardCharsets.UTF_8);
	}

	/**
	 * Convert the resource stream from the classpath to a string
	 * @param classpathDir String class path dir of the resource
	 * @return String
	 * @throws IOException
	 */
	public static String toStringFromResourceAsStream (final String classpathDir) throws IOException {
		try (final InputStream in = FileUtil.class.getResourceAsStream(classpathDir)) {
			return toString(in);
		}
	}

	/**
	 * Convert the resource stream from the classpath to a string throwing only RuntimeException in case of error
	 * @param classpathDir String class path dir of the resource
	 * @return String
	 */
	public static String toStringFromResourceAsStreamNoThrown (final String classpathDir) {
		try {
			return toStringFromResourceAsStream(classpathDir);
		} catch (IOException e) {
			throw new DotRuntimeException(e);
		}
	}

	public static Properties toProperties(InputStream is) {
		Properties props = new Properties();

		try {
			props.load(is);
		}
		catch (IOException ignored) {
			//Ignored
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

	@SuppressWarnings("java:S6300")
	public static void write(File file, String s) throws IOException {
		if (file.getParent() != null) {
			mkdirs(file.getParent());
		}
		try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {

			bw.flush();
			bw.write(s);

		}
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
			changedByteVal = size.divide(byteVal,MathContext.UNLIMITED);
			finalVal = Long.toString(Math.round(Math.ceil(changedByteVal.doubleValue())))+" KB";
		}else if(filesize< GIGA_BYTE ){
			byteVal = new BigDecimal(MEGA_BYTE);
			changedByteVal = size.divide(byteVal,MathContext.UNLIMITED);
			finalVal = Math.round(Math.ceil(changedByteVal.doubleValue())) +" MB";
		}else if(filesize< TERA_BYTE){
			byteVal = new BigDecimal(GIGA_BYTE);
			changedByteVal = size.divide(byteVal,MathContext.UNLIMITED);
			finalVal = Math.round(Math.ceil(changedByteVal.doubleValue())) +" GB";
		} else{
			byteVal = new BigDecimal(TERA_BYTE);
			changedByteVal = size.divide(byteVal,MathContext.UNLIMITED);
			finalVal = Math.round(Math.ceil(changedByteVal.doubleValue())) +" TB";
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
			((Buffer)buffer).flip();
			dest.write(buffer);
			buffer.compact();
		}

		((Buffer)buffer).flip();

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
	public static List<File> listFilesRecursively(File aStartingDir)  {
		return listFilesRecursively(aStartingDir, null);
	}



	/**
	 * Recursively walk a directory tree and return a List of all
	 * Files found; the List is sorted using File.compareTo().
	 *
	 * @param aStartingDir is a valid directory, which can be read.
	 */
	public static List<File> listFilesRecursively(File aStartingDir, FileFilter filter)  {
		validateDirectory(aStartingDir);
		List<File> result = getFileListingNoSort(aStartingDir, filter);
		Collections.sort(result);
		return result;
	}


	// PRIVATE //
	private static List<File> getFileListingNoSort(File aStartingDir, FileFilter filter) {
		List<File> result = new ArrayList<>();

		File[] filesAndDirs = null;
		if(filter !=null){
			filesAndDirs = aStartingDir.listFiles(filter);
		}
		else{
			filesAndDirs = aStartingDir.listFiles();
		}
		if (null != filesAndDirs) {
			for (File file : filesAndDirs) {
				result.add(file); //always add, even if directory
				if (!file.isFile()) {
					//must be a directory
					//recursive call!
					List<File> deeperList = getFileListingNoSort(file, filter);
					result.addAll(deeperList);
				}
			}
		}
		return result;
	}

	/**
	 * Directory is valid if it exists, does not represent a file, and can be read.
	 */
	private static void validateDirectory (File aDirectory)  {
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
	public static File mkDirsIfNeeded(final String path) {
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

	public enum StreamCompressorType {

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
