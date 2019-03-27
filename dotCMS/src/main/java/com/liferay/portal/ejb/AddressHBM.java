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
 * <a href="AddressHBM.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.22 $
 *
 */
public class AddressHBM {
	protected AddressHBM() {
	}

	protected AddressHBM(String addressId) {
		_addressId = addressId;
	}

	protected AddressHBM(String addressId, String companyId, String userId,
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

	protected void setPrimaryKey(String pk) {
		_addressId = pk;
	}

	protected String getAddressId() {
		return _addressId;
	}

	protected void setAddressId(String addressId) {
		_addressId = addressId;
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

	protected String getUserName() {
		return _userName;
	}

	protected void setUserName(String userName) {
		_userName = userName;
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

	protected String getClassName() {
		return _className;
	}

	protected void setClassName(String className) {
		_className = className;
	}

	protected String getClassPK() {
		return _classPK;
	}

	protected void setClassPK(String classPK) {
		_classPK = classPK;
	}

	protected String getDescription() {
		return _description;
	}

	protected void setDescription(String description) {
		_description = description;
	}

	protected String getStreet1() {
		return _street1;
	}

	protected void setStreet1(String street1) {
		_street1 = street1;
	}

	protected String getStreet2() {
		return _street2;
	}

	protected void setStreet2(String street2) {
		_street2 = street2;
	}

	protected String getCity() {
		return _city;
	}

	protected void setCity(String city) {
		_city = city;
	}

	protected String getState() {
		return _state;
	}

	protected void setState(String state) {
		_state = state;
	}

	protected String getZip() {
		return _zip;
	}

	protected void setZip(String zip) {
		_zip = zip;
	}

	protected String getCountry() {
		return _country;
	}

	protected void setCountry(String country) {
		_country = country;
	}

	protected String getPhone() {
		return _phone;
	}

	protected void setPhone(String phone) {
		_phone = phone;
	}

	protected String getFax() {
		return _fax;
	}

	protected void setFax(String fax) {
		_fax = fax;
	}

	protected String getCell() {
		return _cell;
	}

	protected void setCell(String cell) {
		_cell = cell;
	}

	protected int getPriority() {
		return _priority;
	}

	protected void setPriority(int priority) {
		_priority = priority;
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