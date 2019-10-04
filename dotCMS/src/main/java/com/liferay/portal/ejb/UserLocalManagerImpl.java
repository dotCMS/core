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

import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotcms.repackage.com.liferay.mail.ejb.MailManagerUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UUIDUtil;
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
import com.liferay.portal.util.PropsUtil;
import com.liferay.portlet.admin.ejb.AdminConfigManagerUtil;
import com.liferay.portlet.admin.model.EmailConfig;
import com.liferay.portlet.admin.model.UserConfig;
import com.liferay.util.GetterUtil;
import com.liferay.util.InstancePool;
import com.liferay.util.StringUtil;
import com.liferay.util.SystemProperties;
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

	private final String USERNAME_REGEXP_PATTERN = GetterUtil.getString( SystemProperties.get( "UserName.regexp.pattern" ) );
	
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

		if (autoUserId && (userId == null || userId.isEmpty())) {
			userId = "user-" + UUIDUtil.uuid();
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

        // Use new password hash method
        try {
            user.setPassword(PasswordFactoryProxy.generateHash(password1));
        } catch (PasswordException e) {
            Logger.error(UserLocalManagerImpl.class,
                        "An error occurred generating the hashed password for userId: " + userId, e);
            throw new SystemException("An error occurred generating the hashed password.");
        }

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
		// Use new password hash method
        try {
            user.setPassword(PasswordFactoryProxy.generateHash(password1));
        } catch (PasswordException e) {
            Logger.error(UserLocalManagerImpl.class,
                    "An error occurred generating the hashed password for userId: " + userId, e);
            throw new SystemException("An error occurred generating the hashed password.");
        }

		int passwordsLifespan = GetterUtil.getInteger(
			PropsUtil.get(PropsUtil.PASSWORDS_LIFESPAN));

		Date expirationDate = null;
		if (passwordsLifespan > 0) {
			expirationDate = new Date(
				System.currentTimeMillis() + Time.DAY * passwordsLifespan);
		}

		user.setPasswordExpirationDate(expirationDate);
		user.setPasswordReset(passwordReset);

		UserUtil.update(user);

		PasswordTrackerLocalManagerUtil.trackPassword(userId, oldEncPwd);

		return user;
	}

	@Override
	public User updateUser(User user) throws PortalException, SystemException {
		if (Validator.isNotNull(user.getFirstName())) {
			String firstName = user.getFirstName().replaceAll("\\s+", " ").trim();
			user.setFirstName(firstName);
		}
		if (Validator.isNotNull(user.getLastName())) {
			String lastName = user.getLastName().replaceAll("\\s+", " ").trim();
			user.setLastName(lastName);
		}
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
			throw new UserEmailAddressException("Please enter a valid Email Address");
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

	@Override
	public void validate(
			String userId, String firstName, String lastName,
			String emailAddress, String smsId)
		throws PortalException, SystemException {
		if (Validator.isNull(firstName) || !RegEX.contains( firstName, USERNAME_REGEXP_PATTERN ) || firstName.length()>50) {
			throw new UserFirstNameException();
		    
		}
		else if (Validator.isNull(lastName) || !RegEX.contains( lastName, USERNAME_REGEXP_PATTERN ) || lastName.length()>50) {
			throw new UserLastNameException();
		}

		User user = UserUtil.findByPrimaryKey(userId);

		if (!Validator.isEmailAddress(emailAddress)) {
			throw new UserEmailAddressException("Please enter a valid Email Address");
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