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

import com.liferay.portal.NoSuchPortletPreferencesException;
import com.liferay.portal.SystemException;
import com.liferay.portal.util.HibernateUtil;
import com.liferay.util.dao.hibernate.OrderByComparator;

/**
 * <a href="PortletPreferencesPersistence.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.12 $
 *
 */
public class PortletPreferencesPersistence extends BasePersistence {
	protected com.liferay.portal.model.PortletPreferences create(
		PortletPreferencesPK portletPreferencesPK) {
		return new com.liferay.portal.model.PortletPreferences(portletPreferencesPK);
	}

	protected com.liferay.portal.model.PortletPreferences remove(
		PortletPreferencesPK portletPreferencesPK)
		throws NoSuchPortletPreferencesException, SystemException {
		Session session = null;

		try {
			session = openSession();

			PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)session.load(PortletPreferencesHBM.class,
					portletPreferencesPK);
			com.liferay.portal.model.PortletPreferences portletPreferences = PortletPreferencesHBMUtil.model(portletPreferencesHBM);
			session.delete(portletPreferencesHBM);
			session.flush();
			PortletPreferencesPool.remove(portletPreferencesPK);

			return portletPreferences;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchPortletPreferencesException(portletPreferencesPK.toString());
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.PortletPreferences update(
		com.liferay.portal.model.PortletPreferences portletPreferences)
		throws SystemException {
		Session session = null;

		try {
			if (portletPreferences.isNew() || portletPreferences.isModified()) {
				session = openSession();

				if (portletPreferences.isNew()) {
					PortletPreferencesHBM portletPreferencesHBM = new PortletPreferencesHBM(portletPreferences.getPortletId(),
							portletPreferences.getLayoutId(),
							portletPreferences.getUserId(),
							portletPreferences.getPreferences());
					session.save(portletPreferencesHBM);
					session.flush();
				}
				else {
					try {
						PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)session.load(PortletPreferencesHBM.class,
								portletPreferences.getPrimaryKey());
						portletPreferencesHBM.setPreferences(portletPreferences.getPreferences());
						session.flush();
					}
					catch (ObjectNotFoundException onfe) {
						PortletPreferencesHBM portletPreferencesHBM = new PortletPreferencesHBM(portletPreferences.getPortletId(),
								portletPreferences.getLayoutId(),
								portletPreferences.getUserId(),
								portletPreferences.getPreferences());
						session.save(portletPreferencesHBM);
						session.flush();
					}
				}

				portletPreferences.setNew(false);
				portletPreferences.setModified(false);
				portletPreferences.protect();
				PortletPreferencesPool.remove(portletPreferences.getPrimaryKey());
				PortletPreferencesPool.put(portletPreferences.getPrimaryKey(),
					portletPreferences);
			}

			return portletPreferences;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.PortletPreferences findByPrimaryKey(
		PortletPreferencesPK portletPreferencesPK)
		throws NoSuchPortletPreferencesException, SystemException {
		com.liferay.portal.model.PortletPreferences portletPreferences = PortletPreferencesPool.get(portletPreferencesPK);
		Session session = null;

		try {
			if (portletPreferences == null) {
				session = openSession();

				PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)session.load(PortletPreferencesHBM.class,
						portletPreferencesPK);
				portletPreferences = PortletPreferencesHBMUtil.model(portletPreferencesHBM);
			}

			return portletPreferences;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchPortletPreferencesException(portletPreferencesPK.toString());
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByLayoutId(String layoutId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("layoutId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, layoutId);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)itr.next();
				list.add(PortletPreferencesHBMUtil.model(portletPreferencesHBM));
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

	protected List findByLayoutId(String layoutId, int begin, int end)
		throws SystemException {
		return findByLayoutId(layoutId, begin, end, null);
	}

	protected List findByLayoutId(String layoutId, int begin, int end,
		OrderByComparator obc) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("layoutId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, layoutId);

			List list = new ArrayList();

			if (getDialect().supportsLimit()) {
				q.setMaxResults(end - begin);
				q.setFirstResult(begin);

				Iterator itr = q.list().iterator();

				while (itr.hasNext()) {
					PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)itr.next();
					list.add(PortletPreferencesHBMUtil.model(
							portletPreferencesHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)sr.get(0);
						list.add(PortletPreferencesHBMUtil.model(
								portletPreferencesHBM));

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

	protected com.liferay.portal.model.PortletPreferences findByLayoutId_First(
		String layoutId, OrderByComparator obc)
		throws NoSuchPortletPreferencesException, SystemException {
		List list = findByLayoutId(layoutId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchPortletPreferencesException();
		}
		else {
			return (com.liferay.portal.model.PortletPreferences)list.get(0);
		}
	}

	protected com.liferay.portal.model.PortletPreferences findByLayoutId_Last(
		String layoutId, OrderByComparator obc)
		throws NoSuchPortletPreferencesException, SystemException {
		int count = countByLayoutId(layoutId);
		List list = findByLayoutId(layoutId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchPortletPreferencesException();
		}
		else {
			return (com.liferay.portal.model.PortletPreferences)list.get(0);
		}
	}

	protected com.liferay.portal.model.PortletPreferences[] findByLayoutId_PrevAndNext(
		PortletPreferencesPK portletPreferencesPK, String layoutId,
		OrderByComparator obc)
		throws NoSuchPortletPreferencesException, SystemException {
		com.liferay.portal.model.PortletPreferences portletPreferences = findByPrimaryKey(portletPreferencesPK);
		int count = countByLayoutId(layoutId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("layoutId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, layoutId);

			com.liferay.portal.model.PortletPreferences[] array = new com.liferay.portal.model.PortletPreferences[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)sr.get(0);

					if (portletPreferencesHBM == null) {
						break;
					}

					com.liferay.portal.model.PortletPreferences curPortletPreferences =
						PortletPreferencesHBMUtil.model(portletPreferencesHBM);
					int value = obc.compare(portletPreferences,
							curPortletPreferences);

					if (value == 0) {
						if (!portletPreferences.equals(curPortletPreferences)) {
							break;
						}

						array[1] = curPortletPreferences;

						if (sr.previous()) {
							array[0] = PortletPreferencesHBMUtil.model((PortletPreferencesHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = PortletPreferencesHBMUtil.model((PortletPreferencesHBM)sr.get(
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
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)itr.next();
				list.add(PortletPreferencesHBMUtil.model(portletPreferencesHBM));
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
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
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
					PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)itr.next();
					list.add(PortletPreferencesHBMUtil.model(
							portletPreferencesHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)sr.get(0);
						list.add(PortletPreferencesHBMUtil.model(
								portletPreferencesHBM));

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

	protected com.liferay.portal.model.PortletPreferences findByUserId_First(
		String userId, OrderByComparator obc)
		throws NoSuchPortletPreferencesException, SystemException {
		List list = findByUserId(userId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchPortletPreferencesException();
		}
		else {
			return (com.liferay.portal.model.PortletPreferences)list.get(0);
		}
	}

	protected com.liferay.portal.model.PortletPreferences findByUserId_Last(
		String userId, OrderByComparator obc)
		throws NoSuchPortletPreferencesException, SystemException {
		int count = countByUserId(userId);
		List list = findByUserId(userId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchPortletPreferencesException();
		}
		else {
			return (com.liferay.portal.model.PortletPreferences)list.get(0);
		}
	}

	protected com.liferay.portal.model.PortletPreferences[] findByUserId_PrevAndNext(
		PortletPreferencesPK portletPreferencesPK, String userId,
		OrderByComparator obc)
		throws NoSuchPortletPreferencesException, SystemException {
		com.liferay.portal.model.PortletPreferences portletPreferences = findByPrimaryKey(portletPreferencesPK);
		int count = countByUserId(userId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			com.liferay.portal.model.PortletPreferences[] array = new com.liferay.portal.model.PortletPreferences[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)sr.get(0);

					if (portletPreferencesHBM == null) {
						break;
					}

					com.liferay.portal.model.PortletPreferences curPortletPreferences =
						PortletPreferencesHBMUtil.model(portletPreferencesHBM);
					int value = obc.compare(portletPreferences,
							curPortletPreferences);

					if (value == 0) {
						if (!portletPreferences.equals(curPortletPreferences)) {
							break;
						}

						array[1] = curPortletPreferences;

						if (sr.previous()) {
							array[0] = PortletPreferencesHBMUtil.model((PortletPreferencesHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = PortletPreferencesHBMUtil.model((PortletPreferencesHBM)sr.get(
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

	protected List findByL_U(String layoutId, String userId)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("layoutId = ?");
			query.append(" AND ");
			query.append("userId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, layoutId);
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)itr.next();
				list.add(PortletPreferencesHBMUtil.model(portletPreferencesHBM));
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

	protected List findByL_U(String layoutId, String userId, int begin, int end)
		throws SystemException {
		return findByL_U(layoutId, userId, begin, end, null);
	}

	protected List findByL_U(String layoutId, String userId, int begin,
		int end, OrderByComparator obc) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("layoutId = ?");
			query.append(" AND ");
			query.append("userId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, layoutId);
			q.setString(queryPos++, userId);

			List list = new ArrayList();

			if (getDialect().supportsLimit()) {
				q.setMaxResults(end - begin);
				q.setFirstResult(begin);

				Iterator itr = q.list().iterator();

				while (itr.hasNext()) {
					PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)itr.next();
					list.add(PortletPreferencesHBMUtil.model(
							portletPreferencesHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)sr.get(0);
						list.add(PortletPreferencesHBMUtil.model(
								portletPreferencesHBM));

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

	protected com.liferay.portal.model.PortletPreferences findByL_U_First(
		String layoutId, String userId, OrderByComparator obc)
		throws NoSuchPortletPreferencesException, SystemException {
		List list = findByL_U(layoutId, userId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchPortletPreferencesException();
		}
		else {
			return (com.liferay.portal.model.PortletPreferences)list.get(0);
		}
	}

	protected com.liferay.portal.model.PortletPreferences findByL_U_Last(
		String layoutId, String userId, OrderByComparator obc)
		throws NoSuchPortletPreferencesException, SystemException {
		int count = countByL_U(layoutId, userId);
		List list = findByL_U(layoutId, userId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchPortletPreferencesException();
		}
		else {
			return (com.liferay.portal.model.PortletPreferences)list.get(0);
		}
	}

	protected com.liferay.portal.model.PortletPreferences[] findByL_U_PrevAndNext(
		PortletPreferencesPK portletPreferencesPK, String layoutId,
		String userId, OrderByComparator obc)
		throws NoSuchPortletPreferencesException, SystemException {
		com.liferay.portal.model.PortletPreferences portletPreferences = findByPrimaryKey(portletPreferencesPK);
		int count = countByL_U(layoutId, userId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("layoutId = ?");
			query.append(" AND ");
			query.append("userId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, layoutId);
			q.setString(queryPos++, userId);

			com.liferay.portal.model.PortletPreferences[] array = new com.liferay.portal.model.PortletPreferences[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)sr.get(0);

					if (portletPreferencesHBM == null) {
						break;
					}

					com.liferay.portal.model.PortletPreferences curPortletPreferences =
						PortletPreferencesHBMUtil.model(portletPreferencesHBM);
					int value = obc.compare(portletPreferences,
							curPortletPreferences);

					if (value == 0) {
						if (!portletPreferences.equals(curPortletPreferences)) {
							break;
						}

						array[1] = curPortletPreferences;

						if (sr.previous()) {
							array[0] = PortletPreferencesHBMUtil.model((PortletPreferencesHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = PortletPreferencesHBMUtil.model((PortletPreferencesHBM)sr.get(
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
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM ");

			Iterator itr = session.find(query.toString()).iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)itr.next();
				list.add(PortletPreferencesHBMUtil.model(portletPreferencesHBM));
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

	protected void removeByLayoutId(String layoutId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("layoutId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, layoutId);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)itr.next();
				PortletPreferencesPool.remove((PortletPreferencesPK)portletPreferencesHBM.getPrimaryKey());
				session.delete(portletPreferencesHBM);
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
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)itr.next();
				PortletPreferencesPool.remove((PortletPreferencesPK)portletPreferencesHBM.getPrimaryKey());
				session.delete(portletPreferencesHBM);
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

	protected void removeByL_U(String layoutId, String userId)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("layoutId = ?");
			query.append(" AND ");
			query.append("userId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, layoutId);
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				PortletPreferencesHBM portletPreferencesHBM = (PortletPreferencesHBM)itr.next();
				PortletPreferencesPool.remove((PortletPreferencesPK)portletPreferencesHBM.getPrimaryKey());
				session.delete(portletPreferencesHBM);
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

	protected int countByLayoutId(String layoutId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("layoutId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, layoutId);

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
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
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

	protected int countByL_U(String layoutId, String userId)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM PortletPreferences IN CLASS com.liferay.portal.ejb.PortletPreferencesHBM WHERE ");
			query.append("layoutId = ?");
			query.append(" AND ");
			query.append("userId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, layoutId);
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
}