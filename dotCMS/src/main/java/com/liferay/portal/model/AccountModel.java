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

import java.util.Date;

import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.Xss;

/**
 * <a href="AccountModel.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.1 $
 *
 */
public class AccountModel extends BaseModel {
	public static boolean CACHEABLE = GetterUtil.get(PropsUtil.get(
				"value.object.cacheable.com.liferay.portal.model.Account"),
			VALUE_OBJECT_CACHEABLE);
	public static int MAX_SIZE = GetterUtil.get(PropsUtil.get(
				"value.object.max.size.com.liferay.portal.model.Account"),
			VALUE_OBJECT_MAX_SIZE);
	public static boolean XSS_ALLOW_BY_MODEL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account"), XSS_ALLOW);
	public static boolean XSS_ALLOW_ACCOUNTID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.accountId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_COMPANYID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.companyId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_USERID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.userId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_USERNAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.userName"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_PARENTACCOUNTID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.parentAccountId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_NAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.name"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_LEGALNAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.legalName"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_LEGALID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.legalId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_LEGALTYPE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.legalType"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_SICCODE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.sicCode"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_TICKERSYMBOL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.tickerSymbol"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_INDUSTRY = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.industry"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_TYPE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.type"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_SIZE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.size"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_WEBSITE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.website"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_EMAILADDRESS1 = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.emailAddress1"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_EMAILADDRESS2 = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Account.emailAddress2"),
			XSS_ALLOW_BY_MODEL);
	public static long LOCK_EXPIRATION_TIME = GetterUtil.getLong(PropsUtil.get(
				"lock.expiration.time.com.liferay.portal.model.AccountModel"));

	public AccountModel() {
	}

	public AccountModel(String accountId) {
		_accountId = accountId;
		setNew(true);
	}

	public AccountModel(String accountId, String companyId, String userId,
		String userName, Date createDate, Date modifiedDate,
		String parentAccountId, String name, String legalName, String legalId,
		String legalType, String sicCode, String tickerSymbol, String industry,
		String type, String size, String website, String emailAddress1,
		String emailAddress2) {
		_accountId = accountId;
		_companyId = companyId;
		_userId = userId;
		_userName = userName;
		_createDate = createDate;
		_modifiedDate = modifiedDate;
		_parentAccountId = parentAccountId;
		_name = name;
		_legalName = legalName;
		_legalId = legalId;
		_legalType = legalType;
		_sicCode = sicCode;
		_tickerSymbol = tickerSymbol;
		_industry = industry;
		_type = type;
		_size = size;
		_website = website;
		_emailAddress1 = emailAddress1;
		_emailAddress2 = emailAddress2;
	}

	public String getPrimaryKey() {
		return _accountId;
	}

	public String getAccountId() {
		return _accountId;
	}

	public void setAccountId(String accountId) {
		if (((accountId == null) && (_accountId != null)) ||
				((accountId != null) && (_accountId == null)) ||
				((accountId != null) && (_accountId != null) &&
				!accountId.equals(_accountId))) {
			if (!XSS_ALLOW_ACCOUNTID) {
				accountId = Xss.strip(accountId);
			}

			_accountId = accountId;
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

	public String getParentAccountId() {
		return _parentAccountId;
	}

	public void setParentAccountId(String parentAccountId) {
		if (((parentAccountId == null) && (_parentAccountId != null)) ||
				((parentAccountId != null) && (_parentAccountId == null)) ||
				((parentAccountId != null) && (_parentAccountId != null) &&
				!parentAccountId.equals(_parentAccountId))) {
			if (!XSS_ALLOW_PARENTACCOUNTID) {
				parentAccountId = Xss.strip(parentAccountId);
			}

			_parentAccountId = parentAccountId;
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

	public String getLegalName() {
		return _legalName;
	}

	public void setLegalName(String legalName) {
		if (((legalName == null) && (_legalName != null)) ||
				((legalName != null) && (_legalName == null)) ||
				((legalName != null) && (_legalName != null) &&
				!legalName.equals(_legalName))) {
			if (!XSS_ALLOW_LEGALNAME) {
				legalName = Xss.strip(legalName);
			}

			_legalName = legalName;
			setModified(true);
		}
	}

	public String getLegalId() {
		return _legalId;
	}

	public void setLegalId(String legalId) {
		if (((legalId == null) && (_legalId != null)) ||
				((legalId != null) && (_legalId == null)) ||
				((legalId != null) && (_legalId != null) &&
				!legalId.equals(_legalId))) {
			if (!XSS_ALLOW_LEGALID) {
				legalId = Xss.strip(legalId);
			}

			_legalId = legalId;
			setModified(true);
		}
	}

	public String getLegalType() {
		return _legalType;
	}

	public void setLegalType(String legalType) {
		if (((legalType == null) && (_legalType != null)) ||
				((legalType != null) && (_legalType == null)) ||
				((legalType != null) && (_legalType != null) &&
				!legalType.equals(_legalType))) {
			if (!XSS_ALLOW_LEGALTYPE) {
				legalType = Xss.strip(legalType);
			}

			_legalType = legalType;
			setModified(true);
		}
	}

	public String getSicCode() {
		return _sicCode;
	}

	public void setSicCode(String sicCode) {
		if (((sicCode == null) && (_sicCode != null)) ||
				((sicCode != null) && (_sicCode == null)) ||
				((sicCode != null) && (_sicCode != null) &&
				!sicCode.equals(_sicCode))) {
			if (!XSS_ALLOW_SICCODE) {
				sicCode = Xss.strip(sicCode);
			}

			_sicCode = sicCode;
			setModified(true);
		}
	}

	public String getTickerSymbol() {
		return _tickerSymbol;
	}

	public void setTickerSymbol(String tickerSymbol) {
		if (((tickerSymbol == null) && (_tickerSymbol != null)) ||
				((tickerSymbol != null) && (_tickerSymbol == null)) ||
				((tickerSymbol != null) && (_tickerSymbol != null) &&
				!tickerSymbol.equals(_tickerSymbol))) {
			if (!XSS_ALLOW_TICKERSYMBOL) {
				tickerSymbol = Xss.strip(tickerSymbol);
			}

			_tickerSymbol = tickerSymbol;
			setModified(true);
		}
	}

	public String getIndustry() {
		return _industry;
	}

	public void setIndustry(String industry) {
		if (((industry == null) && (_industry != null)) ||
				((industry != null) && (_industry == null)) ||
				((industry != null) && (_industry != null) &&
				!industry.equals(_industry))) {
			if (!XSS_ALLOW_INDUSTRY) {
				industry = Xss.strip(industry);
			}

			_industry = industry;
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

	public String getWebsite() {
		return _website;
	}

	public void setWebsite(String website) {
		if (((website == null) && (_website != null)) ||
				((website != null) && (_website == null)) ||
				((website != null) && (_website != null) &&
				!website.equals(_website))) {
			if (!XSS_ALLOW_WEBSITE) {
				website = Xss.strip(website);
			}

			_website = website;
			setModified(true);
		}
	}

	public String getEmailAddress1() {
		return _emailAddress1;
	}

	public void setEmailAddress1(String emailAddress1) {
		if (((emailAddress1 == null) && (_emailAddress1 != null)) ||
				((emailAddress1 != null) && (_emailAddress1 == null)) ||
				((emailAddress1 != null) && (_emailAddress1 != null) &&
				!emailAddress1.equals(_emailAddress1))) {
			if (!XSS_ALLOW_EMAILADDRESS1) {
				emailAddress1 = Xss.strip(emailAddress1);
			}

			_emailAddress1 = emailAddress1;
			setModified(true);
		}
	}

	public String getEmailAddress2() {
		return _emailAddress2;
	}

	public void setEmailAddress2(String emailAddress2) {
		if (((emailAddress2 == null) && (_emailAddress2 != null)) ||
				((emailAddress2 != null) && (_emailAddress2 == null)) ||
				((emailAddress2 != null) && (_emailAddress2 != null) &&
				!emailAddress2.equals(_emailAddress2))) {
			if (!XSS_ALLOW_EMAILADDRESS2) {
				emailAddress2 = Xss.strip(emailAddress2);
			}

			_emailAddress2 = emailAddress2;
			setModified(true);
		}
	}

	public BaseModel getProtected() {
		return null;
	}

	public void protect() {
	}

	public Object clone() {
		return new Account(getAccountId(), getCompanyId(), getUserId(),
			getUserName(), getCreateDate(), getModifiedDate(),
			getParentAccountId(), getName(), getLegalName(), getLegalId(),
			getLegalType(), getSicCode(), getTickerSymbol(), getIndustry(),
			getType(), getSize(), getWebsite(), getEmailAddress1(),
			getEmailAddress2());
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		Account account = (Account)obj;
		String pk = account.getPrimaryKey();

		return getPrimaryKey().compareTo(pk);
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		Account account = null;

		try {
			account = (Account)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		String pk = account.getPrimaryKey();

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

	private String _accountId;
	private String _companyId;
	private String _userId;
	private String _userName;
	private Date _createDate;
	private Date _modifiedDate;
	private String _parentAccountId;
	private String _name;
	private String _legalName;
	private String _legalId;
	private String _legalType;
	private String _sicCode;
	private String _tickerSymbol;
	private String _industry;
	private String _type;
	private String _size;
	private String _website;
	private String _emailAddress1;
	private String _emailAddress2;
}