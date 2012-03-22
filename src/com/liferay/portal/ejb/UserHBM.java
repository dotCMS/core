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

import java.util.Date;
import java.util.Set;

/**
 * <a href="UserHBM.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.25 $
 *
 */
public class UserHBM {
	protected UserHBM() {
	}

	protected UserHBM(String userId) {
		_userId = userId;
	}

	protected UserHBM(String userId, String companyId, String password,
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

	protected void setPrimaryKey(String pk) {
		_userId = pk;
	}

	protected String getUserId() {
		return _userId;
	}

	protected void setUserId(String userId) {
		_userId = userId;
	}

	protected String getCompanyId() {
		return _companyId;
	}

	protected void setCompanyId(String companyId) {
		_companyId = companyId;
	}

	protected String getPassword() {
		return _password;
	}

	protected void setPassword(String password) {
		_password = password;
	}

	protected boolean getPasswordEncrypted() {
		return _passwordEncrypted;
	}

	protected void setPasswordEncrypted(boolean passwordEncrypted) {
		_passwordEncrypted = passwordEncrypted;
	}

	protected Date getPasswordExpirationDate() {
		return _passwordExpirationDate;
	}

	protected void setPasswordExpirationDate(Date passwordExpirationDate) {
		_passwordExpirationDate = passwordExpirationDate;
	}

	protected boolean getPasswordReset() {
		return _passwordReset;
	}

	protected void setPasswordReset(boolean passwordReset) {
		_passwordReset = passwordReset;
	}

	protected String getFirstName() {
		return _firstName;
	}

	protected void setFirstName(String firstName) {
		_firstName = firstName;
	}

	protected String getMiddleName() {
		return _middleName;
	}

	protected void setMiddleName(String middleName) {
		_middleName = middleName;
	}

	protected String getLastName() {
		return _lastName;
	}

	protected void setLastName(String lastName) {
		_lastName = lastName;
	}

	protected String getNickName() {
		return _nickName;
	}

	protected void setNickName(String nickName) {
		_nickName = nickName;
	}

	protected boolean getMale() {
		return _male;
	}

	protected void setMale(boolean male) {
		_male = male;
	}

	protected Date getBirthday() {
		return _birthday;
	}

	protected void setBirthday(Date birthday) {
		_birthday = birthday;
	}

	protected String getEmailAddress() {
		return _emailAddress;
	}

	protected void setEmailAddress(String emailAddress) {
		_emailAddress = emailAddress;
	}

	protected String getSmsId() {
		return _smsId;
	}

	protected void setSmsId(String smsId) {
		_smsId = smsId;
	}

	protected String getAimId() {
		return _aimId;
	}

	protected void setAimId(String aimId) {
		_aimId = aimId;
	}

	protected String getIcqId() {
		return _icqId;
	}

	protected void setIcqId(String icqId) {
		_icqId = icqId;
	}

	protected String getMsnId() {
		return _msnId;
	}

	protected void setMsnId(String msnId) {
		_msnId = msnId;
	}

	protected String getYmId() {
		return _ymId;
	}

	protected void setYmId(String ymId) {
		_ymId = ymId;
	}

	protected String getFavoriteActivity() {
		return _favoriteActivity;
	}

	protected void setFavoriteActivity(String favoriteActivity) {
		_favoriteActivity = favoriteActivity;
	}

	protected String getFavoriteBibleVerse() {
		return _favoriteBibleVerse;
	}

	protected void setFavoriteBibleVerse(String favoriteBibleVerse) {
		_favoriteBibleVerse = favoriteBibleVerse;
	}

	protected String getFavoriteFood() {
		return _favoriteFood;
	}

	protected void setFavoriteFood(String favoriteFood) {
		_favoriteFood = favoriteFood;
	}

	protected String getFavoriteMovie() {
		return _favoriteMovie;
	}

	protected void setFavoriteMovie(String favoriteMovie) {
		_favoriteMovie = favoriteMovie;
	}

	protected String getFavoriteMusic() {
		return _favoriteMusic;
	}

	protected void setFavoriteMusic(String favoriteMusic) {
		_favoriteMusic = favoriteMusic;
	}

	protected String getLanguageId() {
		return _languageId;
	}

	protected void setLanguageId(String languageId) {
		_languageId = languageId;
	}

	protected String getTimeZoneId() {
		return _timeZoneId;
	}

	protected void setTimeZoneId(String timeZoneId) {
		_timeZoneId = timeZoneId;
	}

	protected String getSkinId() {
		return _skinId;
	}

	protected void setSkinId(String skinId) {
		_skinId = skinId;
	}

	protected boolean getDottedSkins() {
		return _dottedSkins;
	}

	protected void setDottedSkins(boolean dottedSkins) {
		_dottedSkins = dottedSkins;
	}

	protected boolean getRoundedSkins() {
		return _roundedSkins;
	}

	protected void setRoundedSkins(boolean roundedSkins) {
		_roundedSkins = roundedSkins;
	}

	protected String getGreeting() {
		return _greeting;
	}

	protected void setGreeting(String greeting) {
		_greeting = greeting;
	}

	protected String getResolution() {
		return _resolution;
	}

	protected void setResolution(String resolution) {
		_resolution = resolution;
	}

	protected String getRefreshRate() {
		return _refreshRate;
	}

	protected void setRefreshRate(String refreshRate) {
		_refreshRate = refreshRate;
	}

	protected String getLayoutIds() {
		return _layoutIds;
	}

	protected void setLayoutIds(String layoutIds) {
		_layoutIds = layoutIds;
	}

	protected String getComments() {
		return _comments;
	}

	protected void setComments(String comments) {
		_comments = comments;
	}

	protected Date getCreateDate() {
		return _createDate;
	}

	protected void setCreateDate(Date createDate) {
		_createDate = createDate;
	}

	protected Date getLoginDate() {
		return _loginDate;
	}

	protected void setLoginDate(Date loginDate) {
		_loginDate = loginDate;
	}

	protected String getLoginIP() {
		return _loginIP;
	}

	protected void setLoginIP(String loginIP) {
		_loginIP = loginIP;
	}

	protected Date getLastLoginDate() {
		return _lastLoginDate;
	}

	protected void setLastLoginDate(Date lastLoginDate) {
		_lastLoginDate = lastLoginDate;
	}

	protected String getLastLoginIP() {
		return _lastLoginIP;
	}

	protected void setLastLoginIP(String lastLoginIP) {
		_lastLoginIP = lastLoginIP;
	}

	protected int getFailedLoginAttempts() {
		return _failedLoginAttempts;
	}

	protected void setFailedLoginAttempts(int failedLoginAttempts) {
		_failedLoginAttempts = failedLoginAttempts;
	}

	protected boolean getAgreedToTermsOfUse() {
		return _agreedToTermsOfUse;
	}

	protected void setAgreedToTermsOfUse(boolean agreedToTermsOfUse) {
		_agreedToTermsOfUse = agreedToTermsOfUse;
	}

	protected boolean getActive() {
		return _active;
	}

	protected void setActive(boolean active) {
		_active = active;
	}

	protected Set getGroups() {
		return _groups;
	}

	protected void setGroups(Set groups) {
		_groups = groups;
	}

	protected Set getRoles() {
		return _roles;
	}

	protected void setRoles(Set roles) {
		_roles = roles;
	}

	protected Set getProjProjects() {
		return _projProjects;
	}

	protected void setProjProjects(Set projProjects) {
		_projProjects = projProjects;
	}

	protected Set getProjTasks() {
		return _projTasks;
	}

	protected void setProjTasks(Set projTasks) {
		_projTasks = projTasks;
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
	private Set _groups;
	private Set _roles;
	private Set _projProjects;
	private Set _projTasks;
}