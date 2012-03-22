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

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.dotmarketing.util.UtilMethods;
import com.liferay.counter.ejb.CounterManagerUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.polls.DuplicateVoteException;
import com.liferay.portlet.polls.NoSuchVoteException;
import com.liferay.portlet.polls.QuestionChoiceException;
import com.liferay.portlet.polls.QuestionDescriptionException;
import com.liferay.portlet.polls.QuestionExpirationDateException;
import com.liferay.portlet.polls.QuestionTitleException;
import com.liferay.portlet.polls.model.PollsChoice;
import com.liferay.portlet.polls.model.PollsQuestion;
import com.liferay.portlet.polls.model.PollsVote;
import com.liferay.util.Validator;

/**
 * <a href="PollsQuestionLocalManagerImpl.java.html"><b><i>View Source</i></b>
 * </a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class PollsQuestionLocalManagerImpl
	implements PollsQuestionLocalManager {

	// Business methods

	public PollsQuestion addQuestion(
			String userId, String portletId, String title,
			String description, int expMonth, int expDay, int expYear,
			boolean neverExpires, List choices)
		throws PortalException, SystemException {

		_validate(title, description, choices);

		User user = UserLocalManagerUtil.getUserById(userId);

		String questionId = Long.toString(CounterManagerUtil.increment(
			PollsQuestion.class.getName()));

		PollsQuestion question = PollsQuestionUtil.create(questionId);

		Date now = new Date();

		Date expirationDate = null;
		if (!neverExpires) {
			expirationDate = PortalUtil.getDate(
				expMonth, expDay, expYear,
				new QuestionExpirationDateException());
		}

		question.setPortletId(portletId);
		question.setCompanyId(user.getCompanyId());
		question.setUserId(user.getUserId());
		question.setUserName(user.getFullName());
		question.setCreateDate(now);
		question.setModifiedDate(now);
		question.setTitle(title);
		question.setDescription(description);
		question.setExpirationDate(expirationDate);
		question.setGroupId("");

		PollsQuestionUtil.update(question);

		// Add choices

		Iterator itr = choices.iterator();

		while (itr.hasNext()) {
			PollsChoice choice = (PollsChoice)itr.next();

			choice.setQuestionId(questionId);

			PollsChoiceUtil.update(choice);
		}

		return question;
	}

	public void deleteAll(String groupId)
		throws PortalException, SystemException {

		Iterator itr = PollsQuestionUtil.findByGroupId(groupId).iterator();

		while (itr.hasNext()) {
			PollsQuestion question = (PollsQuestion)itr.next();

			deleteQuestion(question.getQuestionId());
		}
	}

	public void deleteQuestion(String questionId)
		throws PortalException, SystemException {

		PollsQuestion question = PollsQuestionUtil.findByPrimaryKey(questionId);

		// Delete all displays associated with this question

		PollsDisplayUtil.removeByQuestionId(questionId);

		// Delete votes

		PollsVoteUtil.removeByQuestionId(questionId);

		// Delete choices

		PollsChoiceUtil.removeByQuestionId(questionId);

		// Delete question

		PollsQuestionUtil.remove(questionId);
	}

	public PollsQuestion updateQuestion(
			String userId, String questionId, String title, String description,
			int expMonth, int expDay, int expYear, boolean neverExpires,
			List choices)
		throws PortalException, SystemException {

		_validate(title, description, choices);

		User user = UserLocalManagerUtil.getUserById(userId);

		PollsQuestion question = PollsQuestionUtil.findByPrimaryKey(questionId);

		Date expirationDate = null;
		if (!neverExpires) {
			expirationDate = PortalUtil.getDate(
				expMonth, expDay, expYear,
				new QuestionExpirationDateException());
		}

		question.setModifiedDate(new Date());
		question.setTitle(title);
		question.setDescription(description);
		question.setExpirationDate(expirationDate);

		PollsQuestionUtil.update(question);

		// Delete old choices

		PollsChoiceUtil.removeByQuestionId(questionId);

		// Add choices

		Iterator itr = choices.iterator();

		while (itr.hasNext()) {
			PollsChoice choice = (PollsChoice)itr.next();

			choice.setQuestionId(questionId);

			PollsChoiceUtil.update(choice);
		}

		return question;
	}

	public void vote(String userId, String questionId, String choiceId)
		throws PortalException, SystemException {

		PollsQuestion question = PollsQuestionUtil.findByPrimaryKey(questionId);

		Date now = new Date();

		question.setLastVoteDate(now);

		PollsQuestionUtil.update(question);

		PollsVotePK votePK = new PollsVotePK(questionId, userId);

		PollsVote vote = null;

		try {
			vote = PollsVoteUtil.findByPrimaryKey(votePK);

			throw new DuplicateVoteException();
		}
		catch (NoSuchVoteException nsve) {
			vote = PollsVoteUtil.create(votePK);

			PollsChoicePK choicePK =
				new PollsChoicePK(questionId, choiceId);

			PollsChoice choice = PollsChoiceUtil.findByPrimaryKey(choicePK);

			vote.setChoiceId(choice.getChoiceId());
			vote.setVoteDate(now);

			PollsVoteUtil.update(vote);
		}
	}

	// Private methods

	private void _validate(String title, String description, List choices)
		throws PortalException {

		if (Validator.isNull(title)) {
			throw new QuestionTitleException();
		}
		else if (Validator.isNull(description)) {
			throw new QuestionDescriptionException();
		}

		if (choices.size() < 2) {
			throw new QuestionChoiceException();
		}

		for (int i = 0; i < choices.size(); i++) {
			PollsChoice choice = (PollsChoice)choices.get(i);

			if (Validator.isNull(choice.getChoiceId()) ||
				Validator.isNull(choice.getDescription())) {

				throw new QuestionChoiceException();
			}
		}
	}

}