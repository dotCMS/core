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

package com.liferay.portlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.portlet.RenderRequest;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

/**
 * <a href="PortletServletRequest.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.7 $
 *
 */
public class PortletServletRequest extends HttpServletRequestWrapper {

	public PortletServletRequest(HttpServletRequest req,
								 RenderRequest renderRequest, String pathInfo,
								 String queryString, String requestURI,
								 String servletPath) {

		super(req);

		_ses = req.getSession();
		_renderRequest = renderRequest;
		_pathInfo = pathInfo;
		_queryString = queryString;
		_requestURI = requestURI;
		_servletPath = servletPath;
	}

	public Object getAttribute(String name) {
		if (name == null) {
			return super.getAttribute(name);
		}

		/*if (name.equals("javax.servlet.include.context_path")) {
			return getContextPath();
		}
		else if (name.equals("javax.servlet.include.path_info")) {
			return getPathInfo();
		}
		else if (name.equals("javax.servlet.include.query_string")) {
			return getQueryString();
		}
		else if (name.equals("javax.servlet.include.request_uri")) {
			return getRequestURI();
		}
		else if (name.equals("javax.servlet.include.servlet_path")) {
			return getServletPath();
		}*/

		if (name.equals("javax.servlet.include.query_string")) {
			return getQueryString();
		}
		else {
			return super.getAttribute(name);
		}
	}

	public String getCharacterEncoding() {
		return null;
	}

	public void setCharacterEncoding(String env)
		throws UnsupportedEncodingException {
	}

	public int getContentLength() {
		return 0;
	}

	public String getContentType() {
		return null;
	}

	public String getContextPath() {
		return _renderRequest.getContextPath();
	}

	public ServletInputStream getInputStream() throws IOException{
		return null;
	}

	public String getPathInfo() {
		return _pathInfo;
	}

	public String getProtocol() {
		return null;
	}

	public String getQueryString() {
		return _queryString;
	}

	public BufferedReader getReader() throws IOException {
		return null;
	}

	public String getRemoteAddr() {
		return null;
	}

	public String getRemoteHost() {
		return null;
	}

	public String getRealPath(String path) {
		return null;
	}

	public String getRequestURI() {
		return _requestURI;
	}

	public StringBuffer getRequestURL() {
		return null;
	}

	public String getServletPath() {
		return _servletPath;
	}

	public HttpSession getSession() {
		return _ses;
	}

	public boolean isUserInRole(String roleId) {
		String remoteUser = getRemoteUser();

		if (remoteUser == null) {
			return false;
		}
		else {
			try {
				boolean userInRole =
					APILocator.getRoleAPI().doesUserHaveRole(APILocator.getUserAPI().loadUserById(remoteUser, APILocator.getUserAPI().getSystemUser(), true), roleId);

				if (userInRole) {
					return userInRole;
				}
				else {

					try {
						APILocator.getRoleAPI().loadRoleById(roleId);

						return false;
					}
					catch (Exception e) {
						Logger.error(this, e.getMessage(), e);
					}
				}
			}
			catch (Exception e) {
				Logger.error(this,e.getMessage(),e);
			}

			return super.isUserInRole(roleId);
		}
	}

	private HttpSession _ses;
	private RenderRequest _renderRequest;
	private String _pathInfo;
	private String _queryString;
	private String _requestURI;
	private String _servletPath;

}