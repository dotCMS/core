package com.dotcms.rest.api.v1.temp;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.security.multipart.IllegalFileExtensionsValidator;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.PathContainmentUtil;
import com.dotcms.util.SecurityUtils;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.Encryptor;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;
import net.jodah.failsafe.CircuitBreaker;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.http.Header;

import static com.dotcms.storage.FileMetadataAPIImpl.META_TMP;

/**
 * This API allows for the creation and retrieval of temporary files in the content repository.
 * <p>dotCMS allows you to upload temporary files in situations where users/developers need to store
 * temporary information somewhere in the repository without having to worry how/when it must be
 * deleted. For instance, this very API is used by dotCMS when content authors upload binary files
 * via the UI and haven't saved or published them.</p>
 *
 * @author Will Ezell
 * @since Jul 8th, 2019
 */
public class TempFileAPI {

  public static final String TEMP_RESOURCE_MAX_AGE_SECONDS = "TEMP_RESOURCE_MAX_AGE_SECONDS";
  public static final String TEMP_RESOURCE_ALLOW_ANONYMOUS = "TEMP_RESOURCE_ALLOW_ANONYMOUS";
  public static final String TEMP_RESOURCE_ALLOW_NO_REFERER = "TEMP_RESOURCE_ALLOW_NO_REFERER";
  public static final String TEMP_RESOURCE_MAX_FILE_SIZE = "TEMP_RESOURCE_MAX_FILE_SIZE";
  public static final String TEMP_RESOURCE_MAX_FILE_SIZE_ANONYMOUS = "TEMP_RESOURCE_MAX_FILE_SIZE_ANONYMOUS";
  
  public static final String TEMP_RESOURCE_ENABLED = "TEMP_RESOURCE_ENABLED";
  public static final String TEMP_RESOURCE_PREFIX = "temp_";

  private static final String WHO_CAN_USE_TEMP_FILE = "whoCanUse.tmp";
  private static final String TEMP_RESOURCE_BY_URL_ADMIN_ONLY="TEMP_RESOURCE_BY_URL_ADMIN_ONLY";

  /**
   * The extensions a file downloaded by URL may have, as a comma-separated list compared without
   * regard to letter case. Defaults to {@link #DEFAULT_URL_DOWNLOAD_EXTENSIONS}.
   */
  public static final String TEMP_RESOURCE_BY_URL_ALLOWED_EXTENSIONS = "TEMP_RESOURCE_BY_URL_ALLOWED_EXTENSIONS";

