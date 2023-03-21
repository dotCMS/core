package com.dotcms.rest.api.v1.temp;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.SecurityUtils;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.Encryptor;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.dotcms.storage.FileMetadataAPIImpl.META_TMP;

/**
 * This API allows you to create temporary files in dotCMS. This API is very useful for uploading resources that can be
 * safely deleted after a given amount of time, and also used by the File Asset edit mode when a new file is being
 * uploaded to the repository.
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
  public static final String MAX_FILE_LENGTH_PARAM = "maxFileLength";
  
  public static final String TEMP_RESOURCE_ENABLED = "TEMP_RESOURCE_ENABLED";
  public static final String TEMP_RESOURCE_PREFIX = "temp_";

  private static final String WHO_CAN_USE_TEMP_FILE = "whoCanUse.tmp";
  private static final String TEMP_RESOURCE_BY_URL_ADMIN_ONLY="TEMP_RESOURCE_BY_URL_ADMIN_ONLY";

  /**
   * Returns an empty TempFile of a unique id and file handle that can be used to write and access a temp file. The
   * request will be used to create a fingerprint that will be written to the "allowList" and can be used to retrieve
   * the temp resource in other requests.
   *
   * @param incomingFileName The name of the Temporary File.
   * @param request          The current instance of the {@link HttpServletRequest}
   *
   * @return The empty {@link DotTempFile}.
   *
   * @throws DotSecurityException An error occurred when creating the Temporary File.
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
    final String tempFileId = TEMP_RESOURCE_PREFIX + UUIDGenerator.shorty();
    
    final String tempFileUri = File.separator + tempFileId + File.separator + incomingFileName;
    final File tempFile = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + tempFileUri);
    final File tempFolder = tempFile.getParentFile();

    if (!tempFolder.mkdirs()) {
      throw new DotRuntimeException("Unable to create temp directory:" + tempFolder);
    }

    final String absFilePath = FileUtil.getAbsolutlePath(tempFile.getPath());
    final String absTmpPath = FileUtil.getAbsolutlePath(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary());
    if (!absFilePath.startsWith(absTmpPath)) {
      SecurityLogger.logInfo(this.getClass(), () -> "Attempted file upload outside of temp folder: " + absFilePath);
      throw new DotRuntimeException("Invalid file upload");
    }
    createTempPermissionFile(tempFolder, allowList);
    SecurityLogger.logInfo(this.getClass(),"Temp File Created with id: " + tempFileId + ", uploaded by userId: " + user.getUserId());
    return new DotTempFile(tempFileId, tempFile);
  }

  /**
   * This method takes a request and based upon it returns the max file size that can be uploaded, based on the user
   * uploading. Anonymous users can have smaller file size limitations than authenticated users. A return of -1 means
   * unlimited.
   *
   * @param request The current instance of the {@link HttpServletRequest}
   *
   * @return The maximum file size allowed by dotCMS.
   */
  @VisibleForTesting
  public long maxFileSize(final HttpServletRequest request) {
    final long requestedMax = ConversionUtils.toLongFromByteCountHumanDisplaySize(request.getParameter(MAX_FILE_LENGTH_PARAM), -1L);
    final long systemMax    =  Config.getLongProperty(TEMP_RESOURCE_MAX_FILE_SIZE, -1L);
    final long anonMax      =  Config.getLongProperty(TEMP_RESOURCE_MAX_FILE_SIZE_ANONYMOUS, -1L);
    final boolean isAnon    = PortalUtil.getUserId(request) == null || UserAPI.CMS_ANON_USER_ID.equals(PortalUtil.getUserId(request));
    
    final List<Long> longs = (isAnon) ? Lists.newArrayList(requestedMax,systemMax,anonMax) : Lists.newArrayList(requestedMax,systemMax);
    longs.removeIf(i-> i < 0);
    Collections.sort(longs); 
    return longs.isEmpty() ? -1L : longs.get(0);
  }

  /**
   * Writes an InputStream to a temp file and returns the tempFile with a unique id and file handle that can be used to
   * access the temp file. The request will be used to create a fingerprint that will be written to the "allowList" and
   * can be used to retrieve the temp resource in other requests.
   *
   * @param incomingFileName The name of the Temporary File.
   * @param request          The current instance of the {@link HttpServletRequest}
   * @param inputStream      The content of the Temporary File.
   *
   * @return The new {@link DotTempFile}
   *
   * @throws DotSecurityException An error occurred when creating the Temporary File.
   */
  public DotTempFile createTempFile(final String incomingFileName,final HttpServletRequest request, final InputStream inputStream)
      throws DotSecurityException {
    final DotTempFile dotTempFile = this.createEmptyTempFile(incomingFileName, request);
    this.writeFile(request, dotTempFile, inputStream);
    return dotTempFile;
  }

  /**
   * Takes a URL, downloads it and the returns the resulting file as tempFile with a unique id and file handle that can
   * be used to access the temp file. The request will be used to create a fingerprint that will be written to the
   * "allowList" and can be used to retrieve the temp resource in other requests
   *
   * @param incomingFileName The name of the Temporary File, if required.
   * @param request          The current instance of the {@link HttpServletRequest}
   * @param url              The {@link URL} pointing to the file that must be retrieved.
   * @param timeoutSeconds   The specified timeout for reading the files via the URL.
   * @param maxLength        The maximum allowed size of the file being retrieved.
   *
   * @return The {@link DotTempFile} with the file specified via the URL.
   *
   * @throws DotSecurityException An error occurred when creating the Temporary File.
   * @throws IOException          An error occurred retrieving the contents of the file via URL.
   */
  public DotTempFile createTempFileFromUrl(final String incomingFileName,
          final HttpServletRequest request, final URL url, final int timeoutSeconds,
          final long maxLength)
          throws DotSecurityException, IOException {

      final String fileName = resolveFileName(incomingFileName, url);

      final DotTempFile dotTempFile = createEmptyTempFile(fileName, request);
      final File tempFile = dotTempFile.file;

      
      final boolean tempFilesByUrlAdminOnly = Config
                      .getBooleanProperty(TEMP_RESOURCE_BY_URL_ADMIN_ONLY, false);
      
      
      /**
       * If url requested is on a private subnet, block by default
       */
      if(IPUtils.isIpPrivateSubnet(url.getHost())) {
          throw new DotRuntimeException("Unable to load file by url:" + url);
      }
      
            
      /**
       * by adding the source IP give visibility to the 
       * remote server of who initiatied the reuqest
       */
      final String sourceIpAddress = request.getRemoteAddr();
      final String finalUrl = url.toString().contains("?") ? url.toString() + "&sourceIp=" + sourceIpAddress :  url.toString() + "?sourceIp=" + sourceIpAddress ;
      
      
      
      /**
       * Only allow admins to use the URL functionality
       */
      User user = PortalUtil.getUser(request);
      if(user == null || tempFilesByUrlAdminOnly && !user.isAdmin()) {
          throw new DotRuntimeException("Only Admins can import a file by URL");
      }
      
      try(final OutputStream out = new BoundedOutputStream(maxFileSize(request),
                      Files.newOutputStream(tempFile.toPath()))){

              final CircuitBreakerUrl urlGetter =
                      CircuitBreakerUrl.builder().setMethod(Method.GET).setUrl(finalUrl)
                              .setTimeout(timeoutSeconds * 1000).build();
        
              urlGetter.doOut(out);
      }

      return dotTempFile;

  }

  /**
   * Updates the contents of an existing Temporary File. If the ID of such a file equals the word {@code "new"} or if
   * the ID doesn't exist anymore, a new temporary file with the specified content will be returned instead.
   *
   * @param request          The current instance of the {@link HttpServletRequest}.
   * @param tempFileId       The ID of the Temporary File.
   * @param incomingFileName The actual file name of the Temporary File. This is used only when the original
   *                         Temporary File ID doesn't exist.
   * @param inputStream      The new content of the Temporary File.
   *
   * @return The {@link DotTempFile} representing the specified file, or a new one if the ID doesn't exist.
   *
   * @throws DotSecurityException An error occurred when creating Temporary File.
   */
  public DotTempFile upsertTempFile(final HttpServletRequest request, final String tempFileId, final String incomingFileName, final InputStream inputStream) throws DotSecurityException {
    if ("new".equalsIgnoreCase(tempFileId)) {
      return this.createTempFile(incomingFileName, request, inputStream);
    }
    final Optional<DotTempFile> dotTempFile = this.getTempFile(tempFileId);
    if (dotTempFile.isEmpty()) {
      return this.createTempFile(incomingFileName, request, inputStream);
    }
    this.writeFile(request, dotTempFile.get(), inputStream);
    return dotTempFile.get();
  }

  /**
   * This method receives a URL and checks if starts with http or https, and also makes a request to the URL and if
   * returns 200 the URL is valid, if returns any other response will be false.
   *
   * @param url The specified URL
   *
   * @return boolean if the url is valid or not
   */
  public boolean validUrl(final String url) {

    if(!(url.toLowerCase().startsWith("http://") ||
            url.toLowerCase().startsWith("https://"))){
      Logger.error(this, String.format("URL [ %s ] does not start with http or https", url));
      return false;
    }
    try {
      final CircuitBreakerUrl urlGetter =
              CircuitBreakerUrl.builder().setMethod(Method.GET).setUrl(url).build();
      urlGetter.doString();
    } catch (IOException | BadRequestException e) {//If response is not 200, CircuitBreakerUrl throws BadRequestException
      return false;
    }

    return true;
  }

  /**
   * Resolves the name of the Temporary File based on either the specified desired name, or by retrieving it from the
   * URL.
   *
   * @param desiredName The specified desired file name.
   * @param url         The URL that contains the file name.
   *
   * @return The name of the Temporary File.
   */
  private String resolveFileName(final String desiredName, final URL url) {
    final String path=(url!=null)? url.getPath() : UUIDGenerator.shorty();
    final String tryFileName = (desiredName!=null) 
        ? desiredName 
            : path.indexOf(StringPool.FORWARD_SLASH) > -1
              ? path.substring(path.lastIndexOf(StringPool.FORWARD_SLASH) + 1, path.length())
              : path;
    return FileUtil.sanitizeFileName(tryFileName);
  }

  /**
   * Creates a {@code whoCanUse.tmp} file for every empty Temporary File. Such a file contains the following
   * information:
   * <ul>
   *     <li>The User ID who created the Temporary File.</li>
   *     <li>The Session ID.</li>
   *     <li>The request's fingerprint.</li>
   * </ul>
   *
   * @param parentFolder          The folder that this TMP file will be created in.
   * @param incomingAccessingList The list of properties that will be included in the file.
   *
   * @return The temporary permissions file.
   */
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

  /**
   * Returns the Temporary File based on its ID. If such a file doesn't exist or if it was created more than 30 minutes
   * ago -- of the value specified by {@link #TEMP_RESOURCE_MAX_AGE_SECONDS} -- then an empty {@link DotTempFile} object
   * will be returned instead.
   *
   * @param tempFileId The ID of the Temporary File.
   *
   * @return The {@link DotTempFile} matching the specified ID.
   */
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
    Logger.error(this, String.format("Temp File '%s' does not exist or its TTL already expired", tempFileId));
    return Optional.empty();
  }

  /**
   * Determines whether the incoming Access List has "use" permissions over a given Temporary File.
   *
   * @param incomingAccessingList The Access List being checked.
   * @param dotTempFile           The {@link DotTempFile} object.
   *
   * @return If the incoming list matches the existing permission list, returne {@code true}.
   */
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
      final List<String> perms = (file.exists()) ? new ObjectMapper().readValue(file, List.class) : List.of();
      return !Collections.disjoint(perms, accessingList);
    } catch (IOException e) {
      throw new DotStateException(e.getMessage(), e);
    }
  }

  /**
   * Optionally retrieves the Temp Resource. The temp resource will only be returned if:
   * <ol>
   *     <li>The {@code accessingList} contains a value that is also contained in the {@code whoCanUse.tmp} file that
   *     was created when the temp resource was written.</li>
   *     <li>And that the file modification time on the temp resource is newer than the value configured by
   *     {@link #TEMP_RESOURCE_MAX_AGE_SECONDS}, which defaults to 30m.</li>
   * </ol>
   *
   * @param accessingList The incoming Access List.
   * @param tempFileId    The ID of the Temporary File.
   *
   * @return The optional with the {@link DotTempFile} if the Access List matched the existing one. Otherwise, an empty
   * optional will be returned.
   */
  public Optional<DotTempFile> getTempFile(final List<String> accessingList, final String tempFileId) {
    Optional<DotTempFile> tempFile = getTempFile(tempFileId);
    if (tempFile.isPresent() && canUseTempFile(accessingList, tempFile.get())) {
      return tempFile;
    }
    return Optional.empty();
  }

  /**
   * Optionally retrieves the Temp Resource using the request. The temp resource will only be returned if:
   * <ol>
   *     <li>The fingerprint or sessionId is contained in the {@code whoCanUse.tmp} file that was created when the temp
   *     resource was written.</li>
   *     <li>And that the file modification time on the temp resource is newer than the value configured by
   *     {@link #TEMP_RESOURCE_MAX_AGE_SECONDS}, which defaults to 30m.</li>
   * </ol>
   *
   * @param request    The current instance of the {@link HttpServletRequest}.
   * @param tempFileId The ID of the Temporary File.
   *
   * @return The optional with the {@link DotTempFile}
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
   * Checks whether the specified Temporary File ID exists or not.
   *
   * @param tempFileId The ID of the Temporary File.
   *
   * @return If the Temporary File exists, returns {@code true}.
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

  /**
   * Generates a String representing the fingerprint of the specified {@link HttpServletRequest}. It takes a specified
   * set of properties from the request and generates a unique identifier for the request.
   *
   * @param request The current instance of the {@link HttpServletRequest}.
   *
   * @return The request's fingerprint.
   */
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
    Logger.debug(this.getClass(), "Unique browser fingerprint: " + fingerPrint);
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

  /**
   * Writes the specified content in the form of an Input Stream to the specified Temporary File.
   *
   * @param request     The current instance of the {@link HttpServletRequest}.
   * @param dotTempFile The {@link DotTempFile} whose content will be overwritten.
   * @param inputStream The new file content as an {@link InputStream}.
   */
  private void writeFile(final HttpServletRequest request, final DotTempFile dotTempFile, final InputStream inputStream) {
    final File tempFile = dotTempFile.file;
    final long maxLength = this.maxFileSize(request);
    try (final OutputStream out = new BoundedOutputStream(maxLength, Files.newOutputStream(tempFile.toPath()))) {
      int read;
      final byte[] bytes = new byte[4096];
      while ((read = inputStream.read(bytes)) != -1) {
        out.write(bytes, 0, read);
      }
    } catch (final IOException e) {
      final String message =
              APILocator.getLanguageAPI().getStringKey(WebAPILocator.getLanguageWebAPI().getLanguage(request), "temp.file.max.file.size.error").replace("{0}", UtilMethods.prettyByteify(maxLength));
      throw new DotStateException(message, e);
    } catch (final Exception e) {
      throw new DotRuntimeException(e.getMessage(), e);
    } finally {
      CloseUtils.closeQuietly(inputStream);
    }
  }

}
