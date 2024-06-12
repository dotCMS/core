package com.dotcms.rest.api.v1.temp;

import com.dotcms.content.model.hydration.MetadataDelegate;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.image.filter.ImageFilterAPI;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vavr.Lazy;
import io.vavr.control.Try;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.liferay.util.StringPool.FORWARD_SLASH;

/**
 * This class represents a temporary file that can be uploaded to dotCMS that is not an actual File
 * Asset object that has been stored in the current content repository.
 *
 * @author Will Ezell
 * @since Jul 8th, 2019
 */
public class DotTempFile {

  public final String id,mimeType,referenceUrl,thumbnailUrl,fileName,folder;
  public final boolean image;
  public final Map<String, Serializable> metadata;

  private static final String UNKNOWN_FILE_NAME = "unknown";
  private static final Lazy<String> REFERENCE_URL_FORMAT =
          Lazy.of(() -> Config.getStringProperty("TEMP_FILE_REFERENCE_URL_FORMAT", "/dA/%s/tmp/%s"));
  private static final Lazy<String> THUMBNAIL_URL_FORMAT =
          Lazy.of(() -> Config.getStringProperty("TEMP_FILE_THUMBNAIL_URL_FORMAT", "/contentAsset" +
                  "/image/%s/tmp/filter/Thumbnail/thumbnail_w/250/thumbnail_h/250/%s"));

  @JsonIgnore
  public final File file;
  @JsonIgnore
  private static final SecurityUtils securityUtils = new SecurityUtils();

  /**
   * Creates an instance of a Temporary File.
   *
   * @param id   The ID of the temporary file.
   * @param file The actual binary {@link File} that is being represented.
   */
  public DotTempFile(final String id, final File file) {
    super();
    this.id = id;
    this.file = file;
    final Map<String, Serializable> initialMetadata = this.getMetadata(file);
    this.mimeType = (null != initialMetadata && initialMetadata.containsKey(MetadataDelegate.CONTENT_TYPE))
            ? (String) initialMetadata.get(MetadataDelegate.CONTENT_TYPE)
            : FileAsset.UNKNOWN_MIME_TYPE;
    final String retrievedFileName = this.getFileName(file);
    this.image = this.isImage(retrievedFileName);
    this.referenceUrl = String.format(REFERENCE_URL_FORMAT.get(), id, retrievedFileName);
    this.thumbnailUrl = this.image || this.mimeType.contains(ImageFilterAPI.PDF)
            ? String.format(THUMBNAIL_URL_FORMAT.get(), id, retrievedFileName)
            : null;
    this.metadata = this.getUpdatedMetadataOrFallback(file, retrievedFileName, initialMetadata);
    this.fileName = retrievedFileName;
    this.folder = this.resolveFolder();
  }

  /**
   * Retrieves the metadata of the specified file, if and only if such a file can be read.
   *
   * @param file The {@link File} to retrieve metadata from.
   *
   * @return A {@link Map} containing the file's metadata, or {@code null} if the file cannot be
   * read.
   */
  private @Nullable Map<String, Serializable> getMetadata(final File file) {
    return this.canRead(file)
            ? Try.of(() -> APILocator.getFileStorageAPI().generateRawBasicMetaData(file)).getOrNull()
            : null;
  }

