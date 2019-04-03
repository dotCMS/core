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

import com.liferay.portal.ejb.PortletPK;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.Xss;

/**
 * <a href="PortletModel.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.91 $
 *
 */
public class PortletModel extends BaseModel {
	public static boolean CACHEABLE = GetterUtil.get(PropsUtil.get(
				"value.object.cacheable.com.liferay.portal.model.Portlet"),
			VALUE_OBJECT_CACHEABLE);
	public static int MAX_SIZE = GetterUtil.get(PropsUtil.get(
				"value.object.max.size.com.liferay.portal.model.Portlet"),
			VALUE_OBJECT_MAX_SIZE);
	public static boolean XSS_ALLOW_BY_MODEL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Portlet"), XSS_ALLOW);
	public static boolean XSS_ALLOW_PORTLETID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Portlet.portletId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_GROUPID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Portlet.groupId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_COMPANYID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Portlet.companyId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_DEFAULTPREFERENCES = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Portlet.defaultPreferences"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_ROLES = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Portlet.roles"),
			XSS_ALLOW_BY_MODEL);
	public static long LOCK_EXPIRATION_TIME = GetterUtil.getLong(PropsUtil.get(
				"lock.expiration.time.com.liferay.portal.model.PortletModel"));
  private final static String noGroup="none";
	public PortletModel() {
	}

	public PortletModel(PortletPK pk) {
		_portletId = pk.portletId;
		_groupId = (pk.groupId==null)?noGroup:pk.groupId;
		_companyId = pk.companyId;
		setNew(true);
	}

	public PortletModel(String portletId, String groupId, String companyId,
		String defaultPreferences, boolean narrow, String roles, boolean active) {
		_portletId = portletId;
		_groupId = groupId;
		_companyId = companyId;
		_defaultPreferences = defaultPreferences;
		_narrow = narrow;
		_roles = roles;
		_active = active;
		
	}

	public PortletPK getPrimaryKey() {
		return new PortletPK(_portletId, _groupId, _companyId);
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

	public String getGroupId() {
		return (_groupId==null)? noGroup : _groupId;
	}

	public void setGroupId(String groupId) {
		if (((groupId == null) && (_groupId != null)) ||
				((groupId != null) && (_groupId == null)) ||
				((groupId != null) && (_groupId != null) &&
				!groupId.equals(_groupId))) {
			if (!XSS_ALLOW_GROUPID) {
				groupId = Xss.strip(groupId);
			}

			_groupId = (groupId==null)? noGroup : groupId;
			setModified(true);
		}
	}

	public String getCompanyId() {
		return _companyId;
	}

	public void setCompanyId(String companyId) {
		if (((companyId == null) && (_companyId != null)) ||
				((companyId != null) && (_companyId == null)) ||
				((companyId != null) && (_companyId != null) &&
				!companyId.equals(_companyId))) {
			if (!XSS_ALLOW_COMPANYID) {
				companyId = Xss.strip(companyId);
			}

			_companyId = companyId;
			setModified(true);
		}
	}

	public String getDefaultPreferences() {
		return _defaultPreferences;
	}

	public void setDefaultPreferences(String defaultPreferences) {
		if (((defaultPreferences == null) && (_defaultPreferences != null)) ||
				((defaultPreferences != null) && (_defaultPreferences == null)) ||
				((defaultPreferences != null) && (_defaultPreferences != null) &&
				!defaultPreferences.equals(_defaultPreferences))) {
			if (!XSS_ALLOW_DEFAULTPREFERENCES) {
				defaultPreferences = Xss.strip(defaultPreferences);
			}

			_defaultPreferences = defaultPreferences;
			setModified(true);
		}
	}

	public boolean getNarrow() {
		return _narrow;
	}

	public boolean isNarrow() {
		return _narrow;
	}

	public void setNarrow(boolean narrow) {
		if (narrow != _narrow) {
			_narrow = narrow;
			setModified(true);
		}
	}

	public String getRoles() {
		return _roles;
	}

	public void setRoles(String roles) {
		if (((roles == null) && (_roles != null)) ||
				((roles != null) && (_roles == null)) ||
				((roles != null) && (_roles != null) && !roles.equals(_roles))) {
			if (!XSS_ALLOW_ROLES) {
				roles = Xss.strip(roles);
			}

			_roles = roles;
			setModified(true);
		}
	}

	public boolean getActive() {
		return _active;
	}

	public boolean isActive() {
		return _active;
	}

	public void setActive(boolean active) {
		if (active != _active) {
			_active = active;
			setModified(true);
		}
	}

	public BaseModel getProtected() {
		return null;
	}

	public void protect() {
	}



	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		Portlet portlet = (Portlet)obj;
		PortletPK pk = portlet.getPrimaryKey();

		return getPrimaryKey().compareTo(pk);
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		Portlet portlet = null;

		try {
			portlet = (Portlet)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		PortletPK pk = portlet.getPrimaryKey();

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
	private String _groupId;
	private String _companyId;
	private String _defaultPreferences;
	private boolean _narrow;
	private String _roles;
	private boolean _active;
}