  /** Image formats and PDF: the kinds of file the URL download is meant for. */
  static final List<String> DEFAULT_URL_DOWNLOAD_EXTENSIONS =
          List.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "tif", "tiff", "svg", "pdf");

  /** How much of a response body the URL validation probe reads before it stops. */
  static final int URL_PROBE_MAX_BYTES = 64 * 1024;

  /**
   * The content type family a response must declare for a given extension. Extensions outside
   * this map are only checked against {@link #NEVER_ACCEPTED_CONTENT_TYPES}.
   */
  private static final Map<String, String> CONTENT_TYPE_FAMILY_BY_EXTENSION = Map.ofEntries(
          Map.entry("jpg", "image/"), Map.entry("jpeg", "image/"), Map.entry("png", "image/"),
          Map.entry("gif", "image/"), Map.entry("webp", "image/"), Map.entry("bmp", "image/"),
          Map.entry("tif", "image/"), Map.entry("tiff", "image/"), Map.entry("svg", "image/"),
          Map.entry("pdf", "application/pdf"),
          Map.entry("md", "text/"), Map.entry("txt", "text/"));

  /** Content types that are never stored under a name from the allow list. */
  private static final Set<String> NEVER_ACCEPTED_CONTENT_TYPES = Set.of(
          "text/html", "application/xhtml+xml", "text/javascript", "application/javascript",
          "application/ecmascript");

  /** Content types that carry no information about the body. */
  private static final Set<String> UNSPECIFIC_CONTENT_TYPES = Set.of("application/octet-stream", FileAsset.UNKNOWN_MIME_TYPE);
  private static final Lazy<Boolean> allowAccessToPrivateSubnets = Lazy.of(()->Config.getBooleanProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", false));
  private static final IllegalFileExtensionsValidator fileExtensionValidator = new IllegalFileExtensionsValidator();
  

  /**
   * Refuses a temp file location that does not resolve strictly inside the temp upload directory.
   * The comparison is made on canonical paths, so {@code ..} segments and symbolic links are
   * resolved, and it runs before anything is created on disk. With {@link #createEmptyTempFile}
   * keeping only the file name part of what it receives, this guard is a second line of defence
   * rather than the first one; it is kept so the location rule holds whatever the name handling
   * above it does.
   *
   * @param tmpRoot   the temp upload directory
   * @param candidate the file about to be created
   * @throws DotRuntimeException if the candidate falls outside the temp upload directory
   */
  static void requireInsideTempUploadDir(final File tmpRoot, final File candidate) {

    if (!PathContainmentUtil.isContainedIn(tmpRoot, candidate)) {
      SecurityLogger.logInfo(TempFileAPI.class, () -> "Attempted file upload outside of temp folder: " + candidate.getPath());
      throw new DotRuntimeException("Attempted file upload outside of temp folder");
    }
  }

  /**
   * Returns an empty temp file, with a unique id, that the caller can write to and later look up by
   * that id. The request is used to build the allow list (user, session and request fingerprint)
   * stored next to the file, which decides who can read the temp resource in later requests.
   * <p>
   * Only the file name part of {@code incomingFileName} is used: any directory part is dropped, so
   * the file always lands in its own folder under the temp upload directory. The location is
   * checked on canonical paths before anything is created on disk. A name whose extension is on
   * the upload block list is refused before anything is created, and the check runs on the
   * sanitised name so the client's spelling cannot sidestep it.
   *
   * @param incomingFileName the name the file was uploaded with, as received from the client
   * @param request          the current request, used for the allow list
   * @return the new {@link DotTempFile}, pointing at a file that does not exist yet
   * @throws DotSecurityException     if the request is not allowed to create temp files
   * @throws IllegalArgumentException if the name's extension is on the block list
   * @throws DotRuntimeException      if the name is missing, the file would fall outside the temp
   *                              upload directory, or its folder cannot be created
   */
  public DotTempFile createEmptyTempFile(final String incomingFileName,final HttpServletRequest request) throws DotSecurityException {
    final String anon = Try.of(() -> APILocator.getUserAPI().getAnonymousUser().getUserId()).getOrElse("anonymous");

    
    
    final User user = PortalUtil.getUser(request);

    final String sessionId = (request.getSession(false)!=null) ? request.getSession().getId() : null;

    final String requestFingerprint = this.getRequestFingerprint(request);
    
    
    final List<String> allowList = new ArrayList<>();
    if (user != null && user.getUserId() != null && !user.getUserId().equals(anon)) {
      allowList.add(user.getUserId());
    }
    if (sessionId != null) {
      allowList.add(sessionId);
    }
    if (requestFingerprint != null) {
      allowList.add(requestFingerprint);
    }
    if (incomingFileName == null) {
      throw new DotRuntimeException("Unable to create temp file without a name");
    }
    // Keep the file name only: callers pass the client's name through, and it may carry a path
    final String fileName = FileUtil.sanitizeFileName(incomingFileName);
    // Every temp file can become an asset, so the extension block list applies here, whichever
    // API the name arrived through (multipart, JSON content, URL fetch, import, bulk upload)
    fileExtensionValidator.validate(fileName);
    final String tempFileId = TEMP_RESOURCE_PREFIX + UUIDGenerator.shorty();

    final File tmpRoot = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary());
    final File tempFolder = new File(tmpRoot, tempFileId);
    final File tempFile = new File(tempFolder, fileName);

    requireInsideTempUploadDir(tmpRoot, tempFile);

    if (!tempFolder.mkdirs()) {
      throw new DotRuntimeException("Unable to create temp directory:" + tempFolder);
    }
    createTempPermissionFile(tempFolder, allowList);
    SecurityLogger.logInfo(this.getClass(),"Temp File Created with id: " + tempFileId + ", uploaded by userId: " + user.getUserId());
    return new DotTempFile(tempFileId, tempFile);
  }

  /**
   * This method takes a request and based upon it it returns the max file size that can be
   * uploaded, based on the user uploading.  Anonymous users can have smaller file size limitations
   * than authenticated users.  A return of -1 means unlimited.
   * @param request
   * @return
   */
  @VisibleForTesting
  public long maxFileSize(final HttpServletRequest request) {
    

    final long requestedMax = ConversionUtils.toLongFromByteCountHumanDisplaySize(request.getParameter("maxFileLength"), -1);
    final long systemMax    =  Config.getLongProperty(TEMP_RESOURCE_MAX_FILE_SIZE, -1l);
    final long anonMax      =  Config.getLongProperty(TEMP_RESOURCE_MAX_FILE_SIZE_ANONYMOUS, -1l);
    final boolean isAnon    = PortalUtil.getUserId(request) == null || UserAPI.CMS_ANON_USER_ID.equals(PortalUtil.getUserId(request));
    
    List<Long> longs = (isAnon) ? Lists.newArrayList(requestedMax,systemMax,anonMax) : Lists.newArrayList(requestedMax,systemMax);
    longs.removeIf(i-> i < 0);
    Collections.sort(longs); 
    return longs.isEmpty() ? -1l : longs.get(0);

  }
  
  
  
  /**
   * Writes an InputStream to a temp file and returns the tempFile with a unique id and file handle
   * that can be used to access the temp file. The request will be used to create a fingerprint
   * that will be written to the "allowList" and can
   * be used to retreive the temp resource in other requests
   * 
   * @param incomingFileName
   * @param request
   * @param inputStream
   * @return
   * @throws DotSecurityException
   */
  public DotTempFile createTempFile(final String incomingFileName,final HttpServletRequest request, final InputStream inputStream)
      throws DotSecurityException {
    
    final DotTempFile dotTempFile = this.createEmptyTempFile(incomingFileName, request);
    final File tempFile = dotTempFile.file;
    final long maxLength = maxFileSize(request);
        
    try (final OutputStream out = new BoundedOutputStream(maxLength,Files.newOutputStream(tempFile.toPath()))) {


      int read = 0;
      final byte[] bytes = new byte[4096];
      while ((read = inputStream.read(bytes)) != -1) {
        out.write(bytes, 0, read);
      }

      if (dotTempFile.metadata == null && dotTempFile.file.exists()) {

        return new DotTempFile(dotTempFile.id, dotTempFile.file);
      }
      return dotTempFile;
    } catch (IOException e) {
      final String message = APILocator.getLanguageAPI().getStringKey(WebAPILocator.getLanguageWebAPI().getLanguage(request), "temp.file.max.file.size.error").replace("{0}", UtilMethods.prettyByteify(maxLength));
      throw new DotStateException(message, e);
    } catch (Exception e) {
      throw new DotRuntimeException(e.getMessage(), e);
    } finally {
      CloseUtils.closeQuietly(inputStream);
    }
  }

  /**
   * Takes a URL, downloads it and the returns the resulting file as tempFile with a unique ID and
   * file handle that can be used to access the temp file. The request will be used to create a
   * fingerprint that will be written to the "allowList" and can be used to retrieve the temp
   * resource in other requests.
   *
   * @param incomingFileName The name of the file to be created.
   * @param request          The current instance of the {@link HttpServletRequest}.
   * @param url              The URL pointing to the file that must be downloaded.
   * @param timeoutSeconds   The number of seconds to wait for the download to complete.
   *
   * @return The {@link DotTempFile} instance representing the downloaded file.
   *
   * @throws DotSecurityException The Temporary File could not be created.
   * @throws IOException          An error occurred when retrieving the binary file from the
   *                              download URL.
   */
  public DotTempFile createTempFileFromUrl(final String incomingFileName,
                                           final HttpServletRequest request,
                                           final URL url,
                                           final int timeoutSeconds)
          throws DotSecurityException, IOException {
      final User user = PortalUtil.getUser(request);
      if (!isUrlDownloadPermitted(user)) {
        throw new DotSecurityException("Downloads by URL are restricted to administrators");
      }
      if (!isAllowedRemoteUrl(url)) {
        throw new IllegalArgumentException("Failed to download file by URL: the host is not an allowed target");
      }
      final String fileName = resolveDownloadFileName(incomingFileName, url);
      if (!isAllowedDownloadName(fileName)) {
        SecurityLogger.logInfo(this.getClass(), "Refused a download by URL: the file name's extension is not allowed");
        throw new IllegalArgumentException("The file name's extension is not allowed for downloads by URL");
      }
      final DotTempFile dotTempFile = createEmptyTempFile(fileName, request);
      final File tempFile = dotTempFile.file;

      // By adding the source IP, we give visibility to the remote server of who initiated the request
      final String sourceIpAddress = request.getRemoteAddr();
      final String finalUrl = url.toString().contains("?") ? url + "&sourceIp=" + sourceIpAddress :  url + "?sourceIp=" + sourceIpAddress ;
      final CircuitBreakerUrl urlGetter =
              CircuitBreakerUrl.builder().setMethod(Method.GET).setUrl(finalUrl)
                      .setTimeout(timeoutSeconds * 1000L).build();
      try {
        try(final OutputStream out = new BoundedOutputStream(maxFileSize(request),
                        Files.newOutputStream(tempFile.toPath()))){
          urlGetter.doOut(out);
        }
        if (!isAllowedDownloadName(fileName, contentTypeOf(urlGetter.getResponseHeaders()))) {
          SecurityLogger.logInfo(this.getClass(), "Discarded a download by URL: the declared content type does not match the file name's extension");
          throw new IllegalArgumentException("The downloaded content type does not match the file name's extension");
        }
        return storedDownload(dotTempFile.id, tempFile);
      } catch (final IOException | RuntimeException e) {
        // nothing of a refused or failed download stays behind
        com.liferay.util.FileUtil.deltree(tempFile.getParentFile());
        throw e;
      }
  }

  /**
   * The temp file as it is stored once the download completed: its type is detected from the
   * content, which must agree with the name like the declared content type had to. A name with
   * no extension gets the extension the detected type implies, so the file is always stored
   * under a name with an allowed extension.
   *
   * @param id       the temp file id
   * @param tempFile the downloaded file
   * @return the stored temp file
   * @throws IOException if the file cannot be renamed
   */
  private DotTempFile storedDownload(final String id, final File tempFile) throws IOException {
    final DotTempFile stored = new DotTempFile(id, tempFile);
    if (!isAllowedDownloadName(stored.fileName, stored.mimeType)) {
      SecurityLogger.logInfo(this.getClass(), "Discarded a download by URL: the detected content type does not match the file name's extension");
      throw new IllegalArgumentException("The downloaded content does not match the file name's extension");
    }
    // DotTempFile may already have renamed an image after its detected type; the file now lives under stored.fileName
    final Path current = new File(tempFile.getParentFile(), stored.fileName).toPath();
    if (UtilMethods.isSet(UtilMethods.getFileExtension(stored.fileName))) {
      // describe the file under its final name, so the response reports the stored file and its length
      return stored.fileName.equals(tempFile.getName()) ? stored : new DotTempFile(id, current.toFile());
    }
    final Path named = current.resolveSibling(stored.fileName + StringPool.PERIOD + extensionImpliedBy(mediaTypeOf(stored.mimeType)));
    Files.move(current, named);
    return new DotTempFile(id, named.toFile());
  }

  /**
   * Tells whether a user may ask the server to download a file by URL. Nobody may without a
   * user. When {@code TEMP_RESOURCE_BY_URL_ADMIN_ONLY} is on, only administrators may; otherwise
   * every user the request admitted may, the anonymous user included when
   * {@code TEMP_RESOURCE_ALLOW_ANONYMOUS} lets anonymous requests in.
   *
   * @param user the user making the request, or {@code null} when there is none
   * @return {@code true} when the user may download files by URL
   */
  public boolean isUrlDownloadPermitted(final User user) {
    return isUrlDownloadPermitted(user, Config.getBooleanProperty(TEMP_RESOURCE_BY_URL_ADMIN_ONLY, false));
  }

  /**
   * The rule behind {@link #isUrlDownloadPermitted(User)}, with the setting passed in.
   *
   * @param user      the user making the request, or {@code null} when there is none
   * @param adminOnly whether downloads by URL are restricted to administrators
   * @return {@code true} when the user may download files by URL
   */
  @VisibleForTesting
  static boolean isUrlDownloadPermitted(final User user, final boolean adminOnly) {
    if (user == null) {
      return false;
    }
    return !adminOnly || user.isAdmin();
  }

  /**
   * The check that runs before a download by URL is attempted: a name whose extension is not on
   * {@code TEMP_RESOURCE_BY_URL_ALLOWED_EXTENSIONS} (by default
   * {@link #DEFAULT_URL_DOWNLOAD_EXTENSIONS}) is refused without contacting the remote server.
   * A name with no extension passes here and is settled by
   * {@link #isAllowedDownloadName(String, String)} once the response's content type is known.
   *
   * @param fileName the name the file would be stored under
   * @return {@code false} when the name can be refused before the download
   */
  public boolean isAllowedDownloadName(final String fileName) {
    return UtilMethods.isSet(fileName)
            && (!UtilMethods.isSet(UtilMethods.getFileExtension(fileName))
            || isAllowedDownloadName(fileName, null, configuredDownloadExtensions()));
  }

  /**
   * Tells whether a file downloaded by URL may be stored under the given name once the remote
   * server has answered: its extension must be on {@code TEMP_RESOURCE_BY_URL_ALLOWED_EXTENSIONS}
   * (by default {@link #DEFAULT_URL_DOWNLOAD_EXTENSIONS}), and the content type, declared by the
   * remote server or detected from the content, must agree with that extension when it says
   * anything. A name with no extension is accepted only when the content type names an allowed
   * kind of file. The download applies this rule twice, to the declared and to the detected type,
   * and stores a file whose name had no extension under the extension the detected type implies.
   *
   * @param fileName    the name the file would be stored under
   * @param contentType the content type, or {@code null} when none is known
   * @return {@code true} when the file may be stored under that name
   */
  public boolean isAllowedDownloadName(final String fileName, final String contentType) {
    return isAllowedDownloadName(fileName, contentType, configuredDownloadExtensions());
  }

  /**
   * The rule behind {@link #isAllowedDownloadName(String, String)}, with the allowed extensions
   * passed in. A content type that says nothing about the body, such as
   * {@code application/octet-stream}, is accepted for a name with an allowed extension. A web
   * page or script content type is never accepted. For an extension with a known family (images,
   * PDF, plain text) the content type must then belong to that family; any other extension
   * accepts the remaining content types. A name with no extension is accepted only when the
   * content type's subtype is itself an allowed extension (for instance {@code image/jpeg} for
   * {@code jpeg}).
   *
   * @param fileName          the name the file would be stored under
   * @param contentType       the declared content type, or {@code null}
   * @param allowedExtensions the allowed extensions, lower-cased and without their leading dot
   * @return {@code true} when the file may be stored under that name
   */
  @VisibleForTesting
  static boolean isAllowedDownloadName(final String fileName, final String contentType,
                                       final List<String> allowedExtensions) {
    if (!UtilMethods.isSet(fileName)) {
      return false;
    }
    final String mediaType = mediaTypeOf(contentType);
    if (NEVER_ACCEPTED_CONTENT_TYPES.contains(mediaType)) {
      return false;
    }
    final String extension = UtilMethods.getFileExtension(fileName);
    if (!UtilMethods.isSet(extension)) {
      return allowedExtensions.contains(extensionImpliedBy(mediaType));
    }
    if (!allowedExtensions.contains(extension)) {
      return false;
    }
    if (mediaType.isEmpty() || UNSPECIFIC_CONTENT_TYPES.contains(mediaType)) {
      return true;
    }
    final String family = CONTENT_TYPE_FAMILY_BY_EXTENSION.get(extension);
    return family == null || mediaType.startsWith(family);
  }

  /**
   * The media type of a content type value: the first value, without its parameters, trimmed
   * and lower-cased. Empty when there is none.
   */
  private static String mediaTypeOf(final String contentType) {
    return UtilMethods.isSet(contentType)
            ? contentType.split("[;,]")[0].trim().toLowerCase(Locale.ROOT) : "";
  }

  /**
   * The extension a media type implies: its subtype, up to any {@code +} suffix, so
   * {@code image/png} gives {@code png} and {@code application/pdf} gives {@code pdf}. Empty
   * when the media type has no subtype.
   */
  private static String extensionImpliedBy(final String mediaType) {
    final int slash = mediaType.indexOf('/');
    if (slash < 0 || slash == mediaType.length() - 1) {
      return "";
    }
    final String subtype = mediaType.substring(slash + 1);
    final int plus = subtype.indexOf('+');
    return plus < 0 ? subtype : subtype.substring(0, plus);
  }

  /**
   * The configured allowed extensions, or the default list when none is configured.
   */
  private static List<String> configuredDownloadExtensions() {
    return parseExtensions(Config.getStringArrayProperty(TEMP_RESOURCE_BY_URL_ALLOWED_EXTENSIONS, null));
  }

  /**
   * Turns a configured list into extensions: entries are trimmed, lower-cased and stripped of a
   * leading dot, blank entries are skipped, and a list that names no extension means the default.
   *
   * @param configured the configured entries, or {@code null} when the setting is unset
   * @return the allowed extensions
   */
  @VisibleForTesting
  static List<String> parseExtensions(final String[] configured) {
    if (configured == null || configured.length == 0) {
      return DEFAULT_URL_DOWNLOAD_EXTENSIONS;
    }
    final List<String> extensions = new ArrayList<>(configured.length);
    for (final String entry : configured) {
      if (UtilMethods.isSet(entry)) {
        final String extension = entry.trim().toLowerCase(Locale.ROOT);
        final String bare = extension.startsWith(StringPool.PERIOD) ? extension.substring(1) : extension;
        if (!bare.isEmpty()) {
          extensions.add(bare);
        }
      }
    }
    return extensions.isEmpty() ? DEFAULT_URL_DOWNLOAD_EXTENSIONS : extensions;
  }

  /**
   * The {@code Content-Type} among the response headers, or {@code null} when there is none.
   */
  private static String contentTypeOf(final Header[] headers) {
    if (headers == null) {
      return null;
    }
    for (final Header header : headers) {
      if ("Content-Type".equalsIgnoreCase(header.getName())) {
        return header.getValue();
      }
    }
    return null;
  }

  /**
   * Tells whether the server may fetch a URL a caller supplied. The URL must use http or https
   * and must not point at a loopback, link-local, private or otherwise internal address, unless
   * {@code ALLOW_ACCESS_TO_PRIVATE_SUBNETS} opens those ranges. The host name is resolved for
   * the check, so {@code localhost} and names that resolve to internal addresses are refused as
   * well. No request is made to the URL.
   *
   * @param url the URL the caller asked the server to fetch
   * @return {@code true} when the server may contact the URL
   */
  public boolean isAllowedRemoteUrl(final URL url) {

    if (null == url) {
      return false;
    }
    final String protocol = url.getProtocol();
    if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
      Logger.error(this, "URL does not starts with http or https");
      return false;
    }
    if (IPUtils.isIpPrivateSubnet(url.getHost()) && !allowAccessToPrivateSubnets.get()) {
      SecurityLogger.logInfo(this.getClass(), "Refused to fetch a URL whose host '" + url.getHost()
              + "' resolves to a private or internal address");
      return false;
    }
    return true;
  }

  /**
   * This method receives a URL and checks that it starts with http or https and that the server
   * may contact it (see {@link #isAllowedRemoteUrl(URL)}). Only then it makes a request to the
   * URL: the URL is valid when that request returns 200, and invalid on any other response.
   *
   * <p>The probe reads at most {@link #URL_PROBE_MAX_BYTES} of the body and discards them; the
   * status decides, however long the body is. The probe keeps its own circuit breaker, so a
   * probe cut off at the limit never counts against the URL's breaker for the download.</p>
   *
   * @param url the URL to validate
   * @return boolean if the url is valid or not
   */
  public boolean validUrl(final String url) {

    final URL parsedUrl;
    try {
      parsedUrl = new URL(url);
    } catch (MalformedURLException e) {
      Logger.error(this, "Invalid URL: " + e.getMessage());
      return false;
    }
    if (!isAllowedRemoteUrl(parsedUrl)) {
      return false;
    }
    final CircuitBreakerUrl urlGetter = CircuitBreakerUrl.builder().setMethod(Method.GET).setUrl(url)
            .setMaxResponseBytes(URL_PROBE_MAX_BYTES)
            .setCircuitBreaker(new CircuitBreaker().withFailureThreshold(Integer.MAX_VALUE))
            .build();
    try {
      urlGetter.doOut(OutputStream.nullOutputStream());
    } catch (CircuitBreakerUrl.ResponseSizeLimitExceededException e) {
      return e.getStatusCode() == 200;
    } catch (IOException | BadRequestException e) {//If response is not 200, CircuitBreakerUrl throws BadRequestException
      return false;
    }
    return urlGetter.response() == 200;
  }

  /**
   * The name a file downloaded by URL is stored under: the requested name when one is given,
   * otherwise the last segment of the URL path when it carries an extension, otherwise a random
   * name. Only the file name part is kept.
   *
   * @param desiredName the name the caller asked for, or {@code null}
   * @param url         the URL being downloaded
   * @return the sanitized file name
   */
  public String resolveDownloadFileName(final String desiredName, final URL url) {
    final String path=(url!=null &&   url.getPath().contains(StringPool.PERIOD))? url.getPath() : UUIDGenerator.shorty();
    final String tryFileName = (desiredName!=null) 
        ? desiredName 
            : path.indexOf(StringPool.FORWARD_SLASH) > -1
              ? path.substring(path.lastIndexOf(StringPool.FORWARD_SLASH) + 1, path.length())
              : path;
    return FileUtil.sanitizeFileName(tryFileName);
  }
  
  
  
  
  private File createTempPermissionFile(final File parentFolder, final List<String> incomingAccessingList) {
    List<String> accessingList = new ArrayList<>(incomingAccessingList);
    accessingList.removeIf(Objects::isNull);
    parentFolder.mkdirs();
    final File file = new File(parentFolder, WHO_CAN_USE_TEMP_FILE);
    try {
      new ObjectMapper().writeValue(file, accessingList);
    } catch (IOException e) {
      throw new DotStateException(e.getMessage(), e);
    }
    return parentFolder;

  }

  private Optional<DotTempFile> getTempFile(final String tempFileId) {

    if (tempFileId == null || !tempFileId.startsWith(TEMP_RESOURCE_PREFIX)) {
      return Optional.empty();
    }

    final int tempResourceMaxAgeSeconds = Config.getIntProperty(TEMP_RESOURCE_MAX_AGE_SECONDS, 1800);
    final File testFile = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + File.separator + tempFileId);
    

    final File tempFile = testFile.isDirectory() ? Try.of(() -> com.liferay.util.FileUtil.listFilesRecursively(testFile, tempFileFilter).stream().filter(d->d.isFile()).findFirst().get()).getOrNull() : testFile;

    if (tempFile != null && tempFile.exists()
        && tempFile.lastModified() + (tempResourceMaxAgeSeconds * 1000) > System.currentTimeMillis()) {
      
      return Optional.of(new DotTempFile(tempFileId, tempFile));

    }

    Logger.error(this,"Temp File does not exists or TTL of the file already expired");
    return Optional.empty();

  }

  private boolean canUseTempFile(final List<String> incomingAccessingList, final DotTempFile dotTempFile) {
    final File tempFile = dotTempFile.file;
    final List<String> accessingList = new ArrayList<>(incomingAccessingList);
    accessingList.removeIf(Objects::isNull);
    if (tempFile == null || !tempFile.exists() || accessingList == null) {
      return false;
    }

    final File file = (new File(tempFile, WHO_CAN_USE_TEMP_FILE).exists()) ? new File(tempFile, WHO_CAN_USE_TEMP_FILE)
        : new File(tempFile.getParentFile(), WHO_CAN_USE_TEMP_FILE);

    try {
      final List<String> perms = (file.exists()) ? new ObjectMapper().readValue(file, List.class) : ImmutableList.of();
      return !Collections.disjoint(perms, accessingList);
    } catch (IOException e) {
      throw new DotStateException(e.getMessage(), e);
    }
  }

  /**
   * Optionally retreives the Temp Resource. The temp resource will only be returned if 1) the
   * accessingList contains a value that is also contained whoCanUse.tmp file that was created when
   * the temp resource was written and 2) that the file modification time on the temp resource is
   * newer than the value configured by TEMP_RESOURCE_MAX_AGE_SECONDS, which defaults to 30m.
   * 
   * @param accessingList
   * @param tempFileId
   * @return
   */
  public Optional<DotTempFile> getTempFile(final List<String> accessingList, final String tempFileId) {
    Optional<DotTempFile> tempFile = getTempFile(tempFileId);
    if (tempFile.isPresent() && canUseTempFile(accessingList, tempFile.get())) {
      return tempFile;
    }
    return Optional.empty();
  }

  /**
   * Optionally retreives the Temp Resource using the request. The temp
   * resource will only be returned if 1) the fingerprint or sessionId is contained in the whoCanUse.tmp
   * file that was created when the temp resource was written and 2) that the file modification time
   * on the temp resource is newer than the value configured by TEMP_RESOURCE_MAX_AGE_SECONDS, which
   * defaults to 30m.
   * 
   * @param request
   * @param tempFileId
   * @return
   */
  public Optional<DotTempFile> getTempFile(final HttpServletRequest request, final String tempFileId) {
    final String anon = Try.of(() -> APILocator.getUserAPI().getAnonymousUser().getUserId()).getOrElse("anonymous");
    final User user = PortalUtil.getUser(request);
    if(!UtilMethods.isSet(user)){
      request.setAttribute(WebKeys.USER_ID,anon);
    }
    final String sessionId = request!=null && request.getSession(false)!=null ? request.getSession().getId() : null;
    final String requestFingerprint = this.getRequestFingerprint(request);
    
    
    final List<String> accessingList = new ArrayList<>();
    if (user != null && user.getUserId() != null && !user.getUserId().equals(anon)) {
      accessingList.add(user.getUserId());
    }
    accessingList.add(requestFingerprint);
    accessingList.add(sessionId);

    return getTempFile(accessingList, tempFileId);
  }

  /**
   * returns if a temp resource exits
   * 
   * @param tempFileId
   * @return
   */
  public boolean isTempResource(final String tempFileId) {
    return getTempFile(tempFileId).isPresent();
  }

  private final FileFilter tempFileFilter = new FileFilter() {
    @Override
    public boolean accept(final File pathname) {
      return !pathname.getName().equalsIgnoreCase(WHO_CAN_USE_TEMP_FILE) &&
             !pathname.getName().startsWith(".") &&
             !pathname.getName().endsWith(META_TMP)

          ;
    }
  };
  
  public String getRequestFingerprint(final HttpServletRequest request) {
    
    final List<String> uniqList = new ArrayList<>();
    uniqList.add(request.getHeader("User-Agent"));
    uniqList.add(request.getHeader("Host"));
    uniqList.add(request.getHeader("Accept-Language"));
    uniqList.add(request.getHeader("Accept-Encoding"));
    uniqList.add(request.getHeader("X-Forwarded-For"));
    uniqList.add(request.getRemoteHost());
    uniqList.add(String.valueOf(request.isSecure()));
    uniqList.add(request.getRemoteAddr());
    
    final String incomingReferer = (request.getHeader("Origin")!=null) ? request.getHeader("Origin") :  request.getHeader("referer");
    uniqList.add(new SecurityUtils().hostFromUrl(incomingReferer));

    
    uniqList.removeIf(Objects::isNull);
    if(uniqList.size() < 4) {
      Logger.warn(this.getClass(),"request does not have enough params to create a valid fingerprint");
      uniqList.add(UUIDGenerator.generateUuid());
    }
    
    final String fingerPrint = String.join(" , ", uniqList);
    Logger.info(this.getClass(), "Unique browser fingerprint: " + fingerPrint);
    return Encryptor.digest(fingerPrint);
    
    
  }

  /**
   * given a file We explore the parent folder to figure out if it represents a temp resource
   * and if it is so.. We return it.
   * @param file
   * @return
   */
  public Optional<String> getTempResourceId(final File file){
    try {
      final String tempResourceId = file.toPath().getParent().getFileName().toString();
      if (isTempResource(tempResourceId)) {
        return Optional.of(tempResourceId);
      }
    }catch (Exception e){
      Logger.warnAndDebug(TempFileAPI.class, e.getMessage(), e);
    }
    return Optional.empty();
  }

}
