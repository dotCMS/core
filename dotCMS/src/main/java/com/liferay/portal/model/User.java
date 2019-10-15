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

package com.liferay.portal.model;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.codec.digest.DigestUtils;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.user.ajax.UserAjax;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.Recipient;
import com.liferay.util.LocaleUtil;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;
import com.liferay.util.Validator;

import io.vavr.control.Try;

/**
 * <a href="User.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.34 $
 *
 */
public class User extends UserModel implements Recipient {

	public static final String DEFAULT = "default";

	public static String getDefaultUserId(String companyId) {
		return companyId + "." + DEFAULT;
	}

	public static String getFullName(
		String firstName, String middleName, String lastName) {

		if (Validator.isNull(middleName)) {
			return firstName + StringPool.SPACE + lastName;
		}
		else {
			return firstName + StringPool.SPACE + middleName +
				StringPool.SPACE + lastName;
		}
	}

	@Override
	public String toString() {
		return this.getFullName() + " [ID: " + this.getUserId() + "][email:" + this.getEmailAddress() + "]";
	}

	public User() {
		super();
	}

	public User(String userId) {
		super(userId);
	}

	public User(String userId, String companyId, String password,
				boolean passwordEncrypted, Date passwordExpirationDate,
				boolean passwordReset, String firstName, String middleName,
				String lastName, String nickName, boolean male, Date birthday,
				String emailAddress, String smsId, String aimId, String icqId,
				String msnId, String ymId, String favoriteActivity,
				String favoriteBibleVerse, String favoriteFood,
				String favoriteMovie, String favoriteMusic, String languageId,
				String timeZoneId, String skinId, boolean dottedSkins,
				boolean roundedSkins, String greeting, String resolution,
				String refreshRate, String layoutIds, String comments,
				Date createDate, Date loginDate, String loginIP,
				Date lastLoginDate, String lastLoginIP, int failedLoginAttempts,
				boolean agreedToTermsOfUse, boolean active, boolean deleteInProgress, Date deleteDate) {

		super(userId, companyId, password, passwordEncrypted,
			  passwordExpirationDate, passwordReset, firstName, middleName,
			  lastName, nickName, male, birthday, emailAddress, smsId, aimId,
			  icqId, msnId, ymId, favoriteActivity, favoriteBibleVerse,
			  favoriteFood, favoriteMovie, favoriteMusic, languageId,
			  timeZoneId, skinId, dottedSkins, roundedSkins, greeting,
			  resolution, refreshRate, layoutIds, comments, createDate,
			  loginDate, loginIP, lastLoginDate, lastLoginIP,
			  failedLoginAttempts, agreedToTermsOfUse, active, deleteInProgress, deleteDate);

		setCompanyId(companyId);
		setLanguageId(languageId);
		setTimeZoneId(timeZoneId);
		setResolution(resolution);
		setRefreshRate(refreshRate);
	}

	public boolean isDefaultUser() {
		return _defaultUser;
	}

	public void setCompanyId(String companyId) {
		if (companyId.equalsIgnoreCase(DEFAULT)) {
			_defaultUser = true;
		}
		else {
			_defaultUser = false;
		}

		super.setCompanyId(companyId);
	}

	public String getActualCompanyId() {
		if (isDefaultUser()) {
			return getUserId().substring(
				0, getUserId().indexOf(User.DEFAULT) - 1);
		}
		else {
			return getCompanyId();
		}
	}



	public boolean isPasswordExpired() {
		if (getPasswordExpirationDate() != null &&
			getPasswordExpirationDate().before(new Date())) {

			return true;
		}
		else {
			return false;
		}
	}

	public String getFullName() {
		String firstName = getFirstName();
		firstName = (UtilMethods.isSet(firstName) ? firstName : "");
		String middleName = getMiddleName();
		middleName = (UtilMethods.isSet(middleName) ? middleName : "");
		String lastName = getLastName();
		lastName = (UtilMethods.isSet(lastName) ? lastName : "");
		return getFullName(firstName,middleName,lastName);
	}

	public boolean getFemale() {
		return !getMale();
	}

	public boolean isFemale() {
		return !isMale();
	}

	public void setFemale(boolean female) {
		super.setMale(!female);
	}

	public Locale getLocale() {
		return _locale;
	}

	public void setLanguageId(String languageId) {
		_locale = LocaleUtil.fromLanguageId(languageId);

		super.setLanguageId(_locale.getLanguage() + "_" + _locale.getCountry());
	}
	public void setLocale(Locale locale) {
		_locale = locale;

		super.setLanguageId(_locale.getLanguage() + "_" + _locale.getCountry());
	}

	public TimeZone getTimeZone() {
		return _timeZone;
	}

	public void setTimeZoneId(String timeZoneId) {
		if (Validator.isNull(timeZoneId)) {
			timeZoneId = TimeZone.getDefault().getID();
		}

		_timeZone = TimeZone.getTimeZone(timeZoneId);

		super.setTimeZoneId(timeZoneId);
	}

