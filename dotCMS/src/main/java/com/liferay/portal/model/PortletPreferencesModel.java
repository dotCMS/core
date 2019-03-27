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

package com.liferay.portal.model;

import com.liferay.portal.ejb.PortletPreferencesPK;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.Xss;

/**
 * <a href="PortletPreferencesModel.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.12 $
 *
 */
public class PortletPreferencesModel extends BaseModel {
	public static boolean CACHEABLE = GetterUtil.get(PropsUtil.get(
				"value.object.cacheable.com.liferay.portal.model.PortletPreferences"),
			VALUE_OBJECT_CACHEABLE);
	public static int MAX_SIZE = GetterUtil.get(PropsUtil.get(
				"value.object.max.size.com.liferay.portal.model.PortletPreferences"),
			VALUE_OBJECT_MAX_SIZE);
	public static boolean XSS_ALLOW_BY_MODEL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.PortletPreferences"),
			XSS_ALLOW);
	public static boolean XSS_ALLOW_PORTLETID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.PortletPreferences.portletId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_LAYOUTID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.PortletPreferences.layoutId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_USERID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.PortletPreferences.userId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_PREFERENCES = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.PortletPreferences.preferences"),
			XSS_ALLOW_BY_MODEL);
	public static long LOCK_EXPIRATION_TIME = GetterUtil.getLong(PropsUtil.get(
				"lock.expiration.time.com.liferay.portal.model.PortletPreferencesModel"));

	public PortletPreferencesModel() {
	}

	public PortletPreferencesModel(PortletPreferencesPK pk) {
		_portletId = pk.portletId;
		_layoutId = pk.layoutId;
		_userId = pk.userId;
		setNew(true);
	}

	public PortletPreferencesModel(String portletId, String layoutId,
		String userId, String preferences) {
		_portletId = portletId;
		_layoutId = layoutId;
		_userId = userId;
		_preferences = preferences;
	}

	public PortletPreferencesPK getPrimaryKey() {
		return new PortletPreferencesPK(_portletId, _layoutId, _userId);
	}

	public String getPortletId() {
		return _portletId;
	}

	public void setPortletId(String portletId) {
		if (((portletId == null) && (_portletId != null)) ||
				((portletId != null) && (_portletId == null)) ||
				((portletId != null) && (_portletId != null) &&
				!portletId.equals(_portletId))) {
			if (!XSS_ALLOW_PORTLETID) {
				portletId = Xss.strip(portletId);
			}

			_portletId = portletId;
			setModified(true);
		}
	}

	public String getLayoutId() {
		return _layoutId;
	}

	public void setLayoutId(String layoutId) {
		if (((layoutId == null) && (_layoutId != null)) ||
				((layoutId != null) && (_layoutId == null)) ||
				((layoutId != null) && (_layoutId != null) &&
				!layoutId.equals(_layoutId))) {
			if (!XSS_ALLOW_LAYOUTID) {
				layoutId = Xss.strip(layoutId);
			}

			_layoutId = layoutId;
			setModified(true);
		}
	}

	public String getUserId() {
		return _userId;
	}

	public void setUserId(String userId) {
		if (((userId == null) && (_userId != null)) ||
				((userId != null) && (_userId == null)) ||
				((userId != null) && (_userId != null) &&
				!userId.equals(_userId))) {
			if (!XSS_ALLOW_USERID) {
				userId = Xss.strip(userId);
			}

			_userId = userId;
			setModified(true);
		}
	}

	public String getPreferences() {
		return _preferences;
	}

	public void setPreferences(String preferences) {
		if (((preferences == null) && (_preferences != null)) ||
				((preferences != null) && (_preferences == null)) ||
				((preferences != null) && (_preferences != null) &&
				!preferences.equals(_preferences))) {
			if (!XSS_ALLOW_PREFERENCES) {
				preferences = Xss.strip(preferences);
			}

			_preferences = preferences;
			setModified(true);
		}
	}

	public BaseModel getProtected() {
		return null;
	}

	public void protect() {
	}

	public Object clone() {
		return new PortletPreferences(getPortletId(), getLayoutId(),
			getUserId(), getPreferences());
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		PortletPreferences portletPreferences = (PortletPreferences)obj;
		PortletPreferencesPK pk = portletPreferences.getPrimaryKey();

		return getPrimaryKey().compareTo(pk);
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		PortletPreferences portletPreferences = null;

		try {
			portletPreferences = (PortletPreferences)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		PortletPreferencesPK pk = portletPreferences.getPrimaryKey();

		if (getPrimaryKey().equals(pk)) {
			return true;
		}
		else {
			return false;
		}
	}

	public int hashCode() {
		return getPrimaryKey().hashCode();
	}

	private String _portletId;
	private String _layoutId;
	private String _userId;
	private String _preferences;
}