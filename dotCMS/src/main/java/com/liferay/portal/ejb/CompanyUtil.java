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
 * <a href="CompanyUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.83 $
 *
 */
public class CompanyUtil {
	public static String PERSISTENCE = GetterUtil.get(PropsUtil.get(
				"value.object.persistence.com.liferay.portal.model.Company"),
			"com.liferay.portal.ejb.CompanyPersistence");
	public static String LISTENER = GetterUtil.getString(PropsUtil.get(
				"value.object.listener.com.liferay.portal.model.Company"));

	public static com.liferay.portal.model.Company create(
		java.lang.String companyId) {
		CompanyPersistence persistence = (CompanyPersistence)InstancePool.get(PERSISTENCE);

		return persistence.create(companyId);
	}

	protected static com.liferay.portal.model.Company remove(
		java.lang.String companyId)
		throws com.liferay.portal.NoSuchCompanyException, 
			com.liferay.portal.SystemException {
		CompanyPersistence persistence = (CompanyPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(CompanyUtil.class,e.getMessage(),e);
			}
		}

		if (listener != null) {
			listener.onBeforeRemove(findByPrimaryKey(companyId));
		}

		com.liferay.portal.model.Company company = persistence.remove(companyId);

		if (listener != null) {
			listener.onAfterRemove(company);
		}

		return company;
	}

	public static com.liferay.portal.model.Company update(
		com.liferay.portal.model.Company company)
		throws com.liferay.portal.SystemException {
		CompanyPersistence persistence = (CompanyPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(CompanyUtil.class,e.getMessage(),e);
			}
		}

		boolean isNew = company.isNew();

		if (listener != null) {
			if (isNew) {
				listener.onBeforeCreate(company);
			}
			else {
				listener.onBeforeUpdate(company);
			}
		}

		company = persistence.update(company);

		if (listener != null) {
			if (isNew) {
				listener.onAfterCreate(company);
			}
			else {
				listener.onAfterUpdate(company);
			}
		}

		return company;
	}

	public static com.liferay.portal.model.Company findByPrimaryKey(
		java.lang.String companyId)
		throws com.liferay.portal.NoSuchCompanyException, 
			com.liferay.portal.SystemException {
		CompanyPersistence persistence = (CompanyPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByPrimaryKey(companyId);
	}

	protected static java.util.List findAll()
		throws com.liferay.portal.SystemException {
		CompanyPersistence persistence = (CompanyPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findAll();
	}
}