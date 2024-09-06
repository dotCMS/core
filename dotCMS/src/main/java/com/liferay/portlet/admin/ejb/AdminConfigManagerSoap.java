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

import java.rmi.RemoteException;

/**
 * <a href="AdminConfigManagerSoap.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class AdminConfigManagerSoap {
	public static com.liferay.portlet.admin.model.AdminConfigModel[] getAdminConfig(
		java.lang.String companyId, java.lang.String type)
		throws RemoteException {
		try {

			return new com.liferay.portlet.admin.model.AdminConfig[0];
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage(),e);
		}
	}

	public static com.liferay.portlet.admin.model.JournalConfig getJournalConfig(
		java.lang.String companyId, java.lang.String portletId)
		throws RemoteException {
		try {


			return new com.liferay.portlet.admin.model.JournalConfig();
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage(),e);
		}
	}

	public static com.liferay.portlet.admin.model.ShoppingConfig getShoppingConfig(
		java.lang.String companyId) throws RemoteException {
		try {

			return new com.liferay.portlet.admin.model.ShoppingConfig();
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage(),e);
		}
	}

	public static com.liferay.portlet.admin.model.UserConfig getUserConfig(
		java.lang.String companyId) throws RemoteException {
		try {

			return new com.liferay.portlet.admin.model.UserConfig();
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage(),e);
		}
	}

	public static void updateJournalConfig(
		com.liferay.portlet.admin.model.JournalConfig journalConfig,
		java.lang.String portletId) throws RemoteException {

	}

	public static void updateShoppingConfig(
		com.liferay.portlet.admin.model.ShoppingConfig shoppingConfig)
		throws RemoteException {

	}

	public static void updateUserConfig(
		com.liferay.portlet.admin.model.UserConfig userConfig)
		throws RemoteException {

	}
}
