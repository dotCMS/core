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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.mail.internet.InternetAddress;

import com.liferay.counter.ejb.CounterManagerUtil;
import com.liferay.mail.ejb.MailManagerUtil;
import com.liferay.portal.DuplicateUserEmailAddressException;
import com.liferay.portal.DuplicateUserIdException;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.PortalException;
import com.liferay.portal.RequiredUserException;
import com.liferay.portal.ReservedUserEmailAddressException;
import com.liferay.portal.ReservedUserIdException;
import com.liferay.portal.SystemException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.UserFirstNameException;
import com.liferay.portal.UserIdException;
import com.liferay.portal.UserIdValidator;
import com.liferay.portal.UserLastNameException;
import com.liferay.portal.UserPasswordException;
import com.liferay.portal.UserSmsException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.pwd.PwdToolkitUtil;
import com.liferay.portal.util.PortalInstances;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portlet.admin.ejb.AdminConfigManagerUtil;
import com.liferay.portlet.admin.model.EmailConfig;
import com.liferay.portlet.admin.model.UserConfig;
import com.liferay.util.Encryptor;
import com.liferay.util.GetterUtil;
import com.liferay.util.InstancePool;
import com.liferay.util.StringUtil;
import com.liferay.util.Time;
import com.liferay.util.Validator;
import com.liferay.util.mail.MailMessage;

/**
 * <a href="UserLocalManagerImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */


public class UserLocalManagerImpl implements UserLocalManager {

	// Business methods

	public User addUser(
			String companyId, boolean autoUserId, String userId,
			boolean autoPassword, String password1, String password2,
			boolean passwordReset, String firstName, String middleName,
			String lastName, String nickName, boolean male, Date birthday,
			String emailAddress, Locale locale)
		throws PortalException, SystemException {

		userId = userId.trim().toLowerCase();
		emailAddress = emailAddress.trim().toLowerCase();

		boolean alwaysAutoUserId = GetterUtil.getBoolean(
			PropsUtil.get(PropsUtil.USERS_ID_ALWAYS_AUTOGENERATE));

		if (alwaysAutoUserId) {
			autoUserId = true;
		}

		validate(
			companyId, autoUserId, userId, autoPassword, password1, password2,
			firstName, lastName, emailAddress);

		Company company = CompanyUtil.findByPrimaryKey(companyId);

		if (autoUserId) {
			userId =
				companyId + "." +
				Long.toString(CounterManagerUtil.increment(
					User.class.getName() + "." + companyId));
		}

		User user = UserUtil.create(userId);

		if (autoPassword) {
			password1 = PwdToolkitUtil.generate();
		}

		int passwordsLifespan = GetterUtil.getInteger(
			PropsUtil.get(PropsUtil.PASSWORDS_LIFESPAN));

		Date expirationDate = null;
		if (passwordsLifespan > 0) {
			expirationDate = new Date(
				System.currentTimeMillis() + Time.DAY * passwordsLifespan);
		}

		user.setCompanyId(companyId);
		user.setCreateDate(new Date());
		user.setPassword(Encryptor.digest(password1));
		user.setPasswordEncrypted(true);
		user.setPasswordExpirationDate(expirationDate);
		user.setPasswordReset(passwordReset);
		user.setFirstName(firstName);
		user.setMiddleName(middleName);
		user.setLastName(lastName);
		user.setNickName(nickName);
		user.setMale(male);
		user.setBirthday(birthday);
		user.setEmailAddress(emailAddress);

		//removed because we don't need this and on import the system was throwing a null pointer for users with @dotcms.org
//		if (user.hasCompanyMx()) {
//			MailManagerUtil.addUser(
//				userId, password1, firstName, middleName, lastName,
//				emailAddress);
//		}

		User defaultUser = getDefaultUser(companyId);

		String greeting = null;
		try {
			greeting =
				LanguageUtil.get(companyId, locale, "welcome") +
				", " + user.getFullName() + "!";
		}
		catch (LanguageException le) {
			greeting = "Welcome, " + user.getFullName() + "!";
		}

		user.setLanguageId(locale.toString());
		user.setTimeZoneId(defaultUser.getTimeZoneId());
		user.setSkinId(defaultUser.getSkinId());
		user.setDottedSkins(defaultUser.isDottedSkins());
		user.setRoundedSkins(defaultUser.isRoundedSkins());
		user.setGreeting(greeting);
		user.setResolution(defaultUser.getResolution());
		user.setRefreshRate(defaultUser.getRefreshRate());
		user.setLayoutIds("");
		user.setActive(true);

		UserUtil.update(user);

		UserConfig userConfig =
			AdminConfigManagerUtil.getUserConfig(companyId);

		// Add user groups

//		List groups = new ArrayList();
//
//		String groupNames[] = userConfig.getGroupNames();
//
//		for (int i = 0; groupNames != null && i < groupNames.length; i++) {
//			try {
//				groups.add(GroupUtil.findByC_N(companyId, groupNames[i]));
//			}
//			catch (NoSuchGroupException nsge) {
//			}
//		}
//
//		UserUtil.setGroups(userId, groups);

		// Add user roles

//		List roles = new ArrayList();
//
//		String roleNames[] = userConfig.getRoleNames();
//
//		for (int i = 0; roleNames != null && i < roleNames.length; i++) {
//			try {
//				Role role =
//					RoleLocalManagerUtil.getRoleByName(companyId, roleNames[i]);
//
//				roles.add(role);
//			}
//			catch (NoSuchRoleException nsre) {
//			}
//		}
//
//		UserUtil.setRoles(userId, roles);

		// Send email notification

		EmailConfig registrationEmail = userConfig.getRegistrationEmail();

		if (registrationEmail != null && registrationEmail.isSend()) {
			String adminName = company.getAdminName();

			String subject = registrationEmail.getSubject();
			subject = StringUtil.replace(
				subject,
				new String[] {"[$ADMIN_EMAIL_ADDRESS$]", "[$ADMIN_NAME$]",
							  "[$COMPANY_MX$]", "[$COMPANY_NAME$]",
							  "[$PORTAL_URL$]",
							  "[$USER_EMAIL_ADDRESS$]", "[$USER_ID$]",
							  "[$USER_NAME$]", "[$USER_PASSWORD$]"},
				new String[] {company.getEmailAddress(), adminName,
							  company.getMx(), company.getName(),
							  company.getPortalURL(),
							  user.getEmailAddress(), user.getUserId(),
							  user.getFullName(), password1});

			String body = registrationEmail.getBody();
			body = StringUtil.replace(
				body,
				new String[] {"[$ADMIN_EMAIL_ADDRESS$]", "[$ADMIN_NAME$]",
							  "[$COMPANY_MX$]", "[$COMPANY_NAME$]",
							  "[$PORTAL_URL$]",
							  "[$USER_EMAIL_ADDRESS$]", "[$USER_ID$]",
							  "[$USER_NAME$]", "[$USER_PASSWORD$]"},
				new String[] {company.getEmailAddress(), adminName,
							  company.getMx(), company.getName(),
							  company.getPortalURL(),
							  user.getEmailAddress(), user.getUserId(),
							  user.getFullName(), password1});

			try {
				InternetAddress from =
					new InternetAddress(company.getEmailAddress(), adminName);

				InternetAddress[] to = new InternetAddress[] {
					new InternetAddress(
						user.getEmailAddress(), user.getFullName())
				};

				InternetAddress[] cc = null;

				InternetAddress[] bcc = new InternetAddress[] {
					new InternetAddress(company.getEmailAddress(), adminName)
				};

				MailManagerUtil.sendEmail(new MailMessage(
					from, to, cc, bcc, subject, body));
			}
			catch (IOException ioe) {
				throw new SystemException(ioe);
			}
		}


		return user;
	}


