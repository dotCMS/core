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

import java.util.Map;

import com.liferay.portal.NoSuchPortletPreferencesException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.PortletPreferences;
import com.liferay.portlet.PortletPreferencesImpl;
import com.liferay.portlet.PortletPreferencesSerializer;

/**
 * <a href="PortletPreferencesLocalManagerImpl.java.html"><b><i>View Source</i>
 * </b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class PortletPreferencesLocalManagerImpl
	implements PortletPreferencesLocalManager {

//	public void deleteAllByLayout(LayoutPK pk) throws SystemException {
//		if (Layout.isGroup(pk.layoutId)) {
//			PortletPreferencesUtil.removeByLayoutId(pk.layoutId);
//
//			PortletPreferencesLocalUtil.clearPreferencesPool();
//		}
//		else {
//			PortletPreferencesUtil.removeByL_U(pk.layoutId, pk.userId);
//
//			PortletPreferencesLocalUtil.clearPreferencesPool(pk.userId);
//		}
//	}

	public void deleteAllByUser(String userId) throws SystemException {
		PortletPreferencesUtil.removeByUserId(userId);

		PortletPreferencesLocalUtil.clearPreferencesPool(userId);
	}

	public javax.portlet.PortletPreferences getDefaultPreferences(
			String companyId, String portletId)
		throws PortalException, SystemException {

		Portlet portlet = PortletManagerUtil.getPortletById(
			companyId, portletId);

		return PortletPreferencesSerializer.fromDefaultXML(
			portlet.getDefaultPreferences());
	}
	
	public java.util.List<PortletPreferencesImpl> getPreferences()
		throws PortalException, SystemException{
			
		return PortletPreferencesUtil.findAll();
	}
	
	public javax.portlet.PortletPreferences getPreferences(
			String companyId, PortletPreferencesPK pk)
		throws PortalException, SystemException {

//		String groupId = Layout.getGroupId(pk.layoutId);

//		if (groupId != null) {
//			if (!GetterUtil.getBoolean(
//					PropsUtil.get(PropsUtil.GROUP_PAGES_PERSONALIZATION))) {
//
//				pk.userId = Layout.GROUP + groupId;
//			}
//		}

		Map prefsPool = PortletPreferencesLocalUtil.getPreferencesPool(
			pk.userId);

		PortletPreferencesImpl prefs =
			(PortletPreferencesImpl)prefsPool.get(pk);

		if (prefs == null) {
			PortletPreferences portletPreferences = null;

			Portlet portlet = null;
//			if (groupId != null) {
//				portlet = PortletManagerUtil.getPortletById(
//					companyId, groupId, pk.portletId);
//			}
//			else {
				portlet = PortletManagerUtil.getPortletById(
					companyId, pk.portletId);
//			}

			try {
				portletPreferences =
					PortletPreferencesUtil.findByPrimaryKey(pk);
			}
			catch (NoSuchPortletPreferencesException nsppe) {
				portletPreferences = PortletPreferencesUtil.create(pk);

				portletPreferences.setPreferences(
					portlet.getDefaultPreferences());

				PortletPreferencesUtil.update(portletPreferences);
			}

			prefs = PortletPreferencesSerializer.fromXML(
				companyId, pk, portletPreferences.getPreferences());

			prefsPool.put(pk, prefs);
		}

		return (PortletPreferencesImpl)prefs.clone();
	}

	public PortletPreferences updatePreferences(
			PortletPreferencesPK pk, PortletPreferencesImpl prefs)
		throws PortalException, SystemException {

		PortletPreferences portletPrefences =
			PortletPreferencesUtil.findByPrimaryKey(pk);

		String xml = PortletPreferencesSerializer.toXML(prefs);

		PortletPreferencesLocalUtil.getPreferencesPool(
			pk.userId).put(pk, prefs);

		portletPrefences.setPreferences(xml);

		PortletPreferencesUtil.update(portletPrefences);

		return portletPrefences;
	}

}