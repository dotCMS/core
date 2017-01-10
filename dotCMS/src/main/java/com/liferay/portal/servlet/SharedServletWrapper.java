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

package com.liferay.portal.servlet;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.CookieUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;

/**
 * <a href="SharedServletWrapper.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.11 $
 *
 */
public class SharedServletWrapper extends GenericServlet {

	public void init(ServletConfig sc) throws ServletException {
		super.init(sc);

		ServletContext ctx = getServletContext();

		_servletContextName = StringUtil.replace(
			ctx.getServletContextName(), StringPool.SPACE,
			StringPool.UNDERLINE);

		_servletClass = sc.getInitParameter("servlet-class");

		ClassLoader contextClassLoader =
			Thread.currentThread().getContextClassLoader();

		try {
			_servletInstance =
				(Servlet)contextClassLoader.loadClass(
					_servletClass).newInstance();
		}
		catch (ClassNotFoundException cnfe) {
			throw new ServletException(cnfe.getMessage());
		}
		catch (IllegalAccessException iae) {
			throw new ServletException(iae.getMessage());
		}
		catch (InstantiationException ie) {
			throw new ServletException(ie.getMessage());
		}

		if (_servletInstance instanceof HttpServlet) {
			_httpServletInstance =
				(HttpServlet)_servletInstance;

			_httpServletInstance.init(sc);
		}
		else {
			_servletInstance.init(sc);
		}
	}

	public void service(ServletRequest req, ServletResponse res)
		throws IOException, ServletException {

		if ((_httpServletInstance == null) ||
			(GetterUtil.getBoolean(PropsUtil.get(PropsUtil.TCK_URL)))) {

			_servletInstance.service(req, res);
		}
		else {
			HttpServletRequest httpReq = (HttpServletRequest)req;

			String sharedSessionId = CookieUtil.get(
				httpReq.getCookies(), CookieKeys.SHARED_SESSION_ID);

			_log.debug("Shared session id is " + sharedSessionId);

			HttpSession portalSes = null;

			if (sharedSessionId != null) {
				portalSes = SharedSessionPool.get(sharedSessionId);
			}

			HttpSession portletSes = httpReq.getSession();

			if (portalSes == null) {
				portalSes = portletSes;
			}
			else {
				try {
					portalSes.getCreationTime();
				}
				catch (IllegalStateException ise) {
					_log.debug("Removing session from pool");

					SharedSessionPool.remove(sharedSessionId);

					portalSes = portletSes;
				}
			}

			HttpServletRequest sharedReq =
				new SharedServletRequest(httpReq, portalSes);

			sharedReq.setAttribute(
				WebKeys.SERVLET_CONTEXT_NAME, _servletContextName);

			_httpServletInstance.service(sharedReq, res);

			sharedReq.removeAttribute(WebKeys.SERVLET_CONTEXT_NAME);
		}
	}

	public void destroy() {
		_servletInstance.destroy();
	}

	private static final Log _log =
		LogFactory.getLog(SharedServletWrapper.class);

	private String _servletContextName;
	private String _servletClass;
	private Servlet _servletInstance;
	private HttpServlet _httpServletInstance;

}