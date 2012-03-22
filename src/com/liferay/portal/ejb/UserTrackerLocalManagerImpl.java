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

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.liferay.counter.ejb.CounterManagerUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.UserTracker;
import com.liferay.portal.model.UserTrackerPath;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;

/**
 * <a href="UserTrackerLocalManagerImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class UserTrackerLocalManagerImpl implements UserTrackerLocalManager {

	// Business methods

	public UserTracker addUserTracker(
			String companyId, String userId, Date modifiedDate,
			String remoteAddr, String remoteHost, String userAgent,
			List userTrackerPaths)
		throws SystemException {

		if (GetterUtil.get(PropsUtil.get(
				PropsUtil.LOG_USER_PATHS), false)) {

			String userTrackerId = Long.toString(CounterManagerUtil.increment(
				UserTracker.class.getName()));

			UserTracker userTracker = UserTrackerUtil.create(userTrackerId);

			userTracker.setCompanyId(companyId);
			userTracker.setUserId(userId);
			userTracker.setModifiedDate(modifiedDate);
			userTracker.setRemoteAddr(remoteAddr);
			userTracker.setRemoteHost(remoteHost);
			userTracker.setUserAgent(userAgent);

			UserTrackerUtil.update(userTracker);

			Iterator itr = userTrackerPaths.iterator();

			while (itr.hasNext()) {
				UserTrackerPath userTrackerPath = (UserTrackerPath)itr.next();

				String pathId = Long.toString(CounterManagerUtil.increment(
					UserTrackerPath.class.getName()));

				userTrackerPath.setUserTrackerPathId(pathId);
				userTrackerPath.setUserTrackerId(userTrackerId);

				UserTrackerPathUtil.update(userTrackerPath);
			}

			return userTracker;
		}
		else {
			return null;
		}
	}

	public void deleteUserTracker(String userTrackerId)
		throws PortalException, SystemException {

		// Delete paths

		UserTrackerPathUtil.removeByUserTrackerId(userTrackerId);

		// Delete user tracker

		UserTrackerUtil.remove(userTrackerId);
	}

	public List getUserTrackers(String companyId, int begin, int end)
		throws SystemException {

		return UserTrackerUtil.findByCompanyId(companyId, begin, end);
	}

}