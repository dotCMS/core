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

import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portlet.PortletPreferencesImpl;

/**
 * <a href="PortletPreferencesManagerImpl.java.html"><b><i>View Source</i></b>
 * </a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class PortletPreferencesManagerImpl
	extends PrincipalBean implements PortletPreferencesManager {

	// Business methods

//	public void deleteAllByGroup(String groupId) {
//		PortletPreferencesLocalUtil.clearPreferencesPool(
//			Layout.GROUP + groupId);
//	}

	public com.dotcms.repackage.javax.portlet.PortletPreferences getDefaultPreferences(
			String companyId, String portletId)
		throws PortalException, SystemException {

		return PortletPreferencesLocalManagerUtil.getDefaultPreferences(
			companyId, portletId);
	}

	public com.dotcms.repackage.javax.portlet.PortletPreferences getPreferences(
			String companyId, PortletPreferencesPK pk)
		throws PortalException, SystemException {

		return PortletPreferencesLocalManagerUtil.getPreferences(companyId, pk);
	}

	public void updatePreferences(
			PortletPreferencesPK pk, PortletPreferencesImpl prefs)
		throws PortalException, SystemException {

		PortletPreferencesLocalManagerUtil.updatePreferences(pk, prefs);
	}

}