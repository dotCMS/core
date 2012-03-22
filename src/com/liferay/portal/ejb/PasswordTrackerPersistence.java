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

import com.liferay.portal.NoSuchPasswordTrackerException;
import com.liferay.portal.SystemException;
import com.liferay.portal.util.HibernateUtil;
import com.liferay.util.dao.hibernate.OrderByComparator;

/**
 * <a href="PasswordTrackerPersistence.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.14 $
 *
 */
public class PasswordTrackerPersistence extends BasePersistence {
	protected com.liferay.portal.model.PasswordTracker create(
		String passwordTrackerId) {
		return new com.liferay.portal.model.PasswordTracker(passwordTrackerId);
	}

	protected com.liferay.portal.model.PasswordTracker remove(
		String passwordTrackerId)
		throws NoSuchPasswordTrackerException, SystemException {
		Session session = null;

		try {
			session = openSession();

			PasswordTrackerHBM passwordTrackerHBM = (PasswordTrackerHBM)session.load(PasswordTrackerHBM.class,
					passwordTrackerId);
			com.liferay.portal.model.PasswordTracker passwordTracker = PasswordTrackerHBMUtil.model(passwordTrackerHBM);
			session.delete(passwordTrackerHBM);
			session.flush();
			PasswordTrackerPool.remove(passwordTrackerId);

			return passwordTracker;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchPasswordTrackerException(passwordTrackerId.toString());
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.PasswordTracker update(
		com.liferay.portal.model.PasswordTracker passwordTracker)
		throws SystemException {
		Session session = null;

		try {
			if (passwordTracker.isNew() || passwordTracker.isModified()) {
				session = openSession();

				if (passwordTracker.isNew()) {
					PasswordTrackerHBM passwordTrackerHBM = new PasswordTrackerHBM(passwordTracker.getPasswordTrackerId(),
							passwordTracker.getUserId(),
							passwordTracker.getCreateDate(),
							passwordTracker.getPassword());
					session.save(passwordTrackerHBM);
					session.flush();
				}
				else {
					try {
						PasswordTrackerHBM passwordTrackerHBM = (PasswordTrackerHBM)session.load(PasswordTrackerHBM.class,
								passwordTracker.getPrimaryKey());
						passwordTrackerHBM.setUserId(passwordTracker.getUserId());
						passwordTrackerHBM.setCreateDate(passwordTracker.getCreateDate());
						passwordTrackerHBM.setPassword(passwordTracker.getPassword());
						session.flush();
					}
					catch (ObjectNotFoundException onfe) {
						PasswordTrackerHBM passwordTrackerHBM = new PasswordTrackerHBM(passwordTracker.getPasswordTrackerId(),
								passwordTracker.getUserId(),
								passwordTracker.getCreateDate(),
								passwordTracker.getPassword());
						session.save(passwordTrackerHBM);
						session.flush();
					}
				}

				passwordTracker.setNew(false);
				passwordTracker.setModified(false);
				passwordTracker.protect();
				PasswordTrackerPool.remove(passwordTracker.getPrimaryKey());
				PasswordTrackerPool.put(passwordTracker.getPrimaryKey(),
					passwordTracker);
			}

			return passwordTracker;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.PasswordTracker findByPrimaryKey(
		String passwordTrackerId)
		throws NoSuchPasswordTrackerException, SystemException {
		com.liferay.portal.model.PasswordTracker passwordTracker = PasswordTrackerPool.get(passwordTrackerId);
		Session session = null;

		try {
			if (passwordTracker == null) {
				session = openSession();

				PasswordTrackerHBM passwordTrackerHBM = (PasswordTrackerHBM)session.load(PasswordTrackerHBM.class,
						passwordTrackerId);
				passwordTracker = PasswordTrackerHBMUtil.model(passwordTrackerHBM);
			}

			return passwordTracker;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchPasswordTrackerException(passwordTrackerId.toString());
			}
			else {
				throw new SystemException(he);
			}
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
				"FROM PasswordTracker IN CLASS com.liferay.portal.ejb.PasswordTrackerHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("userId DESC").append(", ");
			query.append("createDate DESC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				PasswordTrackerHBM passwordTrackerHBM = (PasswordTrackerHBM)itr.next();
				list.add(PasswordTrackerHBMUtil.model(passwordTrackerHBM));
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
				"FROM PasswordTracker IN CLASS com.liferay.portal.ejb.PasswordTrackerHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("userId DESC").append(", ");
				query.append("createDate DESC");
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
					PasswordTrackerHBM passwordTrackerHBM = (PasswordTrackerHBM)itr.next();
					list.add(PasswordTrackerHBMUtil.model(passwordTrackerHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						PasswordTrackerHBM passwordTrackerHBM = (PasswordTrackerHBM)sr.get(0);
						list.add(PasswordTrackerHBMUtil.model(
								passwordTrackerHBM));

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

	protected com.liferay.portal.model.PasswordTracker findByUserId_First(
		String userId, OrderByComparator obc)
		throws NoSuchPasswordTrackerException, SystemException {
		List list = findByUserId(userId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchPasswordTrackerException();
		}
		else {
			return (com.liferay.portal.model.PasswordTracker)list.get(0);
		}
	}

	protected com.liferay.portal.model.PasswordTracker findByUserId_Last(
		String userId, OrderByComparator obc)
		throws NoSuchPasswordTrackerException, SystemException {
		int count = countByUserId(userId);
		List list = findByUserId(userId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchPasswordTrackerException();
		}
		else {
			return (com.liferay.portal.model.PasswordTracker)list.get(0);
		}
	}

	protected com.liferay.portal.model.PasswordTracker[] findByUserId_PrevAndNext(
		String passwordTrackerId, String userId, OrderByComparator obc)
		throws NoSuchPasswordTrackerException, SystemException {
		com.liferay.portal.model.PasswordTracker passwordTracker = findByPrimaryKey(passwordTrackerId);
		int count = countByUserId(userId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PasswordTracker IN CLASS com.liferay.portal.ejb.PasswordTrackerHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("userId DESC").append(", ");
				query.append("createDate DESC");
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			com.liferay.portal.model.PasswordTracker[] array = new com.liferay.portal.model.PasswordTracker[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					PasswordTrackerHBM passwordTrackerHBM = (PasswordTrackerHBM)sr.get(0);

					if (passwordTrackerHBM == null) {
						break;
					}

					com.liferay.portal.model.PasswordTracker curPasswordTracker = PasswordTrackerHBMUtil.model(passwordTrackerHBM);
					int value = obc.compare(passwordTracker, curPasswordTracker);

					if (value == 0) {
						if (!passwordTracker.equals(curPasswordTracker)) {
							break;
						}

						array[1] = curPasswordTracker;

						if (sr.previous()) {
							array[0] = PasswordTrackerHBMUtil.model((PasswordTrackerHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = PasswordTrackerHBMUtil.model((PasswordTrackerHBM)sr.get(
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
				"FROM PasswordTracker IN CLASS com.liferay.portal.ejb.PasswordTrackerHBM ");
			query.append("ORDER BY ");
			query.append("userId DESC").append(", ");
			query.append("createDate DESC");

			Iterator itr = session.find(query.toString()).iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				PasswordTrackerHBM passwordTrackerHBM = (PasswordTrackerHBM)itr.next();
				list.add(PasswordTrackerHBMUtil.model(passwordTrackerHBM));
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

	protected void removeByUserId(String userId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PasswordTracker IN CLASS com.liferay.portal.ejb.PasswordTrackerHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("userId DESC").append(", ");
			query.append("createDate DESC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				PasswordTrackerHBM passwordTrackerHBM = (PasswordTrackerHBM)itr.next();
				PasswordTrackerPool.remove((String)passwordTrackerHBM.getPrimaryKey());
				session.delete(passwordTrackerHBM);
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

	protected int countByUserId(String userId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM PasswordTracker IN CLASS com.liferay.portal.ejb.PasswordTrackerHBM WHERE ");
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
}