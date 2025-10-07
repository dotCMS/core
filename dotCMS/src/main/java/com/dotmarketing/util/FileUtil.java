package com.dotmarketing.util;

import com.dotcms.util.CloseUtils;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.util.Encryptor;
import com.liferay.util.HashBuilder;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Provide utility methods to work with binary files in dotCMS.
 *
 * @author root
 * @since Mar22nd, 2012
 */
public class FileUtil {

	private static final int BUFFER_SIZE = Config.getIntProperty("FILE_BUFFER", 4096);
	private static final Set<String> extensions = new HashSet<>();
	private static final Lazy<Set<String>> EDITABLE_AS_TEXT_FILE_TYPES = Lazy.of(FileUtil::getEditableAsTextFileTypes);

	protected static final String[] DEFAULT_IMAGE_EXTENSIONS = {
			"png", "gif", "webp", "jpeg", ".jpg", "tiff", "bpm", "svg", "avif",
			"bmp", "tif", "tiff"
	};

	/**
	 * returns the valid image extensions with a . in front of the extension, e.g.
	 * png -> .png
	 */
	public static final Lazy<String[]> IMAGE_EXTENSIONS = Lazy.of(() ->

			Try.of(() -> Arrays.stream(Config.getStringArrayProperty("VALID_IMAGE_EXTENSIONS",DEFAULT_IMAGE_EXTENSIONS))
					.map(x -> x.startsWith(".") ? x : "." + x)
					.toArray(String[]::new)
			).getOrElse(() -> Arrays.stream(DEFAULT_IMAGE_EXTENSIONS)
					.map(x -> x.startsWith(".") ? x : "." + x)
					.toArray(String[]::new))
	);

	/**
	 * Returns the MIME Types of files whose contents can be safely edited inside the dotCMS Edit
	 * Mode. You can add your own types via the {@code DOT_EDITABLE_AS_TEXT_FILE_TYPES}
	 * configuration property.
	 *
	 * @return The MIME Types of editable files.
	 */
	private static Set<String> getEditableAsTextFileTypes() {
		final Set<String> editableTypes = new HashSet<>();
		editableTypes.addAll(Set.of(
				// Scripts and source code
				"application/javascript",
				"application/ecmascript",
				"application/x-typescript",
				"application/x-sh",              // Shell script
				"application/x-httpd-php",       // PHP scripts
				"application/x-latex",           // LaTeX documents

				// Structured data formats
				"application/json",
				"application/xml",
				"application/x-yaml",
				"application/toml",
				"application/x-toml",
				"application/x-www-form-urlencoded",
				"application/x-sql",

				// React/TSX extensions
				"application/jsx",
				"application/tsx"
		));
		editableTypes.addAll(new HashSet<>(Arrays.asList(Config.getStringArrayProperty(
				"EDITABLE_AS_TEXT_FILE_TYPES", new String[]{}))));
		return editableTypes;
	}

	/**
	 * Creates a temporal file with unique name
	 * @param prefix String name
	 * @return File
	 * @throws IOException
	 */
	public static File createTemporaryFile(final String prefix) throws IOException {
		return createTemporaryFile(prefix, null);
	}

	public static File createTemporaryDirectory(final String prefix) throws IOException {
		final Path tempDirectory = Files.createTempDirectory(prefix);
		return tempDirectory.toFile();
	}

	/**
	 * Creates a temporary file with unique name
	 * @param prefix String name
	 * @param extension String optional extension, if null "tmp" will be use
	 * @return File
	 * @throws IOException
	 */
	public static File createTemporaryFile(final String prefix, final String extension) throws IOException {
		return createTemporaryFile(prefix, extension, false);
	}

	/**
	 * Creates a temporary file with unique name
	 * @param prefix String name
	 * @param extension String optional extension, if null "tmp" will be use
	 * @param useDotGeneratedPath the file will be written under dotGeneratedPath
	 * @return File
	 * @throws IOException
	 */
	public static File createTemporaryFile(final String prefix, final String extension,
			final boolean useDotGeneratedPath) throws IOException {
		if (useDotGeneratedPath) {
			final String dotGeneratedPath = ConfigUtils.getDotGeneratedPath();
			final File dotGeneratedDir = Paths.get(dotGeneratedPath).normalize().toFile();
			if (!dotGeneratedDir.exists()) {
				dotGeneratedDir.mkdir();
			}
			return File.createTempFile(prefix + System.currentTimeMillis(),
					UtilMethods.isSet(extension) ? extension : "tmp", dotGeneratedDir);
		}
		return File.createTempFile(prefix + System.currentTimeMillis(),
				UtilMethods.isSet(extension) ? extension : "tmp");
	}

