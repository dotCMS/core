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
 * <a href="PollsQuestionUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.91 $
 *
 */
public class PollsQuestionUtil {
	public static String PERSISTENCE = GetterUtil.get(PropsUtil.get(
				"value.object.persistence.com.liferay.portlet.polls.model.PollsQuestion"),
			"com.liferay.portlet.polls.ejb.PollsQuestionPersistence");
	public static String LISTENER = GetterUtil.getString(PropsUtil.get(
				"value.object.listener.com.liferay.portlet.polls.model.PollsQuestion"));

	protected static com.liferay.portlet.polls.model.PollsQuestion create(
		java.lang.String questionId) {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.create(questionId);
	}

	protected static com.liferay.portlet.polls.model.PollsQuestion remove(
		java.lang.String questionId)
		throws com.liferay.portlet.polls.NoSuchQuestionException, 
			com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(PollsQuestionUtil.class,e.getMessage(),e);
			}
		}

		if (listener != null) {
			listener.onBeforeRemove(findByPrimaryKey(questionId));
		}

		com.liferay.portlet.polls.model.PollsQuestion pollsQuestion = persistence.remove(questionId);

		if (listener != null) {
			listener.onAfterRemove(pollsQuestion);
		}

		return pollsQuestion;
	}

	protected static com.liferay.portlet.polls.model.PollsQuestion update(
		com.liferay.portlet.polls.model.PollsQuestion pollsQuestion)
		throws com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);
		ModelListener listener = null;

		if (Validator.isNotNull(LISTENER)) {
			try {
				listener = (ModelListener)Class.forName(LISTENER).newInstance();
			}
			catch (Exception e) {
				Logger.error(PollsQuestionUtil.class,e.getMessage(),e);
			}
		}

		boolean isNew = pollsQuestion.isNew();

		if (listener != null) {
			if (isNew) {
				listener.onBeforeCreate(pollsQuestion);
			}
			else {
				listener.onBeforeUpdate(pollsQuestion);
			}
		}

		pollsQuestion = persistence.update(pollsQuestion);

		if (listener != null) {
			if (isNew) {
				listener.onAfterCreate(pollsQuestion);
			}
			else {
				listener.onAfterUpdate(pollsQuestion);
			}
		}

		return pollsQuestion;
	}

	protected static com.liferay.portlet.polls.model.PollsQuestion findByPrimaryKey(
		java.lang.String questionId)
		throws com.liferay.portlet.polls.NoSuchQuestionException, 
			com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByPrimaryKey(questionId);
	}

	protected static java.util.List findByGroupId(java.lang.String groupId)
		throws com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByGroupId(groupId);
	}

	protected static java.util.List findByGroupId(java.lang.String groupId,
		int begin, int end) throws com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByGroupId(groupId, begin, end);
	}

	protected static java.util.List findByGroupId(java.lang.String groupId,
		int begin, int end, com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByGroupId(groupId, begin, end, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsQuestion findByGroupId_First(
		java.lang.String groupId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchQuestionException, 
			com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByGroupId_First(groupId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsQuestion findByGroupId_Last(
		java.lang.String groupId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchQuestionException, 
			com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByGroupId_Last(groupId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsQuestion[] findByGroupId_PrevAndNext(
		java.lang.String questionId, java.lang.String groupId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchQuestionException, 
			com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByGroupId_PrevAndNext(questionId, groupId, obc);
	}

	protected static java.util.List findByP_G_C(java.lang.String portletId,
		java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByP_G_C(portletId, companyId);
	}

	protected static java.util.List findByP_G_C(java.lang.String portletId,
		java.lang.String companyId, int begin, int end)
		throws com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByP_G_C(portletId, companyId, begin, end);
	}

	protected static java.util.List findByP_G_C(java.lang.String portletId,
		java.lang.String companyId, int begin,
		int end, com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByP_G_C(portletId, companyId, begin,
			end, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsQuestion findByP_G_C_First(
		java.lang.String portletId, java.lang.String groupId,
		java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchQuestionException, 
			com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByP_G_C_First(portletId, groupId, companyId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsQuestion findByP_G_C_Last(
		java.lang.String portletId, java.lang.String groupId,
		java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchQuestionException, 
			com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByP_G_C_Last(portletId, groupId, companyId, obc);
	}

	protected static com.liferay.portlet.polls.model.PollsQuestion[] findByP_G_C_PrevAndNext(
		java.lang.String questionId, java.lang.String portletId,
		java.lang.String companyId,
		com.liferay.util.dao.hibernate.OrderByComparator obc)
		throws com.liferay.portlet.polls.NoSuchQuestionException, 
			com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findByP_G_C_PrevAndNext(questionId, portletId,
			companyId, obc);
	}

	protected static java.util.List findAll()
		throws com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.findAll();
	}

	protected static void removeByGroupId(java.lang.String groupId)
		throws com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByGroupId(groupId);
	}

	protected static void removeByP_G_C(java.lang.String portletId,
		java.lang.String groupId, java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);
		persistence.removeByP_G_C(portletId, groupId, companyId);
	}

	protected static int countByGroupId(java.lang.String groupId)
		throws com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByGroupId(groupId);
	}

	protected static int countByP_G_C(java.lang.String portletId,
		java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		PollsQuestionPersistence persistence = (PollsQuestionPersistence)InstancePool.get(PERSISTENCE);

		return persistence.countByP_G_C(portletId, companyId);
	}
}