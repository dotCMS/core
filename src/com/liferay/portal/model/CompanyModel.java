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

/**
 * <a href="CompanyModel.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.88 $
 *
 */
public class CompanyModel extends BaseModel {
	public static boolean CACHEABLE = GetterUtil.get(PropsUtil.get(
				"value.object.cacheable.com.liferay.portal.model.Company"),
			VALUE_OBJECT_CACHEABLE);
	public static int MAX_SIZE = GetterUtil.get(PropsUtil.get(
				"value.object.max.size.com.liferay.portal.model.Company"),
			VALUE_OBJECT_MAX_SIZE);
	public static boolean XSS_ALLOW_BY_MODEL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company"), XSS_ALLOW);
	public static boolean XSS_ALLOW_COMPANYID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.companyId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_KEY = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.key"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_PORTALURL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.portalURL"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_HOMEURL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.homeURL"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_MX = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.mx"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_NAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.name"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_SHORTNAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.shortName"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_TYPE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.type"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_SIZE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.size"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_STREET = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.street"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_CITY = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.city"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_STATE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.state"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_ZIP = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.zip"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_PHONE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.phone"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_FAX = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.fax"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_EMAILADDRESS = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.emailAddress"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_AUTHTYPE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Company.authType"),
			XSS_ALLOW_BY_MODEL);
	public static long LOCK_EXPIRATION_TIME = GetterUtil.getLong(PropsUtil.get(
				"lock.expiration.time.com.liferay.portal.model.CompanyModel"));

	public CompanyModel() {
	}

	public CompanyModel(String companyId) {
		_companyId = companyId;
		setNew(true);
	}

	public CompanyModel(String companyId, String key, String portalURL,
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

	public String getKey() {
		return _key;
	}

	public void setKey(String key) {
		if (((key == null) && (_key != null)) ||
				((key != null) && (_key == null)) ||
				((key != null) && (_key != null) && !key.equals(_key))) {
			if (!XSS_ALLOW_KEY) {
				key = Xss.strip(key);
			}

			_key = key;
			setModified(true);
		}
	}

	public String getPortalURL() {
		return _portalURL;
	}

	public void setPortalURL(String portalURL) {
		if (((portalURL == null) && (_portalURL != null)) ||
				((portalURL != null) && (_portalURL == null)) ||
				((portalURL != null) && (_portalURL != null) &&
				!portalURL.equals(_portalURL))) {
			if (!XSS_ALLOW_PORTALURL) {
				portalURL = Xss.strip(portalURL);
			}

			_portalURL = portalURL;
			setModified(true);
		}
	}

	public String getHomeURL() {
		return _homeURL;
	}

	public void setHomeURL(String homeURL) {
		if (((homeURL == null) && (_homeURL != null)) ||
				((homeURL != null) && (_homeURL == null)) ||
				((homeURL != null) && (_homeURL != null) &&
				!homeURL.equals(_homeURL))) {
			if (!XSS_ALLOW_HOMEURL) {
				homeURL = Xss.strip(homeURL);
			}

			_homeURL = homeURL;
			setModified(true);
		}
	}

	public String getMx() {
		return _mx;
	}

	public void setMx(String mx) {
		if (((mx == null) && (_mx != null)) || ((mx != null) && (_mx == null)) ||
				((mx != null) && (_mx != null) && !mx.equals(_mx))) {
			if (!XSS_ALLOW_MX) {
				mx = Xss.strip(mx);
			}

			_mx = mx;
			setModified(true);
		}
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		if (((name == null) && (_name != null)) ||
				((name != null) && (_name == null)) ||
				((name != null) && (_name != null) && !name.equals(_name))) {
			if (!XSS_ALLOW_NAME) {
				name = Xss.strip(name);
			}

			_name = name;
			setModified(true);
		}
	}

	public String getShortName() {
		return _shortName;
	}

	public void setShortName(String shortName) {
		if (((shortName == null) && (_shortName != null)) ||
				((shortName != null) && (_shortName == null)) ||
				((shortName != null) && (_shortName != null) &&
				!shortName.equals(_shortName))) {
			if (!XSS_ALLOW_SHORTNAME) {
				shortName = Xss.strip(shortName);
			}

			_shortName = shortName;
			setModified(true);
		}
	}

	public String getType() {
		return _type;
	}

	public void setType(String type) {
		if (((type == null) && (_type != null)) ||
				((type != null) && (_type == null)) ||
				((type != null) && (_type != null) && !type.equals(_type))) {
			if (!XSS_ALLOW_TYPE) {
				type = Xss.strip(type);
			}

			_type = type;
			setModified(true);
		}
	}

	public String getSize() {
		return _size;
	}

	public void setSize(String size) {
		if (((size == null) && (_size != null)) ||
				((size != null) && (_size == null)) ||
				((size != null) && (_size != null) && !size.equals(_size))) {
			if (!XSS_ALLOW_SIZE) {
				size = Xss.strip(size);
			}

			_size = size;
			setModified(true);
		}
	}

	public String getStreet() {
		return _street;
	}

	public void setStreet(String street) {
		if (((street == null) && (_street != null)) ||
				((street != null) && (_street == null)) ||
				((street != null) && (_street != null) &&
				!street.equals(_street))) {
			if (!XSS_ALLOW_STREET) {
				street = Xss.strip(street);
			}

			_street = street;
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

	public String getEmailAddress() {
		return _emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		if (((emailAddress == null) && (_emailAddress != null)) ||
				((emailAddress != null) && (_emailAddress == null)) ||
				((emailAddress != null) && (_emailAddress != null) &&
				!emailAddress.equals(_emailAddress))) {
			if (!XSS_ALLOW_EMAILADDRESS) {
				emailAddress = Xss.strip(emailAddress);
			}

			_emailAddress = emailAddress;
			setModified(true);
		}
	}

	public String getAuthType() {
		return _authType;
	}

	public void setAuthType(String authType) {
		if (((authType == null) && (_authType != null)) ||
				((authType != null) && (_authType == null)) ||
				((authType != null) && (_authType != null) &&
				!authType.equals(_authType))) {
			if (!XSS_ALLOW_AUTHTYPE) {
				authType = Xss.strip(authType);
			}

			_authType = authType;
			setModified(true);
		}
	}

	public boolean getAutoLogin() {
		return _autoLogin;
	}

	public boolean isAutoLogin() {
		return _autoLogin;
	}

	public void setAutoLogin(boolean autoLogin) {
		if (autoLogin != _autoLogin) {
			_autoLogin = autoLogin;
			setModified(true);
		}
	}

	public boolean getStrangers() {
		return _strangers;
	}

	public boolean isStrangers() {
		return _strangers;
	}

	public void setStrangers(boolean strangers) {
		if (strangers != _strangers) {
			_strangers = strangers;
			setModified(true);
		}
	}

	public BaseModel getProtected() {
		return null;
	}

	public void protect() {
	}

	public Object clone() {
		return new Company(getCompanyId(), getKey(), getPortalURL(),
			getHomeURL(), getMx(), getName(), getShortName(), getType(),
			getSize(), getStreet(), getCity(), getState(), getZip(),
			getPhone(), getFax(), getEmailAddress(), getAuthType(),
			getAutoLogin(), getStrangers());
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		Company company = (Company)obj;
		String pk = company.getPrimaryKey();

		return getPrimaryKey().compareTo(pk);
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		Company company = null;

		try {
			company = (Company)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		String pk = company.getPrimaryKey();

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