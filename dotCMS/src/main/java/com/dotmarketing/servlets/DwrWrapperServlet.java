package com.dotmarketing.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

public class DwrWrapperServlet extends com.dotcms.repackage.org.directwebremoting.servlet.DwrServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dotcms.repackage.org.directwebremoting.servlet.DwrServlet#doGet(javax
	 * .servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

		String uri = req.getRequestURI();
		if (!uri.startsWith("/dwr/engine") && !uri.startsWith("/dwr/util")) {
			try {
				validateUser(req);
			} catch ( DotSecurityException e) {
				resp.setHeader("Cache-Control", "no-cache");
				return;

			}
		}
		super.doGet(req, resp);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		String uri = req.getRequestURI();
		if (!uri.startsWith("/dwr/engine") && !uri.startsWith("/dwr/util")) {
			try {
				validateUser(req);
			} catch ( DotSecurityException e) {
				resp.setHeader("Cache-Control", "no-cache");
				return;
			}
		}

		super.doPost(req, resp);
	}

	private void validateUser(HttpServletRequest request) throws  DotSecurityException {

        User loggedInUser = PortalUtil.getUser(request);
        // lock down to users with access to Users portlet
        String remoteIp = request.getRemoteHost();
        String userId = "[not logged in]";
        if (loggedInUser != null && loggedInUser.getUserId() != null) {
            userId = loggedInUser.getUserId();
        }
        
        if (loggedInUser == null) {
            String referer = request.getHeader("Referer");
            if (referer == null || !referer.contains("login.jsp")) {
                SecurityLogger.logInfo(this.getClass(), "unauthorized attempt to call to DWR by user " + userId + " from page "
                                + referer + " from ip" + remoteIp);
            }
            throw new DotSecurityException("not authorized");
        }
		
        if (!loggedInUser.isBackendUser()) {
            SecurityLogger.logInfo(this.getClass(), "unauthorized attempt to call " + request.getRequestURL() + " by user " + userId + " from ip" + remoteIp);
            throw new DotSecurityException("not authorized");
            
        }


	}


}