	public void deleteUser(String userId)
		throws PortalException, SystemException {

		if (!GetterUtil.getBoolean(PropsUtil.get(PropsUtil.USERS_DELETE))&& PropsUtil.get(PropsUtil.USERS_DELETE)!=null) {
			throw new RequiredUserException();
		}

		User user = UserUtil.findByPrimaryKey(userId);

		// Delete user's portrait

		ImageLocalUtil.remove(userId);

		// Delete user's skin

//		SkinLocalManagerUtil.deleteSkin(userId);

		// Delete user's portlet preferences

		PortletPreferencesLocalManagerUtil.deleteAllByUser(userId);

		// Delete user's layouts

//		LayoutLocalManagerUtil.deleteAll(userId);

		// Delete user's old passwords

		PasswordTrackerLocalManagerUtil.deleteAll(userId);

		// Delete user's addresses

		AddressLocalManagerUtil.deleteAll(
			user.getCompanyId(), User.class.getName(), userId);

		// Delete user's address book contacts and lists



		// Delete user's mail

		//MailManagerUtil.deleteUser(userId);

		// Delete user

		UserUtil.remove(userId);
	}

	public List findByC_SMS(String companyId) throws SystemException {
		return UserFinder.findByC_SMS(companyId);
	}

	public User getDefaultUser(String companyId)
		throws PortalException, SystemException {

		return UserUtil.findByPrimaryKey(User.getDefaultUserId(companyId));
	}

	public User getUserByEmailAddress(
			String companyId, String emailAddress)
		throws PortalException, SystemException {

		emailAddress = emailAddress.trim().toLowerCase();

		return UserUtil.findByC_EA(companyId, emailAddress);
	}

