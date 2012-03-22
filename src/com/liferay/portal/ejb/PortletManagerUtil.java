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
 * <a href="PortletManagerUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.78 $
 *
 */
public class PortletManagerUtil {
	public static java.util.Map getEARDisplay(java.lang.String xml)
		throws com.liferay.portal.SystemException {
		try {
			PortletManager portletManager = PortletManagerFactory.getManager();

			return portletManager.getEARDisplay(xml);
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static java.util.Map getWARDisplay(
		java.lang.String servletContextName, java.lang.String xml)
		throws com.liferay.portal.SystemException {
		try {
			PortletManager portletManager = PortletManagerFactory.getManager();

			return portletManager.getWARDisplay(servletContextName, xml);
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.Portlet getPortletById(
		java.lang.String companyId, java.lang.String portletId)
		throws com.liferay.portal.SystemException {
		try {
			PortletManager portletManager = PortletManagerFactory.getManager();

			return portletManager.getPortletById(companyId, portletId);
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.Portlet getPortletById(
		java.lang.String companyId, java.lang.String groupId,
		java.lang.String portletId) throws com.liferay.portal.SystemException {
		try {
			PortletManager portletManager = PortletManagerFactory.getManager();

			return portletManager.getPortletById(companyId, groupId, portletId);
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.Portlet getPortletByStrutsPath(
		java.lang.String companyId, java.lang.String strutsPath)
		throws com.liferay.portal.SystemException {
		try {
			PortletManager portletManager = PortletManagerFactory.getManager();

			return portletManager.getPortletByStrutsPath(companyId, strutsPath);
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.Portlet getPortletByStrutsPath(
		java.lang.String companyId, java.lang.String groupId,
		java.lang.String strutsPath) throws com.liferay.portal.SystemException {
		try {
			PortletManager portletManager = PortletManagerFactory.getManager();

			return portletManager.getPortletByStrutsPath(companyId, groupId,
				strutsPath);
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static java.util.List getPortlets(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		try {
			PortletManager portletManager = PortletManagerFactory.getManager();

			return portletManager.getPortlets(companyId);
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static void initEAR(java.lang.String[] xmls)
		throws com.liferay.portal.SystemException {
		try {
			PortletManager portletManager = PortletManagerFactory.getManager();
			portletManager.initEAR(xmls);
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static java.util.List initWAR(java.lang.String servletContextName,
		java.lang.String[] xmls) throws com.liferay.portal.SystemException {
		try {
			PortletManager portletManager = PortletManagerFactory.getManager();

			return portletManager.initWAR(servletContextName, xmls);
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portal.model.Portlet updatePortlet(
		java.lang.String portletId, java.lang.String groupId,
		java.lang.String defaultPreferences, boolean narrow,
		java.lang.String roles, boolean active)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PortletManager portletManager = PortletManagerFactory.getManager();

			return portletManager.updatePortlet(portletId, groupId,
				defaultPreferences, narrow, roles, active);
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