	/**
	 * Creates a temporal file with unique name, in case you have a small initial content to write, you can include as a third parameter
	 * In case you need to write a long string, use another strategy
	 * @param prefix String name
	 * @param extension String optional extension, if null "tmp" will be use
	 * @param initialContent String small content to add to the file.
	 *
	 * @return File
	 * @throws IOException
	 */
	public static File createTemporaryFile(final String prefix, final String extension, final String initialContent) throws IOException {

		final File file = createTemporaryFile(prefix, extension);
		try (final FileWriter fileWriter = new FileWriter(file)) {

			fileWriter.write(initialContent);
		}

		return file;
	}

	/**
	 * This method takes a string of a filename or extension and maps it to a
	 * known .png file in the /html/image/icons/directory
	 * 
	 * @param x
	 *            is the filename or extension
	 * @return
	 */
	public static String getIconExtension(String x) {
		if(x==null){
			return "ukn";
		}
		x = x.toLowerCase();
		if (x.lastIndexOf(".") > -1) {
			x = x.substring(x.lastIndexOf(".")+1, x.length());
		}

		if (extensions.size() == 0) {
			synchronized (FileUtil.class) {
				if (extensions.size() == 0) {
					String path = com.liferay.util.FileUtil.getRealPath("/html/images/icons");

					String[] files = new File(path).list(new PNGFileNameFilter());
					for (String name : files) {
						name =name.toLowerCase();
						if (name.indexOf(".png") > -1)
							extensions.add(name.replace(".png", ""));
					}
				}
			}
		}
		// if known extension
		if (extensions.contains(x)) {
			return x;
		} else {
			return "ukn";
		}

	}

