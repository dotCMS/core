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
 * <a href="ContactModel.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class ContactModel extends BaseModel {
	public static boolean CACHEABLE = GetterUtil.get(PropsUtil.get(
				"value.object.cacheable.com.liferay.portal.model.Contact"),
			VALUE_OBJECT_CACHEABLE);
	public static int MAX_SIZE = GetterUtil.get(PropsUtil.get(
				"value.object.max.size.com.liferay.portal.model.Contact"),
			VALUE_OBJECT_MAX_SIZE);
	public static boolean XSS_ALLOW_BY_MODEL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact"), XSS_ALLOW);
	public static boolean XSS_ALLOW_CONTACTID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.contactId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_COMPANYID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.companyId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_USERID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.userId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_USERNAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.userName"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_PARENTCONTACTID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.parentContactId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_FIRSTNAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.firstName"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_MIDDLENAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.middleName"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_LASTNAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.lastName"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_NICKNAME = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.nickName"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_EMAILADDRESS1 = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.emailAddress1"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_EMAILADDRESS2 = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.emailAddress2"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_SMSID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.smsId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_AIMID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.aimId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_ICQID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.icqId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_MSNID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.msnId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_SKYPEID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.skypeId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_YMID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.ymId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_WEBSITE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.website"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_TIMEZONEID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.timeZoneId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_EMPLOYEENUMBER = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.employeeNumber"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_JOBTITLE = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.jobTitle"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_JOBCLASS = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.jobClass"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_HOURSOFOPERATION = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portal.model.Contact.hoursOfOperation"),
			XSS_ALLOW_BY_MODEL);
	public static long LOCK_EXPIRATION_TIME = GetterUtil.getLong(PropsUtil.get(
				"lock.expiration.time.com.liferay.portal.model.ContactModel"));

	public ContactModel() {
	}

	public ContactModel(String contactId) {
		_contactId = contactId;
		setNew(true);
	}

	public ContactModel(String contactId, String companyId, String userId,
		String userName, Date createDate, Date modifiedDate,
		String parentContactId, String firstName, String middleName,
		String lastName, String nickName, String emailAddress1,
		String emailAddress2, String smsId, String aimId, String icqId,
		String msnId, String skypeId, String ymId, String website,
		boolean male, Date birthday, String timeZoneId, String employeeNumber,
		String jobTitle, String jobClass, String hoursOfOperation) {
		_contactId = contactId;
		_companyId = companyId;
		_userId = userId;
		_userName = userName;
		_createDate = createDate;
		_modifiedDate = modifiedDate;
		_parentContactId = parentContactId;
		_firstName = firstName;
		_middleName = middleName;
		_lastName = lastName;
		_nickName = nickName;
		_emailAddress1 = emailAddress1;
		_emailAddress2 = emailAddress2;
		_smsId = smsId;
		_aimId = aimId;
		_icqId = icqId;
		_msnId = msnId;
		_skypeId = skypeId;
		_ymId = ymId;
		_website = website;
		_male = male;
		_birthday = birthday;
		_timeZoneId = timeZoneId;
		_employeeNumber = employeeNumber;
		_jobTitle = jobTitle;
		_jobClass = jobClass;
		_hoursOfOperation = hoursOfOperation;
	}

	public String getPrimaryKey() {
		return _contactId;
	}

	public String getContactId() {
		return _contactId;
	}

	public void setContactId(String contactId) {
		if (((contactId == null) && (_contactId != null)) ||
				((contactId != null) && (_contactId == null)) ||
				((contactId != null) && (_contactId != null) &&
				!contactId.equals(_contactId))) {
			if (!XSS_ALLOW_CONTACTID) {
				contactId = Xss.strip(contactId);
			}

			_contactId = contactId;
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

	public String getParentContactId() {
		return _parentContactId;
	}

	public void setParentContactId(String parentContactId) {
		if (((parentContactId == null) && (_parentContactId != null)) ||
				((parentContactId != null) && (_parentContactId == null)) ||
				((parentContactId != null) && (_parentContactId != null) &&
				!parentContactId.equals(_parentContactId))) {
			if (!XSS_ALLOW_PARENTCONTACTID) {
				parentContactId = Xss.strip(parentContactId);
			}

			_parentContactId = parentContactId;
			setModified(true);
		}
	}

	public String getFirstName() {
		return _firstName;
	}

	public void setFirstName(String firstName) {
		if (((firstName == null) && (_firstName != null)) ||
				((firstName != null) && (_firstName == null)) ||
				((firstName != null) && (_firstName != null) &&
				!firstName.equals(_firstName))) {
			if (!XSS_ALLOW_FIRSTNAME) {
				firstName = Xss.strip(firstName);
			}

			_firstName = firstName;
			setModified(true);
		}
	}

	public String getMiddleName() {
		return _middleName;
	}

	public void setMiddleName(String middleName) {
		if (((middleName == null) && (_middleName != null)) ||
				((middleName != null) && (_middleName == null)) ||
				((middleName != null) && (_middleName != null) &&
				!middleName.equals(_middleName))) {
			if (!XSS_ALLOW_MIDDLENAME) {
				middleName = Xss.strip(middleName);
			}

			_middleName = middleName;
			setModified(true);
		}
	}

	public String getLastName() {
		return _lastName;
	}

	public void setLastName(String lastName) {
		if (((lastName == null) && (_lastName != null)) ||
				((lastName != null) && (_lastName == null)) ||
				((lastName != null) && (_lastName != null) &&
				!lastName.equals(_lastName))) {
			if (!XSS_ALLOW_LASTNAME) {
				lastName = Xss.strip(lastName);
			}

			_lastName = lastName;
			setModified(true);
		}
	}

	public String getNickName() {
		return _nickName;
	}

	public void setNickName(String nickName) {
		if (((nickName == null) && (_nickName != null)) ||
				((nickName != null) && (_nickName == null)) ||
				((nickName != null) && (_nickName != null) &&
				!nickName.equals(_nickName))) {
			if (!XSS_ALLOW_NICKNAME) {
				nickName = Xss.strip(nickName);
			}

			_nickName = nickName;
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

	public String getSmsId() {
		return _smsId;
	}

	public void setSmsId(String smsId) {
		if (((smsId == null) && (_smsId != null)) ||
				((smsId != null) && (_smsId == null)) ||
				((smsId != null) && (_smsId != null) && !smsId.equals(_smsId))) {
			if (!XSS_ALLOW_SMSID) {
				smsId = Xss.strip(smsId);
			}

			_smsId = smsId;
			setModified(true);
		}
	}

	public String getAimId() {
		return _aimId;
	}

	public void setAimId(String aimId) {
		if (((aimId == null) && (_aimId != null)) ||
				((aimId != null) && (_aimId == null)) ||
				((aimId != null) && (_aimId != null) && !aimId.equals(_aimId))) {
			if (!XSS_ALLOW_AIMID) {
				aimId = Xss.strip(aimId);
			}

			_aimId = aimId;
			setModified(true);
		}
	}

	public String getIcqId() {
		return _icqId;
	}

	public void setIcqId(String icqId) {
		if (((icqId == null) && (_icqId != null)) ||
				((icqId != null) && (_icqId == null)) ||
				((icqId != null) && (_icqId != null) && !icqId.equals(_icqId))) {
			if (!XSS_ALLOW_ICQID) {
				icqId = Xss.strip(icqId);
			}

			_icqId = icqId;
			setModified(true);
		}
	}

	public String getMsnId() {
		return _msnId;
	}

	public void setMsnId(String msnId) {
		if (((msnId == null) && (_msnId != null)) ||
				((msnId != null) && (_msnId == null)) ||
				((msnId != null) && (_msnId != null) && !msnId.equals(_msnId))) {
			if (!XSS_ALLOW_MSNID) {
				msnId = Xss.strip(msnId);
			}

			_msnId = msnId;
			setModified(true);
		}
	}

	public String getSkypeId() {
		return _skypeId;
	}

	public void setSkypeId(String skypeId) {
		if (((skypeId == null) && (_skypeId != null)) ||
				((skypeId != null) && (_skypeId == null)) ||
				((skypeId != null) && (_skypeId != null) &&
				!skypeId.equals(_skypeId))) {
			if (!XSS_ALLOW_SKYPEID) {
				skypeId = Xss.strip(skypeId);
			}

			_skypeId = skypeId;
			setModified(true);
		}
	}

	public String getYmId() {
		return _ymId;
	}

	public void setYmId(String ymId) {
		if (((ymId == null) && (_ymId != null)) ||
				((ymId != null) && (_ymId == null)) ||
				((ymId != null) && (_ymId != null) && !ymId.equals(_ymId))) {
			if (!XSS_ALLOW_YMID) {
				ymId = Xss.strip(ymId);
			}

			_ymId = ymId;
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

	public boolean getMale() {
		return _male;
	}

	public boolean isMale() {
		return _male;
	}

	public void setMale(boolean male) {
		if (male != _male) {
			_male = male;
			setModified(true);
		}
	}

	public Date getBirthday() {
		return _birthday;
	}

	public void setBirthday(Date birthday) {
		if (((birthday == null) && (_birthday != null)) ||
				((birthday != null) && (_birthday == null)) ||
				((birthday != null) && (_birthday != null) &&
				!birthday.equals(_birthday))) {
			_birthday = birthday;
			setModified(true);
		}
	}

	public String getTimeZoneId() {
		return _timeZoneId;
	}

	public void setTimeZoneId(String timeZoneId) {
		if (((timeZoneId == null) && (_timeZoneId != null)) ||
				((timeZoneId != null) && (_timeZoneId == null)) ||
				((timeZoneId != null) && (_timeZoneId != null) &&
				!timeZoneId.equals(_timeZoneId))) {
			if (!XSS_ALLOW_TIMEZONEID) {
				timeZoneId = Xss.strip(timeZoneId);
			}

			_timeZoneId = timeZoneId;
			setModified(true);
		}
	}

	public String getEmployeeNumber() {
		return _employeeNumber;
	}

	public void setEmployeeNumber(String employeeNumber) {
		if (((employeeNumber == null) && (_employeeNumber != null)) ||
				((employeeNumber != null) && (_employeeNumber == null)) ||
				((employeeNumber != null) && (_employeeNumber != null) &&
				!employeeNumber.equals(_employeeNumber))) {
			if (!XSS_ALLOW_EMPLOYEENUMBER) {
				employeeNumber = Xss.strip(employeeNumber);
			}

			_employeeNumber = employeeNumber;
			setModified(true);
		}
	}

	public String getJobTitle() {
		return _jobTitle;
	}

	public void setJobTitle(String jobTitle) {
		if (((jobTitle == null) && (_jobTitle != null)) ||
				((jobTitle != null) && (_jobTitle == null)) ||
				((jobTitle != null) && (_jobTitle != null) &&
				!jobTitle.equals(_jobTitle))) {
			if (!XSS_ALLOW_JOBTITLE) {
				jobTitle = Xss.strip(jobTitle);
			}

			_jobTitle = jobTitle;
			setModified(true);
		}
	}

	public String getJobClass() {
		return _jobClass;
	}

	public void setJobClass(String jobClass) {
		if (((jobClass == null) && (_jobClass != null)) ||
				((jobClass != null) && (_jobClass == null)) ||
				((jobClass != null) && (_jobClass != null) &&
				!jobClass.equals(_jobClass))) {
			if (!XSS_ALLOW_JOBCLASS) {
				jobClass = Xss.strip(jobClass);
			}

			_jobClass = jobClass;
			setModified(true);
		}
	}

	public String getHoursOfOperation() {
		return _hoursOfOperation;
	}

	public void setHoursOfOperation(String hoursOfOperation) {
		if (((hoursOfOperation == null) && (_hoursOfOperation != null)) ||
				((hoursOfOperation != null) && (_hoursOfOperation == null)) ||
				((hoursOfOperation != null) && (_hoursOfOperation != null) &&
				!hoursOfOperation.equals(_hoursOfOperation))) {
			if (!XSS_ALLOW_HOURSOFOPERATION) {
				hoursOfOperation = Xss.strip(hoursOfOperation);
			}

			_hoursOfOperation = hoursOfOperation;
			setModified(true);
		}
	}

	public BaseModel getProtected() {
		return null;
	}

	public void protect() {
	}

	public Object clone() {
		return new Contact(getContactId(), getCompanyId(), getUserId(),
			getUserName(), getCreateDate(), getModifiedDate(),
			getParentContactId(), getFirstName(), getMiddleName(),
			getLastName(), getNickName(), getEmailAddress1(),
			getEmailAddress2(), getSmsId(), getAimId(), getIcqId(), getMsnId(),
			getSkypeId(), getYmId(), getWebsite(), getMale(), getBirthday(),
			getTimeZoneId(), getEmployeeNumber(), getJobTitle(), getJobClass(),
			getHoursOfOperation());
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		Contact contact = (Contact)obj;
		String pk = contact.getPrimaryKey();

		return getPrimaryKey().compareTo(pk);
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		Contact contact = null;

		try {
			contact = (Contact)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		String pk = contact.getPrimaryKey();

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

	private String _contactId;
	private String _companyId;
	private String _userId;
	private String _userName;
	private Date _createDate;
	private Date _modifiedDate;
	private String _parentContactId;
	private String _firstName;
	private String _middleName;
	private String _lastName;
	private String _nickName;
	private String _emailAddress1;
	private String _emailAddress2;
	private String _smsId;
	private String _aimId;
	private String _icqId;
	private String _msnId;
	private String _skypeId;
	private String _ymId;
	private String _website;
	private boolean _male;
	private Date _birthday;
	private String _timeZoneId;
	private String _employeeNumber;
	private String _jobTitle;
	private String _jobClass;
	private String _hoursOfOperation;
}