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
 * <a href="UserTrackerManagerSoap.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class UserTrackerManagerSoap {
	public static void deleteUserTracker(java.lang.String userTrackerId)
		throws RemoteException {
		try {
			UserTrackerManagerUtil.deleteUserTracker(userTrackerId);
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage(),e);
		}
	}

	public static com.liferay.portal.model.UserTrackerModel[] getUserTrackers(
		int begin, int end) throws RemoteException {
		try {
			java.util.List returnValue = UserTrackerManagerUtil.getUserTrackers(begin,
					end);

			return (com.liferay.portal.model.UserTracker[])returnValue.toArray(new com.liferay.portal.model.UserTracker[0]);
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage(),e);
		}
	}
}