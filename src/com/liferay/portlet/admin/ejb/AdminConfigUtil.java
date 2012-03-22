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

import com.dotmarketing.util.Logger;
import com.liferay.portal.model.ModelListener;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.InstancePool;
import com.liferay.util.Validator;

/**
 * <a href="AdminConfigUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.81 $
 *
 */
public class AdminConfigUtil {
	public static String PERSISTENCE = GetterUtil.get(PropsUtil.get(
				"value.object.persistence.com.liferay.portlet.admin.model.AdminConfig"),
			"com.liferay.portlet.admin.ejb.AdminConfigPersistence");
	public static String LISTENER = GetterUtil.getString(PropsUtil.get(
				"value.object.listener.com.liferay.portlet.admin.model.AdminConfig"));

	protected static com.liferay.portlet.admin.model.AdminConfig create(
		java.lang.String configId) {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.create(configId);
	}

	protected static com.liferay.portlet.admin.model.AdminConfig remove(
		java.lang.String configId)
		throws com.liferay.portlet.admin.NoSuchConfigException, 
			com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(AdminConfigUtil.class,e.getMessage(),e);
			}
		}

		if (listener != null) {
			listener.onBeforeRemove(findByPrimaryKey(configId));
		}

		com.liferay.portlet.admin.model.AdminConfig adminConfig = persistence.remove(configId);

		if (listener != null) {
			listener.onAfterRemove(adminConfig);
		}

		return adminConfig;
	}

	protected static com.liferay.portlet.admin.model.AdminConfig update(
		com.liferay.portlet.admin.model.AdminConfig adminConfig)
		throws com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(AdminConfigUtil.class,e.getMessage(),e);
			}
		}

		boolean isNew = adminConfig.isNew();

		if (listener != null) {
			if (isNew) {
				listener.onBeforeCreate(adminConfig);
			}
			else {
				listener.onBeforeUpdate(adminConfig);
			}
		}

		adminConfig = persistence.update(adminConfig);

		if (listener != null) {
			if (isNew) {
				listener.onAfterCreate(adminConfig);
			}
			else {
				listener.onAfterUpdate(adminConfig);
			}
		}

		return adminConfig;
	}

	protected static com.liferay.portlet.admin.model.AdminConfig findByPrimaryKey(
		java.lang.String configId)
		throws com.liferay.portlet.admin.NoSuchConfigException, 
			com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByPrimaryKey(configId);
	}

	protected static java.util.List findByCompanyId(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId);
	}

	protected static java.util.List findByCompanyId(
		java.lang.String companyId, int begin, int end)
		throws com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId, begin, end);
	}

	protected static java.util.List findByCompanyId(
		java.lang.String companyId, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId, begin, end, obc);
	}

	protected static com.liferay.portlet.admin.model.AdminConfig findByCompanyId_First(
		java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.admin.NoSuchConfigException, 
			com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId_First(companyId, obc);
	}

	protected static com.liferay.portlet.admin.model.AdminConfig findByCompanyId_Last(
		java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.admin.NoSuchConfigException, 
			com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId_Last(companyId, obc);
	}

	protected static com.liferay.portlet.admin.model.AdminConfig[] findByCompanyId_PrevAndNext(
		java.lang.String configId, java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.admin.NoSuchConfigException, 
			com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId_PrevAndNext(configId, companyId, obc);
	}

	protected static java.util.List findByC_T(java.lang.String companyId,
		java.lang.String type) throws com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_T(companyId, type);
	}

	protected static java.util.List findByC_T(java.lang.String companyId,
		java.lang.String type, int begin, int end)
		throws com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_T(companyId, type, begin, end);
	}

	protected static java.util.List findByC_T(java.lang.String companyId,
		java.lang.String type, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_T(companyId, type, begin, end, obc);
	}

	protected static com.liferay.portlet.admin.model.AdminConfig findByC_T_First(
		java.lang.String companyId, java.lang.String type,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.admin.NoSuchConfigException, 
			com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_T_First(companyId, type, obc);
	}

	protected static com.liferay.portlet.admin.model.AdminConfig findByC_T_Last(
		java.lang.String companyId, java.lang.String type,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.admin.NoSuchConfigException, 
			com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_T_Last(companyId, type, obc);
	}

	protected static com.liferay.portlet.admin.model.AdminConfig[] findByC_T_PrevAndNext(
		java.lang.String configId, java.lang.String companyId,
		java.lang.String type,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.admin.NoSuchConfigException, 
			com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_T_PrevAndNext(configId, companyId, type, obc);
	}

	protected static java.util.List findAll()
		throws com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findAll();
	}

	protected static void removeByCompanyId(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByCompanyId(companyId);
	}

	protected static void removeByC_T(java.lang.String companyId,
		java.lang.String type) throws com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByC_T(companyId, type);
	}

	protected static int countByCompanyId(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByCompanyId(companyId);
	}

	protected static int countByC_T(java.lang.String companyId,
		java.lang.String type) throws com.liferay.portal.SystemException {
		AdminConfigPersistence persistence = (AdminConfigPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByC_T(companyId, type);
	}
}