  /**
   * Attempts to read the metadata from the specified file. If such a file hasn't been persisted
   * to the File System -- which means that the metadata cannot be retrieved -- then the specified
   * fallback metadata Map is returned.
   * <p>Now, if the file does exist and it's file extension has been determined, we need to
   * re-generate its metadata Map so that it includes the appropriate information. This will only
   * happen initially when an extensionless file is created. If the file's extension is already
   * present, the metadata will NOT be re-generated.</p>
   *
   * @param file            The binary {@link File} to retrieve metadata from.
   * @param fileName        The name of the file, which may include its extension.
   * @param initialMetadata The initial metadata Map to fall back to if the file is still "unknown"
   *                        or doesn't have any extension.
   *
   * @return The file's metadata Map.
   */
  private Map<String, Serializable> getUpdatedMetadataOrFallback(final File file, final String fileName, final Map<String, Serializable> initialMetadata) {
    final String nameFromFile = null != file ? file.getName(): UNKNOWN_FILE_NAME;
    if (!UNKNOWN_FILE_NAME.equals(nameFromFile) && !nameFromFile.equals(fileName)) {
      final String targetDirectory = file.getParentFile().getAbsolutePath();
      final Path targetPathStr = new File(targetDirectory).toPath().normalize();
      final Path fileToMovePath = Paths.get(file.getAbsolutePath());
      final Path targetPath = Paths.get(targetPathStr.toString(), fileName);
      try {
        final Path renamedFile = Files.move(fileToMovePath, targetPath);
        return this.getMetadata(new File(renamedFile.toString()));
      } catch (final IOException e) {
        Logger.warn(this, String.format("Unable to rename file '%s' to '%s'",
                file.getAbsolutePath(), fileName));
      }
    }
    return initialMetadata;
  }

  /**
   * Determines whether the specified file can be read or not. That is, whether it exists in the
   * File System or not.
   *
   * @param file The {@link File} to check.
   *
   * @return If the file exists and can be read, returns {@code true}.
   */
  private boolean canRead(final File file) {
    return null != file && file.exists() && file.canRead();
  }

  @JsonProperty("length")
  public long length() {
    return file.length();
  }
  
  @Override
  public String toString() {
    return "DotTempFile [id=" + id + ", mimeType=" + mimeType + ", referenceUrl=" + referenceUrl + ", thumbnailUrl=" + thumbnailUrl
        + ", image=" + image + ", file=" + file + ", folder=" + folder + "]";
  }

  /**
   * Attempts to resolve the folder path of the Temporary File.
   *
   * @return The resolved folder path.
   */
  private String resolveFolder() {
    final int begin = file.getPath().indexOf(id)+id.length();
    final int end = file.getPath().lastIndexOf(File.separator);
    final String path = file.getPath().substring(begin,end);
    return path.startsWith(FORWARD_SLASH) ? path.substring(1) : path;
  }

  /**
   * Retrieves the file name of the specified binary file, or attempts to retrieve it from the
   * file's metadata Map. If the file is null, then the file name is set to "unknown".
   *
   * @param file The binary {@link File} to retrieve the name from.
   *
   * @return The file name of the specified file.
   */
  private String getFileName(final File file) {
    if (null == file) {
      return UNKNOWN_FILE_NAME;
    }
    final String nameFromFile = file.getName();
    String fileExtension = UtilMethods.getFileExtension(nameFromFile);
    if (!FileAsset.UNKNOWN_MIME_TYPE.equals(this.mimeType) && UtilMethods.isNotSet(fileExtension)) {
      fileExtension = FileUtil.getImageExtensionFromMIMEType(this.mimeType);
      if (UtilMethods.isSet(fileExtension)) {
        return nameFromFile + fileExtension;
      }
    }
    return nameFromFile;
  }

  /**
   * Determines whether the specified file is an image or not based on the file's extension. If it
   * doesn't have any, then the file's MIME type is used to determine if it's an image.
   *
   * @param fileName The name of the file to check.
   *
   * @return If the file is an image, returns {@code true}.
   */
  private boolean isImage(final String fileName) {
    if (FileAsset.UNKNOWN_MIME_TYPE.equals(this.mimeType)) {
      return false;
    }
    final String extension = UtilMethods.getFileExtension(fileName);
    if (UtilMethods.isSet(extension)) {
      return UtilMethods.isImage(fileName);
    }
    final String extensionFromMIMEType = FileUtil.getImageExtensionFromMIMEType(this.mimeType);
    return UtilMethods.isSet(extensionFromMIMEType);
  }

}
