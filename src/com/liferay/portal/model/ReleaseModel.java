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
 * <a href="ReleaseModel.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class ReleaseModel extends BaseModel {
	public static boolean CACHEABLE = GetterUtil.get(PropsUtil.get(
				"value.object.cacheable.com.liferay.portal.model.Release"),
			VALUE_OBJECT_CACHEABLE);
	public static int MAX_SIZE = GetterUtil.get(PropsUtil.get(
				"value.object.max.size.com.liferay.portal.model.Release"),
			VALUE_OBJECT_MAX_SIZE);
	public static boolean XSS_ALLOW_BY_MODEL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Release"), XSS_ALLOW);
	public static boolean XSS_ALLOW_RELEASEID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Release.releaseId"),
			XSS_ALLOW_BY_MODEL);
	public static long LOCK_EXPIRATION_TIME = GetterUtil.getLong(PropsUtil.get(
				"lock.expiration.time.com.liferay.portal.model.ReleaseModel"));

	public ReleaseModel() {
	}

	public ReleaseModel(String releaseId) {
		_releaseId = releaseId;
		setNew(true);
	}

	public ReleaseModel(String releaseId, Date createDate, Date modifiedDate,
		int buildNumber, Date buildDate) {
		_releaseId = releaseId;
		_createDate = createDate;
		_modifiedDate = modifiedDate;
		_buildNumber = buildNumber;
		_buildDate = buildDate;
	}

	public String getPrimaryKey() {
		return _releaseId;
	}

	public String getReleaseId() {
		return _releaseId;
	}

	public void setReleaseId(String releaseId) {
		if (((releaseId == null) && (_releaseId != null)) ||
				((releaseId != null) && (_releaseId == null)) ||
				((releaseId != null) && (_releaseId != null) &&
				!releaseId.equals(_releaseId))) {
			if (!XSS_ALLOW_RELEASEID) {
				releaseId = Xss.strip(releaseId);
			}

			_releaseId = releaseId;
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

	public int getBuildNumber() {
		return _buildNumber;
	}

	public void setBuildNumber(int buildNumber) {
		if (buildNumber != _buildNumber) {
			_buildNumber = buildNumber;
			setModified(true);
		}
	}

	public Date getBuildDate() {
		return _buildDate;
	}

	public void setBuildDate(Date buildDate) {
		if (((buildDate == null) && (_buildDate != null)) ||
				((buildDate != null) && (_buildDate == null)) ||
				((buildDate != null) && (_buildDate != null) &&
				!buildDate.equals(_buildDate))) {
			_buildDate = buildDate;
			setModified(true);
		}
	}

	public BaseModel getProtected() {
		return null;
	}

	public void protect() {
	}

	public Object clone() {
		return new Release(getReleaseId(), getCreateDate(), getModifiedDate(),
			getBuildNumber(), getBuildDate());
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		Release release = (Release)obj;
		String pk = release.getPrimaryKey();

		return getPrimaryKey().compareTo(pk);
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		Release release = null;

		try {
			release = (Release)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		String pk = release.getPrimaryKey();

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

	private String _releaseId;
	private Date _createDate;
	private Date _modifiedDate;
	private int _buildNumber;
	private Date _buildDate;
}