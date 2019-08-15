package com.dotcms.rest.api.v1.temp;

import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.util.WebKeys;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.Encryptor;
import com.liferay.util.StringPool;

import io.vavr.control.Try;

public class TempFileAPI {

  public static final String TEMP_RESOURCE_MAX_AGE_SECONDS = "TEMP_RESOURCE_MAX_AGE_SECONDS";
  public static final String TEMP_RESOURCE_ALLOW_ANONYMOUS = "TEMP_RESOURCE_ALLOW_ANONYMOUS";
  public static final String TEMP_RESOURCE_ALLOW_NO_REFERER = "TEMP_RESOURCE_ALLOW_NO_REFERER";
  public static final String TEMP_RESOURCE_ENABLED = "TEMP_RESOURCE_ENABLED";
  public static final String TEMP_RESOURCE_PREFIX = "temp_";

  private static final String WHO_CAN_USE_TEMP_FILE = "whoCanUse.tmp";

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

    return new DotTempFile(tempFileId, tempFile);
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

    try (final OutputStream out = new FileOutputStream(tempFile)) {
      int read = 0;
      byte[] bytes = new byte[4096];
      while ((read = inputStream.read(bytes)) != -1) {
        out.write(bytes, 0, read);
      }
      return dotTempFile;
    } catch (Exception e) {
      throw new DotRuntimeException("unable to create tmpFile:" + dotTempFile, e);
    } finally {
      CloseUtils.closeQuietly(inputStream);
    }
  }

  /**
   * Takes a url, downloads it and the returns the resulting file as tempFile with a unique id and
   * file handle that can be used to access the temp file. The request will be used to create a fingerprint
   * that will be written to the "allowList" and can be used to retreive the temp resource in other requests
   * 
   * @param incomingFileName
   * @param request
   * @return
   * @throws DotSecurityException
   */
  public DotTempFile createTempFileFromUrl(final String incomingFileName,final HttpServletRequest request, final URL url, final int timeoutSeconds)
      throws DotSecurityException {

    if (!validUrl(url)) {
      throw new DotSecurityException("Invalid url attempted for tempFile:" + url);
    }



    final String fileName = resolveFileName(incomingFileName, url);

    final DotTempFile dotTempFile = createEmptyTempFile(fileName, request);
    final String tempFileId = dotTempFile.id;
    final File tempFile = dotTempFile.file;

    try (OutputStream fileOut = new FileOutputStream(tempFile)) {
      final CircuitBreakerUrl urlGetter =
          CircuitBreakerUrl.builder().setMethod(Method.GET).setUrl(url.toString()).setTimeout(timeoutSeconds * 1000).build();
      urlGetter.doOut(fileOut);
      if (urlGetter.response() != 200) {
        throw new DoesNotExistException("Url not found. Got a " + urlGetter.response());
      }
    } catch (Exception e) {
      Logger.warnAndDebug(this.getClass(), "unable to save temp file:" + tempFileId, e);
      throw new DotRuntimeException(e);
    }
    return dotTempFile;

  }

  private boolean validUrl(final URL url) {
    return Try.of(() -> url.toString().toLowerCase().startsWith("http://") || url.toString().toLowerCase().startsWith("https://"))
        .getOrElse(false);
  }

  private String resolveFileName(final String desiredName, final URL url) {
    final String path=(url!=null)? url.getPath() : UUIDGenerator.shorty();
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

    if (tempFile.exists()
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
             !pathname.getName().startsWith(".")
          
          ;
    }
  };
  
  public String getRequestFingerprint(final HttpServletRequest request) {
    
    final List<String> uniqList = new ArrayList<String>();
    uniqList.add("User-Agent:" + request.getHeader("User-Agent"));
    uniqList.add("Host:" + request.getHeader("Host"));
    uniqList.add("Accept-Language:" + request.getHeader("Accept-Language"));

    uniqList.add("Accept-Encoding:" + request.getHeader("Accept-Encoding"));
    uniqList.add("X-Forwarded-For:" + request.getHeader("X-Forwarded-For"));
    uniqList.add("getRemoteHost:" + request.getRemoteHost());
    uniqList.add("isSecure:" + request.isSecure());
    uniqList.add("getRemoteAddr:" + request.getRemoteAddr());
    
    final String incomingReferer = (request.getHeader("Origin")!=null) ? request.getHeader("Origin") :  request.getHeader("referer");
    uniqList.add("incomingReferer:" + new SecurityUtils().hostFromUrl(incomingReferer));

    uniqList.add("userId:" + PortalUtil.getUserId(request));

    if(request.getSession(false)!=null) {
      uniqList.add("getSession:" + request.getSession().getId());
    }
    uniqList.removeIf(Objects::isNull);
    if(uniqList.isEmpty()) {
      throw new DotRuntimeException("Invalid request - no unique identifiers passed in");
    }
    
    final String fingerPrint = String.join(" , ", uniqList);
    Logger.info(this.getClass(), "Unique browser fingerprint: " + fingerPrint);
    return Encryptor.digest(fingerPrint);
    
    
  }
  

}
