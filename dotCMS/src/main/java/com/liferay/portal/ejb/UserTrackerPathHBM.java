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
 * <a href="UserTrackerPathHBM.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.21 $
 *
 */
public class UserTrackerPathHBM {
	protected UserTrackerPathHBM() {
	}

	protected UserTrackerPathHBM(String userTrackerPathId) {
		_userTrackerPathId = userTrackerPathId;
	}

	protected UserTrackerPathHBM(String userTrackerPathId,
		String userTrackerId, String path, Date pathDate) {
		_userTrackerPathId = userTrackerPathId;
		_userTrackerId = userTrackerId;
		_path = path;
		_pathDate = pathDate;
	}

	public String getPrimaryKey() {
		return _userTrackerPathId;
	}

	protected void setPrimaryKey(String pk) {
		_userTrackerPathId = pk;
	}

	protected String getUserTrackerPathId() {
		return _userTrackerPathId;
	}

	protected void setUserTrackerPathId(String userTrackerPathId) {
		_userTrackerPathId = userTrackerPathId;
	}

	protected String getUserTrackerId() {
		return _userTrackerId;
	}

	protected void setUserTrackerId(String userTrackerId) {
		_userTrackerId = userTrackerId;
	}

	protected String getPath() {
		return _path;
	}

	protected void setPath(String path) {
		_path = path;
	}

	protected Date getPathDate() {
		return _pathDate;
	}

	protected void setPathDate(Date pathDate) {
		_pathDate = pathDate;
	}

	private String _userTrackerPathId;
	private String _userTrackerId;
	private String _path;
	private Date _pathDate;
}