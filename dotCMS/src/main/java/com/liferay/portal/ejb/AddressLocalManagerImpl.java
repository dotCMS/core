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
import java.util.List;

import com.dotcms.repackage.com.liferay.counter.ejb.CounterManagerUtil;
import com.liferay.portal.AddressCellException;
import com.liferay.portal.AddressCityException;
import com.liferay.portal.AddressCountryException;
import com.liferay.portal.AddressDescriptionException;
import com.liferay.portal.AddressFaxException;
import com.liferay.portal.AddressPhoneException;
import com.liferay.portal.AddressStateException;
import com.liferay.portal.AddressStreetException;
import com.liferay.portal.AddressZipException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;
import com.liferay.util.PhoneNumber;
import com.liferay.util.Validator;

/**
 * <a href="AddressLocalManagerImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class AddressLocalManagerImpl implements AddressLocalManager {

	// Business methods

	public Address addAddress(
			String userId, String className, String classPK, String description,
			String street1, String street2, String city, String state,
			String zip, String country, String phone, String fax, String cell)
		throws PortalException, SystemException {

		_validate(
			description, street1, city, state, zip, country, phone, fax, cell);

		User user = UserUtil.findByPrimaryKey(userId);

		String addressId = Long.toString(CounterManagerUtil.increment(
			Address.class.getName()));

		Address address = AddressUtil.create(addressId);

		Date now = new Date();

		address.setCompanyId(user.getCompanyId());
		address.setUserId(user.getUserId());
		address.setUserName(user.getFullName());
		address.setCreateDate(now);
		address.setModifiedDate(now);
		address.setClassName(className);
		address.setClassPK(classPK);
		address.setDescription(description);
		address.setStreet1(street1);
		address.setStreet2(street2);
		address.setCity(city);
		address.setState(state);
		address.setZip(zip);
		address.setCountry(country);
		address.setPhone(PhoneNumber.strip(phone));
		address.setFax(PhoneNumber.strip(fax));
		address.setCell(PhoneNumber.strip(cell));
		address.setPriority(
			_getNextPriority(user.getCompanyId(), className, classPK));

		AddressUtil.update(address);

		return address;
	}

	public void deleteAddress(String addressId)
		throws PortalException, SystemException {

		AddressUtil.remove(addressId);
	}

	public void deleteAll(String companyId, String className, String classPK)
		throws SystemException {

		AddressUtil.removeByC_C_C(companyId, className, classPK);
	}

	public Address updateAddress(
			String addressId, String description, String street1,
			String street2, String city, String state, String zip,
			String country, String phone, String fax, String cell)
		throws PortalException, SystemException {

		_validate(
			description, street1, city, state, zip, country, phone, fax, cell);

		Address address = AddressUtil.findByPrimaryKey(addressId);

		address.setDescription(description);
		address.setStreet1(street1);
		address.setStreet2(street2);
		address.setCity(city);
		address.setState(state);
		address.setZip(zip);
		address.setCountry(country);
		address.setPhone(PhoneNumber.strip(phone));
		address.setFax(PhoneNumber.strip(fax));
		address.setCell(PhoneNumber.strip(cell));

		AddressUtil.update(address);

		return address;
	}

	// Private methods

	private int _getNextPriority(
			String companyId, String className, String classPK)
		throws SystemException {

		List addresses = AddressUtil.findByC_C_C(companyId, className, classPK);

		if (addresses.size() == 0) {
			return 0;
		}

		Address address = (Address)addresses.get(addresses.size() - 1);

		return address.getPriority() + 1;
	}

	private void _validate(
			String description, String street1, String city, String state,
			String zip, String country, String phone, String fax, String cell)
		throws PortalException {

		if (Validator.isNull(description)) {
			throw new AddressDescriptionException();
		}
		else if (Validator.isNull(street1)) {
			throw new AddressStreetException();
		}
		else if (Validator.isNull(city)) {
			throw new AddressCityException();
		}
		else if (Validator.isNull(state)) {
			throw new AddressStateException();
		}
		else if (Validator.isNull(zip)) {
			throw new AddressZipException();
		}
		else if (Validator.isNull(country)) {
			throw new AddressCountryException();
		}
		else if (Validator.isNotNull(phone) &&
				 !Validator.isPhoneNumber(phone)) {

			throw new AddressPhoneException();
		}
		else if (Validator.isNotNull(fax) && !Validator.isPhoneNumber(fax)) {
			throw new AddressFaxException();
		}
		else if (Validator.isNotNull(cell) && !Validator.isPhoneNumber(cell)) {
			throw new AddressCellException();
		}
	}

}