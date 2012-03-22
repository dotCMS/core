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
import com.liferay.portlet.polls.NoSuchChoiceException;
import com.liferay.util.dao.hibernate.OrderByComparator;

/**
 * <a href="PollsChoicePersistence.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.13 $
 *
 */
public class PollsChoicePersistence extends BasePersistence {
	protected com.liferay.portlet.polls.model.PollsChoice create(
		PollsChoicePK pollsChoicePK) {
		return new com.liferay.portlet.polls.model.PollsChoice(pollsChoicePK);
	}

	protected com.liferay.portlet.polls.model.PollsChoice remove(
		PollsChoicePK pollsChoicePK)
		throws NoSuchChoiceException, SystemException {
		Session session = null;

		try {
			session = openSession();

			PollsChoiceHBM pollsChoiceHBM = (PollsChoiceHBM)session.load(PollsChoiceHBM.class,
					pollsChoicePK);
			com.liferay.portlet.polls.model.PollsChoice pollsChoice = PollsChoiceHBMUtil.model(pollsChoiceHBM);
			session.delete(pollsChoiceHBM);
			session.flush();
			PollsChoicePool.remove(pollsChoicePK);

			return pollsChoice;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchChoiceException(pollsChoicePK.toString());
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portlet.polls.model.PollsChoice update(
		com.liferay.portlet.polls.model.PollsChoice pollsChoice)
		throws SystemException {
		Session session = null;

		try {
			if (pollsChoice.isNew() || pollsChoice.isModified()) {
				session = openSession();

				if (pollsChoice.isNew()) {
					PollsChoiceHBM pollsChoiceHBM = new PollsChoiceHBM(pollsChoice.getQuestionId(),
							pollsChoice.getChoiceId(),
							pollsChoice.getDescription());
					session.save(pollsChoiceHBM);
					session.flush();
				}
				else {
					try {
						PollsChoiceHBM pollsChoiceHBM = (PollsChoiceHBM)session.load(PollsChoiceHBM.class,
								pollsChoice.getPrimaryKey());
						pollsChoiceHBM.setDescription(pollsChoice.getDescription());
						session.flush();
					}
					catch (ObjectNotFoundException onfe) {
						PollsChoiceHBM pollsChoiceHBM = new PollsChoiceHBM(pollsChoice.getQuestionId(),
								pollsChoice.getChoiceId(),
								pollsChoice.getDescription());
						session.save(pollsChoiceHBM);
						session.flush();
					}
				}

				pollsChoice.setNew(false);
				pollsChoice.setModified(false);
				pollsChoice.protect();
				PollsChoicePool.remove(pollsChoice.getPrimaryKey());
				PollsChoicePool.put(pollsChoice.getPrimaryKey(), pollsChoice);
			}

			return pollsChoice;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portlet.polls.model.PollsChoice findByPrimaryKey(
		PollsChoicePK pollsChoicePK)
		throws NoSuchChoiceException, SystemException {
		com.liferay.portlet.polls.model.PollsChoice pollsChoice = PollsChoicePool.get(pollsChoicePK);
		Session session = null;

		try {
			if (pollsChoice == null) {
				session = openSession();

				PollsChoiceHBM pollsChoiceHBM = (PollsChoiceHBM)session.load(PollsChoiceHBM.class,
						pollsChoicePK);
				pollsChoice = PollsChoiceHBMUtil.model(pollsChoiceHBM);
			}

			return pollsChoice;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchChoiceException(pollsChoicePK.toString());
			}
			else {
				throw new SystemException(he);
			}
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
				"FROM PollsChoice IN CLASS com.liferay.portlet.polls.ejb.PollsChoiceHBM WHERE ");
			query.append("questionId = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("choiceId ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, questionId);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				PollsChoiceHBM pollsChoiceHBM = (PollsChoiceHBM)itr.next();
				list.add(PollsChoiceHBMUtil.model(pollsChoiceHBM));
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
				"FROM PollsChoice IN CLASS com.liferay.portlet.polls.ejb.PollsChoiceHBM WHERE ");
			query.append("questionId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("choiceId ASC");
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
					PollsChoiceHBM pollsChoiceHBM = (PollsChoiceHBM)itr.next();
					list.add(PollsChoiceHBMUtil.model(pollsChoiceHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						PollsChoiceHBM pollsChoiceHBM = (PollsChoiceHBM)sr.get(0);
						list.add(PollsChoiceHBMUtil.model(pollsChoiceHBM));

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

	protected com.liferay.portlet.polls.model.PollsChoice findByQuestionId_First(
		String questionId, OrderByComparator obc)
		throws NoSuchChoiceException, SystemException {
		List list = findByQuestionId(questionId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchChoiceException();
		}
		else {
			return (com.liferay.portlet.polls.model.PollsChoice)list.get(0);
		}
	}

	protected com.liferay.portlet.polls.model.PollsChoice findByQuestionId_Last(
		String questionId, OrderByComparator obc)
		throws NoSuchChoiceException, SystemException {
		int count = countByQuestionId(questionId);
		List list = findByQuestionId(questionId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchChoiceException();
		}
		else {
			return (com.liferay.portlet.polls.model.PollsChoice)list.get(0);
		}
	}

	protected com.liferay.portlet.polls.model.PollsChoice[] findByQuestionId_PrevAndNext(
		PollsChoicePK pollsChoicePK, String questionId, OrderByComparator obc)
		throws NoSuchChoiceException, SystemException {
		com.liferay.portlet.polls.model.PollsChoice pollsChoice = findByPrimaryKey(pollsChoicePK);
		int count = countByQuestionId(questionId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PollsChoice IN CLASS com.liferay.portlet.polls.ejb.PollsChoiceHBM WHERE ");
			query.append("questionId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("choiceId ASC");
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, questionId);

			com.liferay.portlet.polls.model.PollsChoice[] array = new com.liferay.portlet.polls.model.PollsChoice[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					PollsChoiceHBM pollsChoiceHBM = (PollsChoiceHBM)sr.get(0);

					if (pollsChoiceHBM == null) {
						break;
					}

					com.liferay.portlet.polls.model.PollsChoice curPollsChoice = PollsChoiceHBMUtil.model(pollsChoiceHBM);
					int value = obc.compare(pollsChoice, curPollsChoice);

					if (value == 0) {
						if (!pollsChoice.equals(curPollsChoice)) {
							break;
						}

						array[1] = curPollsChoice;

						if (sr.previous()) {
							array[0] = PollsChoiceHBMUtil.model((PollsChoiceHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = PollsChoiceHBMUtil.model((PollsChoiceHBM)sr.get(
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
				"FROM PollsChoice IN CLASS com.liferay.portlet.polls.ejb.PollsChoiceHBM ");
			query.append("ORDER BY ");
			query.append("choiceId ASC");

			Iterator itr = session.find(query.toString()).iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				PollsChoiceHBM pollsChoiceHBM = (PollsChoiceHBM)itr.next();
				list.add(PollsChoiceHBMUtil.model(pollsChoiceHBM));
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

	protected void removeByQuestionId(String questionId)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM PollsChoice IN CLASS com.liferay.portlet.polls.ejb.PollsChoiceHBM WHERE ");
			query.append("questionId = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("choiceId ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, questionId);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				PollsChoiceHBM pollsChoiceHBM = (PollsChoiceHBM)itr.next();
				PollsChoicePool.remove((PollsChoicePK)pollsChoiceHBM.getPrimaryKey());
				session.delete(pollsChoiceHBM);
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

	protected int countByQuestionId(String questionId)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM PollsChoice IN CLASS com.liferay.portlet.polls.ejb.PollsChoiceHBM WHERE ");
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
}