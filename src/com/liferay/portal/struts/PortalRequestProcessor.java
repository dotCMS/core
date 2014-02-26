/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.struts;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.WindowState;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.config.ForwardConfig;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PreviewFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.user.business.UserUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.PortletActiveException;
import com.liferay.portal.RequiredRoleException;
import com.liferay.portal.SystemException;
import com.liferay.portal.UserActiveException;
import com.liferay.portal.auth.AutoLogin;
import com.liferay.portal.auth.AutoLoginException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.ejb.PortletPreferencesManagerUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserTracker;
import com.liferay.portal.model.UserTrackerPath;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.WebAppPool;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.CachePortlet;
import com.liferay.portlet.RenderRequestImpl;
import com.liferay.portlet.RenderResponseImpl;
import com.liferay.util.CollectionFactory;
import com.liferay.util.Encryptor;
import com.liferay.util.GetterUtil;
import com.liferay.util.Http;
import com.liferay.util.InstancePool;
import com.liferay.util.ObjectValuePair;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;
import com.liferay.util.Validator;
import com.liferay.util.servlet.SessionErrors;
import com.liferay.util.servlet.UploadServletRequest;
import com.oroad.stxx.plugin.StxxTilesRequestProcessor;

/**
 * <a href="PortalRequestProcessor.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Jorge Ferrer
 * @version $Revision: 1.53 $
 *
 */
public class PortalRequestProcessor extends StxxTilesRequestProcessor {

	public PortalRequestProcessor() {
		_publicPaths = CollectionFactory.getHashSet();

		_publicPaths.add(_PATH_C);
		_publicPaths.add(_PATH_PORTAL_PUBLIC_ABOUT);
		_publicPaths.add(_PATH_PORTAL_PUBLIC_DISCLAIMER);
		_publicPaths.add(_PATH_PORTAL_PUBLIC_J_LOGIN);
		_publicPaths.add(_PATH_PORTAL_PUBLIC_LOGIN);
		_publicPaths.add(_PATH_PORTAL_PUBLIC_TCK);

		for (int i = 0;; i++) {
			String publicPath = PropsUtil.get(PropsUtil.AUTH_PUBLIC_PATH + i);

			if (publicPath == null) {
				break;
			}
			else {
				_publicPaths.add(publicPath);
			}
		}
	}

	public void process(HttpServletRequest req, HttpServletResponse res)
		throws IOException, ServletException {

		String path = super.processPath(req, res);

		ActionMapping mapping =
			(ActionMapping)moduleConfig.findActionConfig(path);

		if ((mapping == null) && !path.startsWith(_PATH_WSRP)) {
			String lastPath = getLastPath(req).toString();


			if(PortalUtil.getUserId(req) == null){
				res.sendRedirect("/html/portal/login.jsp?r=" + System.currentTimeMillis());
			}else{

				if(lastPath != null){
					if( lastPath.contains("?")){
						lastPath = lastPath + "&r=" + System.currentTimeMillis();
					}
					else{
						lastPath = lastPath + "?r=" + System.currentTimeMillis();
					}
				}
				res.sendRedirect(lastPath);
			}
			return;
		}

		super.process(req, res);
	}

	protected void callParentDoForward(
			String uri, HttpServletRequest req, HttpServletResponse res)
		throws IOException, ServletException {

		super.doForward(uri, req, res);
	}

	protected void doForward(
			String uri, HttpServletRequest req, HttpServletResponse res)
		throws IOException, ServletException {

		StrutsUtil.forward(uri, getServletContext(), req, res);
	}

	protected HttpServletRequest callParentProcessMultipart(
		HttpServletRequest req) {

		return super.processMultipart(req);
	}

	protected HttpServletRequest processMultipart(HttpServletRequest req) {

		// Bug in Struts makes it incompatible with
		// com.oreilly.servlet.MultipartRequest

		return req;
	}

	protected String callParentProcessPath(
			HttpServletRequest req, HttpServletResponse res)
		throws IOException {

		return super.processPath(req, res);
	}

