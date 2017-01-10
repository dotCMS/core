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

import com.dotmarketing.util.Logger;
import com.liferay.portal.model.ModelListener;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.InstancePool;
import com.liferay.util.Validator;

/**
 * <a href="UserTrackerPathUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.55 $
 *
 */
public class UserTrackerPathUtil {
	public static String PERSISTENCE = GetterUtil.get(PropsUtil.get(
				"value.object.persistence.com.liferay.portal.model.UserTrackerPath"),
			"com.liferay.portal.ejb.UserTrackerPathPersistence");
	public static String LISTENER = GetterUtil.getString(PropsUtil.get(
				"value.object.listener.com.liferay.portal.model.UserTrackerPath"));

	protected static com.liferay.portal.model.UserTrackerPath create(
		java.lang.String userTrackerPathId) {
		UserTrackerPathPersistence persistence = (UserTrackerPathPersistence)InstancePool.get(PERSISTENCE);

		return persistence.create(userTrackerPathId);
	}

	protected static com.liferay.portal.model.UserTrackerPath remove(
		java.lang.String userTrackerPathId)
		throws com.liferay.portal.NoSuchUserTrackerPathException, 
			com.liferay.portal.SystemException {
		UserTrackerPathPersistence persistence = (UserTrackerPathPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(UserTrackerPathUtil.class,e.getMessage(),e);
			}
		}

		if (listener != null) {
			listener.onBeforeRemove(findByPrimaryKey(userTrackerPathId));
		}

		com.liferay.portal.model.UserTrackerPath userTrackerPath = persistence.remove(userTrackerPathId);

		if (listener != null) {
			listener.onAfterRemove(userTrackerPath);
		}

		return userTrackerPath;
	}

	protected static com.liferay.portal.model.UserTrackerPath update(
		com.liferay.portal.model.UserTrackerPath userTrackerPath)
		throws com.liferay.portal.SystemException {
		UserTrackerPathPersistence persistence = (UserTrackerPathPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(UserTrackerPathUtil.class,e.getMessage(),e);
			}
		}

		boolean isNew = userTrackerPath.isNew();

		if (listener != null) {
			if (isNew) {
				listener.onBeforeCreate(userTrackerPath);
			}
			else {
				listener.onBeforeUpdate(userTrackerPath);
			}
		}

		userTrackerPath = persistence.update(userTrackerPath);

		if (listener != null) {
			if (isNew) {
				listener.onAfterCreate(userTrackerPath);
			}
			else {
				listener.onAfterUpdate(userTrackerPath);
			}
		}

		return userTrackerPath;
	}

	protected static com.liferay.portal.model.UserTrackerPath findByPrimaryKey(
		java.lang.String userTrackerPathId)
		throws com.liferay.portal.NoSuchUserTrackerPathException, 
			com.liferay.portal.SystemException {
		UserTrackerPathPersistence persistence = (UserTrackerPathPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByPrimaryKey(userTrackerPathId);
	}

	protected static java.util.List findByUserTrackerId(
		java.lang.String userTrackerId)
		throws com.liferay.portal.SystemException {
		UserTrackerPathPersistence persistence = (UserTrackerPathPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserTrackerId(userTrackerId);
	}

	protected static java.util.List findByUserTrackerId(
		java.lang.String userTrackerId, int begin, int end)
		throws com.liferay.portal.SystemException {
		UserTrackerPathPersistence persistence = (UserTrackerPathPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserTrackerId(userTrackerId, begin, end);
	}

	protected static java.util.List findByUserTrackerId(
		java.lang.String userTrackerId, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		UserTrackerPathPersistence persistence = (UserTrackerPathPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserTrackerId(userTrackerId, begin, end, obc);
	}

	protected static com.liferay.portal.model.UserTrackerPath findByUserTrackerId_First(
		java.lang.String userTrackerId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchUserTrackerPathException, 
			com.liferay.portal.SystemException {
		UserTrackerPathPersistence persistence = (UserTrackerPathPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserTrackerId_First(userTrackerId, obc);
	}

	protected static com.liferay.portal.model.UserTrackerPath findByUserTrackerId_Last(
		java.lang.String userTrackerId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchUserTrackerPathException, 
			com.liferay.portal.SystemException {
		UserTrackerPathPersistence persistence = (UserTrackerPathPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserTrackerId_Last(userTrackerId, obc);
	}

	protected static com.liferay.portal.model.UserTrackerPath[] findByUserTrackerId_PrevAndNext(
		java.lang.String userTrackerPathId, java.lang.String userTrackerId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchUserTrackerPathException, 
			com.liferay.portal.SystemException {
		UserTrackerPathPersistence persistence = (UserTrackerPathPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserTrackerId_PrevAndNext(userTrackerPathId,
			userTrackerId, obc);
	}

	protected static java.util.List findAll()
		throws com.liferay.portal.SystemException {
		UserTrackerPathPersistence persistence = (UserTrackerPathPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findAll();
	}

	protected static void removeByUserTrackerId(java.lang.String userTrackerId)
		throws com.liferay.portal.SystemException {
		UserTrackerPathPersistence persistence = (UserTrackerPathPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByUserTrackerId(userTrackerId);
	}

	protected static int countByUserTrackerId(java.lang.String userTrackerId)
		throws com.liferay.portal.SystemException {
		UserTrackerPathPersistence persistence = (UserTrackerPathPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByUserTrackerId(userTrackerId);
	}
}