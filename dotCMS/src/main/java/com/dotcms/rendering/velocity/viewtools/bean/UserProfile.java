package com.dotcms.rendering.velocity.viewtools.bean;

import com.liferay.portal.model.User;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Read-only view of a user account, as returned to templates by the {@code $cmsuser} tool's
 * lookups ({@code getUserByEmail}, {@code getUserByUserId}, {@code getAllUsersInGroup}).
 *
 * <p>It carries the display fields templates print, such as the id, names, email address,
 * locale and active state, using the same getter names as {@link User}, so {@code $u.fullName}
 * and {@code $u.getFullName()} keep working. It deliberately leaves out the credential fields
 * kept on the user record (password, password-reset and remember-me tokens), the additional
 * info map and login IP addresses, has no setters, and gives no access to the underlying
 * {@link User}.
 */
public final class UserProfile {

    private final User user;

    /**
     * Creates a profile of the given user.
     *
     * @param user the account to describe; must not be {@code null}
     */
    public UserProfile(final User user) {
        this.user = Objects.requireNonNull(user, "user");
    }

    /** @return the user id */
    public String getUserId() {
        return user.getUserId();
    }

    /** @return the company id the user belongs to */
    public String getCompanyId() {
        return user.getCompanyId();
    }

    /** @return the email address */
    public String getEmailAddress() {
        return user.getEmailAddress();
    }

    /** @return the first name */
    public String getFirstName() {
        return user.getFirstName();
    }

    /** @return the middle name */
    public String getMiddleName() {
        return user.getMiddleName();
    }

    /** @return the last name */
    public String getLastName() {
        return user.getLastName();
    }

    /** @return the nickname */
    public String getNickName() {
        return user.getNickName();
    }

    /** @return the first, middle and last name joined for display */
    public String getFullName() {
        return user.getFullName();
    }

    /** @return the user's language id, for example {@code en_US} */
    public String getLanguageId() {
        return user.getLanguageId();
    }

    /** @return the user's locale */
    public Locale getLocale() {
        return user.getLocale();
    }

    /** @return the user's time zone id */
    public String getTimeZoneId() {
        return user.getTimeZoneId();
    }

    /** @return the user's time zone */
    public TimeZone getTimeZone() {
        return user.getTimeZone();
    }

    /** @return the birthday, if set */
    public Date getBirthday() {
        return user.getBirthday();
    }

    /** @return {@code true} if the user is recorded as male */
    public boolean getMale() {
        return user.getMale();
    }

    /** @return {@code true} if the user is recorded as male */
    public boolean isMale() {
        return user.isMale();
    }

    /** @return {@code true} if the user is recorded as female */
    public boolean getFemale() {
        return user.getFemale();
    }

    /** @return {@code true} if the user is recorded as female */
    public boolean isFemale() {
        return user.isFemale();
    }

    /** @return {@code true} if the account is active */
    public boolean getActive() {
        return user.getActive();
    }

    /** @return {@code true} if the account is active */
    public boolean isActive() {
        return user.isActive();
    }

    /** @return when the account was created */
    public Date getCreateDate() {
        return user.getCreateDate();
    }

    /** @return when the account was last modified */
    public Date getModificationDate() {
        return user.getModificationDate();
    }

    /** @return when the user logged in, for the current or most recent session */
    public Date getLoginDate() {
        return user.getLoginDate();
    }

    /** @return when the user logged in before the current session, usually shown as last access */
    public Date getLastLoginDate() {
        return user.getLastLoginDate();
    }

    /** @return the comments stored on the account */
    public String getComments() {
        return user.getComments();
    }

    /** @return {@code true} if the user holds the CMS Administrator role */
    public boolean isAdmin() {
        return user.isAdmin();
    }

    /** @return {@code true} if the user holds the back-end user role */
    public boolean isBackendUser() {
        return user.isBackendUser();
    }

    /** @return {@code true} if the user holds the front-end user role */
    public boolean isFrontendUser() {
        return user.isFrontendUser();
    }

    /** @return {@code true} if this is the anonymous user */
    public boolean isAnonymousUser() {
        return user.isAnonymousUser();
    }

    /** @return {@code true} if this is the default user */
    public boolean isDefaultUser() {
        return user.isDefaultUser();
    }

    /**
     * Two profiles are equal when they describe the same user id.
     *
     * @param other the object to compare with
     * @return {@code true} if {@code other} is a profile of the same account
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof UserProfile
                && Objects.equals(getUserId(), ((UserProfile) other).getUserId());
    }

    /** @return a hash of the user id */
    @Override
    public int hashCode() {
        return Objects.hashCode(getUserId());
    }

    /** @return the same summary {@link User#toString()} prints: full name, id and email */
    @Override
    public String toString() {
        return user.toString();
    }
}
