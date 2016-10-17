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

package com.liferay.portlet.admin.ejb;

/**
 * <a href="AdminConfigManagerUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.71 $
 *
 */
public class AdminConfigManagerUtil {
	public static final String PORTLET_ID = "9";

	public static java.util.List getAdminConfig(java.lang.String companyId,
		java.lang.String type) throws com.liferay.portal.SystemException {
		try {
			AdminConfigManager adminConfigManager = AdminConfigManagerFactory.getManager();

			return adminConfigManager.getAdminConfig(companyId, type);
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portlet.admin.model.JournalConfig getJournalConfig(
		java.lang.String companyId, java.lang.String portletId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AdminConfigManager adminConfigManager = AdminConfigManagerFactory.getManager();

			return adminConfigManager.getJournalConfig(companyId, portletId);
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

	public static com.liferay.portlet.admin.model.ShoppingConfig getShoppingConfig(
		java.lang.String companyId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AdminConfigManager adminConfigManager = AdminConfigManagerFactory.getManager();

			return adminConfigManager.getShoppingConfig(companyId);
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

	public static com.liferay.portlet.admin.model.UserConfig getUserConfig(
		java.lang.String companyId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AdminConfigManager adminConfigManager = AdminConfigManagerFactory.getManager();

			return adminConfigManager.getUserConfig(companyId);
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

	public static void updateJournalConfig(
		com.liferay.portlet.admin.model.JournalConfig journalConfig,
		java.lang.String portletId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AdminConfigManager adminConfigManager = AdminConfigManagerFactory.getManager();
			adminConfigManager.updateJournalConfig(journalConfig, portletId);
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

	public static void updateShoppingConfig(
		com.liferay.portlet.admin.model.ShoppingConfig shoppingConfig)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AdminConfigManager adminConfigManager = AdminConfigManagerFactory.getManager();
			adminConfigManager.updateShoppingConfig(shoppingConfig);
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

	public static void updateUserConfig(
		com.liferay.portlet.admin.model.UserConfig userConfig)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			AdminConfigManager adminConfigManager = AdminConfigManagerFactory.getManager();
			adminConfigManager.updateUserConfig(userConfig);
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