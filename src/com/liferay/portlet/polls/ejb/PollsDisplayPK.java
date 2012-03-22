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

package com.liferay.portlet.polls.ejb;

import java.io.Serializable;

import com.liferay.util.StringPool;

/**
 * <a href="PollsDisplayPK.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class PollsDisplayPK implements Comparable, Serializable {
	public String userId;
	public String portletId;

	public PollsDisplayPK() {
	}

	public PollsDisplayPK(String userId, String portletId) {
		this.userId = userId;
		this.portletId = portletId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPortletId() {
		return portletId;
	}

	public void setPortletId(String portletId) {
		this.portletId = portletId;
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		PollsDisplayPK pk = (PollsDisplayPK)obj;
		int value = 0;

		value = userId.compareTo(pk.userId);

		if (value != 0) {
			return value;
		}

		value = portletId.compareTo(pk.portletId);

		if (value != 0) {
			return value;
		}

		return 0;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		PollsDisplayPK pk = null;

		try {
			pk = (PollsDisplayPK)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		if ((userId.equals(pk.userId)) && (portletId.equals(pk.portletId))) {
			return true;
		}
		else {
			return false;
		}
	}

	public int hashCode() {
		return (userId + portletId).hashCode();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(StringPool.OPEN_CURLY_BRACE);
		sb.append("userId");
		sb.append(StringPool.EQUAL);
		sb.append(userId);
		sb.append(StringPool.COMMA);
		sb.append(StringPool.SPACE);
		sb.append("portletId");
		sb.append(StringPool.EQUAL);
		sb.append(portletId);
		sb.append(StringPool.CLOSE_CURLY_BRACE);

		return sb.toString();
	}
}