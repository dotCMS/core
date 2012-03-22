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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * <a href="DynamicServletRequest.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.12 $
 *
 */
public class DynamicServletRequest extends HttpServletRequestWrapper {

	public DynamicServletRequest(HttpServletRequest req) {
		this(req, new HashMap(), true);
	}

	public DynamicServletRequest(HttpServletRequest req, Map params) {
		this(req, params, true);
	}

	public DynamicServletRequest(HttpServletRequest req, boolean inherit) {
		this(req, new HashMap(), inherit);
	}

	public DynamicServletRequest(HttpServletRequest req, Map params,
								 boolean inherit) {

		super(req);

		_params = new HashMap();
		_inherit = inherit;

		if (params != null) {
			Iterator itr = params.entrySet().iterator();

			while (itr.hasNext()) {
				Map.Entry entry = (Map.Entry)itr.next();

				_params.put(entry.getKey(), entry.getValue());
			}
		}

		if (_inherit && (req instanceof DynamicServletRequest)) {
			DynamicServletRequest dynamicReq = (DynamicServletRequest)req;

			setRequest(dynamicReq.getRequest());

			params = dynamicReq.getDynamicParameterMap();

			if (params != null) {
				Iterator itr = params.entrySet().iterator();

				while (itr.hasNext()) {
					Map.Entry entry = (Map.Entry)itr.next();

					String name = (String)entry.getKey();
					String[] oldValues = (String[])entry.getValue();

					String[] curValues = (String[])_params.get(name);

					if (curValues == null) {
						_params.put(name, oldValues);
					}
					else {
						String[] newValues =
							new String[oldValues.length + curValues.length];

						System.arraycopy(
							oldValues, 0, newValues, 0, oldValues.length);

						System.arraycopy(
							curValues, 0, newValues, oldValues.length,
							curValues.length);

						_params.put(name, newValues);
					}
				}
			}
		}
	}

	public String getParameter(String name) {
		String[] values = (String[])_params.get(name);

		if (_inherit && (values == null)) {
			return super.getParameter(name);
		}

		if ((values != null) && (values.length > 0)) {
			return values[0];
		}
		else {
			return null;
		}
	}

	public Map getParameterMap() {
		Map map = new HashMap();

		Enumeration enu = getParameterNames();

		while (enu.hasMoreElements()) {
			String s = (String)enu.nextElement();

			map.put(s, getParameterValues(s));
		}

		return map;
	}

	public Enumeration getParameterNames() {
		List names = new ArrayList();

		if (_inherit) {
			Enumeration enu = super.getParameterNames();

			while (enu.hasMoreElements()) {
				names.add(enu.nextElement());
			}
		}

		Iterator i = _params.keySet().iterator();

		while (i.hasNext()) {
			String s = (String)i.next();

			if (!names.contains(s)) {
				names.add(s);
			}
		}

		return Collections.enumeration(names);
	}

	public String[] getParameterValues(String name) {
		String[] values = (String[])_params.get(name);

		if (_inherit && (values == null)) {
			return super.getParameterValues(name);
		}

		return values;
	}

	public void setParameter(String name, String value) {
		_params.put(name, new String[] {value});
	}

	public void setParameterValues(String name, String[] values) {
		_params.put(name, values);
	}

	public Map getDynamicParameterMap() {
		return _params;
	}

	private Map _params;
	private boolean _inherit;

}