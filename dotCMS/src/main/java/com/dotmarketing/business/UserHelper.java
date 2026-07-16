package com.dotmarketing.business;

import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.*;
import com.liferay.portal.ejb.UserUtil;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.pwd.PwdToolkitUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.InstancePool;
import com.liferay.util.Time;
import com.liferay.util.Validator;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Locale;

public class UserHelper {

    private final UserAPI userAPI;
    public static final int MAX_FIELD_LENGTH = 100;

    private static class SingletonHolder {
        private static final UserHelper INSTANCE = new UserHelper();
    }

    public static UserHelper getInstance() {
        return UserHelper.SingletonHolder.INSTANCE;
    }

    private UserHelper() {
        userAPI = APILocator.getUserAPI();
    }

    public User getUserObject(final String companyId, final boolean autoUserId,
            final String userId, final boolean autoPassword,
            final String password1, final String password2,
            final boolean passwordReset, final String firstName,
            final String middleName, final String lastName,
            final String nickName, final boolean male, final Date birthday,
            final String emailAddress, final Locale locale)
            throws DotDataException, SystemException, PortalException {

        final boolean alwaysAutoUserId = GetterUtil.getBoolean(
                PropsUtil.get(PropsUtil.USERS_ID_ALWAYS_AUTOGENERATE));

        validate(
                companyId, alwaysAutoUserId? true: autoUserId, userId.trim().toLowerCase(), autoPassword, password1, password2,
                firstName, lastName, emailAddress.trim().toLowerCase());

        final User user = new User();

        user.setUserId(userId.trim().toLowerCase());
        user.setEmailAddress(emailAddress.trim().toLowerCase());
        user.setPasswordReset(passwordReset);
        user.setFirstName(firstName);
        user.setMiddleName(middleName);
        user.setLastName(lastName);
        user.setNickName(nickName);
        user.setMale(male);
        user.setBirthday(birthday);
        user.setEmailAddress(emailAddress);

        final int passwordsLifespan = GetterUtil.getInteger(PropsUtil.get(PropsUtil.PASSWORDS_LIFESPAN));

        Date expirationDate = null;
        if (passwordsLifespan > 0) {
            expirationDate = new Date(
                    System.currentTimeMillis() + Time.DAY * passwordsLifespan);
        }

        user.setPasswordExpirationDate(expirationDate);
        user.setCompanyId(companyId);
        user.setCreateDate(new Date());

        // Use new password hash method
        try {
            user.setPassword(
                    PasswordFactoryProxy.generateHash(autoPassword? PwdToolkitUtil.generate(): password1));
        } catch (PasswordException e) {
            Logger.error(UserHelper.class,
                    "An error occurred generating the hashed password for userId: " + userId, e);
            throw new SystemException("An error occurred generating the hashed password.");
        }

        User defaultUser = userAPI.getDefaultUser();

        String greeting;
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
        user.setGreeting(StringUtils.abbreviate(greeting, 100));
        user.setResolution(defaultUser.getResolution());
        user.setRefreshRate(defaultUser.getRefreshRate());
        user.setLayoutIds("");
        user.setActive(true);

        return user;

    }

    private void validate(
            final String companyId, final boolean autoUserId, final String userId,
            final boolean autoPassword, final String password1, final String password2,
            final String firstName, final String lastName, final String emailAddress)
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

            final com.dotmarketing.auth.UserIdValidator userIdValidator = (UserIdValidator) InstancePool.get(
                    PropsUtil.get(PropsUtil.USERS_ID_VALIDATOR));

            if (!userIdValidator.validate(userId, companyId)) {
                throw new UserIdException();
            }

            try {
                User user = userAPI.loadUserById(userId);

                if (user != null) {
                    throw new DuplicateUserIdException();
                }
            }
            catch (com.dotmarketing.business.NoSuchUserException | DotDataException | DotSecurityException nsue) {
                Logger.warn(this, "Error loading user with id " + userId);
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

    public static void validateMaximumLength(final String firstName, final String lastName, final String email) throws DotDataException {
        if (UtilMethods.exceedsMaxLength(firstName, MAX_FIELD_LENGTH)) {
            throw new DotDataException("Length of First Name provided exceeds the maximum limit " + MAX_FIELD_LENGTH);
        }
        if (UtilMethods.exceedsMaxLength(lastName, MAX_FIELD_LENGTH)) {
            throw new DotDataException("Length of Last Name provided exceeds the maximum limit " + MAX_FIELD_LENGTH);
        }
        if (UtilMethods.exceedsMaxLength(email, MAX_FIELD_LENGTH)) {
            throw new DotDataException("Length of Email Address provided exceeds the maximum limit "+ MAX_FIELD_LENGTH);
        }
    }

    public static void validateMaximumLength(final String firstName, final String lastName, final String email,
                                             final String middleName, final String nickName, final String birthday) throws DotDataException {

        validateMaximumLength(firstName, lastName, email); // Call the existing method

        // Validate the additional fields
        if (UtilMethods.exceedsMaxLength(middleName, MAX_FIELD_LENGTH)) {
            throw new DotDataException("Length of Middle Name provided exceeds the maximum limit " + MAX_FIELD_LENGTH);
        }
        if (UtilMethods.exceedsMaxLength(nickName, MAX_FIELD_LENGTH)) {
            throw new DotDataException("Length of Nick Name provided exceeds the maximum limit " + MAX_FIELD_LENGTH);
        }
        if (UtilMethods.exceedsMaxLength(birthday, MAX_FIELD_LENGTH)) {
            throw new DotDataException("Length of Birthday provided exceeds the maximum limit " + MAX_FIELD_LENGTH);
        }
    }


}
