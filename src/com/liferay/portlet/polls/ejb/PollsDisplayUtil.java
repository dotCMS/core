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
 * <a href="PollsDisplayUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.13 $
 *
 */
public class PollsDisplayUtil {
	public static String PERSISTENCE = GetterUtil.get(PropsUtil.get(
				"value.object.persistence.com.liferay.portlet.polls.model.PollsDisplay"),
			"com.liferay.portlet.polls.ejb.PollsDisplayPersistence");
	public static String LISTENER = GetterUtil.getString(PropsUtil.get(
				"value.object.listener.com.liferay.portlet.polls.model.PollsDisplay"));

	protected static com.liferay.portlet.polls.model.PollsDisplay create(
		com.liferay.portlet.polls.ejb.PollsDisplayPK pollsDisplayPK) {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.create(pollsDisplayPK);
	}

	protected static com.liferay.portlet.polls.model.PollsDisplay remove(
		com.liferay.portlet.polls.ejb.PollsDisplayPK pollsDisplayPK)
		throws com.liferay.portlet.polls.NoSuchDisplayException, 
			com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(PollsDisplayUtil.class,e.getMessage(),e);
			}
		}

		if (listener != null) {
			listener.onBeforeRemove(findByPrimaryKey(pollsDisplayPK));
		}

		com.liferay.portlet.polls.model.PollsDisplay pollsDisplay = persistence.remove(pollsDisplayPK);

		if (listener != null) {
			listener.onAfterRemove(pollsDisplay);
		}

		return pollsDisplay;
	}

	protected static com.liferay.portlet.polls.model.PollsDisplay update(
		com.liferay.portlet.polls.model.PollsDisplay pollsDisplay)
		throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(PollsDisplayUtil.class,e.getMessage(),e);
			}
		}

		boolean isNew = pollsDisplay.isNew();

		if (listener != null) {
			if (isNew) {
				listener.onBeforeCreate(pollsDisplay);
			}
			else {
				listener.onBeforeUpdate(pollsDisplay);
			}
		}

		pollsDisplay = persistence.update(pollsDisplay);

		if (listener != null) {
			if (isNew) {
				listener.onAfterCreate(pollsDisplay);
			}
			else {
				listener.onAfterUpdate(pollsDisplay);
			}
		}

		return pollsDisplay;
	}

	protected static com.liferay.portlet.polls.model.PollsDisplay findByPrimaryKey(
		com.liferay.portlet.polls.ejb.PollsDisplayPK pollsDisplayPK)
		throws com.liferay.portlet.polls.NoSuchDisplayException, 
			com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByPrimaryKey(pollsDisplayPK);
	}

	protected static java.util.List findByUserId(java.lang.String userId)
		throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId(userId);
	}

	protected static java.util.List findByUserId(java.lang.String userId,
		int begin, int end) throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId(userId, begin, end);
	}

	protected static java.util.List findByUserId(java.lang.String userId,
		int begin, int end, com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId(userId, begin, end, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsDisplay findByUserId_First(
		java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchDisplayException, 
			com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId_First(userId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsDisplay findByUserId_Last(
		java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchDisplayException, 
			com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId_Last(userId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsDisplay[] findByUserId_PrevAndNext(
		com.liferay.portlet.polls.ejb.PollsDisplayPK pollsDisplayPK,
		java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchDisplayException, 
			com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByUserId_PrevAndNext(pollsDisplayPK, userId, obc);
	}

	protected static java.util.List findByQuestionId(
		java.lang.String questionId) throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId(questionId);
	}

	protected static java.util.List findByQuestionId(
		java.lang.String questionId, int begin, int end)
		throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId(questionId, begin, end);
	}

	protected static java.util.List findByQuestionId(
		java.lang.String questionId, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId(questionId, begin, end, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsDisplay findByQuestionId_First(
		java.lang.String questionId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchDisplayException, 
			com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId_First(questionId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsDisplay findByQuestionId_Last(
		java.lang.String questionId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchDisplayException, 
			com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId_Last(questionId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsDisplay[] findByQuestionId_PrevAndNext(
		com.liferay.portlet.polls.ejb.PollsDisplayPK pollsDisplayPK,
		java.lang.String questionId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchDisplayException, 
			com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByQuestionId_PrevAndNext(pollsDisplayPK,
			questionId, obc);
	}

	protected static java.util.List findByL_U(java.lang.String layoutId,
		java.lang.String userId) throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByL_U(layoutId, userId);
	}

	protected static java.util.List findByL_U(java.lang.String layoutId,
		java.lang.String userId, int begin, int end)
		throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByL_U(layoutId, userId, begin, end);
	}

	protected static java.util.List findByL_U(java.lang.String layoutId,
		java.lang.String userId, int begin, int end,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByL_U(layoutId, userId, begin, end, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsDisplay findByL_U_First(
		java.lang.String layoutId, java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchDisplayException, 
			com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByL_U_First(layoutId, userId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsDisplay findByL_U_Last(
		java.lang.String layoutId, java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchDisplayException, 
			com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByL_U_Last(layoutId, userId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsDisplay[] findByL_U_PrevAndNext(
		com.liferay.portlet.polls.ejb.PollsDisplayPK pollsDisplayPK,
		java.lang.String layoutId, java.lang.String userId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchDisplayException, 
			com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByL_U_PrevAndNext(pollsDisplayPK, layoutId,
			userId, obc);
	}

	protected static java.util.List findAll()
		throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findAll();
	}

	protected static void removeByUserId(java.lang.String userId)
		throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByUserId(userId);
	}

	protected static void removeByQuestionId(java.lang.String questionId)
		throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByQuestionId(questionId);
	}

	protected static void removeByL_U(java.lang.String layoutId,
		java.lang.String userId) throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByL_U(layoutId, userId);
	}

	protected static int countByUserId(java.lang.String userId)
		throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByUserId(userId);
	}

	protected static int countByQuestionId(java.lang.String questionId)
		throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByQuestionId(questionId);
	}

	protected static int countByL_U(java.lang.String layoutId,
		java.lang.String userId) throws com.liferay.portal.SystemException {
		PollsDisplayPersistence persistence = (PollsDisplayPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByL_U(layoutId, userId);
	}
}