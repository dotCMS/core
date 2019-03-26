package com.dotmarketing.util;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class HostUtil {

  public static String hostNameUtil(ActionRequest req, User user)
      throws DotDataException, DotSecurityException {

    ActionRequestImpl reqImpl = (ActionRequestImpl) req;
    HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
    HttpSession session = httpReq.getSession();

    String hostId =
        (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);

    Host h = null;

    h = APILocator.getHostAPI().find(hostId, user, false);

    return h.getTitle() != null ? h.getTitle() : "default";
  }
}
