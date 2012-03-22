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

/**
 * <a href="Contact.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class Contact extends ContactModel {

	public Contact() {
	}

	public Contact(String contactId) {
		super(contactId);
	}

	public Contact(String contactId, String companyId, String userId,
				   String userName, Date createDate, Date modifiedDate,
				   String parentContactId, String firstName, String middleName,
				   String lastName, String nickName, String emailAddress1,
				   String emailAddress2, String smsId, String aimId,
				   String icqId, String msnId, String skypeId, String ymId,
				   String website, boolean male, Date birthday,
				   String timeZoneId, String employeeNumber, String jobTitle,
				   String jobClass, String hoursOfOperation) {

		super(contactId, companyId, userId, userName, createDate, modifiedDate,
			  parentContactId, firstName, middleName, lastName, nickName,
			  emailAddress1, emailAddress2, smsId, aimId, icqId, msnId, skypeId,
			  ymId, website, male, birthday, timeZoneId, employeeNumber,
			  jobTitle, jobClass, hoursOfOperation);
	}

}