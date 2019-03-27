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
 * <a href="PortletPreferencesHBM.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class PortletPreferencesHBM {
	protected PortletPreferencesHBM() {
	}

	protected PortletPreferencesHBM(PortletPreferencesPK pk) {
		_portletId = pk.portletId;
		_layoutId = pk.layoutId;
		_userId = pk.userId;
	}

	protected PortletPreferencesHBM(String portletId, String layoutId,
		String userId, String preferences) {
		_portletId = portletId;
		_layoutId = layoutId;
		_userId = userId;
		_preferences = preferences;
	}

	public PortletPreferencesPK getPrimaryKey() {
		return new PortletPreferencesPK(_portletId, _layoutId, _userId);
	}

	protected void setPrimaryKey(PortletPreferencesPK pk) {
		_portletId = pk.portletId;
		_layoutId = pk.layoutId;
		_userId = pk.userId;
	}

	protected String getPortletId() {
		return _portletId;
	}

	protected void setPortletId(String portletId) {
		_portletId = portletId;
	}

	protected String getLayoutId() {
		return _layoutId;
	}

	protected void setLayoutId(String layoutId) {
		_layoutId = layoutId;
	}

	protected String getUserId() {
		return _userId;
	}

	protected void setUserId(String userId) {
		_userId = userId;
	}

	protected String getPreferences() {
		return _preferences;
	}

	protected void setPreferences(String preferences) {
		_preferences = preferences;
	}

	private String _portletId;
	private String _layoutId;
	private String _userId;
	private String _preferences;
}