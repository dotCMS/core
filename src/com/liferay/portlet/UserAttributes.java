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

package com.liferay.portlet;

import java.util.List;

import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

/**
 * <a href="UserAttributes.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class UserAttributes {

	// See page 119 of the JSR 168 spec

	public static final String USER_BDATE = "user.bdate";
	public static final String USER_GENDER = "user.gender";
	public static final String USER_EMPLOYER = "user.employer";
	public static final String USER_DEPARTMENT = "user.department";
	public static final String USER_JOBTITLE = "user.jobtitle";
	public static final String USER_NAME_PREFIX = "user.name.prefix";
	public static final String USER_NAME_GIVEN = "user.name.given";
	public static final String USER_NAME_FAMILY = "user.name.family";
	public static final String USER_NAME_MIDDLE = "user.name.middle";
	public static final String USER_NAME_SUFFIX = "user.name.suffix";
	public static final String USER_NAME_NICKNAME = "user.name.nickName";
	public static final String USER_HOME_INFO_POSTAL_NAME = "user.home-info.postal.name";
	public static final String USER_HOME_INFO_POSTAL_STREET = "user.home-info.postal.street";
	public static final String USER_HOME_INFO_POSTAL_CITY = "user.home-info.postal.city";
	public static final String USER_HOME_INFO_POSTAL_STATEPROV = "user.home-info.postal.stateprov";
	public static final String USER_HOME_INFO_POSTAL_POSTALCODE = "user.home-info.postal.postalcode";
	public static final String USER_HOME_INFO_POSTAL_COUNTRY = "user.home-info.postal.country";
	public static final String USER_HOME_INFO_POSTAL_ORGANIZATION = "user.home-info.postal.organization";
	public static final String USER_HOME_INFO_TELECOM_TELEPHONE_INTCODE = "user.home-info.telecom.telephone.intcode";
	public static final String USER_HOME_INFO_TELECOM_TELEPHONE_LOCCODE = "user.home-info.telecom.telephone.loccode";
	public static final String USER_HOME_INFO_TELECOM_TELEPHONE_NUMBER = "user.home-info.telecom.telephone.number";
	public static final String USER_HOME_INFO_TELECOM_TELEPHONE_EXT = "user.home-info.telecom.telephone.ext";
	public static final String USER_HOME_INFO_TELECOM_TELEPHONE_COMMENT = "user.home-info.telecom.telephone.comment";
	public static final String USER_HOME_INFO_TELECOM_FAX_INTCODE = "user.home-info.telecom.fax.intcode";
	public static final String USER_HOME_INFO_TELECOM_FAX_LOCCODE = "user.home-info.telecom.fax.loccode";
	public static final String USER_HOME_INFO_TELECOM_FAX_NUMBER = "user.home-info.telecom.fax.number";
	public static final String USER_HOME_INFO_TELECOM_FAX_EXT = "user.home-info.telecom.fax.ext";
	public static final String USER_HOME_INFO_TELECOM_FAX_COMMENT = "user.home-info.telecom.fax.comment";
	public static final String USER_HOME_INFO_TELECOM_MOBILE_INTCODE = "user.home-info.telecom.mobile.intcode";
	public static final String USER_HOME_INFO_TELECOM_MOBILE_LOCCODE = "user.home-info.telecom.mobile.loccode";
	public static final String USER_HOME_INFO_TELECOM_MOBILE_NUMBER = "user.home-info.telecom.mobile.number";
	public static final String USER_HOME_INFO_TELECOM_MOBILE_EXT = "user.home-info.telecom.mobile.ext";
	public static final String USER_HOME_INFO_TELECOM_MOBILE_COMMENT = "user.home-info.telecom.mobile.comment";
	public static final String USER_HOME_INFO_TELECOM_PAGER_INTCODE = "user.home-info.telecom.pager.intcode";
	public static final String USER_HOME_INFO_TELECOM_PAGER_LOCCODE = "user.home-info.telecom.pager.loccode";
	public static final String USER_HOME_INFO_TELECOM_PAGER_NUMBER = "user.home-info.telecom.pager.number";
	public static final String USER_HOME_INFO_TELECOM_PAGER_EXT = "user.home-info.telecom.pager.ext";
	public static final String USER_HOME_INFO_TELECOM_PAGER_COMMENT = "user.home-info.telecom.pager.comment";
	public static final String USER_HOME_INFO_ONLINE_EMAIL = "user.home-info.online.email";
	public static final String USER_HOME_INFO_ONLINE_URI = "user.home-info.online.uri";
	public static final String USER_BUSINESS_INFO_POSTAL_NAME = "user.business-info.postal.name";
	public static final String USER_BUSINESS_INFO_POSTAL_STREET = "user.business-info.postal.street";
	public static final String USER_BUSINESS_INFO_POSTAL_CITY = "user.business-info.postal.city";
	public static final String USER_BUSINESS_INFO_POSTAL_STATEPROV = "user.business-info.postal.stateprov";
	public static final String USER_BUSINESS_INFO_POSTAL_POSTALCODE = "user.business-info.postal.postalcode";
	public static final String USER_BUSINESS_INFO_POSTAL_COUNTRY = "user.business-info.postal.country";
	public static final String USER_BUSINESS_INFO_POSTAL_ORGANIZATION = "user.business-info.postal.organization";
	public static final String USER_BUSINESS_INFO_TELECOM_TELEPHONE_INTCODE = "user.business-info.telecom.telephone.intcode";
	public static final String USER_BUSINESS_INFO_TELECOM_TELEPHONE_LOCCODE = "user.business-info.telecom.telephone.loccode";
	public static final String USER_BUSINESS_INFO_TELECOM_TELEPHONE_NUMBER = "user.business-info.telecom.telephone.number";
	public static final String USER_BUSINESS_INFO_TELECOM_TELEPHONE_EXT = "user.business-info.telecom.telephone.ext";
	public static final String USER_BUSINESS_INFO_TELECOM_TELEPHONE_COMMENT = "user.business-info.telecom.telephone.comment";
	public static final String USER_BUSINESS_INFO_TELECOM_FAX_INTCODE = "user.business-info.telecom.fax.intcode";
	public static final String USER_BUSINESS_INFO_TELECOM_FAX_LOCCODE = "user.business-info.telecom.fax.loccode";
	public static final String USER_BUSINESS_INFO_TELECOM_FAX_NUMBER = "user.business-info.telecom.fax.number";
	public static final String USER_BUSINESS_INFO_TELECOM_FAX_EXT = "user.business-info.telecom.fax.ext";
	public static final String USER_BUSINESS_INFO_TELECOM_FAX_COMMENT = "user.business-info.telecom.fax.comment";
	public static final String USER_BUSINESS_INFO_TELECOM_MOBILE_INTCODE = "user.business-info.telecom.mobile.intcode";
	public static final String USER_BUSINESS_INFO_TELECOM_MOBILE_LOCCODE = "user.business-info.telecom.mobile.loccode";
	public static final String USER_BUSINESS_INFO_TELECOM_MOBILE_NUMBER = "user.business-info.telecom.mobile.number";
	public static final String USER_BUSINESS_INFO_TELECOM_MOBILE_EXT = "user.business-info.telecom.mobile.ext";
	public static final String USER_BUSINESS_INFO_TELECOM_MOBILE_COMMENT = "user.business-info.telecom.mobile.comment";
	public static final String USER_BUSINESS_INFO_TELECOM_PAGER_INTCODE = "user.business-info.telecom.pager.intcode";
	public static final String USER_BUSINESS_INFO_TELECOM_PAGER_LOCCODE = "user.business-info.telecom.pager.loccode";
	public static final String USER_BUSINESS_INFO_TELECOM_PAGER_NUMBER = "user.business-info.telecom.pager.number";
	public static final String USER_BUSINESS_INFO_TELECOM_PAGER_EXT = "user.business-info.telecom.pager.ext";
	public static final String USER_BUSINESS_INFO_TELECOM_PAGER_COMMENT = "user.business-info.telecom.pager.comment";
	public static final String USER_BUSINESS_INFO_ONLINE_EMAIL = "user.business-info.online.email";
	public static final String USER_BUSINESS_INFO_ONLINE_URI = "user.business-info.online.uri";

	public UserAttributes(User user) throws PortalException, SystemException {
		_user = user;

		List addresses = user.getAddresses();

		for (int i = 0; i < addresses.size(); i++) {
			Address address = (Address)addresses.get(i);

			if (address.getDescription().equalsIgnoreCase("home")) {
				_homeAddress = address;
			}
			else if (address.getDescription().equalsIgnoreCase("business")) {
				_bizAddress = address;
			}
		}
	}

	public String getValue(String name) {
		if (name == null) {
			return null;
		}

		if (name.equals(USER_BDATE)) {
			return _user.getBirthday().toString();
		}
		else if (name.equals(USER_GENDER)) {
			return _user.isMale() ? "male" : "female";
		}
		else if (name.equals(USER_EMPLOYER)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_DEPARTMENT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_JOBTITLE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_NAME_PREFIX)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_NAME_GIVEN)) {
			return _user.getFirstName();
		}
		else if (name.equals(USER_NAME_FAMILY)) {
			return _user.getLastName();
		}
		else if (name.equals(USER_NAME_MIDDLE)) {
			return _user.getMiddleName();
		}
		else if (name.equals(USER_NAME_SUFFIX)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_NAME_NICKNAME)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_POSTAL_NAME)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_POSTAL_STREET)) {
			if (_homeAddress != null) {
				return _homeAddress.getStreet1();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_HOME_INFO_POSTAL_CITY)) {
			if (_homeAddress != null) {
				return _homeAddress.getCity();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_HOME_INFO_POSTAL_STATEPROV)) {
			if (_homeAddress != null) {
				return _homeAddress.getState();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_HOME_INFO_POSTAL_POSTALCODE)) {
			if (_homeAddress != null) {
				return _homeAddress.getZip();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_HOME_INFO_POSTAL_COUNTRY)) {
			if (_homeAddress != null) {
				return _homeAddress.getCountry();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_HOME_INFO_POSTAL_ORGANIZATION)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_TELEPHONE_INTCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_TELEPHONE_LOCCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_TELEPHONE_NUMBER)) {
			if (_homeAddress != null) {
				return _homeAddress.getPhone();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_TELEPHONE_EXT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_TELEPHONE_COMMENT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_FAX_INTCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_FAX_LOCCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_FAX_NUMBER)) {
			if (_homeAddress != null) {
				return _homeAddress.getFax();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_FAX_EXT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_FAX_COMMENT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_MOBILE_INTCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_MOBILE_LOCCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_MOBILE_NUMBER)) {
			if (_homeAddress != null) {
				return _homeAddress.getCell();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_MOBILE_EXT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_MOBILE_COMMENT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_PAGER_INTCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_PAGER_LOCCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_PAGER_NUMBER)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_PAGER_EXT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_TELECOM_PAGER_COMMENT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_HOME_INFO_ONLINE_EMAIL)) {
			return _user.getEmailAddress();
		}
		else if (name.equals(USER_HOME_INFO_ONLINE_URI)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_POSTAL_NAME)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_POSTAL_STREET)) {
			if (_bizAddress != null) {
				return _bizAddress.getStreet1();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_BUSINESS_INFO_POSTAL_CITY)) {
			if (_bizAddress != null) {
				return _bizAddress.getCity();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_BUSINESS_INFO_POSTAL_STATEPROV)) {
			if (_bizAddress != null) {
				return _bizAddress.getState();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_BUSINESS_INFO_POSTAL_POSTALCODE)) {
			if (_bizAddress != null) {
				return _bizAddress.getZip();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_BUSINESS_INFO_POSTAL_COUNTRY)) {
			if (_bizAddress != null) {
				return _bizAddress.getCountry();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_BUSINESS_INFO_POSTAL_ORGANIZATION)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_TELEPHONE_INTCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_TELEPHONE_LOCCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_TELEPHONE_NUMBER)) {
			if (_bizAddress != null) {
				return _bizAddress.getPhone();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_TELEPHONE_EXT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_TELEPHONE_COMMENT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_FAX_INTCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_FAX_LOCCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_FAX_NUMBER)) {
			if (_bizAddress != null) {
				return _bizAddress.getFax();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_FAX_EXT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_FAX_COMMENT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_MOBILE_INTCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_MOBILE_LOCCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_MOBILE_NUMBER)) {
			if (_bizAddress != null) {
				return _bizAddress.getCell();
			}
			else {
				return StringPool.BLANK;
			}
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_MOBILE_EXT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_MOBILE_COMMENT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_PAGER_INTCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_PAGER_LOCCODE)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_PAGER_NUMBER)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_PAGER_EXT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_TELECOM_PAGER_COMMENT)) {
			return StringPool.BLANK;
		}
		else if (name.equals(USER_BUSINESS_INFO_ONLINE_EMAIL)) {
			return _user.getEmailAddress();
		}
		else if (name.equals(USER_BUSINESS_INFO_ONLINE_URI)) {
			return StringPool.BLANK;
		}
		else {
			return null;
		}
	}

	private User _user;
	private Address _homeAddress;
	private Address _bizAddress;

}