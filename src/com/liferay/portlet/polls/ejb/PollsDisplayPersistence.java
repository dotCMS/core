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

package com.liferay.portlet.polls.ejb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.ObjectNotFoundException;
import net.sf.hibernate.Query;
import net.sf.hibernate.ScrollableResults;
import net.sf.hibernate.Session;

import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.BasePersistence;
import com.liferay.portal.util.HibernateUtil;
import com.liferay.portlet.polls.NoSuchDisplayException;
import com.liferay.util.dao.hibernate.OrderByComparator;

/**
 * <a href="PollsDisplayPersistence.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.12 $
 *
 */
public class PollsDisplayPersistence extends BasePersistence {
	protected com.liferay.portlet.polls.model.PollsDisplay create(
		PollsDisplayPK pollsDisplayPK) {
		return new com.liferay.portlet.polls.model.PollsDisplay(pollsDisplayPK);
	}

	protected com.liferay.portlet.polls.model.PollsDisplay remove(
		PollsDisplayPK pollsDisplayPK)
		throws NoSuchDisplayException, SystemException {
		Session session = null;

		try {
			session = openSession();

			PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)session.load(PollsDisplayHBM.class,
					pollsDisplayPK);
			com.liferay.portlet.polls.model.PollsDisplay pollsDisplay = PollsDisplayHBMUtil.model(pollsDisplayHBM);
			session.delete(pollsDisplayHBM);
			session.flush();
			PollsDisplayPool.remove(pollsDisplayPK);

			return pollsDisplay;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchDisplayException(pollsDisplayPK.toString());
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portlet.polls.model.PollsDisplay update(
		com.liferay.portlet.polls.model.PollsDisplay pollsDisplay)
		throws SystemException {
		Session session = null;

		try {
			if (pollsDisplay.isNew() || pollsDisplay.isModified()) {
				session = openSession();

				if (pollsDisplay.isNew()) {
					PollsDisplayHBM pollsDisplayHBM = new PollsDisplayHBM(
							pollsDisplay.getUserId(),
							pollsDisplay.getPortletId(),
							pollsDisplay.getQuestionId());
					session.save(pollsDisplayHBM);
					session.flush();
				}
				else {
					try {
						PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)session.load(PollsDisplayHBM.class,
								pollsDisplay.getPrimaryKey());
						pollsDisplayHBM.setQuestionId(pollsDisplay.getQuestionId());
						session.flush();
					}
					catch (ObjectNotFoundException onfe) {
						PollsDisplayHBM pollsDisplayHBM = new PollsDisplayHBM(
								pollsDisplay.getUserId(),
								pollsDisplay.getPortletId(),
								pollsDisplay.getQuestionId());
						session.save(pollsDisplayHBM);
						session.flush();
					}
				}

				pollsDisplay.setNew(false);
				pollsDisplay.setModified(false);
				pollsDisplay.protect();
				PollsDisplayPool.remove(pollsDisplay.getPrimaryKey());
				PollsDisplayPool.put(pollsDisplay.getPrimaryKey(), pollsDisplay);
			}

			return pollsDisplay;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portlet.polls.model.PollsDisplay findByPrimaryKey(
		PollsDisplayPK pollsDisplayPK)
		throws NoSuchDisplayException, SystemException {
		com.liferay.portlet.polls.model.PollsDisplay pollsDisplay = PollsDisplayPool.get(pollsDisplayPK);
		Session session = null;

		try {
			if (pollsDisplay == null) {
				session = openSession();

				PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)session.load(PollsDisplayHBM.class,
						pollsDisplayPK);
				pollsDisplay = PollsDisplayHBMUtil.model(pollsDisplayHBM);
			}

			return pollsDisplay;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchDisplayException(pollsDisplayPK.toString());
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
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)itr.next();
				list.add(PollsDisplayHBMUtil.model(pollsDisplayHBM));
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
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
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
					PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)itr.next();
					list.add(PollsDisplayHBMUtil.model(pollsDisplayHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)sr.get(0);
						list.add(PollsDisplayHBMUtil.model(pollsDisplayHBM));

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

	protected com.liferay.portlet.polls.model.PollsDisplay findByUserId_First(
		String userId, OrderByComparator obc)
		throws NoSuchDisplayException, SystemException {
		List list = findByUserId(userId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchDisplayException();
		}
		else {
			return (com.liferay.portlet.polls.model.PollsDisplay)list.get(0);
		}
	}

	protected com.liferay.portlet.polls.model.PollsDisplay findByUserId_Last(
		String userId, OrderByComparator obc)
		throws NoSuchDisplayException, SystemException {
		int count = countByUserId(userId);
		List list = findByUserId(userId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchDisplayException();
		}
		else {
			return (com.liferay.portlet.polls.model.PollsDisplay)list.get(0);
		}
	}

	protected com.liferay.portlet.polls.model.PollsDisplay[] findByUserId_PrevAndNext(
		PollsDisplayPK pollsDisplayPK, String userId, OrderByComparator obc)
		throws NoSuchDisplayException, SystemException {
		com.liferay.portlet.polls.model.PollsDisplay pollsDisplay = findByPrimaryKey(pollsDisplayPK);
		int count = countByUserId(userId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			com.liferay.portlet.polls.model.PollsDisplay[] array = new com.liferay.portlet.polls.model.PollsDisplay[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)sr.get(0);

					if (pollsDisplayHBM == null) {
						break;
					}

					com.liferay.portlet.polls.model.PollsDisplay curPollsDisplay =
						PollsDisplayHBMUtil.model(pollsDisplayHBM);
					int value = obc.compare(pollsDisplay, curPollsDisplay);

					if (value == 0) {
						if (!pollsDisplay.equals(curPollsDisplay)) {
							break;
						}

						array[1] = curPollsDisplay;

						if (sr.previous()) {
							array[0] = PollsDisplayHBMUtil.model((PollsDisplayHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = PollsDisplayHBMUtil.model((PollsDisplayHBM)sr.get(
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

	protected List findByQuestionId(String questionId)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
			query.append("questionId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, questionId);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)itr.next();
				list.add(PollsDisplayHBMUtil.model(pollsDisplayHBM));
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

	protected List findByQuestionId(String questionId, int begin, int end)
		throws SystemException {
		return findByQuestionId(questionId, begin, end, null);
	}

	protected List findByQuestionId(String questionId, int begin, int end,
		OrderByComparator obc) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
			query.append("questionId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, questionId);

			List list = new ArrayList();

			if (getDialect().supportsLimit()) {
				q.setMaxResults(end - begin);
				q.setFirstResult(begin);

				Iterator itr = q.list().iterator();

				while (itr.hasNext()) {
					PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)itr.next();
					list.add(PollsDisplayHBMUtil.model(pollsDisplayHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)sr.get(0);
						list.add(PollsDisplayHBMUtil.model(pollsDisplayHBM));

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

	protected com.liferay.portlet.polls.model.PollsDisplay findByQuestionId_First(
		String questionId, OrderByComparator obc)
		throws NoSuchDisplayException, SystemException {
		List list = findByQuestionId(questionId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchDisplayException();
		}
		else {
			return (com.liferay.portlet.polls.model.PollsDisplay)list.get(0);
		}
	}

	protected com.liferay.portlet.polls.model.PollsDisplay findByQuestionId_Last(
		String questionId, OrderByComparator obc)
		throws NoSuchDisplayException, SystemException {
		int count = countByQuestionId(questionId);
		List list = findByQuestionId(questionId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchDisplayException();
		}
		else {
			return (com.liferay.portlet.polls.model.PollsDisplay)list.get(0);
		}
	}

	protected com.liferay.portlet.polls.model.PollsDisplay[] findByQuestionId_PrevAndNext(
		PollsDisplayPK pollsDisplayPK, String questionId, OrderByComparator obc)
		throws NoSuchDisplayException, SystemException {
		com.liferay.portlet.polls.model.PollsDisplay pollsDisplay = findByPrimaryKey(pollsDisplayPK);
		int count = countByQuestionId(questionId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
			query.append("questionId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, questionId);

			com.liferay.portlet.polls.model.PollsDisplay[] array = new com.liferay.portlet.polls.model.PollsDisplay[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)sr.get(0);

					if (pollsDisplayHBM == null) {
						break;
					}

					com.liferay.portlet.polls.model.PollsDisplay curPollsDisplay =
						PollsDisplayHBMUtil.model(pollsDisplayHBM);
					int value = obc.compare(pollsDisplay, curPollsDisplay);

					if (value == 0) {
						if (!pollsDisplay.equals(curPollsDisplay)) {
							break;
						}

						array[1] = curPollsDisplay;

						if (sr.previous()) {
							array[0] = PollsDisplayHBMUtil.model((PollsDisplayHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = PollsDisplayHBMUtil.model((PollsDisplayHBM)sr.get(
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
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
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
				PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)itr.next();
				list.add(PollsDisplayHBMUtil.model(pollsDisplayHBM));
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
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
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
					PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)itr.next();
					list.add(PollsDisplayHBMUtil.model(pollsDisplayHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)sr.get(0);
						list.add(PollsDisplayHBMUtil.model(pollsDisplayHBM));

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

	protected com.liferay.portlet.polls.model.PollsDisplay findByL_U_First(
		String layoutId, String userId, OrderByComparator obc)
		throws NoSuchDisplayException, SystemException {
		List list = findByL_U(layoutId, userId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchDisplayException();
		}
		else {
			return (com.liferay.portlet.polls.model.PollsDisplay)list.get(0);
		}
	}

	protected com.liferay.portlet.polls.model.PollsDisplay findByL_U_Last(
		String layoutId, String userId, OrderByComparator obc)
		throws NoSuchDisplayException, SystemException {
		int count = countByL_U(layoutId, userId);
		List list = findByL_U(layoutId, userId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchDisplayException();
		}
		else {
			return (com.liferay.portlet.polls.model.PollsDisplay)list.get(0);
		}
	}

	protected com.liferay.portlet.polls.model.PollsDisplay[] findByL_U_PrevAndNext(
		PollsDisplayPK pollsDisplayPK, String layoutId, String userId,
		OrderByComparator obc) throws NoSuchDisplayException, SystemException {
		com.liferay.portlet.polls.model.PollsDisplay pollsDisplay = findByPrimaryKey(pollsDisplayPK);
		int count = countByL_U(layoutId, userId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
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

			com.liferay.portlet.polls.model.PollsDisplay[] array = new com.liferay.portlet.polls.model.PollsDisplay[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)sr.get(0);

					if (pollsDisplayHBM == null) {
						break;
					}

					com.liferay.portlet.polls.model.PollsDisplay curPollsDisplay =
						PollsDisplayHBMUtil.model(pollsDisplayHBM);
					int value = obc.compare(pollsDisplay, curPollsDisplay);

					if (value == 0) {
						if (!pollsDisplay.equals(curPollsDisplay)) {
							break;
						}

						array[1] = curPollsDisplay;

						if (sr.previous()) {
							array[0] = PollsDisplayHBMUtil.model((PollsDisplayHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = PollsDisplayHBMUtil.model((PollsDisplayHBM)sr.get(
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
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM ");

			Iterator itr = session.find(query.toString()).iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)itr.next();
				list.add(PollsDisplayHBMUtil.model(pollsDisplayHBM));
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
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
			query.append("userId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)itr.next();
				PollsDisplayPool.remove((PollsDisplayPK)pollsDisplayHBM.getPrimaryKey());
				session.delete(pollsDisplayHBM);
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

	protected void removeByQuestionId(String questionId)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
			query.append("questionId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, questionId);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)itr.next();
				PollsDisplayPool.remove((PollsDisplayPK)pollsDisplayHBM.getPrimaryKey());
				session.delete(pollsDisplayHBM);
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
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
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
				PollsDisplayHBM pollsDisplayHBM = (PollsDisplayHBM)itr.next();
				PollsDisplayPool.remove((PollsDisplayPK)pollsDisplayHBM.getPrimaryKey());
				session.delete(pollsDisplayHBM);
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
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
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

	protected int countByQuestionId(String questionId)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
			query.append("questionId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, questionId);

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
				"FROM PollsDisplay IN CLASS com.liferay.portlet.polls.ejb.PollsDisplayHBM WHERE ");
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