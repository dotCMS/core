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

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Logger;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.auth.PrincipalFinder;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.InstancePool;
import com.liferay.util.Validator;

/**
 * <a href="PrincipalBean.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class PrincipalBean {

	public com.liferay.portal.model.User getUser()
		throws NoSuchUserException, SystemException, PrincipalException {

		return UserUtil.findByPrimaryKey(getUserId());
	}

	public String getUserId() throws PrincipalException {
		String name = PrincipalThreadLocal.getName();

		if (name == null) {
			throw new PrincipalException();
		}

		PrincipalFinder principalFinder = null;

		try {
			principalFinder = (PrincipalFinder)InstancePool.get(
				PropsUtil.get(PropsUtil.PRINCIPAL_FINDER));

			name = principalFinder.toLiferay(name);
		}
		catch (Exception e) {
		}

		if (Validator.isNull(name)) {
			throw new PrincipalException("Principal cannot be null");
		}

		return name;
	}

	public boolean hasAdministrator(String companyId)
		throws PortalException, SystemException {

		try {

			User u = APILocator.getUserAPI().loadUserById(getUserId(), APILocator.getUserAPI().getSystemUser(), true);
			if(APILocator.getRoleAPI().doesUserHaveRole(u,APILocator.getRoleAPI().loadRoleByKey(Role.ADMINISTRATOR))){
				return true;
			}
			if(APILocator.getRoleAPI().doesUserHaveRole(u,APILocator.getRoleAPI().loadCMSAdminRole())){
				return true;
			}

		} catch (Exception e) {
			Logger.error(PrincipalBean.class,e.getMessage(),e);
			
		}
		return false;
	}

}