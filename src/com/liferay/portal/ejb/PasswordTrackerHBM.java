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
 * <a href="PasswordTrackerHBM.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class PasswordTrackerHBM {
	protected PasswordTrackerHBM() {
	}

	protected PasswordTrackerHBM(String passwordTrackerId) {
		_passwordTrackerId = passwordTrackerId;
	}

	protected PasswordTrackerHBM(String passwordTrackerId, String userId,
		Date createDate, String password) {
		_passwordTrackerId = passwordTrackerId;
		_userId = userId;
		_createDate = createDate;
		_password = password;
	}

	public String getPrimaryKey() {
		return _passwordTrackerId;
	}

	protected void setPrimaryKey(String pk) {
		_passwordTrackerId = pk;
	}

	protected String getPasswordTrackerId() {
		return _passwordTrackerId;
	}

	protected void setPasswordTrackerId(String passwordTrackerId) {
		_passwordTrackerId = passwordTrackerId;
	}

	protected String getUserId() {
		return _userId;
	}

	protected void setUserId(String userId) {
		_userId = userId;
	}

	protected Date getCreateDate() {
		return _createDate;
	}

	protected void setCreateDate(Date createDate) {
		_createDate = createDate;
	}

	protected String getPassword() {
		return _password;
	}

	protected void setPassword(String password) {
		_password = password;
	}

	private String _passwordTrackerId;
	private String _userId;
	private Date _createDate;
	private String _password;
}