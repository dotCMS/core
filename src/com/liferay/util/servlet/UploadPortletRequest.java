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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequestWrapper;

/**
 * <a href="UploadPortletRequest.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class UploadPortletRequest extends HttpServletRequestWrapper {

	public UploadPortletRequest(UploadServletRequest req, String namespace) {
		super(req);

		_req = req;
		_namespace = namespace;
	}

	public String getContentType(String name) {
		String contentType = _req.getContentType(_namespace + name);

		if (contentType == null) {
			contentType = _req.getContentType(name);
		}

		return contentType;
	}

	public File getFile(String name) {
		File file = _req.getFile(_namespace + name);

		if (file == null) {
			file = _req.getFile(name);
		}

		return file;
	}

	public String getFileName(String name) {
		String fileName = _req.getFileName(_namespace + name);

		if (fileName == null) {
			fileName = _req.getFileName(name);
		}

		return fileName;
	}

	public String getFullFileName(String name) {
		String fullFileName = _req.getFullFileName(_namespace + name);

		if (fullFileName == null) {
			fullFileName = _req.getFullFileName(name);
		}

		return fullFileName;
	}

	public String getParameter(String name) {
		String parameter = _req.getParameter(_namespace + name);

		if (parameter == null) {
			parameter = _req.getParameter(name);
		}

		return parameter;
	}

	public Map getParameterMap() {
		Map map = new HashMap();

		Enumeration enu = getParameterNames();

		while (enu.hasMoreElements()) {
			String name = (String)enu.nextElement();

			map.put(name, getParameterValues(name));
		}

		return map;
	}

	public Enumeration getParameterNames() {
		List parameterNames = new ArrayList();

		Enumeration enu = _req.getParameterNames();

		while (enu.hasMoreElements()) {
			String name = (String)enu.nextElement();

			if (name.startsWith(_namespace)) {
				parameterNames.add(
					name.substring(_namespace.length(), name.length()));
			}
			else {
				parameterNames.add(name);
			}
		}

		return Collections.enumeration(parameterNames);
	}

	public String[] getParameterValues(String name) {
		String[] parameterValues = _req.getParameterValues(_namespace + name);

		if (parameterValues == null) {
			parameterValues = _req.getParameterValues(name);
		}

		return parameterValues;
	}

	private UploadServletRequest _req;
	private String _namespace;

}