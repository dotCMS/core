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

import java.security.Key;
import java.util.Locale;
import java.util.TimeZone;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.Base64;

/**
 * <a href="Company.java.html"><b><i>View Source</i></b></a>
 * 
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.16 $
 * 
 */
public class Company extends CompanyModel {

	public static final String[] TYPES = PropsUtil
			.getArray(PropsUtil.COMPANY_TYPES);

	public static final String AUTH_TYPE_EA = "emailAddress";

	public static final String AUTH_TYPE_ID = "userId";

	public static final String SYSTEM = "system";

	public Company() {
		super();
	}

	public Company(String companyId) {
		super(companyId);
	}

	public Company(String companyId, String key, String portalURL,
			String homeURL, String mx, String name, String shortName,
			String type, String size, String street, String city, String state,
			String zip, String phone, String fax, String emailAddress,
			String authType, boolean autoLogin, boolean strangers) {

		super(companyId, key, portalURL, homeURL, mx, name, shortName, type,
				size, street, city, state, zip, phone, fax, emailAddress,
				authType, autoLogin, strangers);

		setKey(key);
		setPortalURL(portalURL);
	}

	public Company(String companyId, String key, Key keyObj, String portalURL,
			String homeURL, String mx, String name, String shortName,
			String type, String size, String street, String city, String state,
			String zip, String phone, String fax, String emailAddress,
			String authType, boolean autoLogin, boolean strangers) {

		super(companyId, key, portalURL, homeURL, mx, name, shortName, type,
				size, street, city, state, zip, phone, fax, emailAddress,
				authType, autoLogin, strangers);

		keyObj = _keyObj;
		setPortalURL(portalURL);
	}

	public void setKey(String key) {
		_keyObj = (Key) Base64.stringToObject(key);

		super.setKey(key);
	}

	public Key getKeyObj() {
		return _keyObj;
	}

	public void setKeyObj(Key keyObj) {
		_keyObj = keyObj;

		super.setKey(Base64.objectToString(keyObj));
	}

	public User getDefaultUser() {
		User defaultUser = null;

		try {
			defaultUser = APILocator.getUserAPI().getDefaultUser();
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}

		return defaultUser;
	}

	public Locale getLocale() {
		return getDefaultUser().getLocale();
	}

	public TimeZone getTimeZone() {
		return getDefaultUser().getTimeZone();
	}

	public String getAdminName() {
		return getShortName() + " Administrator";
	}

	public BaseModel getProtected() {
		if (_company == null) {
			protect();
		}

		return _company;
	}

	public void protect() {
		_company = (Company) this.clone();

		_company.setKey(null);
	}

	public Object clone() {
		return new Company(getCompanyId(), getKey(), getKeyObj(),
				getPortalURL(), getHomeURL(), getMx(), getName(),
				getShortName(), getType(), getSize(), getStreet(), getCity(),
				getState(), getZip(), getPhone(), getFax(), getEmailAddress(),
				getAuthType(), getAutoLogin(), getStrangers());
	}

	private Key _keyObj = null;
	private String _imageURL;
	private Company _company;

}