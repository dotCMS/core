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
 * <a href="AddressUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.94 $
 *
 */
public class AddressUtil {
	public static String PERSISTENCE = GetterUtil.get(PropsUtil.get(
				"value.object.persistence.com.liferay.portal.model.Address"),
			"com.liferay.portal.ejb.AddressPersistence2");
	public static String LISTENER = GetterUtil.getString(PropsUtil.get(
				"value.object.listener.com.liferay.portal.model.Address"));

	protected static com.liferay.portal.model.Address create(
		java.lang.String addressId) {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.create(addressId);
	}

	protected static com.liferay.portal.model.Address remove(
		java.lang.String addressId)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(AddressUtil.class,e.getMessage(),e);
			}
		}

		if (listener != null) {
			listener.onBeforeRemove(findByPrimaryKey(addressId));
		}

		com.liferay.portal.model.Address address = persistence.remove(addressId);

		if (listener != null) {
			listener.onAfterRemove(address);
		}

		return address;
	}

	protected static com.liferay.portal.model.Address update(
		com.liferay.portal.model.Address address)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(AddressUtil.class,e.getMessage(),e);
			}
		}

		boolean isNew = address.isNew();

		if (listener != null) {
			if (isNew) {
				listener.onBeforeCreate(address);
			}
			else {
				listener.onBeforeUpdate(address);
			}
		}

		address = persistence.update(address);

		if (listener != null) {
			if (isNew) {
				listener.onAfterCreate(address);
			}
			else {
				listener.onAfterUpdate(address);
			}
		}

		return address;
	}

	protected static com.liferay.portal.model.Address findByPrimaryKey(
		java.lang.String addressId)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByPrimaryKey(addressId);
	}

	protected static java.util.List findByCompanyId(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId);
	}

	protected static java.util.List findByCompanyId(
		java.lang.String companyId, int begin, int end)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId, begin, end);
	}

	protected static java.util.List findByCompanyId(
		java.lang.String companyId, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId(companyId, begin, end, obc);
	}

	protected static com.liferay.portal.model.Address findByCompanyId_First(
		java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId_First(companyId, obc);
	}

	protected static com.liferay.portal.model.Address findByCompanyId_Last(
		java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId_Last(companyId, obc);
	}

	protected static com.liferay.portal.model.Address[] findByCompanyId_PrevAndNext(
		java.lang.String addressId, java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByCompanyId_PrevAndNext(addressId, companyId, obc);
	}

	protected static java.util.List findByUserId(java.lang.String userId)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId(userId);
	}

	protected static java.util.List findByUserId(java.lang.String userId,
		int begin, int end) throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId(userId, begin, end);
	}

	protected static java.util.List findByUserId(java.lang.String userId,
		int begin, int end, com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId(userId, begin, end, obc);
	}

	protected static com.liferay.portal.model.Address findByUserId_First(
		java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId_First(userId, obc);
	}

	protected static com.liferay.portal.model.Address findByUserId_Last(
		java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId_Last(userId, obc);
	}

	protected static com.liferay.portal.model.Address[] findByUserId_PrevAndNext(
		java.lang.String addressId, java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId_PrevAndNext(addressId, userId, obc);
	}

	protected static java.util.List findByC_C(java.lang.String companyId,
		java.lang.String className) throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_C(companyId, className);
	}

	protected static java.util.List findByC_C(java.lang.String companyId,
		java.lang.String className, int begin, int end)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_C(companyId, className, begin, end);
	}

	protected static java.util.List findByC_C(java.lang.String companyId,
		java.lang.String className, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_C(companyId, className, begin, end, obc);
	}

	protected static com.liferay.portal.model.Address findByC_C_First(
		java.lang.String companyId, java.lang.String className,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_C_First(companyId, className, obc);
	}

	protected static com.liferay.portal.model.Address findByC_C_Last(
		java.lang.String companyId, java.lang.String className,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_C_Last(companyId, className, obc);
	}

	protected static com.liferay.portal.model.Address[] findByC_C_PrevAndNext(
		java.lang.String addressId, java.lang.String companyId,
		java.lang.String className,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_C_PrevAndNext(addressId, companyId,
			className, obc);
	}

	protected static java.util.List findByC_C_C(java.lang.String companyId,
		java.lang.String className, java.lang.String classPK)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_C_C(companyId, className, classPK);
	}

	protected static java.util.List findByC_C_C(java.lang.String companyId,
		java.lang.String className, java.lang.String classPK, int begin, int end)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_C_C(companyId, className, classPK, begin, end);
	}

	protected static java.util.List findByC_C_C(java.lang.String companyId,
		java.lang.String className, java.lang.String classPK, int begin,
		int end, com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_C_C(companyId, className, classPK, begin,
			end, obc);
	}

	protected static com.liferay.portal.model.Address findByC_C_C_First(
		java.lang.String companyId, java.lang.String className,
		java.lang.String classPK,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_C_C_First(companyId, className, classPK, obc);
	}

	protected static com.liferay.portal.model.Address findByC_C_C_Last(
		java.lang.String companyId, java.lang.String className,
		java.lang.String classPK,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_C_C_Last(companyId, className, classPK, obc);
	}

	protected static com.liferay.portal.model.Address[] findByC_C_C_PrevAndNext(
		java.lang.String addressId, java.lang.String companyId,
		java.lang.String className, java.lang.String classPK,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.NoSuchAddressException, 
			com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByC_C_C_PrevAndNext(addressId, companyId,
			className, classPK, obc);
	}

	protected static java.util.List findAll()
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findAll();
	}

	protected static void removeByCompanyId(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByCompanyId(companyId);
	}

	protected static void removeByUserId(java.lang.String userId)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByUserId(userId);
	}

	protected static void removeByC_C(java.lang.String companyId,
		java.lang.String className) throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByC_C(companyId, className);
	}

	protected static void removeByC_C_C(java.lang.String companyId,
		java.lang.String className, java.lang.String classPK)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByC_C_C(companyId, className, classPK);
	}

	protected static int countByCompanyId(java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByCompanyId(companyId);
	}

	protected static int countByUserId(java.lang.String userId)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByUserId(userId);
	}

	protected static int countByC_C(java.lang.String companyId,
		java.lang.String className) throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByC_C(companyId, className);
	}

	protected static int countByC_C_C(java.lang.String companyId,
		java.lang.String className, java.lang.String classPK)
		throws com.liferay.portal.SystemException {
		AddressPersistence persistence = (AddressPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByC_C_C(companyId, className, classPK);
	}
}