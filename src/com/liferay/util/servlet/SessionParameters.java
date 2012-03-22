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

package com.liferay.util.servlet;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.liferay.util.CollectionFactory;
import com.liferay.util.PwdGenerator;

/**
 * <a href="SessionParameters.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class SessionParameters {

	public static final String KEY = SessionParameters.class.getName();

	// Servlet Request

	public static String get(HttpServletRequest req, String parameter) {
		return get(req.getSession(), parameter);
	}

	public static String get(HttpSession ses, String parameter) {
		Map parameters = _getParameters(ses);

		String newParameter = (String)parameters.get(parameter);

		if (newParameter == null) {
			newParameter =
				PwdGenerator.getPassword(PwdGenerator.KEY3, 10) + "_" +
				parameter;

			parameters.put(parameter, newParameter);
		}

		return newParameter;
	}

	private static Map _getParameters(HttpSession ses) {
		Map parameters = null;

		try {
			parameters = (Map)ses.getAttribute(KEY);

			if (parameters == null) {
				parameters = CollectionFactory.getHashMap();

				ses.setAttribute(KEY, parameters);
			}
		}
		catch (IllegalStateException ise) {
			parameters = CollectionFactory.getHashMap();
		}

		return parameters;
	}

	// Portlet Request

	public static String get(PortletRequest req, String parameter) {
		return get(req.getPortletSession(), parameter);
	}

	public static String get(PortletSession ses, String parameter) {
		Map parameters = _getParameters(ses);

		String newParameter = (String)parameters.get(parameter);

		if (newParameter == null) {
			newParameter = PwdGenerator.getPassword() + "_" + parameter;

			parameters.put(parameter, newParameter);
		}

		return newParameter;
	}

	private static Map _getParameters(PortletSession ses) {
		Map parameters = null;

		try {
			parameters = (Map)ses.getAttribute(KEY);

			if (parameters == null) {
				parameters = new LinkedHashMap();

				ses.setAttribute(KEY, parameters);
			}
		}
		catch (IllegalStateException ise) {
			parameters = new LinkedHashMap();
		}

		return parameters;
	}

}