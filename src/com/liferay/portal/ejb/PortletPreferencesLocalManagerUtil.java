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
 * <a href="PortletPreferencesLocalManagerUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.10 $
 *
 */
public class PortletPreferencesLocalManagerUtil {
//	public static void deleteAllByLayout(com.liferay.portal.ejb.LayoutPK pk)
//		throws com.liferay.portal.SystemException {
//		try {
//			PortletPreferencesLocalManager portletPreferencesLocalManager = PortletPreferencesLocalManagerFactory.getManager();
//			portletPreferencesLocalManager.deleteAllByLayout(pk);
//		}
//		catch (com.liferay.portal.SystemException se) {
//			throw se;
//		}
//		catch (Exception e) {
//			throw new com.liferay.portal.SystemException(e);
//		}
//	}

	public static void deleteAllByUser(java.lang.String userId)
		throws com.liferay.portal.SystemException {
		try {
			PortletPreferencesLocalManager portletPreferencesLocalManager = PortletPreferencesLocalManagerFactory.getManager();
			portletPreferencesLocalManager.deleteAllByUser(userId);
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static javax.portlet.PortletPreferences getDefaultPreferences(
		java.lang.String companyId, java.lang.String portletId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PortletPreferencesLocalManager portletPreferencesLocalManager = PortletPreferencesLocalManagerFactory.getManager();

			return portletPreferencesLocalManager.getDefaultPreferences(companyId,
				portletId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static java.util.List<com.liferay.portlet.PortletPreferencesImpl> getPreferences()
		throws com.liferay.portal.PortalException,
			com.liferay.portal.SystemException{
		try{
			PortletPreferencesLocalManager portletPreferencesLocalManager = PortletPreferencesLocalManagerFactory.getManager();
			
			return portletPreferencesLocalManager.getPreferences();
		}catch(com.liferay.portal.PortalException pe){
			throw pe;
		}catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}
	
	public static javax.portlet.PortletPreferences getPreferences(
		java.lang.String companyId,
		com.liferay.portal.ejb.PortletPreferencesPK pk)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PortletPreferencesLocalManager portletPreferencesLocalManager = PortletPreferencesLocalManagerFactory.getManager();

			return portletPreferencesLocalManager.getPreferences(companyId, pk);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.PortletPreferences updatePreferences(
		com.liferay.portal.ejb.PortletPreferencesPK pk,
		com.liferay.portlet.PortletPreferencesImpl prefs)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PortletPreferencesLocalManager portletPreferencesLocalManager = PortletPreferencesLocalManagerFactory.getManager();

			return portletPreferencesLocalManager.updatePreferences(pk, prefs);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}
}