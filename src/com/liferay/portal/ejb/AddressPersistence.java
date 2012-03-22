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

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.ObjectNotFoundException;
import net.sf.hibernate.Query;
import net.sf.hibernate.ScrollableResults;
import net.sf.hibernate.Session;

import com.liferay.portal.NoSuchAddressException;
import com.liferay.portal.SystemException;
import com.liferay.portal.util.HibernateUtil;
import com.liferay.util.dao.hibernate.OrderByComparator;

/**
 * <a href="AddressPersistence.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.14 $
 *
 */
public class AddressPersistence extends BasePersistence {
	protected com.liferay.portal.model.Address create(String addressId) {
		return new com.liferay.portal.model.Address(addressId);
	}

	protected com.liferay.portal.model.Address remove(String addressId)
		throws NoSuchAddressException, SystemException {
		Session session = null;

		try {
			session = openSession();

			AddressHBM addressHBM = (AddressHBM)session.load(AddressHBM.class,
					addressId);
			com.liferay.portal.model.Address address = AddressHBMUtil.model(addressHBM);
			session.delete(addressHBM);
			session.flush();
			AddressPool.remove(addressId);

			return address;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchAddressException(addressId.toString());
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.Address update(
		com.liferay.portal.model.Address address) throws SystemException {
		Session session = null;

		try {
			if (address.isNew() || address.isModified()) {
				session = openSession();

				if (address.isNew()) {
					AddressHBM addressHBM = new AddressHBM(address.getAddressId(),
							address.getCompanyId(), address.getUserId(),
							address.getUserName(), address.getCreateDate(),
							address.getModifiedDate(), address.getClassName(),
							address.getClassPK(), address.getDescription(),
							address.getStreet1(), address.getStreet2(),
							address.getCity(), address.getState(),
							address.getZip(), address.getCountry(),
							address.getPhone(), address.getFax(),
							address.getCell(), address.getPriority());
					session.save(addressHBM);
					session.flush();
				}
				else {
					try {
						AddressHBM addressHBM = (AddressHBM)session.load(AddressHBM.class,
								address.getPrimaryKey());
						addressHBM.setCompanyId(address.getCompanyId());
						addressHBM.setUserId(address.getUserId());
						addressHBM.setUserName(address.getUserName());
						addressHBM.setCreateDate(address.getCreateDate());
						addressHBM.setModifiedDate(address.getModifiedDate());
						addressHBM.setClassName(address.getClassName());
						addressHBM.setClassPK(address.getClassPK());
						addressHBM.setDescription(address.getDescription());
						addressHBM.setStreet1(address.getStreet1());
						addressHBM.setStreet2(address.getStreet2());
						addressHBM.setCity(address.getCity());
						addressHBM.setState(address.getState());
						addressHBM.setZip(address.getZip());
						addressHBM.setCountry(address.getCountry());
						addressHBM.setPhone(address.getPhone());
						addressHBM.setFax(address.getFax());
						addressHBM.setCell(address.getCell());
						addressHBM.setPriority(address.getPriority());
						session.flush();
					}
					catch (ObjectNotFoundException onfe) {
						AddressHBM addressHBM = new AddressHBM(address.getAddressId(),
								address.getCompanyId(), address.getUserId(),
								address.getUserName(), address.getCreateDate(),
								address.getModifiedDate(),
								address.getClassName(), address.getClassPK(),
								address.getDescription(), address.getStreet1(),
								address.getStreet2(), address.getCity(),
								address.getState(), address.getZip(),
								address.getCountry(), address.getPhone(),
								address.getFax(), address.getCell(),
								address.getPriority());
						session.save(addressHBM);
						session.flush();
					}
				}

				address.setNew(false);
				address.setModified(false);
				address.protect();
				AddressPool.remove(address.getPrimaryKey());
				AddressPool.put(address.getPrimaryKey(), address);
			}

			return address;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.Address findByPrimaryKey(
		String addressId) throws NoSuchAddressException, SystemException {
		com.liferay.portal.model.Address address = AddressPool.get(addressId);
		Session session = null;

		try {
			if (address == null) {
				session = openSession();

				AddressHBM addressHBM = (AddressHBM)session.load(AddressHBM.class,
						addressId);
				address = AddressHBMUtil.model(addressHBM);
			}

			return address;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchAddressException(addressId.toString());
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByCompanyId(String companyId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("companyId ASC").append(", ");
			query.append("className ASC").append(", ");
			query.append("classPK ASC").append(", ");
			query.append("priority ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				AddressHBM addressHBM = (AddressHBM)itr.next();
				list.add(AddressHBMUtil.model(addressHBM));
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByCompanyId(String companyId, int begin, int end)
		throws SystemException {
		return findByCompanyId(companyId, begin, end, null);
	}

	protected List findByCompanyId(String companyId, int begin, int end,
		OrderByComparator obc) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("companyId ASC").append(", ");
				query.append("className ASC").append(", ");
				query.append("classPK ASC").append(", ");
				query.append("priority ASC");
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			List list = new ArrayList();

			if (getDialect().supportsLimit()) {
				q.setMaxResults(end - begin);
				q.setFirstResult(begin);

				Iterator itr = q.list().iterator();

				while (itr.hasNext()) {
					AddressHBM addressHBM = (AddressHBM)itr.next();
					list.add(AddressHBMUtil.model(addressHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						AddressHBM addressHBM = (AddressHBM)sr.get(0);
						list.add(AddressHBMUtil.model(addressHBM));

						if (!sr.next()) {
							break;
						}
					}
				}
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.Address findByCompanyId_First(
		String companyId, OrderByComparator obc)
		throws NoSuchAddressException, SystemException {
		List list = findByCompanyId(companyId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchAddressException();
		}
		else {
			return (com.liferay.portal.model.Address)list.get(0);
		}
	}

	protected com.liferay.portal.model.Address findByCompanyId_Last(
		String companyId, OrderByComparator obc)
		throws NoSuchAddressException, SystemException {
		int count = countByCompanyId(companyId);
		List list = findByCompanyId(companyId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchAddressException();
		}
		else {
			return (com.liferay.portal.model.Address)list.get(0);
		}
	}

	protected com.liferay.portal.model.Address[] findByCompanyId_PrevAndNext(
		String addressId, String companyId, OrderByComparator obc)
		throws NoSuchAddressException, SystemException {
		com.liferay.portal.model.Address address = findByPrimaryKey(addressId);
		int count = countByCompanyId(companyId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("companyId ASC").append(", ");
				query.append("className ASC").append(", ");
				query.append("classPK ASC").append(", ");
				query.append("priority ASC");
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			com.liferay.portal.model.Address[] array = new com.liferay.portal.model.Address[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					AddressHBM addressHBM = (AddressHBM)sr.get(0);

					if (addressHBM == null) {
						break;
					}

					com.liferay.portal.model.Address curAddress = AddressHBMUtil.model(addressHBM);
					int value = obc.compare(address, curAddress);

					if (value == 0) {
						if (!address.equals(curAddress)) {
							break;
						}

						array[1] = curAddress;

						if (sr.previous()) {
							array[0] = AddressHBMUtil.model((AddressHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = AddressHBMUtil.model((AddressHBM)sr.get(
										0));
						}

						break;
					}

					if (count == 1) {
						break;
					}

					count = (int)Math.ceil(count / 2.0);

					if (value < 0) {
						if (!sr.scroll(count * -1)) {
							break;
						}
					}
					else {
						if (!sr.scroll(count)) {
							break;
						}
					}
				}
			}

			return array;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByUserId(String userId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("companyId ASC").append(", ");
			query.append("className ASC").append(", ");
			query.append("classPK ASC").append(", ");
			query.append("priority ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				AddressHBM addressHBM = (AddressHBM)itr.next();
				list.add(AddressHBMUtil.model(addressHBM));
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByUserId(String userId, int begin, int end)
		throws SystemException {
		return findByUserId(userId, begin, end, null);
	}

	protected List findByUserId(String userId, int begin, int end,
		OrderByComparator obc) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("companyId ASC").append(", ");
				query.append("className ASC").append(", ");
				query.append("classPK ASC").append(", ");
				query.append("priority ASC");
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			List list = new ArrayList();

			if (getDialect().supportsLimit()) {
				q.setMaxResults(end - begin);
				q.setFirstResult(begin);

				Iterator itr = q.list().iterator();

				while (itr.hasNext()) {
					AddressHBM addressHBM = (AddressHBM)itr.next();
					list.add(AddressHBMUtil.model(addressHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						AddressHBM addressHBM = (AddressHBM)sr.get(0);
						list.add(AddressHBMUtil.model(addressHBM));

						if (!sr.next()) {
							break;
						}
					}
				}
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.Address findByUserId_First(
		String userId, OrderByComparator obc)
		throws NoSuchAddressException, SystemException {
		List list = findByUserId(userId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchAddressException();
		}
		else {
			return (com.liferay.portal.model.Address)list.get(0);
		}
	}

	protected com.liferay.portal.model.Address findByUserId_Last(
		String userId, OrderByComparator obc)
		throws NoSuchAddressException, SystemException {
		int count = countByUserId(userId);
		List list = findByUserId(userId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchAddressException();
		}
		else {
			return (com.liferay.portal.model.Address)list.get(0);
		}
	}

	protected com.liferay.portal.model.Address[] findByUserId_PrevAndNext(
		String addressId, String userId, OrderByComparator obc)
		throws NoSuchAddressException, SystemException {
		com.liferay.portal.model.Address address = findByPrimaryKey(addressId);
		int count = countByUserId(userId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("companyId ASC").append(", ");
				query.append("className ASC").append(", ");
				query.append("classPK ASC").append(", ");
				query.append("priority ASC");
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			com.liferay.portal.model.Address[] array = new com.liferay.portal.model.Address[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					AddressHBM addressHBM = (AddressHBM)sr.get(0);

					if (addressHBM == null) {
						break;
					}

					com.liferay.portal.model.Address curAddress = AddressHBMUtil.model(addressHBM);
					int value = obc.compare(address, curAddress);

					if (value == 0) {
						if (!address.equals(curAddress)) {
							break;
						}

						array[1] = curAddress;

						if (sr.previous()) {
							array[0] = AddressHBMUtil.model((AddressHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = AddressHBMUtil.model((AddressHBM)sr.get(
										0));
						}

						break;
					}

					if (count == 1) {
						break;
					}

					count = (int)Math.ceil(count / 2.0);

					if (value < 0) {
						if (!sr.scroll(count * -1)) {
							break;
						}
					}
					else {
						if (!sr.scroll(count)) {
							break;
						}
					}
				}
			}

			return array;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByC_C(String companyId, String className)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("className = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("companyId ASC").append(", ");
			query.append("className ASC").append(", ");
			query.append("classPK ASC").append(", ");
			query.append("priority ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, className);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				AddressHBM addressHBM = (AddressHBM)itr.next();
				list.add(AddressHBMUtil.model(addressHBM));
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByC_C(String companyId, String className, int begin,
		int end) throws SystemException {
		return findByC_C(companyId, className, begin, end, null);
	}

	protected List findByC_C(String companyId, String className, int begin,
		int end, OrderByComparator obc) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("className = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("companyId ASC").append(", ");
				query.append("className ASC").append(", ");
				query.append("classPK ASC").append(", ");
				query.append("priority ASC");
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, className);

			List list = new ArrayList();

			if (getDialect().supportsLimit()) {
				q.setMaxResults(end - begin);
				q.setFirstResult(begin);

				Iterator itr = q.list().iterator();

				while (itr.hasNext()) {
					AddressHBM addressHBM = (AddressHBM)itr.next();
					list.add(AddressHBMUtil.model(addressHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						AddressHBM addressHBM = (AddressHBM)sr.get(0);
						list.add(AddressHBMUtil.model(addressHBM));

						if (!sr.next()) {
							break;
						}
					}
				}
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.Address findByC_C_First(
		String companyId, String className, OrderByComparator obc)
		throws NoSuchAddressException, SystemException {
		List list = findByC_C(companyId, className, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchAddressException();
		}
		else {
			return (com.liferay.portal.model.Address)list.get(0);
		}
	}

	protected com.liferay.portal.model.Address findByC_C_Last(
		String companyId, String className, OrderByComparator obc)
		throws NoSuchAddressException, SystemException {
		int count = countByC_C(companyId, className);
		List list = findByC_C(companyId, className, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchAddressException();
		}
		else {
			return (com.liferay.portal.model.Address)list.get(0);
		}
	}

	protected com.liferay.portal.model.Address[] findByC_C_PrevAndNext(
		String addressId, String companyId, String className,
		OrderByComparator obc) throws NoSuchAddressException, SystemException {
		com.liferay.portal.model.Address address = findByPrimaryKey(addressId);
		int count = countByC_C(companyId, className);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("className = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("companyId ASC").append(", ");
				query.append("className ASC").append(", ");
				query.append("classPK ASC").append(", ");
				query.append("priority ASC");
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, className);

			com.liferay.portal.model.Address[] array = new com.liferay.portal.model.Address[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					AddressHBM addressHBM = (AddressHBM)sr.get(0);

					if (addressHBM == null) {
						break;
					}

					com.liferay.portal.model.Address curAddress = AddressHBMUtil.model(addressHBM);
					int value = obc.compare(address, curAddress);

					if (value == 0) {
						if (!address.equals(curAddress)) {
							break;
						}

						array[1] = curAddress;

						if (sr.previous()) {
							array[0] = AddressHBMUtil.model((AddressHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = AddressHBMUtil.model((AddressHBM)sr.get(
										0));
						}

						break;
					}

					if (count == 1) {
						break;
					}

					count = (int)Math.ceil(count / 2.0);

					if (value < 0) {
						if (!sr.scroll(count * -1)) {
							break;
						}
					}
					else {
						if (!sr.scroll(count)) {
							break;
						}
					}
				}
			}

			return array;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByC_C_C(String companyId, String className,
		String classPK) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("className = ?");
			query.append(" AND ");
			query.append("classPK = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("companyId ASC").append(", ");
			query.append("className ASC").append(", ");
			query.append("classPK ASC").append(", ");
			query.append("priority ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, className);
			q.setString(queryPos++, classPK);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				AddressHBM addressHBM = (AddressHBM)itr.next();
				list.add(AddressHBMUtil.model(addressHBM));
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByC_C_C(String companyId, String className,
		String classPK, int begin, int end) throws SystemException {
		return findByC_C_C(companyId, className, classPK, begin, end, null);
	}

	protected List findByC_C_C(String companyId, String className,
		String classPK, int begin, int end, OrderByComparator obc)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("className = ?");
			query.append(" AND ");
			query.append("classPK = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("companyId ASC").append(", ");
				query.append("className ASC").append(", ");
				query.append("classPK ASC").append(", ");
				query.append("priority ASC");
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, className);
			q.setString(queryPos++, classPK);

			List list = new ArrayList();

			if (getDialect().supportsLimit()) {
				q.setMaxResults(end - begin);
				q.setFirstResult(begin);

				Iterator itr = q.list().iterator();

				while (itr.hasNext()) {
					AddressHBM addressHBM = (AddressHBM)itr.next();
					list.add(AddressHBMUtil.model(addressHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						AddressHBM addressHBM = (AddressHBM)sr.get(0);
						list.add(AddressHBMUtil.model(addressHBM));

						if (!sr.next()) {
							break;
						}
					}
				}
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.Address findByC_C_C_First(
		String companyId, String className, String classPK,
		OrderByComparator obc) throws NoSuchAddressException, SystemException {
		List list = findByC_C_C(companyId, className, classPK, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchAddressException();
		}
		else {
			return (com.liferay.portal.model.Address)list.get(0);
		}
	}

	protected com.liferay.portal.model.Address findByC_C_C_Last(
		String companyId, String className, String classPK,
		OrderByComparator obc) throws NoSuchAddressException, SystemException {
		int count = countByC_C_C(companyId, className, classPK);
		List list = findByC_C_C(companyId, className, classPK, count - 1,
				count, obc);

		if (list.size() == 0) {
			throw new NoSuchAddressException();
		}
		else {
			return (com.liferay.portal.model.Address)list.get(0);
		}
	}

	protected com.liferay.portal.model.Address[] findByC_C_C_PrevAndNext(
		String addressId, String companyId, String className, String classPK,
		OrderByComparator obc) throws NoSuchAddressException, SystemException {
		com.liferay.portal.model.Address address = findByPrimaryKey(addressId);
		int count = countByC_C_C(companyId, className, classPK);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("className = ?");
			query.append(" AND ");
			query.append("classPK = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("companyId ASC").append(", ");
				query.append("className ASC").append(", ");
				query.append("classPK ASC").append(", ");
				query.append("priority ASC");
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, className);
			q.setString(queryPos++, classPK);

			com.liferay.portal.model.Address[] array = new com.liferay.portal.model.Address[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					AddressHBM addressHBM = (AddressHBM)sr.get(0);

					if (addressHBM == null) {
						break;
					}

					com.liferay.portal.model.Address curAddress = AddressHBMUtil.model(addressHBM);
					int value = obc.compare(address, curAddress);

					if (value == 0) {
						if (!address.equals(curAddress)) {
							break;
						}

						array[1] = curAddress;

						if (sr.previous()) {
							array[0] = AddressHBMUtil.model((AddressHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = AddressHBMUtil.model((AddressHBM)sr.get(
										0));
						}

						break;
					}

					if (count == 1) {
						break;
					}

					count = (int)Math.ceil(count / 2.0);

					if (value < 0) {
						if (!sr.scroll(count * -1)) {
							break;
						}
					}
					else {
						if (!sr.scroll(count)) {
							break;
						}
					}
				}
			}

			return array;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findAll() throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM ");
			query.append("ORDER BY ");
			query.append("companyId ASC").append(", ");
			query.append("className ASC").append(", ");
			query.append("classPK ASC").append(", ");
			query.append("priority ASC");

			Iterator itr = session.find(query.toString()).iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				AddressHBM addressHBM = (AddressHBM)itr.next();
				list.add(AddressHBMUtil.model(addressHBM));
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected void removeByCompanyId(String companyId)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("companyId ASC").append(", ");
			query.append("className ASC").append(", ");
			query.append("classPK ASC").append(", ");
			query.append("priority ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				AddressHBM addressHBM = (AddressHBM)itr.next();
				AddressPool.remove((String)addressHBM.getPrimaryKey());
				session.delete(addressHBM);
			}

			session.flush();
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected void removeByUserId(String userId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("companyId ASC").append(", ");
			query.append("className ASC").append(", ");
			query.append("classPK ASC").append(", ");
			query.append("priority ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				AddressHBM addressHBM = (AddressHBM)itr.next();
				AddressPool.remove((String)addressHBM.getPrimaryKey());
				session.delete(addressHBM);
			}

			session.flush();
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected void removeByC_C(String companyId, String className)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("className = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("companyId ASC").append(", ");
			query.append("className ASC").append(", ");
			query.append("classPK ASC").append(", ");
			query.append("priority ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, className);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				AddressHBM addressHBM = (AddressHBM)itr.next();
				AddressPool.remove((String)addressHBM.getPrimaryKey());
				session.delete(addressHBM);
			}

			session.flush();
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected void removeByC_C_C(String companyId, String className,
		String classPK) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("className = ?");
			query.append(" AND ");
			query.append("classPK = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("companyId ASC").append(", ");
			query.append("className ASC").append(", ");
			query.append("classPK ASC").append(", ");
			query.append("priority ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, className);
			q.setString(queryPos++, classPK);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				AddressHBM addressHBM = (AddressHBM)itr.next();
				AddressPool.remove((String)addressHBM.getPrimaryKey());
				session.delete(addressHBM);
			}

			session.flush();
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected int countByCompanyId(String companyId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			Iterator itr = q.list().iterator();

			if (itr.hasNext()) {
				Integer count = (Integer)itr.next();

				if (count != null) {
					return count.intValue();
				}
			}

			return 0;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected int countByUserId(String userId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();

			if (itr.hasNext()) {
				Integer count = (Integer)itr.next();

				if (count != null) {
					return count.intValue();
				}
			}

			return 0;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected int countByC_C(String companyId, String className)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("className = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, className);

			Iterator itr = q.list().iterator();

			if (itr.hasNext()) {
				Integer count = (Integer)itr.next();

				if (count != null) {
					return count.intValue();
				}
			}

			return 0;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected int countByC_C_C(String companyId, String className,
		String classPK) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM Address IN CLASS com.liferay.portal.ejb.AddressHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("className = ?");
			query.append(" AND ");
			query.append("classPK = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, className);
			q.setString(queryPos++, classPK);

			Iterator itr = q.list().iterator();

			if (itr.hasNext()) {
				Integer count = (Integer)itr.next();

				if (count != null) {
					return count.intValue();
				}
			}

			return 0;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}
}