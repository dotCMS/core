package com.dotmarketing.servlets;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.repackage.org.directwebremoting.Container;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

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
			} catch (PortalException | SystemException | DotSecurityException e) {
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
			} catch (PortalException | SystemException | DotSecurityException e) {
				resp.setHeader("Cache-Control", "no-cache");
				return;
			}
		}

		super.doPost(req, resp);
	}

	private void validateUser(HttpServletRequest request) throws PortalException, SystemException, DotSecurityException {

		User loggedInUser = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
		// lock down to users with access to Users portlet
		String remoteIp = request.getRemoteHost();
		String userId = "[not logged in]";
		if (loggedInUser != null && loggedInUser.getUserId() != null) {
			userId = loggedInUser.getUserId();
		}
		if (loggedInUser == null) {
			String referer = request.getHeader("Referer");
			if(referer==null || !referer.contains("login.jsp")){
				SecurityLogger.logInfo(this.getClass(), "unauthorized attempt to call to DWR by user " + userId + " from page " + referer+ " from ip" + remoteIp );
			}
			throw new DotSecurityException("not authorized");
		}

	}

	@Override
	protected void configureContainer(Container arg0, ServletConfig arg1) throws ServletException, IOException {
		// TODO Auto-generated method stub
		super.configureContainer(arg0, arg1);
	}

	@Override
	protected Container createContainer(ServletConfig servletConfig) throws ServletException {
		// TODO Auto-generated method stub
		return super.createContainer(servletConfig);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		super.destroy();
	}

	@Override
	public Container getContainer() {
		// TODO Auto-generated method stub
		return super.getContainer();
	}

	@Override
	public void init(ServletConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		super.init(arg0);
	}

}