	public boolean hasCustomSkin() {
		if (getUserId().equals(getSkinId())) {
			return true;
		}
		else {
			return false;
		}
	}
	@JsonIgnore
	public void setResolution(String resolution) {
		if (Validator.isNull(resolution)) {
			resolution = PropsUtil.get(
				PropsUtil.DEFAULT_USER_LAYOUT_RESOLUTION);
		}

		super.setResolution(resolution);
	}
	@JsonIgnore
	public void setRefreshRate(String refreshRate) {
		if (Validator.isNull(refreshRate)) {
			refreshRate = PropsUtil.get(
				PropsUtil.DEFAULT_USER_LAYOUT_REFRESH_RATE);
		}

		super.setRefreshRate(refreshRate);
	}


	public BaseModel getProtected() {
		if (_user == null) {
			protect();
		}

		return _user;
	}

	public void protect() {
		_user = (User)this.clone();

		_user.setPassword(null);
		_user.setCreateDate(null);
        _user.setModificationDate(null);
		_user.setLoginDate(null);
		_user.setLoginIP(null);
		_user.setLastLoginDate(null);
		_user.setLastLoginIP(null);
		_user.setFailedLoginAttempts(0);
		_user.setAgreedToTermsOfUse(false);
		_user.setActive(false);
	}

	public String getRecipientId() {
		return getUserId();
	}

	public String getRecipientName() {
		return StringUtil.replace(
			getFullName(),
			new String[] {
				StringPool.COLON, StringPool.COMMA
			},
			new String[] {
				StringPool.BLANK, StringPool.BLANK
			});
	}

	public String getRecipientAddress() {
		return getEmailAddress();
	}

	public String getRecipientInternetAddress() {
		return getRecipientName() + " <" + getEmailAddress() + ">";
	}

	public boolean isMultipleRecipients() {
		return false;
	}

	public int compareTo(Object obj) {
		User user = (User)obj;

		return getFullName().toLowerCase().compareTo(
			user.getFullName().toLowerCase());
	}

    public Date getModificationDate() {

        return this.modificationDate;
    }

    public void setModificationDate(final Date modificationDate) {

        this.modificationDate = modificationDate;
        setModified(true);
    }

  public boolean isAnonymousUser(){
      return UserAPI.CMS_ANON_USER_ID.equals(this.getUserId());
  }

  public boolean isBackendUser() {

    return Try.of(() -> {
      if (isAnonymousUser()) {
        return false;
      }
      return (APILocator.getRoleAPI().doesUserHaveRole(this, APILocator.getRoleAPI().loadBackEndUserRole()));

    }).getOrElse(false);

  }

  public boolean isFrontendUser() {

    return Try.of(() -> {
      if (isAnonymousUser()) {
        return true;
      }
      return (APILocator.getRoleAPI().doesUserHaveRole(this, APILocator.getRoleAPI().loadFrontEndUserRole()));

    }).getOrElse(false);

  }

	
  public boolean hasConsoleAccess() {
    return Try.of(() -> {
      if (isAnonymousUser() || UserAPI.SYSTEM_USER_ID.equals(this.getUserId()) ) {
        return false;
      }
      return isActive() && isBackendUser() && !APILocator.getLayoutAPI().loadLayoutsForUser(this).isEmpty();

    }).getOrElse(false);

  }

    public Map<String, Object> toMap() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("active", this.getActive());
        map.put("actualCompanyId", this.getActualCompanyId());
        map.put("birthday", this.getBirthday());
        map.put("comments", this.getComments());
        map.put("companyId", this.getCompanyId());
        map.put("createDate", this.getCreateDate());
        map.put("modificationDate", this.getModificationDate());
        map.put("emailAddress", this.getEmailAddress());
        map.put("emailaddress", this.getEmailAddress());
        map.put("failedLoginAttempts", this.getFailedLoginAttempts());
        map.put("male", this.getMale());
        map.put("firstName", this.getFirstName());
        map.put("fullName", this.getFullName());
        map.put("name", getFullName());
        map.put("languageId", this.getLanguageId());
        map.put("lastLoginDate", this.getLastLoginDate());
        map.put("lastLoginIP", this.getLastLoginIP());
        map.put("lastName", this.getLastName());
        map.put("middleName", this.getMiddleName());
        map.put("female", this.getFemale());
        map.put("nickname", this.getNickName());
        map.put("timeZoneId", this.getTimeZoneId());
        map.put("deleteInProgress", getDeleteInProgress());
        map.put("deleteDate", getDeleteDate());
        map.put("passwordExpirationDate", getPasswordExpirationDate());
        map.put("passwordExpired", isPasswordExpired());
        map.put("passwordReset", isPasswordReset());
        map.put("userId", getUserId());
        map.put("backendUser", isBackendUser());
        map.put("frontendUser", isFrontendUser());
        map.put("hasConsoleAccess", hasConsoleAccess());
        map.put("id", getUserId());
        map.put("type", UserAjax.USER_TYPE_VALUE);
        map.put("gravitar", DigestUtils.md5Hex(this.getEmailAddress().toLowerCase()).toString());

        return map;
    }

	private boolean _defaultUser;
	private Locale _locale;
	private TimeZone _timeZone;
	private User _user;
	private Date modificationDate;

}