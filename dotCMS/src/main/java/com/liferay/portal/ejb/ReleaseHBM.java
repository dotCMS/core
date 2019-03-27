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

/**
 * <a href="ReleaseHBM.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class ReleaseHBM {
	protected ReleaseHBM() {
	}

	protected ReleaseHBM(String releaseId) {
		_releaseId = releaseId;
	}

	protected ReleaseHBM(String releaseId, Date createDate, Date modifiedDate,
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

	protected void setPrimaryKey(String pk) {
		_releaseId = pk;
	}

	protected String getReleaseId() {
		return _releaseId;
	}

	protected void setReleaseId(String releaseId) {
		_releaseId = releaseId;
	}

	protected Date getCreateDate() {
		return _createDate;
	}

	protected void setCreateDate(Date createDate) {
		_createDate = createDate;
	}

	protected Date getModifiedDate() {
		return _modifiedDate;
	}

	protected void setModifiedDate(Date modifiedDate) {
		_modifiedDate = modifiedDate;
	}

	protected int getBuildNumber() {
		return _buildNumber;
	}

	protected void setBuildNumber(int buildNumber) {
		_buildNumber = buildNumber;
	}

	protected Date getBuildDate() {
		return _buildDate;
	}

	protected void setBuildDate(Date buildDate) {
		_buildDate = buildDate;
	}

	private String _releaseId;
	private Date _createDate;
	private Date _modifiedDate;
	private int _buildNumber;
	private Date _buildDate;
}