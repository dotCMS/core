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

import com.liferay.portal.NoSuchUserTrackerException;
import com.liferay.portal.SystemException;
import com.liferay.portal.util.HibernateUtil;
import com.liferay.util.dao.hibernate.OrderByComparator;

/**
 * <a href="UserTrackerPersistence.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.13 $
 *
 */
public class UserTrackerPersistence extends BasePersistence {
	protected com.liferay.portal.model.UserTracker create(String userTrackerId) {
		return new com.liferay.portal.model.UserTracker(userTrackerId);
	}

	protected com.liferay.portal.model.UserTracker remove(String userTrackerId)
		throws NoSuchUserTrackerException, SystemException {
		Session session = null;

		try {
			session = openSession();

			UserTrackerHBM userTrackerHBM = (UserTrackerHBM)session.load(UserTrackerHBM.class,
					userTrackerId);
			com.liferay.portal.model.UserTracker userTracker = UserTrackerHBMUtil.model(userTrackerHBM);
			session.delete(userTrackerHBM);
			session.flush();
			UserTrackerPool.remove(userTrackerId);

			return userTracker;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchUserTrackerException(userTrackerId.toString());
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.UserTracker update(
		com.liferay.portal.model.UserTracker userTracker)
		throws SystemException {
		Session session = null;

		try {
			if (userTracker.isNew() || userTracker.isModified()) {
				session = openSession();

				if (userTracker.isNew()) {
					UserTrackerHBM userTrackerHBM = new UserTrackerHBM(userTracker.getUserTrackerId(),
							userTracker.getCompanyId(),
							userTracker.getUserId(),
							userTracker.getModifiedDate(),
							userTracker.getRemoteAddr(),
							userTracker.getRemoteHost(),
							userTracker.getUserAgent());
					session.save(userTrackerHBM);
					session.flush();
				}
				else {
					try {
						UserTrackerHBM userTrackerHBM = (UserTrackerHBM)session.load(UserTrackerHBM.class,
								userTracker.getPrimaryKey());
						userTrackerHBM.setCompanyId(userTracker.getCompanyId());
						userTrackerHBM.setUserId(userTracker.getUserId());
						userTrackerHBM.setModifiedDate(userTracker.getModifiedDate());
						userTrackerHBM.setRemoteAddr(userTracker.getRemoteAddr());
						userTrackerHBM.setRemoteHost(userTracker.getRemoteHost());
						userTrackerHBM.setUserAgent(userTracker.getUserAgent());
						session.flush();
					}
					catch (ObjectNotFoundException onfe) {
						UserTrackerHBM userTrackerHBM = new UserTrackerHBM(userTracker.getUserTrackerId(),
								userTracker.getCompanyId(),
								userTracker.getUserId(),
								userTracker.getModifiedDate(),
								userTracker.getRemoteAddr(),
								userTracker.getRemoteHost(),
								userTracker.getUserAgent());
						session.save(userTrackerHBM);
						session.flush();
					}
				}

				userTracker.setNew(false);
				userTracker.setModified(false);
				userTracker.protect();
				UserTrackerPool.remove(userTracker.getPrimaryKey());
				UserTrackerPool.put(userTracker.getPrimaryKey(), userTracker);
			}

			return userTracker;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.UserTracker findByPrimaryKey(
		String userTrackerId)
		throws NoSuchUserTrackerException, SystemException {
		com.liferay.portal.model.UserTracker userTracker = UserTrackerPool.get(userTrackerId);
		Session session = null;

		try {
			if (userTracker == null) {
				session = openSession();

				UserTrackerHBM userTrackerHBM = (UserTrackerHBM)session.load(UserTrackerHBM.class,
						userTrackerId);
				userTracker = UserTrackerHBMUtil.model(userTrackerHBM);
			}

			return userTracker;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchUserTrackerException(userTrackerId.toString());
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
				"FROM UserTracker IN CLASS com.liferay.portal.ejb.UserTrackerHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				UserTrackerHBM userTrackerHBM = (UserTrackerHBM)itr.next();
				list.add(UserTrackerHBMUtil.model(userTrackerHBM));
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
				"FROM UserTracker IN CLASS com.liferay.portal.ejb.UserTrackerHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
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
					UserTrackerHBM userTrackerHBM = (UserTrackerHBM)itr.next();
					list.add(UserTrackerHBMUtil.model(userTrackerHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						UserTrackerHBM userTrackerHBM = (UserTrackerHBM)sr.get(0);
						list.add(UserTrackerHBMUtil.model(userTrackerHBM));

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

	protected com.liferay.portal.model.UserTracker findByCompanyId_First(
		String companyId, OrderByComparator obc)
		throws NoSuchUserTrackerException, SystemException {
		List list = findByCompanyId(companyId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchUserTrackerException();
		}
		else {
			return (com.liferay.portal.model.UserTracker)list.get(0);
		}
	}

	protected com.liferay.portal.model.UserTracker findByCompanyId_Last(
		String companyId, OrderByComparator obc)
		throws NoSuchUserTrackerException, SystemException {
		int count = countByCompanyId(companyId);
		List list = findByCompanyId(companyId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchUserTrackerException();
		}
		else {
			return (com.liferay.portal.model.UserTracker)list.get(0);
		}
	}

	protected com.liferay.portal.model.UserTracker[] findByCompanyId_PrevAndNext(
		String userTrackerId, String companyId, OrderByComparator obc)
		throws NoSuchUserTrackerException, SystemException {
		com.liferay.portal.model.UserTracker userTracker = findByPrimaryKey(userTrackerId);
		int count = countByCompanyId(companyId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM UserTracker IN CLASS com.liferay.portal.ejb.UserTrackerHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			com.liferay.portal.model.UserTracker[] array = new com.liferay.portal.model.UserTracker[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					UserTrackerHBM userTrackerHBM = (UserTrackerHBM)sr.get(0);

					if (userTrackerHBM == null) {
						break;
					}

					com.liferay.portal.model.UserTracker curUserTracker = UserTrackerHBMUtil.model(userTrackerHBM);
					int value = obc.compare(userTracker, curUserTracker);

					if (value == 0) {
						if (!userTracker.equals(curUserTracker)) {
							break;
						}

						array[1] = curUserTracker;

						if (sr.previous()) {
							array[0] = UserTrackerHBMUtil.model((UserTrackerHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = UserTrackerHBMUtil.model((UserTrackerHBM)sr.get(
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
				"FROM UserTracker IN CLASS com.liferay.portal.ejb.UserTrackerHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				UserTrackerHBM userTrackerHBM = (UserTrackerHBM)itr.next();
				list.add(UserTrackerHBMUtil.model(userTrackerHBM));
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
				"FROM UserTracker IN CLASS com.liferay.portal.ejb.UserTrackerHBM WHERE ");
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
					UserTrackerHBM userTrackerHBM = (UserTrackerHBM)itr.next();
					list.add(UserTrackerHBMUtil.model(userTrackerHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						UserTrackerHBM userTrackerHBM = (UserTrackerHBM)sr.get(0);
						list.add(UserTrackerHBMUtil.model(userTrackerHBM));

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

	protected com.liferay.portal.model.UserTracker findByUserId_First(
		String userId, OrderByComparator obc)
		throws NoSuchUserTrackerException, SystemException {
		List list = findByUserId(userId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchUserTrackerException();
		}
		else {
			return (com.liferay.portal.model.UserTracker)list.get(0);
		}
	}

	protected com.liferay.portal.model.UserTracker findByUserId_Last(
		String userId, OrderByComparator obc)
		throws NoSuchUserTrackerException, SystemException {
		int count = countByUserId(userId);
		List list = findByUserId(userId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchUserTrackerException();
		}
		else {
			return (com.liferay.portal.model.UserTracker)list.get(0);
		}
	}

	protected com.liferay.portal.model.UserTracker[] findByUserId_PrevAndNext(
		String userTrackerId, String userId, OrderByComparator obc)
		throws NoSuchUserTrackerException, SystemException {
		com.liferay.portal.model.UserTracker userTracker = findByPrimaryKey(userTrackerId);
		int count = countByUserId(userId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM UserTracker IN CLASS com.liferay.portal.ejb.UserTrackerHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			com.liferay.portal.model.UserTracker[] array = new com.liferay.portal.model.UserTracker[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					UserTrackerHBM userTrackerHBM = (UserTrackerHBM)sr.get(0);

					if (userTrackerHBM == null) {
						break;
					}

					com.liferay.portal.model.UserTracker curUserTracker = UserTrackerHBMUtil.model(userTrackerHBM);
					int value = obc.compare(userTracker, curUserTracker);

					if (value == 0) {
						if (!userTracker.equals(curUserTracker)) {
							break;
						}

						array[1] = curUserTracker;

						if (sr.previous()) {
							array[0] = UserTrackerHBMUtil.model((UserTrackerHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = UserTrackerHBMUtil.model((UserTrackerHBM)sr.get(
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
				"FROM UserTracker IN CLASS com.liferay.portal.ejb.UserTrackerHBM ");

			Iterator itr = session.find(query.toString()).iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				UserTrackerHBM userTrackerHBM = (UserTrackerHBM)itr.next();
				list.add(UserTrackerHBMUtil.model(userTrackerHBM));
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
				"FROM UserTracker IN CLASS com.liferay.portal.ejb.UserTrackerHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				UserTrackerHBM userTrackerHBM = (UserTrackerHBM)itr.next();
				UserTrackerPool.remove((String)userTrackerHBM.getPrimaryKey());
				session.delete(userTrackerHBM);
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
				"FROM UserTracker IN CLASS com.liferay.portal.ejb.UserTrackerHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				UserTrackerHBM userTrackerHBM = (UserTrackerHBM)itr.next();
				UserTrackerPool.remove((String)userTrackerHBM.getPrimaryKey());
				session.delete(userTrackerHBM);
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
				"FROM UserTracker IN CLASS com.liferay.portal.ejb.UserTrackerHBM WHERE ");
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
				"FROM UserTracker IN CLASS com.liferay.portal.ejb.UserTrackerHBM WHERE ");
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