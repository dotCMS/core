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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import com.liferay.util.CollectionFactory;
import com.liferay.util.ListUtil;

/**
 * <a href="SharedSessionWrapper.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.12 $
 *
 */
public class SharedSessionWrapper implements HttpSession {

	public SharedSessionWrapper(HttpSession ses) {
		this(ses, CollectionFactory.getSyncHashMap());
	}

	public SharedSessionWrapper(HttpSession ses, Map sharedAttributes) {
		_ses = ses;
		_sharedAttributes = sharedAttributes;
	}

	public Object getAttribute(String name) {
		Object value = _ses.getAttribute(name);

		if (value == null) {
			value = _sharedAttributes.get(name);
		}

		return value;
	}

	public Enumeration getAttributeNames() {
		if (_sharedAttributes.size() > 0) {
			List names = ListUtil.fromEnumeration(_ses.getAttributeNames());
			
			Iterator itr = _sharedAttributes.keySet().iterator();

			while (itr.hasNext()) {
				String name = (String)itr.next();

				names.add(name);
			}

			return Collections.enumeration(names);
		}
		else {
			return _ses.getAttributeNames();
		}
	}

	public long getCreationTime() {
		return _ses.getCreationTime();
	}

	public String getId() {
		return _ses.getId();
	}

	public long getLastAccessedTime() {
		return _ses.getLastAccessedTime();
	}

	public int getMaxInactiveInterval() {
		return _ses.getMaxInactiveInterval();
	}

	public ServletContext getServletContext() {
		return _ses.getServletContext();
	}

	public HttpSessionContext getSessionContext() {
		return _ses.getSessionContext();
	}

	public Object getValue(String name) {
		return getAttribute(name);
	}

	public String[] getValueNames() {
		List names = ListUtil.fromEnumeration(getAttributeNames());

		return (String[])names.toArray(new String[0]);
	}

	public void invalidate() {
		_ses.invalidate();
	}

	public boolean isNew() {
		return _ses.isNew();
	}

	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	public void removeAttribute(String name) {
		_ses.removeAttribute(name);
	}

	public void removeValue(String name) {
		removeAttribute(name);
	}

	public void setAttribute(String name, Object value) {
		_ses.setAttribute(name, value);
	}

	public void setMaxInactiveInterval(int interval) {
		_ses.setMaxInactiveInterval(interval);
	}

	private HttpSession _ses;
	private Map _sharedAttributes;

}