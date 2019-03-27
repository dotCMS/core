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

package com.liferay.portlet.admin.model;

import java.io.Serializable;

import com.dotmarketing.business.Role;

/**
 * <a href="UserConfig.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.12 $
 *
 */
public class UserConfig implements Serializable {

	public static final String USER_CONFIG = "USER_CONFIG";

	public static final String[] DEFAULT_GROUPS = new String[0];

	public static final String[] DEFAULT_ROLES =
		new String[] {Role.POWER_USER, Role.USER};

	public static final String[] DEFAULT_RESERVED_USER_IDS = new String[0];

	public static final String[] DEFAULT_RESERVED_USER_EMAIL_ADDRESSES =
		new String[0];

	public static final String[] DEFAULT_MAIL_HOST_NAMES = new String[0];

	public UserConfig() {
	}

	public UserConfig(String[] groupNames, String[] roleNames,
					  String[] reservedUserIds,
					  String[] reservedUserEmailAddresses,
					  String[] mailHostNames,
					  EmailConfig registrationEmail) {

		_groupNames = groupNames;
		_roleNames = roleNames;
		_reservedUserIds = reservedUserIds;
		_reservedUserEmailAddresses = reservedUserEmailAddresses;
		_mailHostNames = mailHostNames;
		_registrationEmail = registrationEmail;
	}

	public String[] getGroupNames() {
		return _groupNames;
	}

	public void setGroupNames(String[] groupNames) {
		_groupNames = groupNames;
	}

	public String[] getRoleNames() {
		return _roleNames;
	}

	public void setRoleNames(String[] roleNames) {
		_roleNames = roleNames;
	}

	public String[] getReservedUserIds() {
		return _reservedUserIds;
	}

	public boolean hasReservedUserId(String userId) {
		if (_reservedUserIds == null) {
			return false;
		}

		for (int i = 0; i < _reservedUserIds.length; i++) {
			if (_reservedUserIds[i].equalsIgnoreCase(userId)) {
				return true;
			}
		}

		return false;
	}

	public void setReservedUserIds(String[] reservedUserIds) {
		_reservedUserIds = reservedUserIds;
	}

	public String[] getReservedUserEmailAddresses() {
		return _reservedUserEmailAddresses;
	}

	public boolean hasReservedUserEmailAddress(String emailAddress) {
		if (_reservedUserEmailAddresses == null) {
			return false;
		}

		for (int i = 0; i < _reservedUserEmailAddresses.length; i++) {
			if (_reservedUserEmailAddresses[i].equalsIgnoreCase(emailAddress)) {
				return true;
			}
		}

		return false;
	}

	public void setReservedUserEmailAddresses(String[] ruea) {
		_reservedUserEmailAddresses = ruea;
	}

	public String[] getMailHostNames() {
		return _mailHostNames;
	}

	public boolean hasMailHostName(String mailHostName) {
		if (_mailHostNames == null) {
			return false;
		}

		for (int i = 0; i < _mailHostNames.length; i++) {
			if (_mailHostNames[i].equalsIgnoreCase(mailHostName)) {
				return true;
			}
		}

		return false;
	}

	public void setMailHostNames(String[] mailHostNames) {
		_mailHostNames = mailHostNames;
	}

	public EmailConfig getRegistrationEmail() {
		return _registrationEmail;
	}

	public void setRegistrationEmail(EmailConfig registrationEmail) {
		_registrationEmail = registrationEmail;
	}

	private String[] _groupNames;
	private String[] _roleNames;
	private String[] _reservedUserIds;
	private String[] _reservedUserEmailAddresses;
	private String[] _mailHostNames;
	private EmailConfig _registrationEmail;

}