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

import java.util.Date;

import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.Xss;

/**
 * <a href="UserModel.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.93 $
 *
 */
public class UserModel extends BaseModel {
	public static boolean CACHEABLE = GetterUtil.get(PropsUtil.get(
				"value.object.cacheable.com.liferay.portal.model.User"),
			VALUE_OBJECT_CACHEABLE);
	public static int MAX_SIZE = GetterUtil.get(PropsUtil.get(
				"value.object.max.size.com.liferay.portal.model.User"),
			VALUE_OBJECT_MAX_SIZE);
	public static boolean XSS_ALLOW_BY_MODEL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User"), XSS_ALLOW);
	public static boolean XSS_ALLOW_USERID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.userId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_COMPANYID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.companyId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_PASSWORD = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.password"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_FIRSTNAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.firstName"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_MIDDLENAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.middleName"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_LASTNAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.lastName"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_NICKNAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.nickName"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_EMAILADDRESS = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.emailAddress"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_SMSID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.smsId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_AIMID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.aimId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_ICQID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.icqId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_MSNID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.msnId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_YMID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.ymId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_FAVORITEACTIVITY = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.favoriteActivity"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_FAVORITEBIBLEVERSE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.favoriteBibleVerse"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_FAVORITEFOOD = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.favoriteFood"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_FAVORITEMOVIE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.favoriteMovie"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_FAVORITEMUSIC = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.favoriteMusic"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_LANGUAGEID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.languageId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_TIMEZONEID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.timeZoneId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_SKINID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.skinId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_GREETING = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.greeting"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_RESOLUTION = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.resolution"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_REFRESHRATE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.refreshRate"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_LAYOUTIDS = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.layoutIds"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_COMMENTS = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.comments"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_LOGINIP = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.loginIP"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_LASTLOGINIP = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.User.lastLoginIP"),
			XSS_ALLOW_BY_MODEL);
	public static long LOCK_EXPIRATION_TIME = GetterUtil.getLong(PropsUtil.get(
				"lock.expiration.time.com.liferay.portal.model.UserModel"));

	public UserModel() {
	}

	public UserModel(String userId) {
		_userId = userId;
		setNew(true);
	}

	public UserModel(String userId, String companyId, String password,
		boolean passwordEncrypted, Date passwordExpirationDate,
		boolean passwordReset, String firstName, String middleName,
		String lastName, String nickName, boolean male, Date birthday,
		String emailAddress, String smsId, String aimId, String icqId,
		String msnId, String ymId, String favoriteActivity,
		String favoriteBibleVerse, String favoriteFood, String favoriteMovie,
		String favoriteMusic, String languageId, String timeZoneId,
		String skinId, boolean dottedSkins, boolean roundedSkins,
		String greeting, String resolution, String refreshRate,
		String layoutIds, String comments, Date createDate, Date loginDate,
		String loginIP, Date lastLoginDate, String lastLoginIP,
		int failedLoginAttempts, boolean agreedToTermsOfUse, boolean active) {
		_userId = userId;
		_companyId = companyId;
		_password = password;
		_passwordEncrypted = passwordEncrypted;
		_passwordExpirationDate = passwordExpirationDate;
		_passwordReset = passwordReset;
		_firstName = firstName;
		_middleName = middleName;
		_lastName = lastName;
		_nickName = nickName;
		_male = male;
		_birthday = birthday;
		_emailAddress = emailAddress;
		_smsId = smsId;
		_aimId = aimId;
		_icqId = icqId;
		_msnId = msnId;
		_ymId = ymId;
		_favoriteActivity = favoriteActivity;
		_favoriteBibleVerse = favoriteBibleVerse;
		_favoriteFood = favoriteFood;
		_favoriteMovie = favoriteMovie;
		_favoriteMusic = favoriteMusic;
		_languageId = languageId;
		_timeZoneId = timeZoneId;
		_skinId = skinId;
		_dottedSkins = dottedSkins;
		_roundedSkins = roundedSkins;
		_greeting = greeting;
		_resolution = resolution;
		_refreshRate = refreshRate;
		_layoutIds = layoutIds;
		_comments = comments;
		_createDate = createDate;
		_loginDate = loginDate;
		_loginIP = loginIP;
		_lastLoginDate = lastLoginDate;
		_lastLoginIP = lastLoginIP;
		_failedLoginAttempts = failedLoginAttempts;
		_agreedToTermsOfUse = agreedToTermsOfUse;
		_active = active;
	}

	public String getPrimaryKey() {
		return _userId;
	}

	public String getUserId() {
		return _userId;
	}

	public void setUserId(String userId) {
		if (((userId == null) && (_userId != null)) ||
				((userId != null) && (_userId == null)) ||
				((userId != null) && (_userId != null) &&
				!userId.equals(_userId))) {
			if (!XSS_ALLOW_USERID) {
				userId = Xss.strip(userId);
			}

			_userId = userId;
			setModified(true);
		}
	}

	public String getCompanyId() {
		return _companyId;
	}

	public void setCompanyId(String companyId) {
		if (((companyId == null) && (_companyId != null)) ||
				((companyId != null) && (_companyId == null)) ||
				((companyId != null) && (_companyId != null) &&
				!companyId.equals(_companyId))) {
			if (!XSS_ALLOW_COMPANYID) {
				companyId = Xss.strip(companyId);
			}

			_companyId = companyId;
			setModified(true);
		}
	}

	public String getPassword() {
		return _password;
	}

	public void setPassword(String password) {
		if (((password == null) && (_password != null)) ||
				((password != null) && (_password == null)) ||
				((password != null) && (_password != null) &&
				!password.equals(_password))) {
			if (!XSS_ALLOW_PASSWORD) {
				password = Xss.strip(password);
			}

			_password = password;
			setModified(true);
		}
	}

	public boolean getPasswordEncrypted() {
		return _passwordEncrypted;
	}

	public boolean isPasswordEncrypted() {
		return _passwordEncrypted;
	}

	public void setPasswordEncrypted(boolean passwordEncrypted) {
		if (passwordEncrypted != _passwordEncrypted) {
			_passwordEncrypted = passwordEncrypted;
			setModified(true);
		}
	}

	public Date getPasswordExpirationDate() {
		return _passwordExpirationDate;
	}

	public void setPasswordExpirationDate(Date passwordExpirationDate) {
		if (((passwordExpirationDate == null) &&
				(_passwordExpirationDate != null)) ||
				((passwordExpirationDate != null) &&
				(_passwordExpirationDate == null)) ||
				((passwordExpirationDate != null) &&
				(_passwordExpirationDate != null) &&
				!passwordExpirationDate.equals(_passwordExpirationDate))) {
			_passwordExpirationDate = passwordExpirationDate;
			setModified(true);
		}
	}

	public boolean getPasswordReset() {
		return _passwordReset;
	}

	public boolean isPasswordReset() {
		return _passwordReset;
	}

	public void setPasswordReset(boolean passwordReset) {
		if (passwordReset != _passwordReset) {
			_passwordReset = passwordReset;
			setModified(true);
		}
	}

	public String getFirstName() {
		return _firstName;
	}

	public void setFirstName(String firstName) {
		if (((firstName == null) && (_firstName != null)) ||
				((firstName != null) && (_firstName == null)) ||
				((firstName != null) && (_firstName != null) &&
				!firstName.equals(_firstName))) {
			if (!XSS_ALLOW_FIRSTNAME) {
				firstName = Xss.strip(firstName);
			}

			_firstName = firstName;
			setModified(true);
		}
	}

	public String getMiddleName() {
		return _middleName;
	}

	public void setMiddleName(String middleName) {
		if (((middleName == null) && (_middleName != null)) ||
				((middleName != null) && (_middleName == null)) ||
				((middleName != null) && (_middleName != null) &&
				!middleName.equals(_middleName))) {
			if (!XSS_ALLOW_MIDDLENAME) {
				middleName = Xss.strip(middleName);
			}

			_middleName = middleName;
			setModified(true);
		}
	}

	public String getLastName() {
		return _lastName;
	}

	public void setLastName(String lastName) {
		if (((lastName == null) && (_lastName != null)) ||
				((lastName != null) && (_lastName == null)) ||
				((lastName != null) && (_lastName != null) &&
				!lastName.equals(_lastName))) {
			if (!XSS_ALLOW_LASTNAME) {
				lastName = Xss.strip(lastName);
			}

			_lastName = lastName;
			setModified(true);
		}
	}

	public String getNickName() {
		return _nickName;
	}

	public void setNickName(String nickName) {
		if (((nickName == null) && (_nickName != null)) ||
				((nickName != null) && (_nickName == null)) ||
				((nickName != null) && (_nickName != null) &&
				!nickName.equals(_nickName))) {
			if (!XSS_ALLOW_NICKNAME) {
				nickName = Xss.strip(nickName);
			}

			_nickName = nickName;
			setModified(true);
		}
	}

	public boolean getMale() {
		return _male;
	}

	public boolean isMale() {
		return _male;
	}

	public void setMale(boolean male) {
		if (male != _male) {
			_male = male;
			setModified(true);
		}
	}

	public Date getBirthday() {
		return _birthday;
	}

	public void setBirthday(Date birthday) {
		if (((birthday == null) && (_birthday != null)) ||
				((birthday != null) && (_birthday == null)) ||
				((birthday != null) && (_birthday != null) &&
				!birthday.equals(_birthday))) {
			_birthday = birthday;
			setModified(true);
		}
	}

	public String getEmailAddress() {
		return _emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		if (((emailAddress == null) && (_emailAddress != null)) ||
				((emailAddress != null) && (_emailAddress == null)) ||
				((emailAddress != null) && (_emailAddress != null) &&
				!emailAddress.equals(_emailAddress))) {
			if (!XSS_ALLOW_EMAILADDRESS) {
				emailAddress = Xss.strip(emailAddress);
			}

			_emailAddress = emailAddress;
			setModified(true);
		}
	}

	public String getSmsId() {
		return _smsId;
	}

	public void setSmsId(String smsId) {
		if (((smsId == null) && (_smsId != null)) ||
				((smsId != null) && (_smsId == null)) ||
				((smsId != null) && (_smsId != null) && !smsId.equals(_smsId))) {
			if (!XSS_ALLOW_SMSID) {
				smsId = Xss.strip(smsId);
			}

			_smsId = smsId;
			setModified(true);
		}
	}

	public String getAimId() {
		return _aimId;
	}

	public void setAimId(String aimId) {
		if (((aimId == null) && (_aimId != null)) ||
				((aimId != null) && (_aimId == null)) ||
				((aimId != null) && (_aimId != null) && !aimId.equals(_aimId))) {
			if (!XSS_ALLOW_AIMID) {
				aimId = Xss.strip(aimId);
			}

			_aimId = aimId;
			setModified(true);
		}
	}

	public String getIcqId() {
		return _icqId;
	}

	public void setIcqId(String icqId) {
		if (((icqId == null) && (_icqId != null)) ||
				((icqId != null) && (_icqId == null)) ||
				((icqId != null) && (_icqId != null) && !icqId.equals(_icqId))) {
			if (!XSS_ALLOW_ICQID) {
				icqId = Xss.strip(icqId);
			}

			_icqId = icqId;
			setModified(true);
		}
	}

	public String getMsnId() {
		return _msnId;
	}

	public void setMsnId(String msnId) {
		if (((msnId == null) && (_msnId != null)) ||
				((msnId != null) && (_msnId == null)) ||
				((msnId != null) && (_msnId != null) && !msnId.equals(_msnId))) {
			if (!XSS_ALLOW_MSNID) {
				msnId = Xss.strip(msnId);
			}

			_msnId = msnId;
			setModified(true);
		}
	}

	public String getYmId() {
		return _ymId;
	}

	public void setYmId(String ymId) {
		if (((ymId == null) && (_ymId != null)) ||
				((ymId != null) && (_ymId == null)) ||
				((ymId != null) && (_ymId != null) && !ymId.equals(_ymId))) {
			if (!XSS_ALLOW_YMID) {
				ymId = Xss.strip(ymId);
			}

			_ymId = ymId;
			setModified(true);
		}
	}

	public String getFavoriteActivity() {
		return _favoriteActivity;
	}

	public void setFavoriteActivity(String favoriteActivity) {
		if (((favoriteActivity == null) && (_favoriteActivity != null)) ||
				((favoriteActivity != null) && (_favoriteActivity == null)) ||
				((favoriteActivity != null) && (_favoriteActivity != null) &&
				!favoriteActivity.equals(_favoriteActivity))) {
			if (!XSS_ALLOW_FAVORITEACTIVITY) {
				favoriteActivity = Xss.strip(favoriteActivity);
			}

			_favoriteActivity = favoriteActivity;
			setModified(true);
		}
	}

	public String getFavoriteBibleVerse() {
		return _favoriteBibleVerse;
	}

	public void setFavoriteBibleVerse(String favoriteBibleVerse) {
		if (((favoriteBibleVerse == null) && (_favoriteBibleVerse != null)) ||
				((favoriteBibleVerse != null) && (_favoriteBibleVerse == null)) ||
				((favoriteBibleVerse != null) && (_favoriteBibleVerse != null) &&
				!favoriteBibleVerse.equals(_favoriteBibleVerse))) {
			if (!XSS_ALLOW_FAVORITEBIBLEVERSE) {
				favoriteBibleVerse = Xss.strip(favoriteBibleVerse);
			}

			_favoriteBibleVerse = favoriteBibleVerse;
			setModified(true);
		}
	}

	public String getFavoriteFood() {
		return _favoriteFood;
	}

	public void setFavoriteFood(String favoriteFood) {
		if (((favoriteFood == null) && (_favoriteFood != null)) ||
				((favoriteFood != null) && (_favoriteFood == null)) ||
				((favoriteFood != null) && (_favoriteFood != null) &&
				!favoriteFood.equals(_favoriteFood))) {
			if (!XSS_ALLOW_FAVORITEFOOD) {
				favoriteFood = Xss.strip(favoriteFood);
			}

			_favoriteFood = favoriteFood;
			setModified(true);
		}
	}

	public String getFavoriteMovie() {
		return _favoriteMovie;
	}

	public void setFavoriteMovie(String favoriteMovie) {
		if (((favoriteMovie == null) && (_favoriteMovie != null)) ||
				((favoriteMovie != null) && (_favoriteMovie == null)) ||
				((favoriteMovie != null) && (_favoriteMovie != null) &&
				!favoriteMovie.equals(_favoriteMovie))) {
			if (!XSS_ALLOW_FAVORITEMOVIE) {
				favoriteMovie = Xss.strip(favoriteMovie);
			}

			_favoriteMovie = favoriteMovie;
			setModified(true);
		}
	}

	public String getFavoriteMusic() {
		return _favoriteMusic;
	}

	public void setFavoriteMusic(String favoriteMusic) {
		if (((favoriteMusic == null) && (_favoriteMusic != null)) ||
				((favoriteMusic != null) && (_favoriteMusic == null)) ||
				((favoriteMusic != null) && (_favoriteMusic != null) &&
				!favoriteMusic.equals(_favoriteMusic))) {
			if (!XSS_ALLOW_FAVORITEMUSIC) {
				favoriteMusic = Xss.strip(favoriteMusic);
			}

			_favoriteMusic = favoriteMusic;
			setModified(true);
		}
	}

	public String getLanguageId() {
		return _languageId;
	}

	public void setLanguageId(String languageId) {
		if (((languageId == null) && (_languageId != null)) ||
				((languageId != null) && (_languageId == null)) ||
				((languageId != null) && (_languageId != null) &&
				!languageId.equals(_languageId))) {
			if (!XSS_ALLOW_LANGUAGEID) {
				languageId = Xss.strip(languageId);
			}

			_languageId = languageId;
			setModified(true);
		}
	}

	public String getTimeZoneId() {
		return _timeZoneId;
	}

	public void setTimeZoneId(String timeZoneId) {
		if (((timeZoneId == null) && (_timeZoneId != null)) ||
				((timeZoneId != null) && (_timeZoneId == null)) ||
				((timeZoneId != null) && (_timeZoneId != null) &&
				!timeZoneId.equals(_timeZoneId))) {
			if (!XSS_ALLOW_TIMEZONEID) {
				timeZoneId = Xss.strip(timeZoneId);
			}

			_timeZoneId = timeZoneId;
			setModified(true);
		}
	}

	public String getSkinId() {
		return _skinId;
	}

	public void setSkinId(String skinId) {
		if (((skinId == null) && (_skinId != null)) ||
				((skinId != null) && (_skinId == null)) ||
				((skinId != null) && (_skinId != null) &&
				!skinId.equals(_skinId))) {
			if (!XSS_ALLOW_SKINID) {
				skinId = Xss.strip(skinId);
			}

			_skinId = skinId;
			setModified(true);
		}
	}

	public boolean getDottedSkins() {
		return _dottedSkins;
	}

	public boolean isDottedSkins() {
		return _dottedSkins;
	}

	public void setDottedSkins(boolean dottedSkins) {
		if (dottedSkins != _dottedSkins) {
			_dottedSkins = dottedSkins;
			setModified(true);
		}
	}

	public boolean getRoundedSkins() {
		return _roundedSkins;
	}

	public boolean isRoundedSkins() {
		return _roundedSkins;
	}

	public void setRoundedSkins(boolean roundedSkins) {
		if (roundedSkins != _roundedSkins) {
			_roundedSkins = roundedSkins;
			setModified(true);
		}
	}

	public String getGreeting() {
		return _greeting;
	}

	public void setGreeting(String greeting) {
		if (((greeting == null) && (_greeting != null)) ||
				((greeting != null) && (_greeting == null)) ||
				((greeting != null) && (_greeting != null) &&
				!greeting.equals(_greeting))) {
			if (!XSS_ALLOW_GREETING) {
				greeting = Xss.strip(greeting);
			}

			_greeting = greeting;
			setModified(true);
		}
	}

	public String getResolution() {
		return _resolution;
	}

	public void setResolution(String resolution) {
		if (((resolution == null) && (_resolution != null)) ||
				((resolution != null) && (_resolution == null)) ||
				((resolution != null) && (_resolution != null) &&
				!resolution.equals(_resolution))) {
			if (!XSS_ALLOW_RESOLUTION) {
				resolution = Xss.strip(resolution);
			}

			_resolution = resolution;
			setModified(true);
		}
	}

	public String getRefreshRate() {
		return _refreshRate;
	}

	public void setRefreshRate(String refreshRate) {
		if (((refreshRate == null) && (_refreshRate != null)) ||
				((refreshRate != null) && (_refreshRate == null)) ||
				((refreshRate != null) && (_refreshRate != null) &&
				!refreshRate.equals(_refreshRate))) {
			if (!XSS_ALLOW_REFRESHRATE) {
				refreshRate = Xss.strip(refreshRate);
			}

			_refreshRate = refreshRate;
			setModified(true);
		}
	}

	public String getLayoutIds() {
		return _layoutIds;
	}

	public void setLayoutIds(String layoutIds) {
		if (((layoutIds == null) && (_layoutIds != null)) ||
				((layoutIds != null) && (_layoutIds == null)) ||
				((layoutIds != null) && (_layoutIds != null) &&
				!layoutIds.equals(_layoutIds))) {
			if (!XSS_ALLOW_LAYOUTIDS) {
				layoutIds = Xss.strip(layoutIds);
			}

			_layoutIds = layoutIds;
			setModified(true);
		}
	}

	public String getComments() {
		return _comments;
	}

	public void setComments(String comments) {
		if (((comments == null) && (_comments != null)) ||
				((comments != null) && (_comments == null)) ||
				((comments != null) && (_comments != null) &&
				!comments.equals(_comments))) {
			if (!XSS_ALLOW_COMMENTS) {
				comments = Xss.strip(comments);
			}

			_comments = comments;
			setModified(true);
		}
	}

	public Date getCreateDate() {
		return _createDate;
	}

	public void setCreateDate(Date createDate) {
		if (((createDate == null) && (_createDate != null)) ||
				((createDate != null) && (_createDate == null)) ||
				((createDate != null) && (_createDate != null) &&
				!createDate.equals(_createDate))) {
			_createDate = createDate;
			setModified(true);
		}
	}

	public Date getLoginDate() {
		return _loginDate;
	}

	public void setLoginDate(Date loginDate) {
		if (((loginDate == null) && (_loginDate != null)) ||
				((loginDate != null) && (_loginDate == null)) ||
				((loginDate != null) && (_loginDate != null) &&
				!loginDate.equals(_loginDate))) {
			_loginDate = loginDate;
			setModified(true);
		}
	}

	public String getLoginIP() {
		return _loginIP;
	}

	public void setLoginIP(String loginIP) {
		if (((loginIP == null) && (_loginIP != null)) ||
				((loginIP != null) && (_loginIP == null)) ||
				((loginIP != null) && (_loginIP != null) &&
				!loginIP.equals(_loginIP))) {
			if (!XSS_ALLOW_LOGINIP) {
				loginIP = Xss.strip(loginIP);
			}

			_loginIP = loginIP;
			setModified(true);
		}
	}

	public Date getLastLoginDate() {
		return _lastLoginDate;
	}

	public void setLastLoginDate(Date lastLoginDate) {
		if (((lastLoginDate == null) && (_lastLoginDate != null)) ||
				((lastLoginDate != null) && (_lastLoginDate == null)) ||
				((lastLoginDate != null) && (_lastLoginDate != null) &&
				!lastLoginDate.equals(_lastLoginDate))) {
			_lastLoginDate = lastLoginDate;
			setModified(true);
		}
	}

	public String getLastLoginIP() {
		return _lastLoginIP;
	}

	public void setLastLoginIP(String lastLoginIP) {
		if (((lastLoginIP == null) && (_lastLoginIP != null)) ||
				((lastLoginIP != null) && (_lastLoginIP == null)) ||
				((lastLoginIP != null) && (_lastLoginIP != null) &&
				!lastLoginIP.equals(_lastLoginIP))) {
			if (!XSS_ALLOW_LASTLOGINIP) {
				lastLoginIP = Xss.strip(lastLoginIP);
			}

			_lastLoginIP = lastLoginIP;
			setModified(true);
		}
	}

	public int getFailedLoginAttempts() {
		return _failedLoginAttempts;
	}

	public void setFailedLoginAttempts(int failedLoginAttempts) {
		if (failedLoginAttempts != _failedLoginAttempts) {
			_failedLoginAttempts = failedLoginAttempts;
			setModified(true);
		}
	}

	public boolean getAgreedToTermsOfUse() {
		return _agreedToTermsOfUse;
	}

	public boolean isAgreedToTermsOfUse() {
		return _agreedToTermsOfUse;
	}

	public void setAgreedToTermsOfUse(boolean agreedToTermsOfUse) {
		if (agreedToTermsOfUse != _agreedToTermsOfUse) {
			_agreedToTermsOfUse = agreedToTermsOfUse;
			setModified(true);
		}
	}

	public boolean getActive() {
		return _active;
	}

	public boolean isActive() {
		return _active;
	}

	public void setActive(boolean active) {
		if (active != _active) {
			_active = active;
			setModified(true);
		}
	}

	public BaseModel getProtected() {
		return null;
	}

	public void protect() {
	}

	public Object clone() {
		return new User(getUserId(), getCompanyId(), getPassword(),
			getPasswordEncrypted(), getPasswordExpirationDate(),
			getPasswordReset(), getFirstName(), getMiddleName(), getLastName(),
			getNickName(), getMale(), getBirthday(), getEmailAddress(),
			getSmsId(), getAimId(), getIcqId(), getMsnId(), getYmId(),
			getFavoriteActivity(), getFavoriteBibleVerse(), getFavoriteFood(),
			getFavoriteMovie(), getFavoriteMusic(), getLanguageId(),
			getTimeZoneId(), getSkinId(), getDottedSkins(), getRoundedSkins(),
			getGreeting(), getResolution(), getRefreshRate(), getLayoutIds(),
			getComments(), getCreateDate(), getLoginDate(), getLoginIP(),
			getLastLoginDate(), getLastLoginIP(), getFailedLoginAttempts(),
			getAgreedToTermsOfUse(), getActive());
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		User user = (User)obj;
		int value = 0;
		value = getFirstName().toLowerCase().compareTo(user.getFirstName()
														   .toLowerCase());

		if (value != 0) {
			return value;
		}

		value = getMiddleName().toLowerCase().compareTo(user.getMiddleName()
															.toLowerCase());

		if (value != 0) {
			return value;
		}

		value = getLastName().toLowerCase().compareTo(user.getLastName()
														  .toLowerCase());

		if (value != 0) {
			return value;
		}

		return 0;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		User user = null;

		try {
			user = (User)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		String pk = user.getPrimaryKey();

		if (getPrimaryKey().equals(pk)) {
			return true;
		}
		else {
			return false;
		}
	}

	public int hashCode() {
		return getPrimaryKey().hashCode();
	}

	private String _userId;
	private String _companyId;
	private String _password;
	private boolean _passwordEncrypted;
	private Date _passwordExpirationDate;
	private boolean _passwordReset;
	private String _firstName;
	private String _middleName;
	private String _lastName;
	private String _nickName;
	private boolean _male;
	private Date _birthday;
	private String _emailAddress;
	private String _smsId;
	private String _aimId;
	private String _icqId;
	private String _msnId;
	private String _ymId;
	private String _favoriteActivity;
	private String _favoriteBibleVerse;
	private String _favoriteFood;
	private String _favoriteMovie;
	private String _favoriteMusic;
	private String _languageId;
	private String _timeZoneId;
	private String _skinId;
	private boolean _dottedSkins;
	private boolean _roundedSkins;
	private String _greeting;
	private String _resolution;
	private String _refreshRate;
	private String _layoutIds;
	private String _comments;
	private Date _createDate;
	private Date _loginDate;
	private String _loginIP;
	private Date _lastLoginDate;
	private String _lastLoginIP;
	private int _failedLoginAttempts;
	private boolean _agreedToTermsOfUse;
	private boolean _active;
}