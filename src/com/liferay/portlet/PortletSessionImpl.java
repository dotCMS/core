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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.portlet.PortletContext;
import javax.portlet.PortletSession;
import javax.servlet.http.HttpSession;

import com.liferay.util.StringPool;

/**
 * <a href="PortletSessionImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.13 $
 *
 */
public class PortletSessionImpl implements PortletSession {

 	public static final String PORTLET_SCOPE_NAMESPACE = "javax.portlet.p.";

	public static final String getPortletScopeName(
		String portletName, String name) {

		return PORTLET_SCOPE_NAMESPACE + portletName + StringPool.QUESTION +
			name;
	}

	public PortletSessionImpl(HttpSession ses, String portletName,
							  PortletContext ctx) {

		_ses = ses;
		_portletName = portletName;
		_ctx = ctx;
		_creationTime = new Date().getTime();
		_lastAccessedTime = _creationTime;
		_interval = _ses.getMaxInactiveInterval();
		_new = true;
		_invalid = false;
	}

	public PortletContext getPortletContext() {
		return _ctx;
	}

	public String getId() {
		return _ses.getId();
	}

	public long getCreationTime() {
		if (_invalid) {
			throw new IllegalStateException();
		}

		return _creationTime;
	}

	public long getLastAccessedTime() {
		return _lastAccessedTime;
	}

	public void setLastAccessedTime(long lastAccessedTime) {
		_lastAccessedTime = lastAccessedTime;
		_new = false;
	}

	public int getMaxInactiveInterval() {
		return _interval;
	}

	public void setMaxInactiveInterval(int interval) {
		_interval = interval;
	}

	public boolean isNew() {
		if (_invalid) {
			throw new IllegalStateException();
		}

		return _new;
	}

	public Object getAttribute(String name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}

		if (_invalid) {
			throw new IllegalStateException();
		}

		return getAttribute(name, PortletSession.PORTLET_SCOPE);
	}

	public Object getAttribute(String name, int scope) {
		if (name == null) {
			throw new IllegalArgumentException();
		}

		if (_invalid) {
			throw new IllegalStateException();
		}

		if (scope == PortletSession.PORTLET_SCOPE) {
			return _ses.getAttribute(_getPortletScopeName(name));
		}
		else {
			return _ses.getAttribute(name);
		}
	}

	public Enumeration getAttributeNames() {
		if (_invalid) {
			throw new IllegalStateException();
		}

		return getAttributeNames(PortletSession.PORTLET_SCOPE);
	}

	public Enumeration getAttributeNames(int scope) {
		if (_invalid) {
			throw new IllegalStateException();
		}

		if (scope == PortletSession.PORTLET_SCOPE) {
			List attributeNames = new ArrayList();

			Enumeration enu = _ses.getAttributeNames();

			while (enu.hasMoreElements()) {
				String name = (String)enu.nextElement();

				StringTokenizer st = new StringTokenizer(name, "?");

				if (st.countTokens() == 2) {
					if (st.nextToken().equals(
							PORTLET_SCOPE_NAMESPACE + _portletName)) {

						attributeNames.add(st.nextToken());
					}
				}
			}

			return Collections.enumeration(attributeNames);
		}
		else {
			return _ses.getAttributeNames();
		}
	}

	public void removeAttribute(String name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}

		if (_invalid) {
			throw new IllegalStateException();
		}

		removeAttribute(name, PortletSession.PORTLET_SCOPE);
	}

	public void removeAttribute(String name, int scope) {
		if (name == null) {
			throw new IllegalArgumentException();
		}

		if (_invalid) {
			throw new IllegalStateException();
		}

		if (scope == PortletSession.PORTLET_SCOPE) {
			_ses.removeAttribute(_getPortletScopeName(name));
		}
		else {
			_ses.removeAttribute(name);
		}
	}

	public void setAttribute(String name, Object value) {
		if (name == null) {
			throw new IllegalArgumentException();
		}

		if (_invalid) {
			throw new IllegalStateException();
		}

		setAttribute(name, value, PortletSession.PORTLET_SCOPE);
	}

	public void setAttribute(String name, Object value, int scope) {
		if (name == null) {
			throw new IllegalArgumentException();
		}

		if (_invalid) {
			throw new IllegalStateException();
		}

		if (scope == PortletSession.PORTLET_SCOPE) {
			_ses.setAttribute(_getPortletScopeName(name), value);
		}
		else {
			_ses.setAttribute(name, value);
		}
	}

	public void invalidate() {
		if (_invalid) {
			throw new IllegalStateException();
		}

		_ses.invalidate();

		_invalid = true;
	}

	public boolean isValid() {
		return !_invalid;
	}

	public HttpSession getSession() {
		return _ses;
	}

	private String _getPortletScopeName(String name) {
		return getPortletScopeName(_portletName, name);
	}

	private HttpSession _ses;
	private String _portletName;
	private PortletContext _ctx;
	private long _creationTime;
	private long _lastAccessedTime;
	private int _interval;
	private boolean _new;
	private boolean _invalid;

}