	protected String processPath(
			HttpServletRequest req, HttpServletResponse res)
		throws IOException {

		String path = super.processPath(req, res);

		HttpSession ses = req.getSession();

		// Current users

		String companyId = PortalUtil.getCompanyId(req);

		Map currentUsers = (Map)WebAppPool.get(
			companyId, WebKeys.CURRENT_USERS);

		UserTracker userTracker = (UserTracker)currentUsers.get(ses.getId());

		if ((userTracker != null) &&
			((path != null) &&
				(!path.equals(_PATH_C)) &&
				(path.indexOf(_PATH_J_SECURITY_CHECK) == -1) &&
				(path.indexOf(_PATH_PORTAL_PROTECTED) == -1))) {

			/*Map parameterMap = null;

			if (req instanceof UploadServletRequest) {
				UploadServletRequest uploadServletReq =
					(UploadServletRequest)req;

				parameterMap = uploadServletReq.getRequest().getParameterMap();
			}
			else {
				parameterMap = req.getParameterMap();
			}*/

			StringBuffer fullPathSB = new StringBuffer();

			fullPathSB.append(path);
			//fullPathSB.append(Http.parameterMapToString(parameterMap));
			fullPathSB.append(StringPool.QUESTION);
			fullPathSB.append(req.getQueryString());

			userTracker.addPath(
				new UserTrackerPath(
					userTracker.getUserTrackerId(),
					userTracker.getUserTrackerId(), fullPathSB.toString(),
					new Date()));
		}

		String userId = req.getRemoteUser();

		User user = null;

		try {
			user = PortalUtil.getUser(req);
		}
		catch (Exception e) {
		}

		// Last path

		if ((path != null) &&
			(path.equals(_PATH_PORTAL_LAYOUT) ||
			 path.equals(_PATH_PORTAL_PUBLIC_LAYOUT))) {

			String strutsAction = req.getParameter("_2_struts_action");

			if (strutsAction == null ||
				!strutsAction.equals(_PATH_MY_ACCOUNT_CREATE_ACCOUNT)) {

				Map parameterMap = new LinkedHashMap();
				if (req instanceof UploadServletRequest) {
					UploadServletRequest uploadServletReq =
						(UploadServletRequest)req;

					Enumeration paramNames = uploadServletReq.getParameterNames();
					while (paramNames.hasMoreElements()) {
						String paramName = (String) paramNames.nextElement();
						if(uploadServletReq.getFile(paramName) == null) {
							parameterMap.put(paramName, uploadServletReq.getParameterValues(paramName));
						}
					}
				}
				else {
					parameterMap = req.getParameterMap();
				}

				ses.setAttribute(
					WebKeys.LAST_PATH,
					new ObjectValuePair(
						path, new LinkedHashMap(parameterMap)));
			}
		}

		// Auto login

		if ((userId == null) && (ses.getAttribute("j_username") == null)) {
			try {
				String[] autoLogins = PropsUtil.getArray(
					PropsUtil.AUTO_LOGIN_HOOKS);

				for (int i = 0; i < autoLogins.length; i++) {
					AutoLogin autoLogin =
						(AutoLogin)InstancePool.get(autoLogins[i]);

					String[] credentials = autoLogin.login(req, res);

					if ((credentials != null) && (credentials.length == 3)) {
						String jUsername = credentials[0];
						String jPassword = credentials[1];
						boolean encPwd = GetterUtil.getBoolean(credentials[2]);

						if (Validator.isNotNull(jUsername) &&
							Validator.isNotNull(jPassword)) {

							ses.setAttribute("j_username", jUsername);

							// Not having access to the unencrypted password
							// will not allow you to connect to external
							// resources that require it (mail server)

							if (encPwd) {
								ses.setAttribute("j_password", jPassword);
							}
							else {
								ses.setAttribute("j_password",
									Encryptor.digest(jPassword));

								ses.setAttribute(
									WebKeys.USER_PASSWORD, jPassword);
							}

							return _PATH_PORTAL_PUBLIC_LOGIN;
						}
					}
				}

			}
			catch (AutoLoginException ale) {
				Logger.error(this,ale.getMessage(),ale);
			}
		}

		// Authenticated users can always log out
		if ((userId != null || user != null) && (path != null) &&
			(path.equals(_PATH_PORTAL_LOGOUT))) {

			return _PATH_PORTAL_LOGOUT;
		}

		if ((userId != null || user != null) && (path != null) &&
				(path.equals(_PATH_PORTAL_LOGOUT_AS))) {

				return _PATH_PORTAL_LOGOUT_AS;
		}

		if ((userId != null || user != null) && (path != null) &&
				(path.equals(_PATH_PORTAL_LOGIN_AS))) {

				return _PATH_PORTAL_LOGIN_AS;
		}

		// Authenticated users can always agree to terms of use

		if ((userId != null || user != null) && (path != null) &&
			(path.equals(_PATH_PORTAL_UPDATE_TERMS_OF_USE))) {

			return _PATH_PORTAL_UPDATE_TERMS_OF_USE;
		}

		// Authenticated users must still exist in the system

		if ((userId != null) && (user == null)) {
			return _PATH_PORTAL_LOGOUT;
		}

		// Authenticated users must agree to Terms of Use

		if ((user != null) && (!user.isAgreedToTermsOfUse())) {
			boolean termsOfUseRequired = GetterUtil.get(
				PropsUtil.get(PropsUtil.TERMS_OF_USE_REQUIRED), true);

			if (termsOfUseRequired) {
				return _PATH_PORTAL_TERMS_OF_USE;
			}
		}

		// Authenticated users must be active

		if ((user != null) && (!user.isActive())) {
			SessionErrors.add(req, UserActiveException.class.getName());

			return _PATH_PORTAL_ERROR;
		}

		// Authenticated users may not be allowed to have simultaneous logins

		boolean simultaenousLogins = GetterUtil.get(
			PropsUtil.get(PropsUtil.AUTH_SIMULTANEOUS_LOGINS), true);

		if (!simultaenousLogins) {
			Boolean staleSession =
				(Boolean)ses.getAttribute(WebKeys.STALE_SESSION);

			if ((user != null) &&
				(staleSession != null && staleSession.booleanValue() == true)) {

				return _PATH_PORTAL_ERROR;
			}
		}

		// Authenticated users must have a current password

		if ((user != null) && (user.isPasswordReset())) {
			return _PATH_PORTAL_CHANGE_PASSWORD;
		}

		// Users must sign in

		if (!isPublicPath(path)) {
			if (user == null) {
				SessionErrors.add(req, PrincipalException.class.getName());

				res.sendRedirect("/c/portal_public/login?r="  +System.currentTimeMillis());
				return null;
			}
		}


		ActionMapping mapping =
			(ActionMapping)moduleConfig.findActionConfig(path);

		if (path.startsWith(_PATH_WSRP)) {
			path = _PATH_WSRP;
		}
		else {
			path = mapping.getPath();
		}

		// Authenticated users must have at least one role

		if (user != null) {
			try {

				// FIX

				if (false) {
					SessionErrors.add(
						req, RequiredRoleException.class.getName());

					return _PATH_PORTAL_ERROR;
				}
			}
			catch (Exception e) {
				Logger.error(this,e.getMessage(),e);
			}
		}

		// Authenticated users must have proper roles

		if (isPortletPath(path)) {
			try {
				String strutsPath = path.substring(
					1, path.lastIndexOf(StringPool.SLASH));

				Portlet portlet = PortletManagerUtil.getPortletByStrutsPath(
					companyId, strutsPath);

				if (portlet != null && portlet.isActive()) {
					defineObjects(req, res, portlet);
				}
			}
			catch (Exception e) {
				req.setAttribute(PageContext.EXCEPTION, e);

				path = _PATH_COMMON_ERROR;
			}
		}


		/**
		 * Authenticated users must have access to at least one host,
		 * and can only view a host to which they have read permissions
		 *
		 */

		if(user == null){
			return path;
		}

		PermissionAPI pAPI = APILocator.getPermissionAPI();
		HostAPI hostAPI = APILocator.getHostAPI();

		Host host = null;
		// if someone is changing hosts as a parameter, check permissions
		if(UtilMethods.isSet(req.getParameter("host_id"))){
			try{
				host = hostAPI.find(req.getParameter("host_id"), user, false);

				if(host != null && pAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, false)){
					req.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, req.getParameter("host_id"));
					UserUtil.setLastHost(user, host);
				}
				else{
					UserUtil.setLastHost(user, null);
					req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
					Logger.info(this.getClass(), "user " + user.getUserId() + " does not have permission to host " +req.getParameter("host_id"));
				}
			}
			catch(Exception e){
				req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
				Logger.error(this.getClass(), "user " + user.getUserId() + " does not have permission to host " +req.getParameter("host_id"));
			}
		}
		// else check if the user as permissions to the host in their session (can change, login as, etc..)
		else if(UtilMethods.isSet(req.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)) ){
			String x = (String) req.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
			try{
				host = hostAPI.find(x, user, false);
				if(host != null && pAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, false)){
					req.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, host.getIdentifier());
				}
				else{
					Logger.error(this.getClass(), "user " + user.getUserId() + " does not have permission to host " +req.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID));
					req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
					UserUtil.setLastHost(user,null);
				}
			}
			catch(Exception e){
				Logger.error(this.getClass(), "user " + user.getUserId() + " does not have permission to host " +req.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID));
				req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);

			}
		}
		// try to get the last host from the user record
		else{

				try {
					host = UserUtil.getLastHost(user);
					req.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, host.getIdentifier());
				} catch (DotDataException e) {
					Logger.debug(this.getClass(), e.toString());
				} catch (DotSecurityException e) {
					Logger.warn(this.getClass(), "User " + user.getUserId() + " does not have permissions to host " + e.toString());
				}

		}


		// finally, if user does not have a host
		// set to default host if it is not in the session
		if(!UtilMethods.isSet(req.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID))){
			try{
				List<Host> list = hostAPI.getHostsWithPermission(PermissionAPI.PERMISSION_READ, user, false);
				host = null;
				for(Host h : list) {
					if(!h.isSystemHost()){
						if(h.isDefault()){
							host = h;
							break;
						}
						else{
							host = h;
						}
					}
				}
				if(host != null){
					req.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, host.getIdentifier());
				}
			}
			catch(Exception e){
				Logger.error(this.getClass(), "User " + userId + " does not have access to any host");
			}
			if(!UtilMethods.isSet(req.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID))){
				SessionErrors.add(req, "User does not have access to any host");
				req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
			}
		}



		if(req.getSession().getAttribute("LOGIN_TO_EDIT_MODE") != null){
			try{
				Identifier sendMeTo =(Identifier) req.getSession().getAttribute("LOGIN_TO_EDIT_MODE") ;
				req.getSession().removeAttribute("LOGIN_TO_EDIT_MODE");
				Layout layout = null;
				List<Layout> userLayouts;

				userLayouts = APILocator.getLayoutAPI().loadLayoutsForUser(user);
				if(userLayouts != null && userLayouts.size() > 0){
					layout = userLayouts.get(0);
					req.setAttribute(WebKeys.LAYOUT, layout);
				}
				if(layout == null){
					res.sendRedirect("/c/portal/logout?referer=/&r="  +System.currentTimeMillis());
					return null;
				}

				PreviewFactory.setVelocityURLS(req, layout);

				req.getSession().setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, true);
				req.getSession().setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
				req.getSession().setAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION, "true");

				if(host != null || sendMeTo.getHostId().equals(host.getInode())){
					res.sendRedirect(SecurityUtils.stripReferer(req, sendMeTo.getURI() + "?host_id=" +host.getIdentifier() +"&r="  +System.currentTimeMillis()));
					return null;
				}

			}
			catch(Exception e){
				Logger.error(this.getClass(), "Error redirecting after login" + e);
			}

			res.sendRedirect("/");
			return null;
		}



		return path;
	}

	protected boolean callParentProcessRoles(
			HttpServletRequest req, HttpServletResponse res,
			ActionMapping mapping)
		throws IOException, ServletException {

		return super.processRoles(req, res, mapping);
	}

	protected boolean processRoles(
			HttpServletRequest req, HttpServletResponse res,
			ActionMapping mapping)
		throws IOException, ServletException {

		boolean authorized = true;

		User user = null;

		try {
			user = PortalUtil.getUser(req);
		}
		catch (Exception e) {
		}

		String path = mapping.getPath();

		if ((user != null) && isPortletPath(path)) {
			try {

				// Authenticated users can always log out

				if (path.equals(_PATH_PORTAL_LOGOUT)) {
					return true;
				}

				if (path.equals(_PATH_PORTAL_LOGOUT_AS)) {
					return true;
				}

				// Check roles

				String strutsPath = path.substring(
					1, path.lastIndexOf(StringPool.SLASH));

				Portlet portlet = PortletManagerUtil.getPortletByStrutsPath(
					user.getCompanyId(), strutsPath);

				if (portlet != null && portlet.isActive()) {
//					if (!RoleLocalManagerUtil.hasRoles(user.getUserId(), portlet.getRolesArray())) {
//
//						throw new PrincipalException();
//					}
				}
				else if (portlet != null && !portlet.isActive()) {
					SessionErrors.add(
						req, PortletActiveException.class.getName());

					authorized = false;
				}
			}
			catch (Exception e) {
				SessionErrors.add(req, PrincipalException.class.getName());

				authorized = false;
			}
		}

		if (!authorized) {
			ForwardConfig forwardConfig =
				mapping.findForward(_PATH_PORTAL_ERROR);

			processForwardConfig(req, res, forwardConfig);

			return false;
		}
		else {
			return true;
		}
	}

	protected StringBuffer getLastPath(HttpServletRequest req) {
		HttpSession ses = req.getSession();

		String userId = PortalUtil.getUserId(req);

		String portalURL = PortalUtil.getPortalURL(req, req.isSecure());
		String ctxPath = (String)ses.getAttribute(WebKeys.CTX_PATH);

		StringBuffer defaultPathSB = new StringBuffer();

		defaultPathSB.append(portalURL);
		defaultPathSB.append(ctxPath);

		if (userId != null) {
			defaultPathSB.append(_PATH_PORTAL_LAYOUT);
		}
		else {
			defaultPathSB.append(_PATH_PORTAL_PUBLIC_LAYOUT);
		}

		boolean forwardByLastPath = GetterUtil.get(
			PropsUtil.get(PropsUtil.AUTH_FORWARD_BY_LAST_PATH), true);

		if (!forwardByLastPath) {
			return defaultPathSB;
		}

		ObjectValuePair ovp =
			(ObjectValuePair)ses.getAttribute(WebKeys.LAST_PATH);

		if (ovp == null) {
			return defaultPathSB;
		}

		String lastPath = (String)ovp.getKey();

		if (userId != null) {
			lastPath = StringUtil.replace(
				lastPath, _PATH_PORTAL_PUBLIC_LAYOUT, _PATH_PORTAL_LAYOUT);
		}

		Map parameterMap = (Map)ovp.getValue();

		ActionMapping mapping =
			(ActionMapping)moduleConfig.findActionConfig(lastPath);

		if (parameterMap == null) {
			return defaultPathSB;
		}

		StringBuffer lastPathSB = new StringBuffer();

		lastPathSB.append(portalURL);
		lastPathSB.append(ctxPath);
		lastPathSB.append(lastPath);
		lastPathSB.append(Http.parameterMapToStringNoEncode(parameterMap));

		return lastPathSB;
	}

	protected boolean isPortletPath(String path) {
		if ((path != null) &&
			(!path.equals(_PATH_C)) &&
			(!path.startsWith(_PATH_COMMON)) &&
			(path.indexOf(_PATH_J_SECURITY_CHECK) == -1) &&
			(!path.startsWith(_PATH_PORTAL)) &&
			(!path.startsWith(_PATH_PORTAL_PUBLIC)) &&
			(!path.startsWith(_PATH_WSRP))) {

			return true;
		}
		else {
			return false;
		}
	}

	protected boolean isPublicPath(String path) {
		if ((path != null) &&
			(_publicPaths.contains(path)) ||
			(path.startsWith(_PATH_COMMON)) ||
			(path.startsWith(_PATH_WSRP))) {

			return true;
		}
		else {
			return false;
		}
	}

	protected void defineObjects(
			HttpServletRequest req, HttpServletResponse res, Portlet portlet)
		throws PortalException, PortletException, SystemException {

		String portletId = portlet.getPortletId();

		ServletContext ctx =
			(ServletContext)req.getAttribute(WebKeys.CTX);

		CachePortlet cachePortlet = PortalUtil.getPortletInstance(portlet, ctx);

		PortletPreferences portletPrefs =
			PortletPreferencesManagerUtil.getPreferences(
				portlet.getCompanyId(),
				PortalUtil.getPortletPreferencesPK(req, portletId));

		PortletConfig portletConfig =
			PortalUtil.getPortletConfig(portlet, ctx);
		PortletContext portletCtx =
			portletConfig.getPortletContext();

		RenderRequestImpl renderRequest = new RenderRequestImpl(
			req, portlet, cachePortlet, portletCtx, WindowState.MAXIMIZED,
			PortletMode.VIEW, portletPrefs);

		RenderResponseImpl renderResponse = new RenderResponseImpl(
			renderRequest, res, portletId, portlet.getCompanyId());

		renderRequest.defineObjects(portletConfig, renderResponse);
	}

	private static String _PATH_C = "/c";

	private static String _PATH_COMMON = "/common";
	private static String _PATH_COMMON_ERROR = "/common/error";

	private static String _PATH_J_SECURITY_CHECK = "/j_security_check";

	private static String _PATH_MY_ACCOUNT_CREATE_ACCOUNT =
		"/my_account/create_account";

	private static String _PATH_PORTAL = "/portal";
	private static String _PATH_PORTAL_ADD_PAGE = "/portal/add_page";
	private static String _PATH_PORTAL_CHANGE_PASSWORD =
		"/portal/change_password";
	private static String _PATH_PORTAL_ERROR = Constants.PORTAL_ERROR;
	private static String _PATH_PORTAL_LAST_PATH = "/portal/last_path";
	private static String _PATH_PORTAL_LAYOUT = "/portal/layout";
	private static String _PATH_PORTAL_LOGOUT = "/portal/logout";
	private static String _PATH_PORTAL_LOGOUT_AS = "/portal/logout_as";
	private static String _PATH_PORTAL_LOGIN_AS = "/portal/login_as";
	private static String _PATH_PORTAL_PROTECTED = "/portal/protected";
	private static String _PATH_PORTAL_TERMS_OF_USE = "/portal/terms_of_use";
	private static String _PATH_PORTAL_UPDATE_TERMS_OF_USE =
		"/portal/update_terms_of_use";

	private static String _PATH_PORTAL_PUBLIC = "/portal_public";
	private static String _PATH_PORTAL_PUBLIC_ABOUT = "/portal_public/about";
	private static String _PATH_PORTAL_PUBLIC_DISCLAIMER =
		"/portal_public/disclaimer";
	private static String _PATH_PORTAL_PUBLIC_J_LOGIN =
		"/portal_public/j_login";
	private static String _PATH_PORTAL_PUBLIC_LAYOUT = "/portal_public/layout";
	private static String _PATH_PORTAL_PUBLIC_LOGIN = "/portal_public/login";
	private static String _PATH_PORTAL_PUBLIC_TCK = "/portal_public/tck";

	private static String _PATH_WSRP = "/wsrp";

	private Set _publicPaths;

}