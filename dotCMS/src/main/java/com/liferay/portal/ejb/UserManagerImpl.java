/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.liferay.portal.ejb;

import com.dotcms.api.system.user.UserService;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.enterprise.AuthPipeProxy;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.PasswordFactoryProxy.AuthenticationStatus;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotcms.repackage.com.liferay.mail.ejb.MailManagerUtil;
import com.dotcms.rest.api.v1.authentication.DotInvalidTokenException;
import com.dotcms.rest.api.v1.authentication.ResetPasswordTokenUtil;
import com.dotcms.rest.api.v1.authentication.url.UrlStrategy;
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
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.auth.PrincipalFinder;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.pwd.PwdToolkitUtil;
import com.liferay.portal.util.PropsUtil;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This manager provides interaction with {@link User} objects in terms of authentication,
 * verification, maintenance, etc.
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class UserManagerImpl extends PrincipalBean implements UserManager {

    private static final Log _log = LogFactory.getLog(UserManagerImpl.class);

    // Business methods

    @Override
    public User addUser(String companyId, boolean autoUserId, String userId, boolean autoPassword, String password1, String password2,
            boolean passwordReset, String firstName, String middleName, String lastName, String nickName, boolean male, Date birthday,
            String emailAddress, Locale locale) throws PortalException, SystemException {

        Company company = CompanyUtil.findByPrimaryKey(companyId);

        if (!company.isStrangers() && !hasAdministrator(companyId)) {
            throw new PrincipalException();
        }

        return UserLocalManagerUtil.addUser(companyId, autoUserId, userId, autoPassword, password1, password2, passwordReset, firstName,
                middleName, lastName, nickName, male, birthday, emailAddress, locale);
    }

    @CloseDBIfOpened
    @Override
    public int authenticateByEmailAddress(final String companyId, final String emailAddress, final String password) throws PortalException, SystemException {
        return this.authenticate(companyId, emailAddress, password, true);
    }

    @CloseDBIfOpened
    @Override
    public int authenticateByUserId(final String companyId, final String userId, final String password) throws PortalException, SystemException {
        return this.authenticate(companyId, userId, password, false);
    }

    @Override
    public KeyValuePair decryptUserId(String companyId, String userId, String password) throws PortalException, SystemException {

        Company company = CompanyUtil.findByPrimaryKey(companyId);

        try {
            userId = Encryptor.decrypt(company.getKeyObj(), userId);
        } catch (EncryptorException ee) {
            throw new SystemException(ee);
        }

        String liferayUserId = userId;

        try {
            PrincipalFinder principalFinder = (PrincipalFinder) InstancePool.get(PropsUtil.get(PropsUtil.PRINCIPAL_FINDER));

            liferayUserId = principalFinder.toLiferay(userId);
        } catch (Exception e) {
        }

        User user = UserUtil.findByPrimaryKey(liferayUserId);

        AuthenticationStatus authenticationStatus = PasswordFactoryProxy.AuthenticationStatus.NOT_AUTHENTICATED;
        try {
            authenticationStatus = PasswordFactoryProxy.authPassword(password, user.getPassword());
        } catch (PasswordException e) {
            Logger.error(UserManagerImpl.class, "An error occurred generating the hashed password for userId: " + userId, e);
            throw new SystemException("An error occurred generating the hashed password.");
        }

        if (authenticationStatus.equals(PasswordFactoryProxy.AuthenticationStatus.AUTHENTICATED)) {
            if (user.isPasswordExpired()) {
                user.setPasswordReset(true);

                UserUtil.update(user);
            }

            return new KeyValuePair(userId, password);
        } else {
            throw new PrincipalException();
        }
    }

    @Override
    public void deleteUser(String userId) throws PortalException, SystemException {

        if (!hasAdmin(userId)) {
            throw new PrincipalException();
        }

        if (getUserId().equals(userId)) {
            throw new RequiredUserException();
        }

        UserLocalManagerUtil.deleteUser(userId);
    }

    @CloseDBIfOpened
    @Override
    public String encryptUserId(String userId) throws PortalException, SystemException {

        userId = userId.trim().toLowerCase();

        String liferayUserId = userId;

        try {
            PrincipalFinder principalFinder = (PrincipalFinder) InstancePool.get(PropsUtil.get(PropsUtil.PRINCIPAL_FINDER));

            liferayUserId = principalFinder.toLiferay(userId);
        } catch (Exception e) {
        }

        User user = UserUtil.findByPrimaryKey(liferayUserId);

        Company company = CompanyUtil.findByPrimaryKey(user.getCompanyId());

        try {
            return Encryptor.encrypt(company.getKeyObj(), userId);
        } catch (EncryptorException ee) {
            throw new SystemException(ee);
        }
    }

    @Override
    public List<?> findByAnd_C_FN_MN_LN_EA_M_BD_IM_A(String firstName, String middleName, String lastName, String emailAddress,
            Boolean male, Date age1, Date age2, String im, String street1, String street2, String city, String state, String zip,
            String phone, String fax, String cell) throws PortalException, SystemException {

        return UserFinder.findByAnd_C_FN_MN_LN_EA_M_BD_IM_A(getUser().getCompanyId(), firstName, middleName, lastName, emailAddress, male,
                age1, age2, im, street1, street2, city, state, zip, phone, fax, cell);
    }

    @Override
    public List<?> findByC_SMS() throws PortalException, SystemException {
        return UserFinder.findByC_SMS(getUser().getCompanyId());
    }

    @Override
    public List<?> findByOr_C_FN_MN_LN_EA_M_BD_IM_A(String firstName, String middleName, String lastName, String emailAddress, Boolean male,
            Date age1, Date age2, String im, String street1, String street2, String city, String state, String zip, String phone,
            String fax, String cell) throws PortalException, SystemException {

        return UserFinder.findByOr_C_FN_MN_LN_EA_M_BD_IM_A(getUser().getCompanyId(), firstName, middleName, lastName, emailAddress, male,
                age1, age2, im, street1, street2, city, state, zip, phone, fax, cell);
    }

    @Override
    public String getCompanyId(String userId) throws PortalException, SystemException {

        User user = UserUtil.findByPrimaryKey(userId);

        return user.getCompanyId();
    }

    @Override
    public User getDefaultUser(String companyId) throws PortalException, SystemException {

        return UserLocalManagerUtil.getDefaultUser(companyId);
    }

    @Override
    public User getUserByEmailAddress(String emailAddress) throws PortalException, SystemException {

        emailAddress = emailAddress.trim().toLowerCase();

        User user = UserUtil.findByC_EA(getUser().getCompanyId(), emailAddress);

        if (getUserId().equals(user.getUserId()) || hasAdministrator(user.getCompanyId())) {

            return user;
        } else {
            return (User) user.getProtected();
        }
    }

    @Override
    public User getUserById(String userId) throws PortalException, SystemException {

        userId = userId.trim().toLowerCase();

        User user = UserUtil.findByPrimaryKey(userId);

        if (getUserId().equals(userId) || hasAdministrator(user.getCompanyId())) {

            return user;
        } else {
            return (User) user.getProtected();
        }
    }

    @Override
    public User getUserById(String companyId, String userId) throws PortalException, SystemException {

        userId = userId.trim().toLowerCase();

        User user = UserUtil.findByC_U(companyId, userId);

        if (getUserId().equals(userId) || hasAdministrator(user.getCompanyId())) {

            return user;
        } else {
            return (User) user.getProtected();
        }
    }

    @Override
    public String getUserId(String companyId, String emailAddress) throws PortalException, SystemException {

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

        UserConfig userConfig = new UserConfig();

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
            User user = (User) users.get(i);

            user.setPassword(PwdToolkitUtil.generate());

            UserUtil.update(user);

            subject = StringUtil.replace(subject,
                    new String[] {"[$ADMIN_EMAIL_ADDRESS$]", "[$ADMIN_NAME$]", "[$COMPANY_MX$]", "[$COMPANY_NAME$]", "[$PORTAL_URL$]",
                            "[$USER_EMAIL_ADDRESS$]", "[$USER_NAME$]", "[$USER_PASSWORD$]"},
                    new String[] {company.getEmailAddress(), adminName, company.getMx(), company.getName(), company.getPortalURL(),
                            user.getEmailAddress(), user.getFullName(), user.getPassword()});

            body = StringUtil.replace(body,
                    new String[] {"[$ADMIN_EMAIL_ADDRESS$]", "[$ADMIN_NAME$]", "[$COMPANY_MX$]", "[$COMPANY_NAME$]", "[$PORTAL_URL$]",
                            "[$USER_EMAIL_ADDRESS$]", "[$USER_NAME$]", "[$USER_PASSWORD$]"},
                    new String[] {company.getEmailAddress(), adminName, company.getMx(), company.getName(), company.getPortalURL(),
                            user.getEmailAddress(), user.getFullName(), user.getPassword()});

            try {
                MailManagerUtil.sendEmail(new MailMessage(new InternetAddress(company.getEmailAddress(), adminName),
                        new InternetAddress(user.getEmailAddress(), user.getFullName()), subject, body));
            } catch (IOException ioe) {
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
        final String token = ResetPasswordTokenUtil.createToken();
        user.setIcqId(token);

        UserUtil.update(user);

        // Send new password

        Company company = CompanyUtil.findByPrimaryKey(companyId);

        String url = UrlStrategyUtil.getURL(company, Map.of(UrlStrategy.USER, user, UrlStrategy.TOKEN, token, UrlStrategy.LOCALE, locale),
                (fromAngular) ? UserService.ANGULAR_RESET_PASSWORD_URL_STRATEGY : UserService.DEFAULT_RESET_PASSWORD_URL_STRATEGY);

        String body = LanguageUtil.format(locale, "reset-password-email-body", url, false);
        String subject = LanguageUtil.get(locale, "reset-password-email-subject");

        try {
            EmailUtils.sendMail(user, company, subject, body);
        } catch (Exception ioe) {
            throw new SystemException(ioe);
        }
    }

    @Override
    public void test() {
        String userId = null;

        try {
            userId = getUserId();
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }

        _log.info(userId);
    }

    @Override
    public User updateActive(String userId, boolean active) throws PortalException, SystemException {

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
    public User updateAgreedToTermsOfUse(boolean agreedToTermsOfUse) throws PortalException, SystemException {

        User user = UserUtil.findByPrimaryKey(getUserId());

        user.setAgreedToTermsOfUse(agreedToTermsOfUse);

        UserUtil.update(user);

        return user;
    }

    @Override
    public User updateLastLogin(String loginIP) throws PortalException, SystemException {

        User user = UserUtil.findByPrimaryKey(getUserId());

        if (user.getLoginDate() == null && user.getLastLoginDate() == null) {
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
    public void updatePortrait(String userId, byte[] bytes) throws PortalException, SystemException {

        userId = userId.trim().toLowerCase();

        if (!getUserId().equals(userId) && !hasAdmin(userId)) {
            throw new PrincipalException();
        }

        ImageLocalUtil.put(userId, bytes);
    }

    @Override
    public User updateUser(String userId, String password1, String password2, boolean passwordReset)
            throws PortalException, SystemException {

        User user = UserUtil.findByPrimaryKey(userId);

        if (!getUserId().equals(userId) && !hasAdministrator(user.getCompanyId())) {

            throw new PrincipalException();
        }

        return UserLocalManagerUtil.updateUser(userId, password1, password2, passwordReset);
    }

    @Override
    public User updateUser(String userId, String password, String firstName, String middleName, String lastName, String nickName,
            boolean male, Date birthday, String emailAddress, String smsId, String aimId, String icqId, String msnId, String ymId,
            String favoriteActivity, String favoriteBibleVerse, String favoriteFood, String favoriteMovie, String favoriteMusic,
            String languageId, String timeZoneId, String skinId, boolean dottedSkins, boolean roundedSkins, String greeting,
            String resolution, String refreshRate, String comments) throws PortalException, SystemException {

        User user = UserUtil.findByPrimaryKey(userId);

        if (!getUserId().equals(userId) && !hasAdministrator(user.getCompanyId())) {

            throw new PrincipalException();
        }

        return UserLocalManagerUtil.updateUser(userId, password, firstName, middleName, lastName, nickName, male, birthday, emailAddress,
                smsId, aimId, icqId, msnId, ymId, favoriteActivity, favoriteBibleVerse, favoriteFood, favoriteMovie, favoriteMusic,
                languageId, timeZoneId, skinId, dottedSkins, roundedSkins, greeting, resolution, refreshRate, comments);
    }

    // Permission methods

    @Override
    public boolean hasAdmin(String userId) throws PortalException, SystemException {

        User user = UserUtil.findByPrimaryKey(userId);

        if (hasAdministrator(user.getCompanyId())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Authenticates the user based on their e-mail or user ID.
     *
     * @param companyId      The ID of the company that the user belongs to.
     * @param login          The identification mechanism: The user e-mail, or the user ID.
     * @param password       The user password.
     * @param byEmailAddress If the user authentication is performed against e-mail, set this to {@code true}. If it's
     *                       against the user ID, set to {@code false}.
     *
     * @return A status code indicating the result of the operation: {@link Authenticator#SUCCESS},
     * {@link Authenticator#FAILURE}, or {@link Authenticator#DNE}.
     *
     * @throws PortalException There's a problem with the information provided by or retrieved for the user.
     * @throws SystemException User information could not be updated.
     */
    private int authenticate(final String companyId, String login, final String password, final boolean byEmailAddress)
            throws PortalException, SystemException {
        login = login.trim().toLowerCase();
        Logger.debug(this, String.format("Authenticating user '%s'", login));
        this.validateInputData(login, password, byEmailAddress);
        Logger.debug(this, String.format("Running Login PRE authenticators by %s for User '%s'", byEmailAddress ?
                                                                                                         "email" :
                                                                                                         "user ID",
                login));
        int authResult = this.runCustomAuthenticators(companyId, login, password, byEmailAddress,
                PropsUtil.getArray(PropsUtil.AUTH_PIPELINE_PRE));
        final User user = this.findUser(companyId, login, byEmailAddress);
        if (null == user) {
            this.delayRequest(1);
            return Authenticator.DNE;
        }
        this.checkPasswordExpiration(login, user);
        if (authResult == Authenticator.SUCCESS) {
            authResult = LoginFactory.passwordMatch(password, user) ? Authenticator.SUCCESS : Authenticator.FAILURE;
            Logger.debug(this, String.format("Does password for User '%s' match? %s", login,
                    authResult == Authenticator.SUCCESS));
            if (authResult == Authenticator.SUCCESS) {
                this.checkUserStatus(user);
                Logger.debug(this, String.format("Running Login POST authenticators by %s for User '%s'",
                        byEmailAddress ? "email" : "user ID", login));
                authResult = this.runCustomAuthenticators(companyId, login, password, byEmailAddress,
                        PropsUtil.getArray(PropsUtil.AUTH_PIPELINE_POST));
                if (authResult == Authenticator.SUCCESS && user.getFailedLoginAttempts() > 0) {
                    // User authenticated, reset failed attempts
                    Logger.debug(this, String.format("Setting failed login attempts for user '%s' to zero.",
                            user.getUserId()));
                    user.setFailedLoginAttempts(0);
                    UserUtil.update(user);
                }
            }
        }
        if (authResult == Authenticator.FAILURE) {
            this.processFailedLogin(user, login, companyId, byEmailAddress);
        }
        return authResult;
    }

    /**
     * Validates that the incoming authentication parameters are valid.
     *
     * @param login          The user's email or ID.
     * @param password       The human-readable user's password.
     * @param byEmailAddress If the current authentication method is via email, set this to {@code true}. If it's
     *                       done via User ID, set to {@code false}.
     *
     * @throws UserEmailAddressException The specified email address has an invalid format.
     * @throws UserIdException           The specified User ID is null or empty.
     * @throws UserPasswordException     The specified password is null or empty.
     */
    private void validateInputData(final String login, final String password, final boolean byEmailAddress) throws UserEmailAddressException, UserIdException, UserPasswordException {
        if (byEmailAddress) {
            if (!Validator.isEmailAddress(login)) {
                Logger.error(this, String.format("Email '%s' is not valid.", login));
                throw new UserEmailAddressException();
            }
        } else {
            if (Validator.isNull(login)) {
                Logger.error(this, String.format("User Id/email '%s' cannot be null or empty.", login));
                throw new UserIdException();
            }
        }
        if (Validator.isNull(password)) {
            Logger.error(this, "Password cannot be null");
            throw new UserPasswordException(UserPasswordException.PASSWORD_INVALID);
        }
    }

    /**
     * Executes the list of custom authenticators. Developers can execute them before and/or after the default
     * authentication process in dotCMS, in case an additional security layer is required. If the array of
     * authenticators is empty, the {@link Authenticator#SUCCESS} will be returned by default.
     * <p>Here are the properties that can be used to declare such authenticators:
     * <ul>
     *     <li>Pre-Authenticators: {@link PropsUtil#AUTH_PIPELINE_PRE}</li>
     *     <li>Post-Authenticators: {@link PropsUtil#AUTH_PIPELINE_POST}</li>
     * </ul>
     * </p>
     *
     * @param companyId      The current Company ID.
     * @param login          The user's email or ID.
     * @param password       The human-readable user's password.
     * @param byEmailAddress If the current authentication method is via email, set this to {@code true}. If it's
     *                       done via User ID, set to {@code false}.
     * @param classes        The list of pre- or post-authenticator classes, specified in either the
     *                       {@link PropsUtil#AUTH_PIPELINE_PRE} or {@link PropsUtil#AUTH_PIPELINE_POST} properties.
     *
     * @return The result of the authentication process: {@link Authenticator#SUCCESS}, {@link Authenticator#FAILURE},
     * or {@link Authenticator#DNE}.
     *
     * @throws AuthException An error occurred when executing one of the authenticators.
     */
    private int runCustomAuthenticators(final String companyId, final String login, final String password,
                                        final boolean byEmailAddress, final String[] classes) throws AuthException {
        return byEmailAddress ? AuthPipeProxy.authenticateByEmailAddress(classes, companyId, login, password) :
                       AuthPipeProxy.authenticateByUserId(classes, companyId, login, password);
    }

    /**
     * Finds the User in dotCMS that matches the specified email or user ID.
     *
     * @param companyId      The current Company ID.
     * @param login          The user's email or ID.
     * @param byEmailAddress If the current authentication method is via email, set this to {@code true}. If it's done
     *                       via User ID, set to {@code false}.
     *
     * @return The {@link User} matching the specified email or user ID.
     *
     * @throws SystemException An error occurred when trying to find the specified user.
     */
    private User findUser(final String companyId, final String login, final boolean byEmailAddress) throws SystemException {
        User user = null;
        try {
            user = byEmailAddress ? UserUtil.findByC_EA(companyId, login) : UserUtil.findByC_U(companyId, login);
        } catch (final NoSuchUserException e) {
            Logger.error(this, String.format("User '%s' does not exist.", login));
        }
        return user;
    }

    /**
     * Verifies whether the User's password has expired or not. If it has, then such a User must be updated for dotCMS
     * to force them to rest their password the next time they log in.
     *
     * @param login The User's email or ID.
     * @param user  The {@link User} being checked.
     *
     * @throws SystemException An error occurred when updating the User's password reset status.
     */
    private void checkPasswordExpiration(final String login, final User user) throws SystemException {
        if (null != user && user.isPasswordExpired()) {
            Logger.debug(this, String.format("Password for user '%s' has expired.", login));
            user.setPasswordReset(true);
            UserUtil.update(user);
        }
    }

    /**
     * Checks whether the specified User is currently active or not.
     *
     * @param user The {@link User} being checked.
     *
     * @throws UserActiveException The specified User is NOT active.
     */
    private void checkUserStatus(final User user) throws UserActiveException {
        if (null != user && !user.getActive()) {
            final String errorMsg = String.format("User '%s' logged in successfully, but it's not active.",
                    user.getUserId());
            Logger.error(this, errorMsg);
            throw new UserActiveException(errorMsg);
        }
    }

    /**
     * Executes the optional custom on-failure handlers and updates the User information after a failed login attempt.
     *
     * @param user           The {@link User} that failed to log in.
     * @param login          The user's email or ID.
     * @param companyId      The current Company ID.
     * @param byEmailAddress If the current authentication method is via email, set this to {@code true}. If it's
     *                       done via User ID, set to {@code false}.
     */
    private void processFailedLogin(final User user, final String login, final String companyId,
                                    final boolean byEmailAddress) {
        Logger.debug(this, String.format("Authentication for user '%s' has failed.", login));
        try {
            this.runCustomOnFailureHandlers(companyId, login, byEmailAddress);
            this.handleFailedLoginAttempt(user, login, companyId, byEmailAddress);
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when handling failed login for User '%s': " +
                                                          "%s", login, e.getMessage());
            Logger.error(this, errorMsg, e);
        }
    }

    /**
     * Executes the list of custom failure handlers. They can be executed after a given User has failed to authenticate.
     * It can be specified via the following property: {@link PropsUtil#AUTH_FAILURE}.
     *
     * @param companyId      The current Company ID.
     * @param login          The user's email or ID.
     * @param byEmailAddress If the current authentication method is via email, set this to {@code true}. If it's
     *                       done via User ID, set to {@code false}.
     *
     * @throws AuthException An error occurred when executing one of the authenticators.
     */
    private void runCustomOnFailureHandlers(String companyId, String login, boolean byEmailAddress) throws AuthException {
        if (byEmailAddress) {
            AuthPipeProxy.onFailureByEmailAddress(PropsUtil.getArray(PropsUtil.AUTH_FAILURE), companyId, login);
        } else {
            AuthPipeProxy.onFailureByUserId(PropsUtil.getArray(PropsUtil.AUTH_FAILURE), companyId, login);
        }
    }

    /**
     * Handles all the process related to a User that effectively failed to log into the current dotCMS environment. For
     * instance:
     * <ul>
     *     <li>Applies a login-delaying strategy to avoid brute-force attacks.</li>
     *     <li>Increases the counter of failed login attempts for the User.</li>
     *     <li>If the maximum number of failed login attempts has been reached, it executes the custom handlers for
     *     maximum failed login attempts.</li>
     * </ul>
     *
     * @param user           The {@link User} that tried to log into dotCMS.
     * @param login          The User's email or ID.
     * @param companyId      The current Company ID.
     * @param byEmailAddress If the current authentication method is via email, set this to {@code true}. If it's
     *                       done via User ID, set to {@code false}.
     *
     * @throws SystemException User failed attempts data could not be saved.
     * @throws AuthException   An error occurred when executing the handlers for maximum failed loin attempts.
     */
    private void handleFailedLoginAttempt(final User user, final String login, final String companyId, final boolean byEmailAddress) throws SystemException, AuthException {
        int failedLoginAttempts = user.getFailedLoginAttempts();
        Logger.debug(this, String.format("Current failed login attempts for user '%s' is %s", login,
                failedLoginAttempts));
        if (Config.getBooleanProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, true)) {
            Logger.debug(this, String.format("Delaying login attempts for user '%s'", login));
            this.delayRequest(failedLoginAttempts);
        }
        user.setFailedLoginAttempts(++failedLoginAttempts);
        Logger.debug(this, String.format("Setting failed login attempts for user '%s' to %s", login,
                user.getFailedLoginAttempts()));
        UserUtil.update(user);
        final int maxFailures = GetterUtil.get(PropsUtil.get(PropsUtil.AUTH_MAX_FAILURES_LIMIT), 0);
        Logger.debug(this, String.format("Maximum failed login attempts is %s", maxFailures));
        if ((failedLoginAttempts >= maxFailures) && (maxFailures != 0)) {
            Logger.debug(this, String.format("User '%s' failed to authenticate via '%s' after %s failed attempts",
                    login, byEmailAddress ? "email" : "user ID", failedLoginAttempts));
            this.runCustomMaxFailuresHandlers(login, companyId, byEmailAddress);
        }
    }

    /**
     * Executes the list of custom failure handlers when the maximum number of login attempts has been reached. It can
     * be specified via the following property: {@link PropsUtil#AUTH_MAX_FAILURES}.
     *
     * @param login          The user's email or ID.
     * @param companyId      The current Company ID.
     * @param byEmailAddress If the current authentication method is via email, set this to {@code true}. If it's
     *                       done via User ID, set to {@code false}.
     *
     * @throws AuthException An error occurred when executing one of the authenticators.
     */
    private void runCustomMaxFailuresHandlers(final String login, final String companyId,
                                              final boolean byEmailAddress) throws AuthException {
        if (byEmailAddress) {
            AuthPipeProxy.onMaxFailuresByEmailAddress(PropsUtil.getArray(PropsUtil.AUTH_MAX_FAILURES), companyId,
                    login);
        } else {
            AuthPipeProxy.onMaxFailuresByUserId(PropsUtil.getArray(PropsUtil.AUTH_MAX_FAILURES), companyId, login);
        }
    }

    /**
     * If the user trying to authenticate has failed to do so, their login process will be penalized in
     * order to prevent potential hacking attacks. The time that the user will have to wait is based on
     * a specific delay strategy. It defaults to raising the {@code defaultSeed} value to the power of
     * 2.
     * 
     * @param defaultSeed - The default time seed in case the delay strategy does not specify one.
     */
    private void delayRequest(int defaultSeed) {
        int seed = defaultSeed;
        String delayStrat = Config.getStringProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY, "pow");
        String[] stratParams = delayStrat.split(":");
        DelayStrategy strategy;
        try {
            strategy = (UtilMethods.isSet(stratParams[0])) ? DelayStrategy.valueOf(stratParams[0].toUpperCase()) : DelayStrategy.POW;
            if (stratParams.length > 1) {
                seed = ConversionUtils.toInt(stratParams[1], defaultSeed);
            }

            Logger.debug(this, "Doing a delay request, with seed: " + seed + ", defaultSeed: " + defaultSeed + ", strategy: " + strategy);
        } catch (Exception e) {
            Logger.error(this, "The specified delay strategy is invalid. Defaults to POW strategy.", e);
            strategy = DelayStrategy.POW;
        }

        SecurityUtils.delayRequest(seed, strategy);
    }

    /**
     * 
     */
    public void resetPassword(String userId, String token, String newPassword) throws com.dotmarketing.business.NoSuchUserException,
            DotSecurityException, DotInvalidTokenException, DotInvalidPasswordException {
        try {
                final User user = APILocator.getUserAPI().loadUserById(userId);
                ResetPasswordTokenUtil.checkToken(user, token);
                APILocator.getUserAPI().updatePassword(user, newPassword, APILocator.getUserAPI().getSystemUser(), false);
        } catch (DotDataException e) {
            throw new IllegalArgumentException();
        }
    }

}
