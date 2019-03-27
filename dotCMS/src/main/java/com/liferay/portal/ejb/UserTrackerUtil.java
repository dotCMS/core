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
 * <a href="UserTrackerUtil.java.html"><b><i>View Source</i></b></a>
 * 
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.55 $
 * 
 */
public class UserTrackerUtil {
	public static String PERSISTENCE = GetterUtil
			.get(
					PropsUtil
							.get("value.object.persistence.com.liferay.portal.model.UserTracker"),
					"com.liferay.portal.ejb.UserTrackerPersistence");
	public static String LISTENER = GetterUtil.getString(PropsUtil
			.get("value.object.listener.com.liferay.portal.model.UserTracker"));

	protected static com.liferay.portal.model.UserTracker create(
			java.lang.String userTrackerId) {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.create(userTrackerId);
	}

	protected static com.liferay.portal.model.UserTracker remove(
			java.lang.String userTrackerId)
			throws com.liferay.portal.NoSuchUserTrackerException,
			com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener) Class.forName(LISTENER)
						.newInstance();
			} catch (Exception e) {
				Logger.error(UserTrackerUtil.class, e.getMessage(), e);
			}
		}

		if (listener != null) {
			listener.onBeforeRemove(findByPrimaryKey(userTrackerId));
		}

		com.liferay.portal.model.UserTracker userTracker = persistence
				.remove(userTrackerId);

		if (listener != null) {
			listener.onAfterRemove(userTracker);
		}

		return userTracker;
	}

	protected static com.liferay.portal.model.UserTracker update(
			com.liferay.portal.model.UserTracker userTracker)
			throws com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener) Class.forName(LISTENER)
						.newInstance();
			} catch (Exception e) {
				Logger.error(UserTrackerUtil.class, e.getMessage(), e);
			}
		}

		boolean isNew = userTracker.isNew();

		if (listener != null) {
			if (isNew) {
				listener.onBeforeCreate(userTracker);
			} else {
				listener.onBeforeUpdate(userTracker);
			}
		}

		userTracker = persistence.update(userTracker);

		if (listener != null) {
			if (isNew) {
				listener.onAfterCreate(userTracker);
			} else {
				listener.onAfterUpdate(userTracker);
			}
		}

		return userTracker;
	}

	protected static com.liferay.portal.model.UserTracker findByPrimaryKey(
			java.lang.String userTrackerId)
			throws com.liferay.portal.NoSuchUserTrackerException,
			com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findByPrimaryKey(userTrackerId);
	}

	protected static java.util.List findByCompanyId(java.lang.String companyId)
			throws com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId);
	}

	protected static java.util.List findByCompanyId(java.lang.String companyId,
			int begin, int end) throws com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId, begin, end);
	}

	protected static java.util.List findByCompanyId(java.lang.String companyId,
			int begin, int end,
			com.liferay.util.dao.hibernate.OrderByComparator obc)
			throws com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId, begin, end, obc);
	}

	protected static com.liferay.portal.model.UserTracker findByCompanyId_First(
			java.lang.String companyId,
			com.liferay.util.dao.hibernate.OrderByComparator obc)
			throws com.liferay.portal.NoSuchUserTrackerException,
			com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findByCompanyId_First(companyId, obc);
	}

	protected static com.liferay.portal.model.UserTracker findByCompanyId_Last(
			java.lang.String companyId,
			com.liferay.util.dao.hibernate.OrderByComparator obc)
			throws com.liferay.portal.NoSuchUserTrackerException,
			com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findByCompanyId_Last(companyId, obc);
	}

	protected static com.liferay.portal.model.UserTracker[] findByCompanyId_PrevAndNext(
			java.lang.String userTrackerId, java.lang.String companyId,
			com.liferay.util.dao.hibernate.OrderByComparator obc)
			throws com.liferay.portal.NoSuchUserTrackerException,
			com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findByCompanyId_PrevAndNext(userTrackerId,
				companyId, obc);
	}

	protected static java.util.List findByUserId(java.lang.String userId)
			throws com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findByUserId(userId);
	}

	protected static java.util.List findByUserId(java.lang.String userId,
			int begin, int end) throws com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findByUserId(userId, begin, end);
	}

	protected static java.util.List findByUserId(java.lang.String userId,
			int begin, int end,
			com.liferay.util.dao.hibernate.OrderByComparator obc)
			throws com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findByUserId(userId, begin, end, obc);
	}

	protected static com.liferay.portal.model.UserTracker findByUserId_First(
			java.lang.String userId,
			com.liferay.util.dao.hibernate.OrderByComparator obc)
			throws com.liferay.portal.NoSuchUserTrackerException,
			com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findByUserId_First(userId, obc);
	}

	protected static com.liferay.portal.model.UserTracker findByUserId_Last(
			java.lang.String userId,
			com.liferay.util.dao.hibernate.OrderByComparator obc)
			throws com.liferay.portal.NoSuchUserTrackerException,
			com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findByUserId_Last(userId, obc);
	}

	protected static com.liferay.portal.model.UserTracker[] findByUserId_PrevAndNext(
			java.lang.String userTrackerId, java.lang.String userId,
			com.liferay.util.dao.hibernate.OrderByComparator obc)
			throws com.liferay.portal.NoSuchUserTrackerException,
			com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findByUserId_PrevAndNext(userTrackerId, userId, obc);
	}

	protected static java.util.List findAll()
			throws com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.findAll();
	}

	protected static void removeByCompanyId(java.lang.String companyId)
			throws com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);
		persistence.removeByCompanyId(companyId);
	}

	protected static void removeByUserId(java.lang.String userId)
			throws com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);
		persistence.removeByUserId(userId);
	}

	protected static int countByCompanyId(java.lang.String companyId)
			throws com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.countByCompanyId(companyId);
	}

	protected static int countByUserId(java.lang.String userId)
			throws com.liferay.portal.SystemException {
		UserTrackerPersistence persistence = (UserTrackerPersistence) InstancePool
				.get(PERSISTENCE);

		return persistence.countByUserId(userId);
	}
}