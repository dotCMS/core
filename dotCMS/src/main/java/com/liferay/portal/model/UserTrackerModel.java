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
 * <a href="UserTrackerModel.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.18 $
 *
 */
public class UserTrackerModel extends BaseModel {
	public static boolean CACHEABLE = GetterUtil.get(PropsUtil.get(
				"value.object.cacheable.com.liferay.portal.model.UserTracker"),
			VALUE_OBJECT_CACHEABLE);
	public static int MAX_SIZE = GetterUtil.get(PropsUtil.get(
				"value.object.max.size.com.liferay.portal.model.UserTracker"),
			VALUE_OBJECT_MAX_SIZE);
	public static boolean XSS_ALLOW_BY_MODEL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.UserTracker"), XSS_ALLOW);
	public static boolean XSS_ALLOW_USERTRACKERID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.UserTracker.userTrackerId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_COMPANYID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.UserTracker.companyId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_USERID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.UserTracker.userId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_REMOTEADDR = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.UserTracker.remoteAddr"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_REMOTEHOST = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.UserTracker.remoteHost"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_USERAGENT = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.UserTracker.userAgent"),
			XSS_ALLOW_BY_MODEL);
	public static long LOCK_EXPIRATION_TIME = GetterUtil.getLong(PropsUtil.get(
				"lock.expiration.time.com.liferay.portal.model.UserTrackerModel"));

	public UserTrackerModel() {
	}

	public UserTrackerModel(String userTrackerId) {
		_userTrackerId = userTrackerId;
		setNew(true);
	}

	public UserTrackerModel(String userTrackerId, String companyId,
		String userId, Date modifiedDate, String remoteAddr, String remoteHost,
		String userAgent) {
		_userTrackerId = userTrackerId;
		_companyId = companyId;
		_userId = userId;
		_modifiedDate = modifiedDate;
		_remoteAddr = remoteAddr;
		_remoteHost = remoteHost;
		_userAgent = userAgent;
	}

	public String getPrimaryKey() {
		return _userTrackerId;
	}

	public String getUserTrackerId() {
		return _userTrackerId;
	}

	public void setUserTrackerId(String userTrackerId) {
		if (((userTrackerId == null) && (_userTrackerId != null)) ||
				((userTrackerId != null) && (_userTrackerId == null)) ||
				((userTrackerId != null) && (_userTrackerId != null) &&
				!userTrackerId.equals(_userTrackerId))) {
			if (!XSS_ALLOW_USERTRACKERID) {
				userTrackerId = Xss.strip(userTrackerId);
			}

			_userTrackerId = userTrackerId;
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

	public Date getModifiedDate() {
		return _modifiedDate;
	}

	public void setModifiedDate(Date modifiedDate) {
		if (((modifiedDate == null) && (_modifiedDate != null)) ||
				((modifiedDate != null) && (_modifiedDate == null)) ||
				((modifiedDate != null) && (_modifiedDate != null) &&
				!modifiedDate.equals(_modifiedDate))) {
			_modifiedDate = modifiedDate;
			setModified(true);
		}
	}

	public String getRemoteAddr() {
		return _remoteAddr;
	}

	public void setRemoteAddr(String remoteAddr) {
		if (((remoteAddr == null) && (_remoteAddr != null)) ||
				((remoteAddr != null) && (_remoteAddr == null)) ||
				((remoteAddr != null) && (_remoteAddr != null) &&
				!remoteAddr.equals(_remoteAddr))) {
			if (!XSS_ALLOW_REMOTEADDR) {
				remoteAddr = Xss.strip(remoteAddr);
			}

			_remoteAddr = remoteAddr;
			setModified(true);
		}
	}

	public String getRemoteHost() {
		return _remoteHost;
	}

	public void setRemoteHost(String remoteHost) {
		if (((remoteHost == null) && (_remoteHost != null)) ||
				((remoteHost != null) && (_remoteHost == null)) ||
				((remoteHost != null) && (_remoteHost != null) &&
				!remoteHost.equals(_remoteHost))) {
			if (!XSS_ALLOW_REMOTEHOST) {
				remoteHost = Xss.strip(remoteHost);
			}

			_remoteHost = remoteHost;
			setModified(true);
		}
	}

	public String getUserAgent() {
		return _userAgent;
	}

	public void setUserAgent(String userAgent) {
		if (((userAgent == null) && (_userAgent != null)) ||
				((userAgent != null) && (_userAgent == null)) ||
				((userAgent != null) && (_userAgent != null) &&
				!userAgent.equals(_userAgent))) {
			if (!XSS_ALLOW_USERAGENT) {
				userAgent = Xss.strip(userAgent);
			}

			_userAgent = userAgent;
			setModified(true);
		}
	}

	public BaseModel getProtected() {
		return null;
	}

	public void protect() {
	}

	public Object clone() {
		return new UserTracker(getUserTrackerId(), getCompanyId(), getUserId(),
			getModifiedDate(), getRemoteAddr(), getRemoteHost(), getUserAgent());
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		UserTracker userTracker = (UserTracker)obj;
		String pk = userTracker.getPrimaryKey();

		return getPrimaryKey().compareTo(pk);
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		UserTracker userTracker = null;

		try {
			userTracker = (UserTracker)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		String pk = userTracker.getPrimaryKey();

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

	private String _userTrackerId;
	private String _companyId;
	private String _userId;
	private Date _modifiedDate;
	private String _remoteAddr;
	private String _remoteHost;
	private String _userAgent;
}