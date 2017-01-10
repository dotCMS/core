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

import com.dotmarketing.util.Logger;
import com.liferay.portal.model.ModelListener;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.InstancePool;
import com.liferay.util.Validator;

/**
 * <a href="PollsVoteUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.88 $
 *
 */
public class PollsVoteUtil {
	public static String PERSISTENCE = GetterUtil.get(PropsUtil.get(
				"value.object.persistence.com.liferay.portlet.polls.model.PollsVote"),
			"com.liferay.portlet.polls.ejb.PollsVotePersistence");
	public static String LISTENER = GetterUtil.getString(PropsUtil.get(
				"value.object.listener.com.liferay.portlet.polls.model.PollsVote"));

	protected static com.liferay.portlet.polls.model.PollsVote create(
		com.liferay.portlet.polls.ejb.PollsVotePK pollsVotePK) {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.create(pollsVotePK);
	}

	protected static com.liferay.portlet.polls.model.PollsVote remove(
		com.liferay.portlet.polls.ejb.PollsVotePK pollsVotePK)
		throws com.liferay.portlet.polls.NoSuchVoteException, 
			com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(PollsVoteUtil.class,e.getMessage(),e);
			}
		}

		if (listener != null) {
			listener.onBeforeRemove(findByPrimaryKey(pollsVotePK));
		}

		com.liferay.portlet.polls.model.PollsVote pollsVote = persistence.remove(pollsVotePK);

		if (listener != null) {
			listener.onAfterRemove(pollsVote);
		}

		return pollsVote;
	}

	protected static com.liferay.portlet.polls.model.PollsVote update(
		com.liferay.portlet.polls.model.PollsVote pollsVote)
		throws com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(PollsVoteUtil.class,e.getMessage(),e);
			}
		}

		boolean isNew = pollsVote.isNew();

		if (listener != null) {
			if (isNew) {
				listener.onBeforeCreate(pollsVote);
			}
			else {
				listener.onBeforeUpdate(pollsVote);
			}
		}

		pollsVote = persistence.update(pollsVote);

		if (listener != null) {
			if (isNew) {
				listener.onAfterCreate(pollsVote);
			}
			else {
				listener.onAfterUpdate(pollsVote);
			}
		}

		return pollsVote;
	}

	protected static com.liferay.portlet.polls.model.PollsVote findByPrimaryKey(
		com.liferay.portlet.polls.ejb.PollsVotePK pollsVotePK)
		throws com.liferay.portlet.polls.NoSuchVoteException, 
			com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByPrimaryKey(pollsVotePK);
	}

	protected static java.util.List findByQuestionId(
		java.lang.String questionId) throws com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId(questionId);
	}

	protected static java.util.List findByQuestionId(
		java.lang.String questionId, int begin, int end)
		throws com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId(questionId, begin, end);
	}

	protected static java.util.List findByQuestionId(
		java.lang.String questionId, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId(questionId, begin, end, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsVote findByQuestionId_First(
		java.lang.String questionId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchVoteException, 
			com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId_First(questionId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsVote findByQuestionId_Last(
		java.lang.String questionId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchVoteException, 
			com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId_Last(questionId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsVote[] findByQuestionId_PrevAndNext(
		com.liferay.portlet.polls.ejb.PollsVotePK pollsVotePK,
		java.lang.String questionId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchVoteException, 
			com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId_PrevAndNext(pollsVotePK,
			questionId, obc);
	}

	protected static java.util.List findByQ_C(java.lang.String questionId,
		java.lang.String choiceId) throws com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQ_C(questionId, choiceId);
	}

	protected static java.util.List findByQ_C(java.lang.String questionId,
		java.lang.String choiceId, int begin, int end)
		throws com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQ_C(questionId, choiceId, begin, end);
	}

	protected static java.util.List findByQ_C(java.lang.String questionId,
		java.lang.String choiceId, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQ_C(questionId, choiceId, begin, end, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsVote findByQ_C_First(
		java.lang.String questionId, java.lang.String choiceId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchVoteException, 
			com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQ_C_First(questionId, choiceId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsVote findByQ_C_Last(
		java.lang.String questionId, java.lang.String choiceId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchVoteException, 
			com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQ_C_Last(questionId, choiceId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsVote[] findByQ_C_PrevAndNext(
		com.liferay.portlet.polls.ejb.PollsVotePK pollsVotePK,
		java.lang.String questionId, java.lang.String choiceId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchVoteException, 
			com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQ_C_PrevAndNext(pollsVotePK, questionId,
			choiceId, obc);
	}

	protected static java.util.List findAll()
		throws com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findAll();
	}

	protected static void removeByQuestionId(java.lang.String questionId)
		throws com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByQuestionId(questionId);
	}

	protected static void removeByQ_C(java.lang.String questionId,
		java.lang.String choiceId) throws com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByQ_C(questionId, choiceId);
	}

	protected static int countByQuestionId(java.lang.String questionId)
		throws com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByQuestionId(questionId);
	}

	protected static int countByQ_C(java.lang.String questionId,
		java.lang.String choiceId) throws com.liferay.portal.SystemException {
		PollsVotePersistence persistence = (PollsVotePersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByQ_C(questionId, choiceId);
	}
}