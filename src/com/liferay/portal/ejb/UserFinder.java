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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.util.dao.DataAccess;

/**
 * <a href="UserFinder.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Jon Steer
 * @version $Revision: 1.24 $
 *
 */
public class UserFinder {

	/**
	 * Find all Users with custom skins.
	 *
	 * @return		a list of all Users with custom skins
	 */
	protected static List findBySkinId() throws SystemException {
		List list = new ArrayList();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getConnection(Constants.DATA_SOURCE);

			StringBuffer query = new StringBuffer();
			query.append("SELECT userId ");
			query.append("FROM User_ WHERE ");
			query.append("userId = skinId");

			ps = con.prepareStatement(query.toString());

			rs = ps.executeQuery();

			while (rs.next()) {
				User user = UserUtil.findByPrimaryKey(rs.getString(1));

				list.add((User)user.getProtected());
			}
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}

		return list;
	}

	/**
	 * Find all Users associated with the specified company id who have sms
	 * capable cell phones.
	 *
	 * @return		a list of all Users associated with the specified company
	 *				id who have sms capable cell phones
	 */
	protected static List findByC_SMS(String companyId)
		throws SystemException {

		List list = new ArrayList();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getConnection(Constants.DATA_SOURCE);

			StringBuffer query = new StringBuffer();
			query.append("SELECT userId ");
			query.append("FROM User_ WHERE ");
			query.append("companyId = ? AND ");
			query.append("(smsId IS NOT NULL AND smsId != '')");

			ps = con.prepareStatement(query.toString());

			ps.setString(1, companyId);

			rs = ps.executeQuery();

			while (rs.next()) {
				User user = UserUtil.findByPrimaryKey(rs.getString(1));

				list.add((User)user.getProtected());
			}
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}

		return list;
	}

	/**
	 * Find all Users associated with the specified company id, first name, last
	 * name, email address, sex, age, instant messenger handle, and address.
	 *
	 * @return		a list of all Users associated with the specified company
	 *				id, first name, middle name, last name, email address, sex,
	 *				age, instant messenger handle, and address
	 */
	protected static List findByAnd_C_FN_MN_LN_EA_M_BD_IM_A(
			String companyId, String firstName, String middleName,
			String lastName, String emailAddress, Boolean male, Date age1,
			Date age2, String im, String street1, String street2, String city,
			String state, String zip, String phone, String fax, String cell)
		throws SystemException {

		Timestamp age1Timestamp = null;
		if (age1 != null) {
			age1Timestamp = new Timestamp(age1.getTime());
		}

		Timestamp age2Timestamp = null;
		if (age2 != null) {
			age2Timestamp = new Timestamp(age2.getTime());
		}

		return findByAnd_C_FN_MN_LN_EA_M_BD_IM_A(
			companyId, firstName, middleName, lastName, emailAddress,
			male, age1Timestamp, age2Timestamp, im, street1, street2, city,
			state, zip, phone, fax, cell);
	}

	/**
	 * Find all Users associated with the specified company id, first name, last
	 * name, email address, sex, age, instant messenger handle, and address.
	 *
	 * @return		a list of all Users associated with the specified company
	 *				id, first name, middle name, last name, email address, sex,
	 *				age, instant messenger handle, and address
	 */
	protected static List findByAnd_C_FN_MN_LN_EA_M_BD_IM_A(
			String companyId, String firstName, String middleName,
			String lastName, String emailAddress, Boolean male, Timestamp age1,
			Timestamp age2, String im, String street1, String street2,
			String city, String state, String zip, String phone, String fax,
			String cell)
		throws SystemException {

		List list = new ArrayList();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getConnection(Constants.DATA_SOURCE);

			StringBuffer query = new StringBuffer();
			query.append("SELECT DISTINCT User_.userId ");
			query.append("FROM User_ ");
			query.append("LEFT JOIN Address ON (");
			query.append("(Address.companyId = User_.companyId) AND ");
			query.append("(Address.className = 'com.liferay.portal.model.User')");
			query.append(") WHERE ");
			query.append("(User_.companyId = ?) AND (");
			query.append("(lower(firstName) LIKE ? OR ? IS NULL) AND ");
			query.append("(lower(middleName) LIKE ? OR ? IS NULL) AND ");
			query.append("(lower(lastName) LIKE ? OR ? IS NULL) AND ");
			query.append("(lower(emailAddress) LIKE ? OR ? IS NULL) AND ");
			query.append("(male = ? OR ? IS NULL) AND ");
			query.append("(birthday >= ? OR ? IS NULL) AND ");
			query.append("(birthday <= ? OR ? IS NULL) AND ");
			query.append("(lower(aimId) LIKE ? OR lower(icqId) LIKE ? OR lower(msnId) LIKE ? OR lower(ymId) LIKE ? OR ? IS NULL) AND ");
			query.append("(lower(Address.street1) LIKE ? AND Address.classPK = User_.userId OR ? IS NULL) AND ");
			query.append("(lower(Address.street2) LIKE ? AND Address.classPK = User_.userId OR ? IS NULL) AND ");
			query.append("(lower(Address.city) LIKE ? AND Address.classPK = User_.userId OR ? IS NULL) AND ");
			query.append("(lower(Address.state) LIKE ? AND Address.classPK = User_.userId OR ? IS NULL) AND ");
			query.append("(lower(Address.zip) LIKE ? AND Address.classPK = User_.userId OR ? IS NULL) AND ");
			query.append("(lower(Address.phone) LIKE ? AND Address.classPK = User_.userId OR ? IS NULL) AND ");
			query.append("(lower(Address.fax) LIKE ? AND Address.classPK = User_.userId OR ? IS NULL) AND ");
			query.append("(lower(Address.cell) LIKE ? AND Address.classPK = User_.userId OR ? IS NULL) ");
			query.append(")");

			ps = con.prepareStatement(query.toString());

			ps.setString(1, companyId);
			ps.setString(2, firstName);
			ps.setString(3, firstName);
			ps.setString(4, middleName);
			ps.setString(5, middleName);
			ps.setString(6, lastName);
			ps.setString(7, lastName);
			ps.setString(8, emailAddress);
			ps.setString(9, emailAddress);

			if (male != null) {
				ps.setBoolean(10, male.booleanValue());
				ps.setBoolean(11, male.booleanValue());
			}
			else {
				ps.setNull(10, Types.NUMERIC);
				ps.setNull(11, Types.NUMERIC);
			}

			ps.setTimestamp(12, age1);
			ps.setTimestamp(13, age1);
			ps.setTimestamp(14, age2);
			ps.setTimestamp(15, age2);
			ps.setString(16, im);
			ps.setString(17, im);
			ps.setString(18, im);
			ps.setString(19, im);
			ps.setString(20, im);
			ps.setString(21, street1);
			ps.setString(22, street1);
			ps.setString(23, street2);
			ps.setString(24, street2);
			ps.setString(25, city);
			ps.setString(26, city);
			ps.setString(27, state);
			ps.setString(28, state);
			ps.setString(29, zip);
			ps.setString(30, zip);
			ps.setString(31, phone);
			ps.setString(32, phone);
			ps.setString(33, fax);
			ps.setString(34, fax);
			ps.setString(35, cell);
			ps.setString(36, cell);

			rs = ps.executeQuery();

			while (rs.next()) {
				User user = UserUtil.findByPrimaryKey(rs.getString(1));

				list.add((User)user.getProtected());
			}
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}

		return list;
	}

	/**
	 * Find all Users associated with the specified company id, first name, last
	 * name, email address, sex, age, instant messenger handle, or address.
	 *
	 * @return		a list of all Users associated with the specified company
	 *				id, first name, middle name, last name, email address, sex,
	 *				age, instant messenger handle, and address
	 */
	protected static List findByOr_C_FN_MN_LN_EA_M_BD_IM_A(
			String companyId, String firstName, String middleName,
			String lastName, String emailAddress, Boolean male, Date age1,
			Date age2, String im, String street1, String street2, String city,
			String state, String zip, String phone, String fax, String cell)
		throws SystemException {

		Timestamp age1Timestamp = null;
		if (age1 != null) {
			age1Timestamp = new Timestamp(age1.getTime());
		}

		Timestamp age2Timestamp = null;
		if (age2 != null) {
			age2Timestamp = new Timestamp(age2.getTime());
		}

		return findByOr_C_FN_MN_LN_EA_M_BD_IM_A(
			companyId, firstName, middleName, lastName, emailAddress,
			male, age1Timestamp, age2Timestamp, im, street1, street2, city,
			state, zip, phone, fax, cell);
	}

	/**
	 * Find all Users associated with the specified company id, first name, last
	 * name, email address, sex, age, instant messenger handle, or address.
	 *
	 * @return		a list of all Users associated with the specified company
	 *				id, first name, middle name, last name, email address, sex,
	 *				age, instant messenger handle, and address
	 */
	protected static List findByOr_C_FN_MN_LN_EA_M_BD_IM_A(
			String companyId, String firstName, String middleName,
			String lastName, String emailAddress, Boolean male, Timestamp age1,
			Timestamp age2, String im, String street1, String street2,
			String city, String state, String zip, String phone, String fax,
			String cell)
		throws SystemException {

		List list = new ArrayList();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getConnection(Constants.DATA_SOURCE);

			StringBuffer query = new StringBuffer();
			query.append("SELECT DISTINCT User_.userId ");
			query.append("FROM User_ ");
			query.append("LEFT JOIN Address ON (");
			query.append("(Address.companyId = User_.companyId) AND ");
			query.append("(Address.className = 'com.liferay.portal.model.User')");
			query.append(") WHERE ");
			query.append("(User_.companyId = ?) AND (");
			query.append("(lower(firstName) LIKE ? AND ? IS NOT NULL) OR ");
			query.append("(lower(middleName) LIKE ? AND ? IS NOT NULL) OR ");
			query.append("(lower(lastName) LIKE ? AND ? IS NOT NULL) OR ");
			query.append("(lower(emailAddress) LIKE ? AND ? IS NOT NULL) OR ");
			query.append("(male = ? AND ? IS NOT NULL) OR ");
			query.append("(birthday >= ? AND ? IS NOT NULL) OR ");
			query.append("(birthday <= ? AND ? IS NOT NULL) OR ");
			query.append("(lower(aimId) LIKE ? OR lower(icqId) LIKE ? OR lower(msnId) LIKE ? OR lower(ymId) LIKE ? AND ? IS NOT NULL) OR ");
			query.append("(lower(Address.street1) LIKE ? AND Address.classPK = User_.userId AND ? IS NOT NULL) OR ");
			query.append("(lower(Address.street2) LIKE ? AND Address.classPK = User_.userId AND ? IS NOT NULL) OR ");
			query.append("(lower(Address.city) LIKE ? AND Address.classPK = User_.userId AND ? IS NOT NULL) OR ");
			query.append("(lower(Address.state) LIKE ? AND Address.classPK = User_.userId AND ? IS NOT NULL) OR ");
			query.append("(lower(Address.zip) LIKE ? AND Address.classPK = User_.userId AND ? IS NOT NULL) OR ");
			query.append("(lower(Address.phone) LIKE ? AND Address.classPK = User_.userId AND ? IS NOT NULL) OR ");
			query.append("(lower(Address.fax) LIKE ? AND Address.classPK = User_.userId AND ? IS NOT NULL) OR ");
			query.append("(lower(Address.cell) LIKE ? AND Address.classPK = User_.userId AND ? IS NOT NULL) ");
			query.append(")");

			ps = con.prepareStatement(query.toString());

			ps.setString(1, companyId);
			ps.setString(2, firstName);
			ps.setString(3, firstName);
			ps.setString(4, middleName);
			ps.setString(5, middleName);
			ps.setString(6, lastName);
			ps.setString(7, lastName);
			ps.setString(8, emailAddress);
			ps.setString(9, emailAddress);

			if (male != null) {
				ps.setBoolean(10, male.booleanValue());
				ps.setBoolean(11, male.booleanValue());
			}
			else {
				ps.setNull(10, Types.NUMERIC);
				ps.setNull(11, Types.NUMERIC);
			}

			ps.setTimestamp(12, age1);
			ps.setTimestamp(13, age1);
			ps.setTimestamp(14, age2);
			ps.setTimestamp(15, age2);
			ps.setString(16, im);
			ps.setString(17, im);
			ps.setString(18, im);
			ps.setString(19, im);
			ps.setString(20, im);
			ps.setString(21, street1);
			ps.setString(22, street1);
			ps.setString(23, street2);
			ps.setString(24, street2);
			ps.setString(25, city);
			ps.setString(26, city);
			ps.setString(27, state);
			ps.setString(28, state);
			ps.setString(29, zip);
			ps.setString(30, zip);
			ps.setString(31, phone);
			ps.setString(32, phone);
			ps.setString(33, fax);
			ps.setString(34, fax);
			ps.setString(35, cell);
			ps.setString(36, cell);

			rs = ps.executeQuery();

			while (rs.next()) {
				User user = UserUtil.findByPrimaryKey(rs.getString(1));

				list.add((User)user.getProtected());
			}
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}

		return list;
	}

}