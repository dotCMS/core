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

package com.liferay.portal.ejb;


/**
 * <a href="PortletHBM.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.24 $
 *
 */
public class PortletHBM {
	protected PortletHBM() {
	}

	protected PortletHBM(PortletPK pk) {
		_portletId = pk.portletId;
		_groupId = pk.groupId;
		_companyId = pk.companyId;
	}

	protected PortletHBM(String portletId, String groupId, String companyId,
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

	protected void setPrimaryKey(PortletPK pk) {
		_portletId = pk.portletId;
		_groupId = pk.groupId;
		_companyId = pk.companyId;
	}

	protected String getPortletId() {
		return _portletId;
	}

	protected void setPortletId(String portletId) {
		_portletId = portletId;
	}

	protected String getGroupId() {
		return _groupId;
	}

	protected void setGroupId(String groupId) {
		_groupId = groupId;
	}

	protected String getCompanyId() {
		return _companyId;
	}

	protected void setCompanyId(String companyId) {
		_companyId = companyId;
	}

	protected String getDefaultPreferences() {
		return _defaultPreferences;
	}

	protected void setDefaultPreferences(String defaultPreferences) {
		_defaultPreferences = defaultPreferences;
	}

	protected boolean getNarrow() {
		return _narrow;
	}

	protected void setNarrow(boolean narrow) {
		_narrow = narrow;
	}

	protected String getRoles() {
		return _roles;
	}

	protected void setRoles(String roles) {
		_roles = roles;
	}

	protected boolean getActive() {
		return _active;
	}

	protected void setActive(boolean active) {
		_active = active;
	}

	private String _portletId;
	private String _groupId;
	private String _companyId;
	private String _defaultPreferences;
	private boolean _narrow;
	private String _roles;
	private boolean _active;
}