	public User getUserById(String userId)
		throws PortalException, SystemException {

		userId = userId.trim().toLowerCase();

		return UserUtil.findByPrimaryKey(userId);
	}

	public User getUserById(String companyId, String userId)
		throws PortalException, SystemException {

		userId = userId.trim().toLowerCase();

		return UserUtil.findByC_U(companyId, userId);
	}


	public User updateActive(String userId, boolean active)
		throws PortalException, SystemException {

		userId = userId.trim().toLowerCase();

		User user = UserUtil.findByPrimaryKey(userId);

		user.setActive(active);

		UserUtil.update(user);

		return user;
	}

	public User updateUser(
			String userId, String password1, String password2,
			boolean passwordReset)
		throws PortalException, SystemException {

		userId = userId.trim().toLowerCase();

		validate(userId, password1, password2);

		User user = UserUtil.findByPrimaryKey(userId);

		String oldEncPwd = user.getPassword();
		if (!user.isPasswordEncrypted()) {
			oldEncPwd = Encryptor.digest(user.getPassword());
		}

		String newEncPwd = Encryptor.digest(password1);

		int passwordsLifespan = GetterUtil.getInteger(
			PropsUtil.get(PropsUtil.PASSWORDS_LIFESPAN));

		Date expirationDate = null;
		if (passwordsLifespan > 0) {
			expirationDate = new Date(
				System.currentTimeMillis() + Time.DAY * passwordsLifespan);
		}

		if (user.hasCompanyMx()) {
			MailManagerUtil.updatePassword(userId, password1);
		}

		user.setPassword(newEncPwd);
		user.setPasswordEncrypted(true);
		user.setPasswordExpirationDate(expirationDate);
		user.setPasswordReset(passwordReset);

		UserUtil.update(user);

		PasswordTrackerLocalManagerUtil.trackPassword(userId, oldEncPwd);

		return user;
	}

	public User updateUser(User user) throws PortalException, SystemException{
		validate(user.getUserId(), user.getFirstName(), user.getLastName(), user.getEmailAddress(), user.getSmsId());
		UserUtil.update(user);
		return user;
	}

	public User updateUser(
			String userId, String password, String firstName, String middleName,
			String lastName, String nickName, boolean male, Date birthday,
			String emailAddress, String smsId, String aimId, String icqId,
			String msnId, String ymId, String favoriteActivity,
			String favoriteBibleVerse, String favoriteFood,
			String favoriteMovie, String favoriteMusic, String languageId,
			String timeZoneId, String skinId, boolean dottedSkins,
			boolean roundedSkins, String greeting, String resolution,
			String refreshRate, String comments)
		throws PortalException, SystemException {

		userId = userId.trim().toLowerCase();
		emailAddress = emailAddress.trim().toLowerCase();

		validate(userId, firstName, lastName, emailAddress, smsId);

		User user = UserUtil.findByPrimaryKey(userId);

		user.setFirstName(firstName);
		user.setMiddleName(middleName);
		user.setLastName(lastName);
		user.setNickName(nickName);
		user.setMale(male);
		user.setBirthday(birthday);

		if (!emailAddress.equals(user.getEmailAddress())) {

			// test@test.com -> test@liferay.com

			if (!user.hasCompanyMx() && user.hasCompanyMx(emailAddress)) {
				MailManagerUtil.addUser(
					userId, password, firstName, middleName, lastName,
					emailAddress);
			}

			// test@liferay.com -> bob@liferay.com

			else if (user.hasCompanyMx() && user.hasCompanyMx(emailAddress)) {
				MailManagerUtil.updateEmailAddress(userId, emailAddress);
			}

			// test@liferay.com -> test@test.com

			else if (user.hasCompanyMx() && !user.hasCompanyMx(emailAddress)) {
				MailManagerUtil.deleteEmailAddress(userId);
			}

			user.setEmailAddress(emailAddress);
		}

		user.setSmsId(smsId);
		user.setAimId(aimId);
		user.setIcqId(icqId);
		user.setMsnId(msnId);
		user.setYmId(ymId);
		user.setFavoriteActivity(favoriteActivity);
		user.setFavoriteBibleVerse(favoriteBibleVerse);
		user.setFavoriteFood(favoriteFood);
		user.setFavoriteMovie(favoriteMovie);
		user.setFavoriteMusic(favoriteMusic);
		user.setLanguageId(languageId);
		user.setTimeZoneId(timeZoneId);
		user.setSkinId(skinId);
		user.setDottedSkins(dottedSkins);
		user.setRoundedSkins(roundedSkins);
		user.setGreeting(greeting);
		user.setResolution(resolution);
		user.setRefreshRate(refreshRate);
		user.setComments(comments);

		UserUtil.update(user);

		return user;
	}

