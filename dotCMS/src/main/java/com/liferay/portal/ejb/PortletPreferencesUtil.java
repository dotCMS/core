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
 * <a href="PortletPreferencesUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.20 $
 *
 */
public class PortletPreferencesUtil {
	public static String PERSISTENCE = GetterUtil.get(PropsUtil.get(
				"value.object.persistence.com.liferay.portal.model.PortletPreferences"),
			"com.liferay.portal.ejb.PortletPreferencesPersistence");
	public static String LISTENER = GetterUtil.getString(PropsUtil.get(
				"value.object.listener.com.liferay.portal.model.PortletPreferences"));

	protected static com.liferay.portal.model.PortletPreferences create(
		com.liferay.portal.ejb.PortletPreferencesPK portletPreferencesPK) {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.create(portletPreferencesPK);
	}

	protected static com.liferay.portal.model.PortletPreferences remove(
		com.liferay.portal.ejb.PortletPreferencesPK portletPreferencesPK)
		throws com.liferay.portal.NoSuchPortletPreferencesException, 
			com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(PortletPreferencesUtil.class,e.getMessage(),e);
			}
		}

		if (listener != null) {
			listener.onBeforeRemove(findByPrimaryKey(portletPreferencesPK));
		}

		com.liferay.portal.model.PortletPreferences portletPreferences = persistence.remove(portletPreferencesPK);

		if (listener != null) {
			listener.onAfterRemove(portletPreferences);
		}

		return portletPreferences;
	}

	protected static com.liferay.portal.model.PortletPreferences update(
		com.liferay.portal.model.PortletPreferences portletPreferences)
		throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(PortletPreferencesUtil.class,e.getMessage(),e);
			}
		}

		boolean isNew = portletPreferences.isNew();

		if (listener != null) {
			if (isNew) {
				listener.onBeforeCreate(portletPreferences);
			}
			else {
				listener.onBeforeUpdate(portletPreferences);
			}
		}

		portletPreferences = persistence.update(portletPreferences);

		if (listener != null) {
			if (isNew) {
				listener.onAfterCreate(portletPreferences);
			}
			else {
				listener.onAfterUpdate(portletPreferences);
			}
		}

		return portletPreferences;
	}

	protected static com.liferay.portal.model.PortletPreferences findByPrimaryKey(
		com.liferay.portal.ejb.PortletPreferencesPK portletPreferencesPK)
		throws com.liferay.portal.NoSuchPortletPreferencesException, 
			com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByPrimaryKey(portletPreferencesPK);
	}

	protected static java.util.List findByLayoutId(java.lang.String layoutId)
		throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByLayoutId(layoutId);
	}
	
	protected static java.util.List findByLayoutId(java.lang.String layoutId,
		int begin, int end) throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByLayoutId(layoutId, begin, end);
	}

	protected static java.util.List findByLayoutId(java.lang.String layoutId,
		int begin, int end, com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByLayoutId(layoutId, begin, end, obc);
	}

	protected static com.liferay.portal.model.PortletPreferences findByLayoutId_First(
		java.lang.String layoutId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletPreferencesException, 
			com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByLayoutId_First(layoutId, obc);
	}

	protected static com.liferay.portal.model.PortletPreferences findByLayoutId_Last(
		java.lang.String layoutId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletPreferencesException, 
			com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByLayoutId_Last(layoutId, obc);
	}

	protected static com.liferay.portal.model.PortletPreferences[] findByLayoutId_PrevAndNext(
		com.liferay.portal.ejb.PortletPreferencesPK portletPreferencesPK,
		java.lang.String layoutId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletPreferencesException, 
			com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByLayoutId_PrevAndNext(portletPreferencesPK,
			layoutId, obc);
	}

	protected static java.util.List findByUserId(java.lang.String userId)
		throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId(userId);
	}

	protected static java.util.List findByUserId(java.lang.String userId,
		int begin, int end) throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId(userId, begin, end);
	}

	protected static java.util.List findByUserId(java.lang.String userId,
		int begin, int end, com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId(userId, begin, end, obc);
	}

	protected static com.liferay.portal.model.PortletPreferences findByUserId_First(
		java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletPreferencesException, 
			com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId_First(userId, obc);
	}

	protected static com.liferay.portal.model.PortletPreferences findByUserId_Last(
		java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletPreferencesException, 
			com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId_Last(userId, obc);
	}

	protected static com.liferay.portal.model.PortletPreferences[] findByUserId_PrevAndNext(
		com.liferay.portal.ejb.PortletPreferencesPK portletPreferencesPK,
		java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletPreferencesException, 
			com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId_PrevAndNext(portletPreferencesPK,
			userId, obc);
	}

	protected static java.util.List findByL_U(java.lang.String layoutId,
		java.lang.String userId) throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByL_U(layoutId, userId);
	}

	protected static java.util.List findByL_U(java.lang.String layoutId,
		java.lang.String userId, int begin, int end)
		throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByL_U(layoutId, userId, begin, end);
	}

	protected static java.util.List findByL_U(java.lang.String layoutId,
		java.lang.String userId, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByL_U(layoutId, userId, begin, end, obc);
	}

	protected static com.liferay.portal.model.PortletPreferences findByL_U_First(
		java.lang.String layoutId, java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletPreferencesException, 
			com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByL_U_First(layoutId, userId, obc);
	}

	protected static com.liferay.portal.model.PortletPreferences findByL_U_Last(
		java.lang.String layoutId, java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletPreferencesException, 
			com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByL_U_Last(layoutId, userId, obc);
	}

	protected static com.liferay.portal.model.PortletPreferences[] findByL_U_PrevAndNext(
		com.liferay.portal.ejb.PortletPreferencesPK portletPreferencesPK,
		java.lang.String layoutId, java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletPreferencesException, 
			com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByL_U_PrevAndNext(portletPreferencesPK,
			layoutId, userId, obc);
	}

	protected static java.util.List findAll()
		throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findAll();
	}

	protected static void removeByLayoutId(java.lang.String layoutId)
		throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByLayoutId(layoutId);
	}

	protected static void removeByUserId(java.lang.String userId)
		throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByUserId(userId);
	}

	protected static void removeByL_U(java.lang.String layoutId,
		java.lang.String userId) throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByL_U(layoutId, userId);
	}

	protected static int countByLayoutId(java.lang.String layoutId)
		throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByLayoutId(layoutId);
	}

	protected static int countByUserId(java.lang.String userId)
		throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByUserId(userId);
	}

	protected static int countByL_U(java.lang.String layoutId,
		java.lang.String userId) throws com.liferay.portal.SystemException {
		PortletPreferencesPersistence persistence = (PortletPreferencesPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByL_U(layoutId, userId);
	}
}