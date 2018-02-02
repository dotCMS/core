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

import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.Xss;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;

/**
 * <a href="AddressModel.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.89 $
 *
 */
public class AddressModel extends BaseModel {
	public static boolean CACHEABLE = GetterUtil.get(PropsUtil.get(
				"value.object.cacheable.com.liferay.portal.model.Address"),
			VALUE_OBJECT_CACHEABLE);
	public static int MAX_SIZE = GetterUtil.get(PropsUtil.get(
				"value.object.max.size.com.liferay.portal.model.Address"),
			VALUE_OBJECT_MAX_SIZE);
	public static boolean XSS_ALLOW_BY_MODEL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address"), XSS_ALLOW);
	public static boolean XSS_ALLOW_ADDRESSID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.addressId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_COMPANYID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.companyId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_USERID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.userId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_USERNAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.userName"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_CLASSNAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.className"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_CLASSPK = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.classPK"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_DESCRIPTION = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.description"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_STREET1 = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.street1"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_STREET2 = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.street2"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_CITY = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.city"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_STATE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.state"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_ZIP = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.zip"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_COUNTRY = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.country"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_PHONE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.phone"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_FAX = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.fax"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_CELL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Address.cell"),
			XSS_ALLOW_BY_MODEL);
	public static long LOCK_EXPIRATION_TIME = GetterUtil.getLong(PropsUtil.get(
				"lock.expiration.time.com.liferay.portal.model.AddressModel"));

	public AddressModel() {
	}

	public AddressModel(String addressId) {
		_addressId = addressId;
		setNew(true);
	}

	public AddressModel(String addressId, String companyId, String userId,
		String userName, Date createDate, Date modifiedDate, String className,
		String classPK, String description, String street1, String street2,
		String city, String state, String zip, String country, String phone,
		String fax, String cell, int priority) {
		_addressId = addressId;
		_companyId = companyId;
		_userId = userId;
		_userName = userName;
		_createDate = createDate;
		_modifiedDate = modifiedDate;
		_className = className;
		_classPK = classPK;
		_description = description;
		_street1 = street1;
		_street2 = street2;
		_city = city;
		_state = state;
		_zip = zip;
		_country = country;
		_phone = phone;
		_fax = fax;
		_cell = cell;
		_priority = priority;
	}

	public String getPrimaryKey() {
		return _addressId;
	}

	public String getAddressId() {
		return _addressId;
	}

	public void setAddressId(String addressId) {
		if (((addressId == null) && (_addressId != null)) ||
				((addressId != null) && (_addressId == null)) ||
				((addressId != null) && (_addressId != null) &&
				!addressId.equals(_addressId))) {
			if (!XSS_ALLOW_ADDRESSID) {
				addressId = Xss.strip(addressId);
			}

			_addressId = addressId;
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

	public String getUserName() {
		return _userName;
	}

	public void setUserName(String userName) {
		if (((userName == null) && (_userName != null)) ||
				((userName != null) && (_userName == null)) ||
				((userName != null) && (_userName != null) &&
				!userName.equals(_userName))) {
			if (!XSS_ALLOW_USERNAME) {
				userName = Xss.strip(userName);
			}

			_userName = userName;
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

	public String getClassName() {
		return _className;
	}

	public void setClassName(String className) {
		if (((className == null) && (_className != null)) ||
				((className != null) && (_className == null)) ||
				((className != null) && (_className != null) &&
				!className.equals(_className))) {
			if (!XSS_ALLOW_CLASSNAME) {
				className = Xss.strip(className);
			}

			_className = className;
			setModified(true);
		}
	}

	public String getClassPK() {
		return _classPK;
	}

	public void setClassPK(String classPK) {
		if (((classPK == null) && (_classPK != null)) ||
				((classPK != null) && (_classPK == null)) ||
				((classPK != null) && (_classPK != null) &&
				!classPK.equals(_classPK))) {
			if (!XSS_ALLOW_CLASSPK) {
				classPK = Xss.strip(classPK);
			}

			_classPK = classPK;
			setModified(true);
		}
	}

	public String getDescription() {
		return _description;
	}

	public void setDescription(String description) {
		if (((description == null) && (_description != null)) ||
				((description != null) && (_description == null)) ||
				((description != null) && (_description != null) &&
				!description.equals(_description))) {
			if (!XSS_ALLOW_DESCRIPTION) {
				description = Xss.strip(description);
			}

			_description = description;
			setModified(true);
		}
	}

	public String getStreet1() {
		return _street1;
	}

	public void setStreet1(String street1) {
		if (((street1 == null) && (_street1 != null)) ||
				((street1 != null) && (_street1 == null)) ||
				((street1 != null) && (_street1 != null) &&
				!street1.equals(_street1))) {
			if (!XSS_ALLOW_STREET1) {
				street1 = Xss.strip(street1);
			}

			_street1 = street1;
			setModified(true);
		}
	}

	public String getStreet2() {
		return _street2;
	}

	public void setStreet2(String street2) {
		if (((street2 == null) && (_street2 != null)) ||
				((street2 != null) && (_street2 == null)) ||
				((street2 != null) && (_street2 != null) &&
				!street2.equals(_street2))) {
			if (!XSS_ALLOW_STREET2) {
				street2 = Xss.strip(street2);
			}

			_street2 = street2;
			setModified(true);
		}
	}

	public String getCity() {
		return _city;
	}

	public void setCity(String city) {
		if (((city == null) && (_city != null)) ||
				((city != null) && (_city == null)) ||
				((city != null) && (_city != null) && !city.equals(_city))) {
			if (!XSS_ALLOW_CITY) {
				city = Xss.strip(city);
			}

			_city = city;
			setModified(true);
		}
	}

	public String getState() {
		return _state;
	}

	public void setState(String state) {
		if (((state == null) && (_state != null)) ||
				((state != null) && (_state == null)) ||
				((state != null) && (_state != null) && !state.equals(_state))) {
			if (!XSS_ALLOW_STATE) {
				state = Xss.strip(state);
			}

			_state = state;
			setModified(true);
		}
	}

	public String getZip() {
		return _zip;
	}

	public void setZip(String zip) {
		if (((zip == null) && (_zip != null)) ||
				((zip != null) && (_zip == null)) ||
				((zip != null) && (_zip != null) && !zip.equals(_zip))) {
			if (!XSS_ALLOW_ZIP) {
				zip = Xss.strip(zip);
			}

			_zip = zip;
			setModified(true);
		}
	}

	public String getCountry() {
		return _country;
	}

	public void setCountry(String country) {
		if (((country == null) && (_country != null)) ||
				((country != null) && (_country == null)) ||
				((country != null) && (_country != null) &&
				!country.equals(_country))) {
			if (!XSS_ALLOW_COUNTRY) {
				country = Xss.strip(country);
			}

			_country = country;
			setModified(true);
		}
	}

	public String getPhone() {
		return _phone;
	}

	public void setPhone(String phone) {
		if (((phone == null) && (_phone != null)) ||
				((phone != null) && (_phone == null)) ||
				((phone != null) && (_phone != null) && !phone.equals(_phone))) {
			if (!XSS_ALLOW_PHONE) {
				phone = Xss.strip(phone);
			}

			_phone = phone;
			setModified(true);
		}
	}

	public String getFax() {
		return _fax;
	}

	public void setFax(String fax) {
		if (((fax == null) && (_fax != null)) ||
				((fax != null) && (_fax == null)) ||
				((fax != null) && (_fax != null) && !fax.equals(_fax))) {
			if (!XSS_ALLOW_FAX) {
				fax = Xss.strip(fax);
			}

			_fax = fax;
			setModified(true);
		}
	}

	public String getCell() {
		return _cell;
	}

	public void setCell(String cell) {
		if (((cell == null) && (_cell != null)) ||
				((cell != null) && (_cell == null)) ||
				((cell != null) && (_cell != null) && !cell.equals(_cell))) {
			if (!XSS_ALLOW_CELL) {
				cell = Xss.strip(cell);
			}

			_cell = cell;
			setModified(true);
		}
	}

	public int getPriority() {
		return _priority;
	}

	public void setPriority(int priority) {
		if (priority != _priority) {
			_priority = priority;
			setModified(true);
		}
	}

	public BaseModel getProtected() {
		return null;
	}

	public void protect() {
	}

	public Object clone() {
		return new Address(getAddressId(), getCompanyId(), getUserId(),
			getUserName(), getCreateDate(), getModifiedDate(), getClassName(),
			getClassPK(), getDescription(), getStreet1(), getStreet2(),
			getCity(), getState(), getZip(), getCountry(), getPhone(),
			getFax(), getCell(), getPriority());
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		Address address = (Address)obj;
		int value = 0;
		value = getCompanyId().compareTo(address.getCompanyId());

		if (value != 0) {
			return value;
		}

		value = getClassName().compareTo(address.getClassName());

		if (value != 0) {
			return value;
		}

		value = getClassPK().compareTo(address.getClassPK());

		if (value != 0) {
			return value;
		}

		if (getPriority() < address.getPriority()) {
			value = -1;
		}
		else if (getPriority() > address.getPriority()) {
			value = 1;
		}
		else {
			value = 0;
		}

		if (value != 0) {
			return value;
		}

		return 0;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		Address address = null;

		try {
			address = (Address)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		String pk = address.getPrimaryKey();

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
	
	public Map<String, String> toMap ()
	{
		try {
			return BeanUtils.describe(this);
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		} catch (NoSuchMethodException e) {
		}
		return null;
	}

	private String _addressId;
	private String _companyId;
	private String _userId;
	private String _userName;
	private Date _createDate;
	private Date _modifiedDate;
	private String _className;
	private String _classPK;
	private String _description;
	private String _street1;
	private String _street2;
	private String _city;
	private String _state;
	private String _zip;
	private String _country;
	private String _phone;
	private String _fax;
	private String _cell;
	private int _priority;
}