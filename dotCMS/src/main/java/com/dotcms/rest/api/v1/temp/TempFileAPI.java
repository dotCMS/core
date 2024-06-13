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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.Encryptor;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
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
  private static final Lazy<Boolean> allowAccessToPrivateSubnets = Lazy.of(()->Config.getBooleanProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", false));
  

  /**
   * Returns an empty TempFile of a unique id and file handle that can be used to write and access a
   * temp file. The request will be used to create a fingerprint that will be written to the "allowList" and can
   * be used to retreive the temp resource in other requests
   * 
   * @param incomingFileName
   * @param request
   * @return
   * @throws DotSecurityException
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
      final boolean tempFilesByUrlAdminOnly = Config
              .getBooleanProperty(TEMP_RESOURCE_BY_URL_ADMIN_ONLY, false);
      // Only allow admins to use the URL functionality
      final User user = PortalUtil.getUser(request);
      if (user == null || tempFilesByUrlAdminOnly && !user.isAdmin()) {
        throw new DotRuntimeException("Only Admin Users can import files by URL via the Temp API.");
      }
      // If url requested is on a private subnet, block by default
      if(IPUtils.isIpPrivateSubnet(url.getHost()) && !Optional.ofNullable(allowAccessToPrivateSubnets.get()).orElse(false)) {
        throw new DotRuntimeException(String.format("Failed to download file by URL: %s as it is in a private subnet", url));
      }
      final String fileName = resolveFileName(incomingFileName, url);
      final DotTempFile dotTempFile = createEmptyTempFile(fileName, request);
      final File tempFile = dotTempFile.file;

      // By adding the source IP, we give visibility to the remote server of who initiated the request
      final String sourceIpAddress = request.getRemoteAddr();
      final String finalUrl = url.toString().contains("?") ? url + "&sourceIp=" + sourceIpAddress :  url + "?sourceIp=" + sourceIpAddress ;
      try(final OutputStream out = new BoundedOutputStream(maxFileSize(request),
                      Files.newOutputStream(tempFile.toPath()))){
        final CircuitBreakerUrl urlGetter =
                CircuitBreakerUrl.builder().setMethod(Method.GET).setUrl(finalUrl)
                        .setTimeout(timeoutSeconds * 1000L).build();
        urlGetter.doOut(out);
      }

      if (dotTempFile.metadata == null && dotTempFile.file.exists()) {

        return new DotTempFile(dotTempFile.id, dotTempFile.file);
      }
      return dotTempFile;
  }

  /**
   * This method receives a URL and checks if starts with http or https,
   * and also makes a request to the URL and if returns 200 the URL is valid,
   * if returns any other response will be false
   * @param url
   * @return boolean if the url is valid or not
   */
  public boolean validUrl(final String url) {

    if(!(url.toLowerCase().startsWith("http://") ||
            url.toLowerCase().startsWith("https://"))){
      Logger.error(this, "URL does not starts with http or https");
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

  private String resolveFileName(final String desiredName, final URL url) {
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
