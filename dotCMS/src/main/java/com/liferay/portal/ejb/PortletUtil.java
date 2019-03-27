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
 * <a href="PortletUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.97 $
 *
 */
public class PortletUtil {
	public static String PERSISTENCE = GetterUtil.get(PropsUtil.get(
				"value.object.persistence.com.liferay.portal.model.Portlet"),
			"com.liferay.portal.ejb.PortletPersistence");
	public static String LISTENER = GetterUtil.getString(PropsUtil.get(
				"value.object.listener.com.liferay.portal.model.Portlet"));

	protected static com.liferay.portal.model.Portlet create(
		com.liferay.portal.ejb.PortletPK portletPK) {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.create(portletPK);
	}

	protected static com.liferay.portal.model.Portlet remove(
		com.liferay.portal.ejb.PortletPK portletPK)
		throws com.liferay.portal.NoSuchPortletException, 
			com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(PortletUtil.class,e.getMessage(),e);
			}
		}

		if (listener != null) {
			listener.onBeforeRemove(findByPrimaryKey(portletPK));
		}

		com.liferay.portal.model.Portlet portlet = persistence.remove(portletPK);

		if (listener != null) {
			listener.onAfterRemove(portlet);
		}

		return portlet;
	}

	protected static com.liferay.portal.model.Portlet update(
		com.liferay.portal.model.Portlet portlet)
		throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(PortletUtil.class,e.getMessage(),e);
			}
		}

		boolean isNew = portlet.isNew();

		if (listener != null) {
			if (isNew) {
				listener.onBeforeCreate(portlet);
			}
			else {
				listener.onBeforeUpdate(portlet);
			}
		}

		portlet = persistence.update(portlet);

		if (listener != null) {
			if (isNew) {
				listener.onAfterCreate(portlet);
			}
			else {
				listener.onAfterUpdate(portlet);
			}
		}

		return portlet;
	}

	protected static com.liferay.portal.model.Portlet findByPrimaryKey(
		com.liferay.portal.ejb.PortletPK portletPK)
		throws com.liferay.portal.NoSuchPortletException, 
			com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByPrimaryKey(portletPK);
	}

	protected static java.util.List findByGroupId(java.lang.String groupId)
		throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByGroupId(groupId);
	}

	protected static java.util.List findByGroupId(java.lang.String groupId,
		int begin, int end) throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByGroupId(groupId, begin, end);
	}

	protected static java.util.List findByGroupId(java.lang.String groupId,
		int begin, int end, com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByGroupId(groupId, begin, end, obc);
	}

	protected static com.liferay.portal.model.Portlet findByGroupId_First(
		java.lang.String groupId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletException, 
			com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByGroupId_First(groupId, obc);
	}

	protected static com.liferay.portal.model.Portlet findByGroupId_Last(
		java.lang.String groupId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletException, 
			com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByGroupId_Last(groupId, obc);
	}

	protected static com.liferay.portal.model.Portlet[] findByGroupId_PrevAndNext(
		com.liferay.portal.ejb.PortletPK portletPK, java.lang.String groupId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletException, 
			com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByGroupId_PrevAndNext(portletPK, groupId, obc);
	}

	protected static java.util.List findByCompanyId(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId);
	}

	protected static java.util.List findByCompanyId(
		java.lang.String companyId, int begin, int end)
		throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId, begin, end);
	}

	protected static java.util.List findByCompanyId(
		java.lang.String companyId, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId, begin, end, obc);
	}

	protected static com.liferay.portal.model.Portlet findByCompanyId_First(
		java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletException, 
			com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId_First(companyId, obc);
	}

	protected static com.liferay.portal.model.Portlet findByCompanyId_Last(
		java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletException, 
			com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId_Last(companyId, obc);
	}

	protected static com.liferay.portal.model.Portlet[] findByCompanyId_PrevAndNext(
		com.liferay.portal.ejb.PortletPK portletPK, java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletException, 
			com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId_PrevAndNext(portletPK, companyId, obc);
	}

	protected static java.util.List findByG_C(java.lang.String groupId,
		java.lang.String companyId) throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByG_C(groupId, companyId);
	}

	protected static java.util.List findByG_C(java.lang.String groupId,
		java.lang.String companyId, int begin, int end)
		throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByG_C(groupId, companyId, begin, end);
	}

	protected static java.util.List findByG_C(java.lang.String groupId,
		java.lang.String companyId, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByG_C(groupId, companyId, begin, end, obc);
	}

	protected static com.liferay.portal.model.Portlet findByG_C_First(
		java.lang.String groupId, java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletException, 
			com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByG_C_First(groupId, companyId, obc);
	}

	protected static com.liferay.portal.model.Portlet findByG_C_Last(
		java.lang.String groupId, java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletException, 
			com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByG_C_Last(groupId, companyId, obc);
	}

	protected static com.liferay.portal.model.Portlet[] findByG_C_PrevAndNext(
		com.liferay.portal.ejb.PortletPK portletPK, java.lang.String groupId,
		java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchPortletException, 
			com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByG_C_PrevAndNext(portletPK, groupId, companyId,
			obc);
	}

	protected static java.util.List findAll()
		throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findAll();
	}

	protected static void removeByGroupId(java.lang.String groupId)
		throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByGroupId(groupId);
	}

	protected static void removeByCompanyId(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByCompanyId(companyId);
	}

	protected static void removeByG_C(java.lang.String groupId,
		java.lang.String companyId) throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByG_C(groupId, companyId);
	}

	protected static int countByGroupId(java.lang.String groupId)
		throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByGroupId(groupId);
	}

	protected static int countByCompanyId(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByCompanyId(companyId);
	}

	protected static int countByG_C(java.lang.String groupId,
		java.lang.String companyId) throws com.liferay.portal.SystemException {
		PortletPersistence persistence = (PortletPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByG_C(groupId, companyId);
	}
}