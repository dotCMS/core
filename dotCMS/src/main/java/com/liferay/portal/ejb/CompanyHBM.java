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

/**
 * <a href="CompanyHBM.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.21 $
 *
 */
public class CompanyHBM {
	protected CompanyHBM() {
	}

	protected CompanyHBM(String companyId) {
		_companyId = companyId;
	}

	protected CompanyHBM(String companyId, String key, String portalURL,
		String homeURL, String mx, String name, String shortName, String type,
		String size, String street, String city, String state, String zip,
		String phone, String fax, String emailAddress, String authType,
		boolean autoLogin, boolean strangers) {
		_companyId = companyId;
		_key = key;
		_portalURL = portalURL;
		_homeURL = homeURL;
		_mx = mx;
		_name = name;
		_shortName = shortName;
		_type = type;
		_size = size;
		_street = street;
		_city = city;
		_state = state;
		_zip = zip;
		_phone = phone;
		_fax = fax;
		_emailAddress = emailAddress;
		_authType = authType;
		_autoLogin = autoLogin;
		_strangers = strangers;
	}

	public String getPrimaryKey() {
		return _companyId;
	}

	protected void setPrimaryKey(String pk) {
		_companyId = pk;
	}

	protected String getCompanyId() {
		return _companyId;
	}

	protected void setCompanyId(String companyId) {
		_companyId = companyId;
	}

	protected String getKey() {
		return _key;
	}

	protected void setKey(String key) {
		_key = key;
	}

	protected String getPortalURL() {
		return _portalURL;
	}

	protected void setPortalURL(String portalURL) {
		_portalURL = portalURL;
	}

	protected String getHomeURL() {
		return _homeURL;
	}

	protected void setHomeURL(String homeURL) {
		_homeURL = homeURL;
	}

	protected String getMx() {
		return _mx;
	}

	protected void setMx(String mx) {
		_mx = mx;
	}

	protected String getName() {
		return _name;
	}

	protected void setName(String name) {
		_name = name;
	}

	protected String getShortName() {
		return _shortName;
	}

	protected void setShortName(String shortName) {
		_shortName = shortName;
	}

	protected String getType() {
		return _type;
	}

	protected void setType(String type) {
		_type = type;
	}

	protected String getSize() {
		return _size;
	}

	protected void setSize(String size) {
		_size = size;
	}

	protected String getStreet() {
		return _street;
	}

	protected void setStreet(String street) {
		_street = street;
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

	protected String getEmailAddress() {
		return _emailAddress;
	}

	protected void setEmailAddress(String emailAddress) {
		_emailAddress = emailAddress;
	}

	protected String getAuthType() {
		return _authType;
	}

	protected void setAuthType(String authType) {
		_authType = authType;
	}

	protected boolean getAutoLogin() {
		return _autoLogin;
	}

	protected void setAutoLogin(boolean autoLogin) {
		_autoLogin = autoLogin;
	}

	protected boolean getStrangers() {
		return _strangers;
	}

	protected void setStrangers(boolean strangers) {
		_strangers = strangers;
	}

	private String _companyId;
	private String _key;
	private String _portalURL;
	private String _homeURL;
	private String _mx;
	private String _name;
	private String _shortName;
	private String _type;
	private String _size;
	private String _street;
	private String _city;
	private String _state;
	private String _zip;
	private String _phone;
	private String _fax;
	private String _emailAddress;
	private String _authType;
	private boolean _autoLogin;
	private boolean _strangers;
}