  final static Lazy<int[]> illegalChars= Lazy.of(()-> {
      int[] illegalCharacters = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
              24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};
      Arrays.sort(illegalCharacters);
      return illegalCharacters;
  });

  /**
   * cleans filenames and allows unicode- taken from
   * https://stackoverflow.com/questions/1155107/is-there-a-cross-platform-java-method-to-remove-filename-special-chars
   * 
   * @param badFileNameIncoming
   * @return
   */
  public static String sanitizeFileName(final String badFileNameIncoming) {


      final String fileExtention = UtilMethods.isSet(UtilMethods.getFileExtension(badFileNameIncoming)) 
                      ? UtilMethods.getFileExtension(badFileNameIncoming)
                          : "ukn";


      final String replacementFileName = RandomStringUtils.randomAlphabetic(10) + "." + fileExtention;
      final String badFileName= Try.of(()-> Paths.get(badFileNameIncoming).getFileName().toString())
                          .onFailure(e->{
                              Logger.warn(FileUtil.class, "Invalid File Name: '" + badFileNameIncoming +"' replacing with:" + replacementFileName + " " +  e.getMessage());
                              SecurityLogger.logInfo(FileUtil.class, "Invalid File Name: '" + badFileNameIncoming +"' replacing with:" + replacementFileName + " " +  e.getMessage());
                          })
                          .getOrElse(replacementFileName);


      // remove non-valid characters
      final StringBuilder cleanName = new StringBuilder();
      int len = badFileName.codePointCount(0, badFileName.length());
      for (int i = 0; i < len; i++) {
          final int c = badFileName.codePointAt(i);
          if (Arrays.binarySearch(illegalChars.get(), c) < 0) {
              cleanName.appendCodePoint(c);
          }
      }

      //Stripts leading peroids from filename
      final String cleanFileName = StringUtils.stripStart( cleanName.toString(), ".");

      return (cleanFileName.length() > 0) ? cleanFileName : replacementFileName;



  }

	/**
	 * This will write the given InputStream to a new File in the given location
	 * 
	 * @param uploadedInputStream
	 * @param uploadedFileLocation
	 * @return
	 */
	public static void writeToFile(InputStream uploadedInputStream,
			String uploadedFileLocation) {

        OutputStream out = null;
	    try {
			int read;
			byte[] bytes = new byte[1024];

			final File file = new File(uploadedFileLocation);
			out = Files.newOutputStream(file.toPath());
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
            out.flush();

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
            CloseUtils.closeQuietly(out);
        }

	}


	/**
	 * This method will figure out if the passed in path is relative meaning relative to the WAR and will
	 * use the Servlet.CONTEXT to find the real path or if the passed in path is Absolutely meaning absolute 
	 * to the file system.  It will return the full path or the file or directory
	 * @param path the passed in path. If it starts with a / or \ or [a-z]: then it is considered a full path
	 * @return
	 */
	public static String getAbsolutlePath(String path){
		if(RegEX.contains(path, "^[a-zA-Z]:|^/|^\\\\")){
			return path;
		}else{
			return com.liferay.util.FileUtil.getRealPath(path);
		}
	}

	/**
	 * Delete directories recursively including the initial directory, taking into account symbolic links
	 * @param path path to be file/directory to be deleted
	 * @throws IOException
	 */
	public static void deleteDir(String path) throws IOException{
		// initial directory
		Path directory = Paths.get(path);
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
		   @Override
		   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		       Files.delete(file);
		       return FileVisitResult.CONTINUE;
		   }
		   @Override
		   public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		       Files.delete(dir);
		       return FileVisitResult.CONTINUE;
		   }
		});
	}

    public static FileFilter getOnlyFolderFileFilter() {
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if ( pathname.isDirectory() ){
                    return true;
                } else {
                    return false;
                }
            }
        };
    }

	/**
	 * Figure out the sha256 of the file content, assumes that the file exists and can be read
	 * @param file {@link File}
	 * @return String  just as unix sha returns
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static String sha256toUnixHash (final File file) throws NoSuchAlgorithmException, IOException {

		return sha256toUnixHash(file.toPath());
	} // sha256toUnixHash.

	/**
	 * Figure out the sha256 of the file content, assumes that the file exists and can be read
	 * @param path {@link Path}
	 * @return String just as unix sha returns
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
    public static String sha256toUnixHash(final Path path) throws NoSuchAlgorithmException, IOException {

		final HashBuilder sha256Builder = Encryptor.Hashing.sha256();
		final byte[] buffer             = new byte[BUFFER_SIZE];
		int countBytes 					= 0;

		try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {

			countBytes = inputStream.read(buffer);
			while (countBytes > 0) {

				sha256Builder.append(buffer, countBytes);
				countBytes = inputStream.read(buffer);
			}
		}

		return sha256Builder.buildUnixHash();
	} // sha256toUnixHash.


	/**
	 * Given an inode this gets the path to the binary file
	 * @param con
	 * @return
	 */
	public static Optional<Path> binaryPath(final Contentlet con) {
		final String inode = con.getInode();
		final String path = String.format("%s/%s/%s/%s",
				APILocator.getFileAssetAPI().getRealAssetsRootPath(), inode.charAt(0),
				inode.charAt(1), inode);
		final Path p = Paths.get(path);
		return p.toFile().exists() ? Optional.of(p) : Optional.empty();
	}

	/**
	 * Reads the contents of the specified file, using the {@code Files.newInputStream} approach.
	 *
	 * @param file The {@link File} that will be read.
	 *
	 * @return The String contents of the File in the form of an {@link Optional}.
	 *
	 * @throws IOException An error occurred when reading the file.
	 */
	public static Optional<String> read(final File file) throws IOException {
		if (file == null || !file.exists()) {
			return Optional.empty();
		}
		final byte[] buffer = new byte[8192];
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(16384);
			 final InputStream inputStream = Files.newInputStream(file.toPath())) {
			int bytesRead = inputStream.read(buffer);
			if (bytesRead < buffer.length) {
				byteArrayOutputStream.write(buffer, 0, bytesRead);
				return Optional.of(byteArrayOutputStream.toString());
			}
			while (bytesRead != -1) {
				byteArrayOutputStream.write(buffer, 0, bytesRead);
				bytesRead = inputStream.read(buffer);
			}
			return Optional.of(byteArrayOutputStream.toString());
		} catch (final IOException e) {
			throw e;
		}
	}

	/**
	 * Get the content of a File from the resource folder
	 * @param path relative path from the resource folder
	 * @return
	 * @throws IOException
	 */
	public static String getFileContentFromResourceContext(final String path) throws IOException {
		DotPreconditions.checkArgument(!path.startsWith(StringPool.FORWARD_SLASH), "Path must be relative");

		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		final URL initFileURL = classLoader.getResource(path);
		return new String (com.liferay.util.FileUtil.getBytes(new File(initFileURL.getPath())));
	}

	/**
	 * Determines whether the specified MIME Type belongs to a file whose contents can be edited as
	 * text or not. Users can add additional MIME Types to the list of editable as text file types
	 * via the {@code DOT_EDITABLE_AS_TEXT_FILE_TYPES} configuration property.
	 *
	 * @param mimeType The MIME Type to check.
	 *
	 * @return If the file can be edited as text, returns {@code true}.
	 */
	public static boolean isFileEditableAsText(final String mimeType) {
		return UtilMethods.isSet(mimeType) && (mimeType.startsWith("text/") || EDITABLE_AS_TEXT_FILE_TYPES.get().contains(mimeType));
	}

	/**
	 * NIO based method to copy a directory from one location to another
	 * @param src source directory
	 * @param dest destination directory
	 */
	public static void copyDir(Path src, Path dest)  {
		try (Stream<Path> stream = Files.walk(src)) {
			// Iterate over each Path object in the stream
			stream.forEach(source -> {
				// Get the relative path from the source directory
				Path relativePath = src.relativize(source);

				// Get the corresponding path in the destination directory
				Path destination = dest.resolve(relativePath);

				try {
					// Copy each Path object from source to destination
					Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					Logger.debug(FileUtil.class, e.getMessage(), e);
				}
			});
		} catch (IOException e) {
			Logger.debug(FileUtil.class, e.getMessage(), e);
		}
	}

	/**
	 * Attempts to infer the extension of an image file based on its MIME Type.
	 *
	 * @param mimeType The MIME Type in a given file.
	 *
	 * @return The extension of the image file.
	 */
	public static String getImageExtensionFromMIMEType(final String mimeType) {
		if (UtilMethods.isEmpty(mimeType)) {
			return mimeType;
		}
		final String mimeTypeLc = mimeType.toLowerCase();
		for (final String ext : IMAGE_EXTENSIONS.get()) {
			if (mimeTypeLc.contains(ext.replace(StringPool.PERIOD, StringPool.BLANK))) {
				return ext;
			}
		}
		return StringPool.BLANK;
	}


	/**
	 *
	 * @param file
	 * @throws IOException
	 */
	public static Charset detectEncodeType(final File file)  {

		byte[] buf = new byte[4096];
		try (InputStream is = Files.newInputStream(file.toPath())){


			UniversalDetector detector = new UniversalDetector(null);
			int nread;
			while ((nread = is.read(buf)) > 0 && !detector.isDone()) {
				detector.handleData(buf, 0, nread);
			}
			detector.dataEnd();
			return Charset.forName(detector.getDetectedCharset());
		}catch (Exception e){
			Logger.error(FileUtil.class, e.getMessage(),e);

		}
		return Charset.defaultCharset();

	}

	/**
	 * Count the number of lines in the file
	 *
	 * @param file the file to count the lines
	 * @return the number of lines in the file
	 */
	public static Long countFileLines(final File file) throws IOException {

		long totalCount;
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			totalCount = reader.lines().count();
		}

		return totalCount;
	}

	/**
	 * Removes the extension from a filename. This method will remove all characters starting from
	 * the first occurrence of the period character.
	 *
	 * @param filename The filename to remove the extension from.
	 *
	 * @return The filename without the extension.
	 */
	public static String removeExtension(final String filename) {
		if (filename == null) {
			return null;
		}
		final int pos = filename.indexOf(StringPool.PERIOD);
		if (pos == -1) {
			return filename;
		}
		return filename.substring(0, pos);
	}

}



final class PNGFileNameFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return (name.indexOf(".png") > -1);
	}

}