	public void validate(
			String companyId, boolean autoUserId, String userId,
			boolean autoPassword, String password1, String password2,
			String firstName, String lastName, String emailAddress)
		throws PortalException, SystemException {

		if (Validator.isNull(firstName)) {
			throw new UserFirstNameException();
		}
		else if (Validator.isNull(lastName)) {
			throw new UserLastNameException();
		}

		if (!autoUserId) {
			if (Validator.isNull(userId)) {
				throw new UserIdException();
			}

			com.dotmarketing.auth.UserIdValidator userIdValidator = (UserIdValidator)InstancePool.get(
				PropsUtil.get(PropsUtil.USERS_ID_VALIDATOR));

			if (!userIdValidator.validate(userId, companyId)) {
				throw new UserIdException();
			}

//			String[] anonymousNames = PrincipalSessionBean.ANONYMOUS_NAMES;

//			for (int i = 0; i < anonymousNames.length; i++) {
//				if (userId.equalsIgnoreCase(anonymousNames[i])) {
////					throw new UserIdException();
//				}
//			}

//			String[] companyIds = PortalInstances.getCompanyIds();
//
//			for (int i = 0; i < companyIds.length; i++) {
//				if (userId.indexOf(companyIds[i]) != -1) {
//					throw new UserIdException();
//				}
//			}

			try {
				User user = UserUtil.findByPrimaryKey(userId);

				if (user != null) {
					throw new DuplicateUserIdException();
				}
			}
			catch (NoSuchUserException nsue) {
			}

			UserConfig userConfig =
				AdminConfigManagerUtil.getUserConfig(companyId);

			if (userConfig.hasReservedUserId(userId)) {
				throw new ReservedUserIdException();
			}
		}

		if (!Validator.isEmailAddress(emailAddress)) {
			throw new UserEmailAddressException();
		}
		else {
			try {
				User user = UserUtil.findByC_EA(companyId, emailAddress);

				if (user != null) {
					throw new DuplicateUserEmailAddressException();
				}
			}
			catch (NoSuchUserException nsue) {
			}

			UserConfig userConfig =
				AdminConfigManagerUtil.getUserConfig(companyId);

			if (userConfig.hasReservedUserEmailAddress(emailAddress)) {
				throw new ReservedUserEmailAddressException();
			}
		}

		if (!autoPassword) {
			if (!password1.equals(password2)) {
				throw new UserPasswordException(
					UserPasswordException.PASSWORDS_DO_NOT_MATCH);
			}
			else if (!PwdToolkitUtil.validate(password1) ||
					 !PwdToolkitUtil.validate(password2)) {

				throw new UserPasswordException(
					UserPasswordException.PASSWORD_INVALID);
			}
		}
	}

	public void validate(String userId, String password1, String password2)
		throws PortalException, SystemException {

		if (!password1.equals(password2)) {
			throw new UserPasswordException(
				UserPasswordException.PASSWORDS_DO_NOT_MATCH);
		}
		else if (!PwdToolkitUtil.validate(password1) ||
				 !PwdToolkitUtil.validate(password2)) {

			throw new UserPasswordException(
				UserPasswordException.PASSWORD_INVALID);
		}
		else if (!PasswordTrackerLocalManagerUtil.isValidPassword(
					userId, password1)) {

			throw new UserPasswordException(
				UserPasswordException.PASSWORD_ALREADY_USED);
		}
	}

	public void validate(
			String userId, String firstName, String lastName,
			String emailAddress, String smsId)
		throws PortalException, SystemException {

		if (Validator.isNull(firstName)) {
			throw new UserFirstNameException();
		}
		else if (Validator.isNull(lastName)) {
			throw new UserLastNameException();
		}

		User user = UserUtil.findByPrimaryKey(userId);

		if (!Validator.isEmailAddress(emailAddress)) {
			throw new UserEmailAddressException();
		}
		else {
			try {
				if (!user.getEmailAddress().equals(emailAddress)) {
					if (UserUtil.findByC_EA(
							user.getCompanyId(), emailAddress) != null) {

						throw new DuplicateUserEmailAddressException();
					}
				}
			}
			catch (NoSuchUserException nsue) {
			}

			UserConfig userConfig =
				AdminConfigManagerUtil.getUserConfig(user.getCompanyId());

			if (userConfig.hasReservedUserEmailAddress(emailAddress)) {
				throw new ReservedUserEmailAddressException();
			}
		}

		if (Validator.isNotNull(smsId) && !Validator.isEmailAddress(smsId)) {
			throw new UserSmsException();
		}
	}



}