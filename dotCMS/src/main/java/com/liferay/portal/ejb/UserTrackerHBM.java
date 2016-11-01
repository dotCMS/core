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
 * <a href="UserTrackerHBM.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.21 $
 *
 */
public class UserTrackerHBM {
	protected UserTrackerHBM() {
	}

	protected UserTrackerHBM(String userTrackerId) {
		_userTrackerId = userTrackerId;
	}

	protected UserTrackerHBM(String userTrackerId, String companyId,
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

	protected void setPrimaryKey(String pk) {
		_userTrackerId = pk;
	}

	protected String getUserTrackerId() {
		return _userTrackerId;
	}

	protected void setUserTrackerId(String userTrackerId) {
		_userTrackerId = userTrackerId;
	}

	protected String getCompanyId() {
		return _companyId;
	}

	protected void setCompanyId(String companyId) {
		_companyId = companyId;
	}

	protected String getUserId() {
		return _userId;
	}

	protected void setUserId(String userId) {
		_userId = userId;
	}

	protected Date getModifiedDate() {
		return _modifiedDate;
	}

	protected void setModifiedDate(Date modifiedDate) {
		_modifiedDate = modifiedDate;
	}

	protected String getRemoteAddr() {
		return _remoteAddr;
	}

	protected void setRemoteAddr(String remoteAddr) {
		_remoteAddr = remoteAddr;
	}

	protected String getRemoteHost() {
		return _remoteHost;
	}

	protected void setRemoteHost(String remoteHost) {
		_remoteHost = remoteHost;
	}

	protected String getUserAgent() {
		return _userAgent;
	}

	protected void setUserAgent(String userAgent) {
		_userAgent = userAgent;
	}

	private String _userTrackerId;
	private String _companyId;
	private String _userId;
	private Date _modifiedDate;
	private String _remoteAddr;
	private String _remoteHost;
	private String _userAgent;
}