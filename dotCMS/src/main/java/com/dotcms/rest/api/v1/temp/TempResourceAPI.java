package com.dotcms.rest.api.v1.temp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.business.DotStateException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

public class TempResourceAPI {

  private static final String WHO_CAN_USE_TMP_FILE = "whoCanUse.tmp";

  protected File createWhoCanUseTempFile(final HttpServletRequest request, final File parentFolder) {

    final User user = PortalUtil.getUser(request);
    final String sessionId = (request != null && request.getSession() != null) ? request.getSession().getId() : null;

    final List<String> allowList = new ArrayList<>();
    if (user != null && user.getUserId() != null) {
      allowList.add(user.getUserId());
    }
    if (sessionId != null) {
      allowList.add(sessionId);
    }
    parentFolder.mkdirs();
    final File file = new File(parentFolder, WHO_CAN_USE_TMP_FILE);
    try {
      new ObjectMapper().writeValue(file, allowList);
    } catch (IOException e) {
      throw new DotStateException(e.getMessage(), e);
    }
    return parentFolder;

  }

  protected boolean canUseTempFile(final HttpServletRequest request, File tempFile) {

    final User user = PortalUtil.getUser(request);

    final String userId = (user != null && user.getUserId() != null) ? user.getUserId() : null;
    final String sessionId = (request != null && request.getSession() != null) ? request.getSession().getId() : null;

    final File file = (new File(tempFile, WHO_CAN_USE_TMP_FILE).exists()) ? new File(tempFile, WHO_CAN_USE_TMP_FILE) : new File(tempFile.getParentFile(), WHO_CAN_USE_TMP_FILE);

    try {
      final List<String> perms = (file.exists()) ? new ObjectMapper().readValue(file, List.class) : null;
      return perms != null && (perms.contains(userId) || perms.contains(sessionId));
    } catch (IOException e) {
      throw new DotStateException(e.getMessage(), e);
    }
  }
  
  protected Optional<File> getTempFile(final HttpServletRequest request, String tempPath) {
    final File testFile = new File(tempPath);
    if(this.canUseTempFile(request, testFile) && testFile.exists()) {
      return Optional.of(testFile);
    }
    return Optional.empty();
  }


}
