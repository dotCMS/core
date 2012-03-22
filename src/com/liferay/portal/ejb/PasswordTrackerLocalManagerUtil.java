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
 * <a href="PasswordTrackerLocalManagerUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class PasswordTrackerLocalManagerUtil {
	public static void deleteAll(java.lang.String userId)
		throws com.liferay.portal.SystemException {
		try {
			PasswordTrackerLocalManager passwordTrackerLocalManager = PasswordTrackerLocalManagerFactory.getManager();
			passwordTrackerLocalManager.deleteAll(userId);
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static boolean isValidPassword(java.lang.String userId,
		java.lang.String password)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PasswordTrackerLocalManager passwordTrackerLocalManager = PasswordTrackerLocalManagerFactory.getManager();

			return passwordTrackerLocalManager.isValidPassword(userId, password);
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

	public static void trackPassword(java.lang.String userId,
		java.lang.String encPwd)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PasswordTrackerLocalManager passwordTrackerLocalManager = PasswordTrackerLocalManagerFactory.getManager();
			passwordTrackerLocalManager.trackPassword(userId, encPwd);
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