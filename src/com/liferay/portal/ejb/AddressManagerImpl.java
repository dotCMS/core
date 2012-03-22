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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;
import com.liferay.util.CollectionFactory;

/**
 * <a href="AddressManagerImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class AddressManagerImpl
	extends PrincipalBean implements AddressManager {

	// Business methods

	public Address addAddress(
			String userId, String className, String classPK, String description,
			String street1, String street2, String city, String state,
			String zip, String country, String phone, String fax, String cell)
		throws PortalException, SystemException {

		User user = UserUtil.findByPrimaryKey(userId);

		if (!getUserId().equals(userId) &&
			!hasAdministrator(user.getCompanyId())) {

			throw new PrincipalException();
		}

		return AddressLocalManagerUtil.addAddress(
			userId, className, classPK, description, street1, street2, city,
			state, zip, country, phone, fax, cell);
	}

	public void deleteAddress(String addressId)
		throws PortalException, SystemException {

		if (!hasWrite(addressId)) {
			throw new PrincipalException();
		}

		AddressLocalManagerUtil.deleteAddress(addressId);
	}

	public Address getAddress(String addressId)
		throws PortalException, SystemException {

		return AddressUtil.findByPrimaryKey(addressId);
	}

	public List getAddresses(String className, String classPK)
		throws PortalException, SystemException {

		return AddressUtil.findByC_C_C(
			getUser().getCompanyId(), className, classPK);
	}

	public Address getPrimaryAddress(String className, String classPK)
		throws PortalException, SystemException {

		List addresses = getAddresses(className, classPK);

		Address address = null;

		if (addresses.size() > 0) {
			address = (Address)addresses.get(0);
		}

		return address;
	}

	public Address updateAddress(
			String addressId, String description, String street1,
			String street2, String city, String state, String zip,
			String country, String phone, String fax, String cell)
		throws PortalException, SystemException {

		if (!hasWrite(addressId)) {
			throw new PrincipalException();
		}

		return AddressLocalManagerUtil.updateAddress(
			addressId, description, street1, street2, city, state, zip, country,
			phone, fax, cell);
	}

	public void updateAddresses(
			String className, String classPK, String[] addressIds)
		throws PortalException, SystemException {

		if (addressIds == null) {
			return;
		}

		for (int i = 0; i < addressIds.length; i++) {
			if (!hasWrite(addressIds[i])) {
				throw new PrincipalException();
			}
		}

		Map priorities = CollectionFactory.getHashMap();

		for (int i = 0; i < addressIds.length; i++) {
			priorities.put(addressIds[i], new Integer(i));
		}

		Iterator itr = AddressUtil.findByC_C_C(
			getUser().getCompanyId(), className, classPK).iterator();

		while (itr.hasNext()) {
			Address address = (Address)itr.next();

			Integer priority = (Integer)priorities.get(address.getAddressId());

			if (priority == null) {
				AddressUtil.remove(address.getAddressId());
			}
			else {
				address.setPriority(priority.intValue());

				AddressUtil.update(address);
			}
		}
	}

	public void updateAddressPriority(
			String className, String classPK, String addressId,
			boolean priority)
		throws PortalException, SystemException {

		List list = new ArrayList();

		Iterator itr = AddressUtil.findByC_C_C(
			getUser().getCompanyId(), className, classPK).iterator();

		while (itr.hasNext()) {
			Address address = (Address)itr.next();

			list.add(address.getAddressId());
		}

		String[] addressIds = (String[])list.toArray(new String[0]);

		String prevAddressId = null;
		String currAddressId = null;
		String nextAddressId = null;

		for (int i = 0; i < addressIds.length; i++) {
			if (i == 0) {
				prevAddressId = addressIds[addressIds.length - 1];
			}
			else {
				prevAddressId = currAddressId;
			}

			currAddressId = addressIds[i];

			if (i == (addressIds.length - 1)) {
				nextAddressId = addressIds[0];
			}
			else {
				nextAddressId = addressIds[i + 1];
			}

			if (currAddressId.equals(addressId)) {
				if (priority) {
					addressIds[i] = prevAddressId;

					if (i - 1 >= 0) {
						addressIds[i - 1] = currAddressId;
					}
					else {
						addressIds[addressIds.length - 1] = currAddressId;
					}

					break;
				}
				else {
					addressIds[i] = nextAddressId;

					if (i + 1 < addressIds.length) {
						addressIds[i + 1] = currAddressId;
					}
					else {
						addressIds[0] = currAddressId;
					}

					break;
				}
			}
		}

		updateAddresses(className, classPK, addressIds);
	}

	// Permission methods

	public boolean hasWrite(String addressId)
		throws PortalException, SystemException {

		Address address = AddressUtil.findByPrimaryKey(addressId);

		if (getUserId().equals(address.getUserId()) ||
			getUserId().equals(address.getClassPK()) ||
			hasAdministrator(address.getCompanyId())) {

			return true;
		}
		else {
			return false;
		}
	}

}