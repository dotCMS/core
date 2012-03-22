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

import java.rmi.RemoteException;

/**
 * <a href="PortletManagerSoap.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class PortletManagerSoap {
	public static java.util.Map getEARDisplay(java.lang.String xml)
		throws RemoteException {
		try {
			java.util.Map returnValue = PortletManagerUtil.getEARDisplay(xml);

			return returnValue;
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static java.util.Map getWARDisplay(
		java.lang.String servletContextName, java.lang.String xml)
		throws RemoteException {
		try {
			java.util.Map returnValue = PortletManagerUtil.getWARDisplay(servletContextName,
					xml);

			return returnValue;
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static com.liferay.portal.model.PortletModel getPortletById(
		java.lang.String companyId, java.lang.String portletId)
		throws RemoteException {
		try {
			com.liferay.portal.model.Portlet returnValue = PortletManagerUtil.getPortletById(companyId,
					portletId);

			return returnValue;
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static com.liferay.portal.model.PortletModel getPortletById(
		java.lang.String companyId, java.lang.String groupId,
		java.lang.String portletId) throws RemoteException {
		try {
			com.liferay.portal.model.Portlet returnValue = PortletManagerUtil.getPortletById(companyId,
					groupId, portletId);

			return returnValue;
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static com.liferay.portal.model.PortletModel getPortletByStrutsPath(
		java.lang.String companyId, java.lang.String strutsPath)
		throws RemoteException {
		try {
			com.liferay.portal.model.Portlet returnValue = PortletManagerUtil.getPortletByStrutsPath(companyId,
					strutsPath);

			return returnValue;
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static com.liferay.portal.model.PortletModel getPortletByStrutsPath(
		java.lang.String companyId, java.lang.String groupId,
		java.lang.String strutsPath) throws RemoteException {
		try {
			com.liferay.portal.model.Portlet returnValue = PortletManagerUtil.getPortletByStrutsPath(companyId,
					groupId, strutsPath);

			return returnValue;
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static com.liferay.portal.model.PortletModel[] getPortlets(
		java.lang.String companyId) throws RemoteException {
		try {
			java.util.List returnValue = PortletManagerUtil.getPortlets(companyId);

			return (com.liferay.portal.model.Portlet[])returnValue.toArray(new com.liferay.portal.model.Portlet[0]);
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static void initEAR(java.lang.String[] xmls)
		throws RemoteException {
		try {
			PortletManagerUtil.initEAR(xmls);
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static com.liferay.portal.model.PortletModel[] initWAR(
		java.lang.String servletContextName, java.lang.String[] xmls)
		throws RemoteException {
		try {
			java.util.List returnValue = PortletManagerUtil.initWAR(servletContextName,
					xmls);

			return (com.liferay.portal.model.Portlet[])returnValue.toArray(new com.liferay.portal.model.Portlet[0]);
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static com.liferay.portal.model.PortletModel updatePortlet(
		java.lang.String portletId, java.lang.String groupId,
		java.lang.String defaultPreferences, boolean narrow,
		java.lang.String roles, boolean active) throws RemoteException {
		try {
			com.liferay.portal.model.Portlet returnValue = PortletManagerUtil.updatePortlet(portletId,
					groupId, defaultPreferences, narrow, roles, active);

			return returnValue;
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}
}