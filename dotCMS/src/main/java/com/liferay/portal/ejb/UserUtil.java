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
 * <a href="UserUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.112 $
 *
 */
public class UserUtil {
	public static String PERSISTENCE = GetterUtil.get(PropsUtil.get(
				"value.object.persistence.com.liferay.portal.model.User"),
			"com.liferay.portal.ejb.UserPersistence");
	public static String LISTENER = GetterUtil.getString(PropsUtil.get(
				"value.object.listener.com.liferay.portal.model.User"));

	protected static com.liferay.portal.model.User create(
		java.lang.String userId) {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.create(userId);
	}

	protected static com.liferay.portal.model.User remove(
		java.lang.String userId)
		throws com.liferay.portal.NoSuchUserException, 
			com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(UserUtil.class,e.getMessage(),e);
			}
		}

		if (listener != null) {
			listener.onBeforeRemove(findByPrimaryKey(userId));
		}

		com.liferay.portal.model.User user = persistence.remove(userId);

		if (listener != null) {
			listener.onAfterRemove(user);
		}

		return user;
	}

	protected static com.liferay.portal.model.User update(
		com.liferay.portal.model.User user)
		throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(UserUtil.class,e.getMessage(),e);
			}
		}

		boolean isNew = user.isNew();

		if (listener != null) {
			if (isNew) {
				listener.onBeforeCreate(user);
			}
			else {
				listener.onBeforeUpdate(user);
			}
		}

		user = persistence.update(user);

		if (listener != null) {
			if (isNew) {
				listener.onAfterCreate(user);
			}
			else {
				listener.onAfterUpdate(user);
			}
		}

		return user;
	}

	protected static com.liferay.portal.model.User findByPrimaryKey(
		java.lang.String userId)
		throws com.liferay.portal.NoSuchUserException, 
			com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByPrimaryKey(userId);
	}

	protected static java.util.List findByCompanyId(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId);
	}

	protected static java.util.List findByCompanyId(
		java.lang.String companyId, int begin, int end)
		throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId, begin, end);
	}

	protected static java.util.List findByCompanyId(
		java.lang.String companyId, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId, begin, end, obc);
	}

	protected static com.liferay.portal.model.User findByCompanyId_First(
		java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchUserException, 
			com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId_First(companyId, obc);
	}

	protected static com.liferay.portal.model.User findByCompanyId_Last(
		java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchUserException, 
			com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId_Last(companyId, obc);
	}

	protected static com.liferay.portal.model.User[] findByCompanyId_PrevAndNext(
		java.lang.String userId, java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchUserException, 
			com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId_PrevAndNext(userId, companyId, obc);
	}

	protected static com.liferay.portal.model.User findByC_U(
		java.lang.String companyId, java.lang.String userId)
		throws com.liferay.portal.NoSuchUserException, 
			com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_U(companyId, userId);
	}

	protected static java.util.List findByC_P(java.lang.String companyId,
		java.lang.String password) throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_P(companyId, password);
	}

	protected static java.util.List findByC_P(java.lang.String companyId,
		java.lang.String password, int begin, int end)
		throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_P(companyId, password, begin, end);
	}

	protected static java.util.List findByC_P(java.lang.String companyId,
		java.lang.String password, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_P(companyId, password, begin, end, obc);
	}

	protected static com.liferay.portal.model.User findByC_P_First(
		java.lang.String companyId, java.lang.String password,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchUserException, 
			com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_P_First(companyId, password, obc);
	}

	protected static com.liferay.portal.model.User findByC_P_Last(
		java.lang.String companyId, java.lang.String password,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchUserException, 
			com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_P_Last(companyId, password, obc);
	}

	protected static com.liferay.portal.model.User[] findByC_P_PrevAndNext(
		java.lang.String userId, java.lang.String companyId,
		java.lang.String password,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchUserException, 
			com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_P_PrevAndNext(userId, companyId, password,
			obc);
	}

	protected static com.liferay.portal.model.User findByC_EA(
		java.lang.String companyId, java.lang.String emailAddress)
		throws com.liferay.portal.NoSuchUserException, 
			com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_EA(companyId, emailAddress);
	}

	protected static java.util.List findAll()
		throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findAll();
	}

	protected static void removeByCompanyId(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByCompanyId(companyId);
	}

	protected static void removeByC_U(java.lang.String companyId,
		java.lang.String userId)
		throws com.liferay.portal.NoSuchUserException, 
			com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByC_U(companyId, userId);
	}

	protected static void removeByC_P(java.lang.String companyId,
		java.lang.String password) throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByC_P(companyId, password);
	}

	protected static void removeByC_EA(java.lang.String companyId,
		java.lang.String emailAddress)
		throws com.liferay.portal.NoSuchUserException, 
			com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByC_EA(companyId, emailAddress);
	}

	protected static int countByCompanyId(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByCompanyId(companyId);
	}

	protected static int countByC_U(java.lang.String companyId,
		java.lang.String userId) throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByC_U(companyId, userId);
	}

	protected static int countByC_P(java.lang.String companyId,
		java.lang.String password) throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByC_P(companyId, password);
	}

	protected static int countByC_EA(java.lang.String companyId,
		java.lang.String emailAddress)
		throws com.liferay.portal.SystemException {
		UserPersistence persistence = (UserPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByC_EA(companyId, emailAddress);
	}
}