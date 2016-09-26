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
 * <a href="PollsChoiceUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.88 $
 *
 */
public class PollsChoiceUtil {
	public static String PERSISTENCE = GetterUtil.get(PropsUtil.get(
				"value.object.persistence.com.liferay.portlet.polls.model.PollsChoice"),
			"com.liferay.portlet.polls.ejb.PollsChoicePersistence");
	public static String LISTENER = GetterUtil.getString(PropsUtil.get(
				"value.object.listener.com.liferay.portlet.polls.model.PollsChoice"));

	protected static com.liferay.portlet.polls.model.PollsChoice create(
		com.liferay.portlet.polls.ejb.PollsChoicePK pollsChoicePK) {
		PollsChoicePersistence persistence = (PollsChoicePersistence)InstancePool.get(PERSISTENCE);

		return persistence.create(pollsChoicePK);
	}

	protected static com.liferay.portlet.polls.model.PollsChoice remove(
		com.liferay.portlet.polls.ejb.PollsChoicePK pollsChoicePK)
		throws com.liferay.portlet.polls.NoSuchChoiceException, 
			com.liferay.portal.SystemException {
		PollsChoicePersistence persistence = (PollsChoicePersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(PollsChoiceUtil.class,e.getMessage(),e);
			}
		}

		if (listener != null) {
			listener.onBeforeRemove(findByPrimaryKey(pollsChoicePK));
		}

		com.liferay.portlet.polls.model.PollsChoice pollsChoice = persistence.remove(pollsChoicePK);

		if (listener != null) {
			listener.onAfterRemove(pollsChoice);
		}

		return pollsChoice;
	}

	protected static com.liferay.portlet.polls.model.PollsChoice update(
		com.liferay.portlet.polls.model.PollsChoice pollsChoice)
		throws com.liferay.portal.SystemException {
		PollsChoicePersistence persistence = (PollsChoicePersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(PollsChoiceUtil.class,e.getMessage(),e);
			}
		}

		boolean isNew = pollsChoice.isNew();

		if (listener != null) {
			if (isNew) {
				listener.onBeforeCreate(pollsChoice);
			}
			else {
				listener.onBeforeUpdate(pollsChoice);
			}
		}

		pollsChoice = persistence.update(pollsChoice);

		if (listener != null) {
			if (isNew) {
				listener.onAfterCreate(pollsChoice);
			}
			else {
				listener.onAfterUpdate(pollsChoice);
			}
		}

		return pollsChoice;
	}

	protected static com.liferay.portlet.polls.model.PollsChoice findByPrimaryKey(
		com.liferay.portlet.polls.ejb.PollsChoicePK pollsChoicePK)
		throws com.liferay.portlet.polls.NoSuchChoiceException, 
			com.liferay.portal.SystemException {
		PollsChoicePersistence persistence = (PollsChoicePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByPrimaryKey(pollsChoicePK);
	}

	protected static java.util.List findByQuestionId(
		java.lang.String questionId) throws com.liferay.portal.SystemException {
		PollsChoicePersistence persistence = (PollsChoicePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId(questionId);
	}

	protected static java.util.List findByQuestionId(
		java.lang.String questionId, int begin, int end)
		throws com.liferay.portal.SystemException {
		PollsChoicePersistence persistence = (PollsChoicePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId(questionId, begin, end);
	}

	protected static java.util.List findByQuestionId(
		java.lang.String questionId, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PollsChoicePersistence persistence = (PollsChoicePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId(questionId, begin, end, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsChoice findByQuestionId_First(
		java.lang.String questionId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchChoiceException, 
			com.liferay.portal.SystemException {
		PollsChoicePersistence persistence = (PollsChoicePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId_First(questionId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsChoice findByQuestionId_Last(
		java.lang.String questionId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchChoiceException, 
			com.liferay.portal.SystemException {
		PollsChoicePersistence persistence = (PollsChoicePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId_Last(questionId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsChoice[] findByQuestionId_PrevAndNext(
		com.liferay.portlet.polls.ejb.PollsChoicePK pollsChoicePK,
		java.lang.String questionId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchChoiceException, 
			com.liferay.portal.SystemException {
		PollsChoicePersistence persistence = (PollsChoicePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId_PrevAndNext(pollsChoicePK,
			questionId, obc);
	}

	protected static java.util.List findAll()
		throws com.liferay.portal.SystemException {
		PollsChoicePersistence persistence = (PollsChoicePersistence)InstancePool.get(PERSISTENCE);

		return persistence.findAll();
	}

	protected static void removeByQuestionId(java.lang.String questionId)
		throws com.liferay.portal.SystemException {
		PollsChoicePersistence persistence = (PollsChoicePersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByQuestionId(questionId);
	}

	protected static int countByQuestionId(java.lang.String questionId)
		throws com.liferay.portal.SystemException {
		PollsChoicePersistence persistence = (PollsChoicePersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByQuestionId(questionId);
	}
}