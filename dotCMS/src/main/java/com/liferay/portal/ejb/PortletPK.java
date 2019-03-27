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

import java.io.Serializable;

import com.liferay.util.StringPool;

/**
 * <a href="PortletPK.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.81 $
 *
 */
public class PortletPK implements Comparable, Serializable {
	public String portletId;
	public String groupId;
	public String companyId;

	public PortletPK() {
	}

	public PortletPK(String portletId, String groupId, String companyId) {
		this.portletId = portletId;
		this.groupId = groupId;
		this.companyId = companyId;
	}

	public String getPortletId() {
		return portletId;
	}

	public void setPortletId(String portletId) {
		this.portletId = portletId;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getCompanyId() {
		return companyId;
	}

	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		PortletPK pk = (PortletPK)obj;
		int value = 0;
		value = portletId.compareTo(pk.portletId);

		if (value != 0) {
			return value;
		}

		value = groupId.compareTo(pk.groupId);

		if (value != 0) {
			return value;
		}

		value = companyId.compareTo(pk.companyId);

		if (value != 0) {
			return value;
		}

		return 0;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		PortletPK pk = null;

		try {
			pk = (PortletPK)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		if ((portletId.equals(pk.portletId)) && (groupId.equals(pk.groupId)) &&
				(companyId.equals(pk.companyId))) {
			return true;
		}
		else {
			return false;
		}
	}

	public int hashCode() {
		return (portletId + groupId + companyId).hashCode();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(StringPool.OPEN_CURLY_BRACE);
		sb.append("portletId");
		sb.append(StringPool.EQUAL);
		sb.append(portletId);
		sb.append(StringPool.COMMA);
		sb.append(StringPool.SPACE);
		sb.append("groupId");
		sb.append(StringPool.EQUAL);
		sb.append(groupId);
		sb.append(StringPool.COMMA);
		sb.append(StringPool.SPACE);
		sb.append("companyId");
		sb.append(StringPool.EQUAL);
		sb.append(companyId);
		sb.append(StringPool.CLOSE_CURLY_BRACE);

		return sb.toString();
	}
}