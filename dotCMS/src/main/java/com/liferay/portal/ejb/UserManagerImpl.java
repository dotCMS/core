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

import com.dotcms.api.system.user.UserService;
import com.dotcms.rest.api.v1.authentication.url.UrlStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dotcms.enterprise.AuthPipeProxy;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.PasswordFactoryProxy.AuthenticationStatus;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotcms.repackage.com.liferay.mail.ejb.MailManagerUtil;
import com.dotcms.rest.api.v1.authentication.DotInvalidTokenException;
import com.dotcms.rest.api.v1.authentication.ResetPasswordTokenUtil;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.SecurityUtils;
import com.dotcms.util.SecurityUtils.DelayStrategy;
import com.dotcms.util.UrlStrategyUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotInvalidPasswordException;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.EmailUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.PortalException;
import com.liferay.portal.RequiredUserException;
import com.liferay.portal.SystemException;
import com.liferay.portal.UserActiveException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.UserIdException;
import com.liferay.portal.UserPasswordException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.auth.PrincipalFinder;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.pwd.PwdToolkitUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portlet.admin.ejb.AdminConfigManagerUtil;
import com.liferay.portlet.admin.model.EmailConfig;
import com.liferay.portlet.admin.model.UserConfig;
import com.liferay.util.Encryptor;
import com.liferay.util.EncryptorException;
import com.liferay.util.GetterUtil;
import com.liferay.util.InstancePool;
import com.liferay.util.KeyValuePair;
import com.liferay.util.StringUtil;
import com.liferay.util.Validator;
import com.liferay.util.mail.MailMessage;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * This manager provides interaction with {@link User} objects in terms of 
 * authentication, verification, maintenance, etc.
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class UserManagerImpl extends PrincipalBean implements UserManager {

	private static final Log _log = LogFactory.getLog(UserManagerImpl.class);

	// Business methods

	@Override
	public User addUser(
			String companyId, boolean autoUserId, String userId,
			boolean autoPassword, String password1, String password2,
			boolean passwordReset, String firstName, String middleName,
			String lastName, String nickName, boolean male, Date birthday,
			String emailAddress, Locale locale)
		throws PortalException, SystemException {

		Company company = CompanyUtil.findByPrimaryKey(companyId);

		if (!company.isStrangers() && !hasAdministrator(companyId)) {
			throw new PrincipalException();
		}

		return UserLocalManagerUtil.addUser(
			companyId, autoUserId, userId, autoPassword, password1, password2,
			passwordReset, firstName, middleName, lastName, nickName, male,
			birthday, emailAddress, locale);
	}


	@Override
	public KeyValuePair decryptUserId(
			String companyId, String userId, String password)
		throws PortalException, SystemException {

		Company company = CompanyUtil.findByPrimaryKey(companyId);

		try {
			userId = Encryptor.decrypt(company.getKeyObj(), userId);
		}
		catch (EncryptorException ee) {
			throw new SystemException(ee);
		}

		String liferayUserId = userId;

		try {
			PrincipalFinder principalFinder = (PrincipalFinder)InstancePool.get(
				PropsUtil.get(PropsUtil.PRINCIPAL_FINDER));

			liferayUserId = principalFinder.toLiferay(userId);
		}
		catch (Exception e) {
		}

		User user = UserUtil.findByPrimaryKey(liferayUserId);

        AuthenticationStatus authenticationStatus = PasswordFactoryProxy.AuthenticationStatus.NOT_AUTHENTICATED;
        try {
            authenticationStatus = PasswordFactoryProxy.authPassword(password, user.getPassword());
        } catch (PasswordException e) {
            Logger.error(UserManagerImpl.class, "An error occurred generating the hashed password for userId: "
                    + userId, e);
            throw new SystemException("An error occurred generating the hashed password.");
        }

		if (authenticationStatus.equals(PasswordFactoryProxy.AuthenticationStatus.AUTHENTICATED)) {
			if (user.isPasswordExpired()) {
				user.setPasswordReset(true);

				UserUtil.update(user);
			}

			return new KeyValuePair(userId, password);
		}
		else {
			throw new PrincipalException();
		}
	}

	@Override
	public void deleteUser(String userId)
		throws PortalException, SystemException {

		if (!hasAdmin(userId)) {
			throw new PrincipalException();
		}

		if (getUserId().equals(userId)) {
			throw new RequiredUserException();
		}

		UserLocalManagerUtil.deleteUser(userId);
	}

	@Override
	public String encryptUserId(String userId)
		throws PortalException, SystemException {

		userId = userId.trim().toLowerCase();

		String liferayUserId = userId;

		try {
			PrincipalFinder principalFinder = (PrincipalFinder)InstancePool.get(
				PropsUtil.get(PropsUtil.PRINCIPAL_FINDER));

			liferayUserId = principalFinder.toLiferay(userId);
		}
		catch (Exception e) {
		}

		User user = UserUtil.findByPrimaryKey(liferayUserId);

		Company company = CompanyUtil.findByPrimaryKey(user.getCompanyId());

		try {
			return Encryptor.encrypt(company.getKeyObj(), userId);
		}
		catch (EncryptorException ee) {
			throw new SystemException(ee);
		}
	}

	@Override
	public List<?> findByAnd_C_FN_MN_LN_EA_M_BD_IM_A(
			String firstName, String middleName, String lastName,
			String emailAddress, Boolean male, Date age1, Date age2, String im,
			String street1, String street2, String city, String state,
			String zip, String phone, String fax, String cell)
		throws PortalException, SystemException {

		return UserFinder.findByAnd_C_FN_MN_LN_EA_M_BD_IM_A(
			getUser().getCompanyId(), firstName, middleName, lastName,
			emailAddress, male, age1, age2, im, street1, street2, city, state,
			zip, phone,fax, cell);
	}

	@Override
	public List<?> findByC_SMS() throws PortalException, SystemException {
		return UserFinder.findByC_SMS(getUser().getCompanyId());
	}

	@Override
	public List<?> findByOr_C_FN_MN_LN_EA_M_BD_IM_A(
			String firstName, String middleName, String lastName,
			String emailAddress, Boolean male, Date age1, Date age2, String im,
			String street1, String street2, String city, String state,
			String zip, String phone, String fax, String cell)
		throws PortalException, SystemException {

		return UserFinder.findByOr_C_FN_MN_LN_EA_M_BD_IM_A(
			getUser().getCompanyId(), firstName, middleName, lastName,
			emailAddress, male, age1, age2, im, street1, street2, city, state,
			zip, phone,fax, cell);
	}

	@Override
	public String getCompanyId(String userId)
		throws PortalException, SystemException {

		User user = UserUtil.findByPrimaryKey(userId);

		return user.getCompanyId();
	}

	@Override
	public User getDefaultUser(String companyId)
		throws PortalException, SystemException {

		return UserLocalManagerUtil.getDefaultUser(companyId);
	}

	@Override
	public User getUserByEmailAddress(String emailAddress)
		throws PortalException, SystemException {

		emailAddress = emailAddress.trim().toLowerCase();

		User user = UserUtil.findByC_EA(getUser().getCompanyId(), emailAddress);

		if (getUserId().equals(user.getUserId()) ||
			hasAdministrator(user.getCompanyId())) {

			return user;
		}
		else {
			return (User)user.getProtected();
		}
	}

	@Override
	public User getUserById(String userId)
		throws PortalException, SystemException {

		userId = userId.trim().toLowerCase();

		User user = UserUtil.findByPrimaryKey(userId);

		if (getUserId().equals(userId) ||
			hasAdministrator(user.getCompanyId())) {

			return user;
		}
		else {
			return (User)user.getProtected();
		}
	}

	@Override
	public User getUserById(String companyId, String userId)
		throws PortalException, SystemException {

		userId = userId.trim().toLowerCase();

		User user = UserUtil.findByC_U(companyId, userId);

		if (getUserId().equals(userId) ||
			hasAdministrator(user.getCompanyId())) {

			return user;
		}
		else {
			return (User)user.getProtected();
		}
	}

	@Override
	public String getUserId(String companyId, String emailAddress)
		throws PortalException, SystemException {

		emailAddress = emailAddress.trim().toLowerCase();

		User user = UserUtil.findByC_EA(companyId, emailAddress);

		return user.getUserId();
	}

	@Override
	public int notifyNewUsers() throws PortalException, SystemException {
		String companyId = getUser().getCompanyId();

		if (!hasAdministrator(companyId)) {
			throw new PrincipalException();
		}

		UserConfig userConfig = AdminConfigManagerUtil.getUserConfig(companyId);

		EmailConfig registrationEmail = userConfig.getRegistrationEmail();

		if (registrationEmail == null || !registrationEmail.isSend()) {
			return 0;
		}

		// Send email notification

		Company company = CompanyUtil.findByPrimaryKey(companyId);

		String adminName = company.getAdminName();

		String subject = registrationEmail.getSubject();
		String body = registrationEmail.getBody();

		List<?> users = UserUtil.findByC_P(companyId, "password");

		for (int i = 0; i < users.size(); i++) {
			User user = (User)users.get(i);

			user.setPassword(PwdToolkitUtil.generate());

			UserUtil.update(user);

			subject = StringUtil.replace(
				subject,
				new String[] {"[$ADMIN_EMAIL_ADDRESS$]", "[$ADMIN_NAME$]",
							  "[$COMPANY_MX$]", "[$COMPANY_NAME$]",
							  "[$PORTAL_URL$]",
							  "[$USER_EMAIL_ADDRESS$]", "[$USER_NAME$]",
							  "[$USER_PASSWORD$]"},
				new String[] {company.getEmailAddress(), adminName,
							  company.getMx(), company.getName(),
							  company.getPortalURL(),
							  user.getEmailAddress(), user.getFullName(),
							  user.getPassword()});

			body = StringUtil.replace(
				body,
				new String[] {"[$ADMIN_EMAIL_ADDRESS$]", "[$ADMIN_NAME$]",
							  "[$COMPANY_MX$]", "[$COMPANY_NAME$]",
							  "[$PORTAL_URL$]",
							  "[$USER_EMAIL_ADDRESS$]", "[$USER_NAME$]",
							  "[$USER_PASSWORD$]"},
				new String[] {company.getEmailAddress(), adminName,
							  company.getMx(), company.getName(),
							  company.getPortalURL(),
							  user.getEmailAddress(), user.getFullName(),
							  user.getPassword()});

			try {
				MailManagerUtil.sendEmail(new MailMessage(
					new InternetAddress(company.getEmailAddress(), adminName),
					new InternetAddress(
						user.getEmailAddress(), user.getFullName()),
					subject, body));
			}
			catch (IOException ioe) {
				throw new SystemException(ioe);
			}
		}

		return users.size();
	}

	@Override
	public void sendPassword(String companyId, String emailAddress, Locale locale, boolean fromAngular)
		throws PortalException, SystemException {

		emailAddress = emailAddress.trim().toLowerCase();

		if (!Validator.isEmailAddress(emailAddress)) {
			throw new UserEmailAddressException();
		}
		
		User user = UserUtil.findByC_EA(companyId, emailAddress);

		// we use the ICQ field to store the token:timestamp of the
		// password reset request we put in the email
		// the timestamp is used to set an expiration on the token
		String token = ResetPasswordTokenUtil.createToken();
		user.setIcqId(token+":"+new Date().getTime());
		
		UserUtil.update(user);

		// Send new password

		Company company = CompanyUtil.findByPrimaryKey(companyId);

		String url = UrlStrategyUtil.getURL(company,
				map(UrlStrategy.USER, user, UrlStrategy.TOKEN, token, UrlStrategy.LOCALE, locale),
				(fromAngular)? UserService.ANGULAR_RESET_PASSWORD_URL_STRATEGY: UserService.DEFAULT_RESET_PASSWORD_URL_STRATEGY);

		String body = LanguageUtil.format(locale, "reset-password-email-body", url, false);
		String subject = LanguageUtil.get(locale, "reset-password-email-subject");

		try {
			EmailUtils.sendMail(user, company, subject, body);
		}
		catch (Exception ioe) {
			throw new SystemException(ioe);
		}
	}

	@Override
	public void test() {
		String userId = null;

		try {
			userId = getUserId();
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}

		_log.info(userId);
	}

	@Override
	public User updateActive(String userId, boolean active)
		throws PortalException, SystemException {

		userId = userId.trim().toLowerCase();

		User user = UserUtil.findByPrimaryKey(userId);

		if (!hasAdministrator(user.getCompanyId())) {
			throw new PrincipalException();
		}

		if (active == false && getUserId().equals(userId)) {
			throw new RequiredUserException();
		}

		user.setActive(active);

		UserUtil.update(user);

		return user;
	}

	@Override
	public User updateAgreedToTermsOfUse(boolean agreedToTermsOfUse)
		throws PortalException, SystemException {

		User user = UserUtil.findByPrimaryKey(getUserId());

		user.setAgreedToTermsOfUse(agreedToTermsOfUse);

		UserUtil.update(user);

		return user;
	}

	@Override
	public User updateLastLogin(String loginIP)
		throws PortalException, SystemException {

		User user = UserUtil.findByPrimaryKey(getUserId());

		if (user.getLoginDate() == null &&
			user.getLastLoginDate() == null) {
		}

		user.setLastLoginDate(user.getLoginDate());
		user.setLastLoginIP(user.getLoginIP());
		user.setLoginDate(new Date());
		user.setLoginIP(loginIP);
		user.setFailedLoginAttempts(0);

		UserUtil.update(user);

		return user;
	}

	@Override
	public void updatePortrait(String userId, byte[] bytes)
		throws PortalException, SystemException {

		userId = userId.trim().toLowerCase();

		if (!getUserId().equals(userId) && !hasAdmin(userId)) {
			throw new PrincipalException();
		}

		ImageLocalUtil.put(userId, bytes);
	}

	@Override
	public User updateUser(
			String userId, String password1, String password2,
			boolean passwordReset)
		throws PortalException, SystemException {

		User user = UserUtil.findByPrimaryKey(userId);

		if (!getUserId().equals(userId) &&
			!hasAdministrator(user.getCompanyId())) {

			throw new PrincipalException();
		}

		return UserLocalManagerUtil.updateUser(
			userId, password1, password2, passwordReset);
	}

	@Override
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

		User user = UserUtil.findByPrimaryKey(userId);

		if (!getUserId().equals(userId) &&
			!hasAdministrator(user.getCompanyId())) {

			throw new PrincipalException();
		}

		return UserLocalManagerUtil.updateUser(
			userId, password, firstName, middleName, lastName, nickName, male,
			birthday, emailAddress, smsId, aimId, icqId, msnId, ymId,
			favoriteActivity, favoriteBibleVerse, favoriteFood, favoriteMovie,
			favoriteMusic, languageId, timeZoneId, skinId, dottedSkins,
			roundedSkins, greeting, resolution, refreshRate, comments);
	}

	// Permission methods

	@Override
	public boolean hasAdmin(String userId)
		throws PortalException, SystemException {

		User user = UserUtil.findByPrimaryKey(userId);

		if (hasAdministrator(user.getCompanyId())) {
			return true;
		}
		else {
			return false;
		}
	}

	// Private methods



	/**
	 * 
	 */
	public void resetPassword(String userId, String token, String newPassword) throws com.dotmarketing.business.NoSuchUserException,
			DotSecurityException, DotInvalidTokenException, DotInvalidPasswordException {
		try {
			if(UtilMethods.isSet(userId) && UtilMethods.isSet(token)) {
				User user  = APILocator.getUserAPI().loadUserById(userId);

				if (user == null){
					throw new com.dotmarketing.business.NoSuchUserException("");
				}

				ResetPasswordTokenUtil.checkToken(user, token);
				APILocator.getUserAPI().updatePassword(user, newPassword, APILocator.getUserAPI().getSystemUser(), false);
			}
		} catch (DotDataException e) {
			throw new IllegalArgumentException();
		}
	}

}
