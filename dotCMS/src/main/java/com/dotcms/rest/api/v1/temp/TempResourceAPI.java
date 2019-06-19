package com.dotcms.rest.api.v1.temp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;

import io.vavr.Tuple2;
import io.vavr.control.Try;

public class TempResourceAPI {

  private static final String WHO_CAN_USE_TEMP_FILE = "whoCanUse.tmp";

  public static final String TEMP_PREFIX = "temp_";

  /**
   * Returns a TempFile of a unique id and file handle that can be used to write and access a temp file.
   * The userId and uniqueKey will be written to the "allowList" and can be used to retreive the temp
   * resource in other requests
   * 
   * @param incomingFileName
   * @param user
   * @param uniqueKey
   * @return
   * @throws DotSecurityException
   */
  public DotTempFile createTempFile(final String incomingFileName, final User user, final String uniqueKey)
      throws DotSecurityException {
    final String anon = Try.of(() -> APILocator.getUserAPI().getAnonymousUser().getUserId()).getOrElse("anonymous");

    final List<String> allowList = new ArrayList<>();
    if (user != null && user.getUserId() != null && !user.getUserId().equals(anon)) {
      allowList.add(user.getUserId());
    }
    if (uniqueKey != null) {
      allowList.add(uniqueKey);
    }
    return createTempFile(incomingFileName, allowList);
  }

  /**
   * Returns a TempFile of a unique id and file handle that can be used to write and access a temp file.
   * The allowList param acts like a permission and can include a userId, a session id and/or just a
   * unique key that is writen to a whoCanUse.tmp file. When retrieving a temp resource, you will need
   * to pass in another list of ids that will be checked against the values in whoCanUse.tmp and will
   * only return the resource if any of the acceessingIds were in the whoCanUse.tmp original list
   * 
   * @param incomingFileName
   * @param allowList
   * @return
   * @throws DotSecurityException
   */
  public DotTempFile createTempFile(final String incomingFileName, final List<String> allowList) throws DotSecurityException {

    final String tempFileId = TEMP_PREFIX + UUIDGenerator.shorty();
    if (incomingFileName == null) {
      throw new DotRuntimeException("Unable to create temp file without a name");
    }
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

  private File createTempPermissionFile(final File parentFolder, List<String> allowList) {

    parentFolder.mkdirs();
    final File file = new File(parentFolder, WHO_CAN_USE_TEMP_FILE);
    try {
      new ObjectMapper().writeValue(file, allowList);
    } catch (IOException e) {
      throw new DotStateException(e.getMessage(), e);
    }
    return parentFolder;

  }

  private Optional<File> getTempFile(final String tempFileId) {

    if (!tempFileId.startsWith(TEMP_PREFIX)) {
      return Optional.empty();
    }

    int tempResourceMaxAgeSeconds = Config.getIntProperty("TEMP_RESOURCE_MAX_AGE_SECONDS", 1800);
    final File testFile = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + File.separator + tempFileId);
    final File tempFile = testFile.isDirectory() ? Try.of(() -> testFile.listFiles(tempFileFilter)[0]).getOrNull() : testFile;

    if (tempFile.exists() && !tempFile.isDirectory()
        && tempFile.lastModified() + (tempResourceMaxAgeSeconds * 1000) > System.currentTimeMillis()) {
      return Optional.of(tempFile);
    }
    return Optional.empty();

  }

  private boolean canUseTempFile(List<String> accessingList, final File tempFile) {
    if (tempFile == null || !tempFile.exists() || accessingList==null) {
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
  public Optional<File> getTempFile(List<String> accessingList, final String tempFileId) {
    Optional<File> tempFile = getTempFile(tempFileId);
    if (tempFile.isPresent() && canUseTempFile(accessingList, tempFile.get())) {
      return tempFile;
    }
    return Optional.empty();
  }

  /**
   * Optionally retreives the Temp Resource using the userId and/or a unique key (sessionId). The temp
   * resource will only be returned if 1) the userId or uniqueKey is contained in the whoCanUse.tmp
   * file that was created when the temp resource was written and 2) that the file modification time
   * on the temp resource is newer than the value configured by TEMP_RESOURCE_MAX_AGE_SECONDS, which
   * defaults to 30m.
   * 
   * @param user
   * @param uniqueKey
   * @param tempFileId
   * @return
   */
  public Optional<File> getTempFile(final User user, final String uniqueKey, final String tempFileId) {
    final String anon = Try.of(() -> APILocator.getUserAPI().getAnonymousUser().getUserId()).getOrElse("anonymous");

    final List<String> accessingList = new ArrayList<>();
    if (user != null && user.getUserId() != null && !user.getUserId().equals(anon)) {
      accessingList.add(user.getUserId());
    }
    if (uniqueKey != null) {
      accessingList.add(uniqueKey);
    }
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
    public boolean accept(File pathname) {
      return !(pathname.getName().equalsIgnoreCase(WHO_CAN_USE_TEMP_FILE));
    }
  };

}
