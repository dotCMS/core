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

import java.security.Key;
import java.util.Date;
import java.util.List;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.NoSuchCompanyException;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.Encryptor;
import com.liferay.util.EncryptorException;
import com.liferay.util.StringPool;

/**
 * <a href="CompanyLocalManagerImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class CompanyLocalManagerImpl implements CompanyLocalManager {

	// Business methods

	public User createDefaultUser(Company company) throws PortalException, SystemException{
		// Default user

		User defaultUser = null;

		try {
			defaultUser = UserLocalManagerUtil.getDefaultUser(company.getCompanyId());
		}
		catch (NoSuchUserException nsue) {
			defaultUser = UserUtil.create(User.getDefaultUserId(company.getCompanyId()));

			Date now = new Date();

			defaultUser.setCompanyId(User.DEFAULT);
			defaultUser.setCreateDate(now);
			defaultUser.setPassword("password");
			defaultUser.setFirstName(StringPool.BLANK);
			defaultUser.setMiddleName(StringPool.BLANK);
			defaultUser.setLastName(StringPool.BLANK);
			defaultUser.setMale(true);
			defaultUser.setBirthday(now);
			defaultUser.setEmailAddress(User.DEFAULT + "@" + company.getMx());

			defaultUser.setLanguageId(null);
			defaultUser.setTimeZoneId(null);
//			defaultUser.setSkinId(Skin.DEFAULT_SKIN_ID);
			defaultUser.setDottedSkins(false);
			defaultUser.setRoundedSkins(false);
			defaultUser.setGreeting("Welcome!");
			defaultUser.setResolution(
				PropsUtil.get(PropsUtil.DEFAULT_GUEST_LAYOUT_RESOLUTION));
			defaultUser.setRefreshRate(
				PropsUtil.get(PropsUtil.DEFAULT_USER_LAYOUT_REFRESH_RATE));
			defaultUser.setLoginDate(now);
			defaultUser.setFailedLoginAttempts(0);
			defaultUser.setAgreedToTermsOfUse(false);
			defaultUser.setActive(true);

			UserUtil.update(defaultUser);
		}
		return defaultUser;
	}
	
	public void checkCompany(String companyId)
		throws PortalException, SystemException {

		// Company

		Company company = null;

		try {
			company = CompanyUtil.findByPrimaryKey(companyId);
		}
		catch (NoSuchCompanyException nsce) {
			company = CompanyUtil.create(companyId);

			company.setPortalURL("localhost");
			company.setHomeURL("localhost");
			company.setMx(companyId);
			company.setName(companyId);
			company.setShortName(companyId);
			company.setType("biz");
			company.setEmailAddress("test@" + companyId);
			company.setAuthType(Company.AUTH_TYPE_EA);
			company.setAutoLogin(true);
			company.setStrangers(true);

			CompanyUtil.update(company);
		}

		// Key

		checkCompanyKey(companyId);

		// Groups

//		GroupLocalManagerUtil.checkSystemGroups(companyId);

		// Roles

//		RoleLocalManagerUtil.checkSystemRoles(companyId);

		// Default admin
		User defaultUser = createDefaultUser(company); 

		if (countUsers(companyId) == 0) {
			Date now = new Date();

			User user = UserLocalManagerUtil.addUser(
				companyId, true, StringPool.BLANK, false, "test", "test", false,
				"Test", StringPool.BLANK, "Test", StringPool.BLANK, true, now,
				"test@" + company.getMx(), defaultUser.getLocale());

			Role adminRole;
			try {
				adminRole = APILocator.getRoleAPI().loadRoleByKey("Administrator");
			} catch (DotDataException e) {
				Logger.error(CompanyLocalManagerImpl.class,e.getMessage(),e);
				throw new SystemException(e);
			}

			String[] roleIds = new String[] {adminRole.getId()};

			for (String roleId : roleIds) {
				try {
					APILocator.getRoleAPI().addRoleToUser(roleId, user);
				} catch (DotStateException e) {
					Logger.error(CompanyLocalManagerImpl.class,e.getMessage(),e);
				} catch (DotDataException e) {
					Logger.error(CompanyLocalManagerImpl.class,e.getMessage(),e);
				}	
			}
			
		}
	}

	public void checkCompanyKey(String companyId)
		throws PortalException, SystemException {

		try {
			Company company = CompanyUtil.findByPrimaryKey(companyId);

			if (company.getKeyObj() == null) {
				Key key = Encryptor.generateKey();

				company.setKeyObj(Encryptor.generateKey());
			}

			CompanyUtil.update(company);
		}
		catch (EncryptorException ee) {
			throw new SystemException(ee);
		}
	}

	public int countUsers(String companyId) throws SystemException {
		return UserUtil.countByCompanyId(companyId);
	}

	public List getCompanies() throws SystemException {
		return CompanyUtil.findAll();
	}

	@CloseDBIfOpened
	public Company getCompany(String companyId)
		throws PortalException, SystemException {

		return CompanyUtil.findByPrimaryKey(companyId);
	}

	public List getUsers(String companyId) throws SystemException {
		return UserUtil.findByCompanyId(companyId);
	}

	public List getUsers(String companyId, int begin, int end)
		throws SystemException {

		return UserUtil.findByCompanyId(companyId, begin, end);
	}

}