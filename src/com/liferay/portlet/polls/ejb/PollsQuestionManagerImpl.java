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
import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.ejb.PrincipalBean;
import com.liferay.portal.model.User;
import com.liferay.portlet.polls.NoSuchVoteException;
import com.liferay.portlet.polls.model.PollsQuestion;
import com.liferay.portlet.polls.model.PollsVote;

/**
 * <a href="PollsQuestionManagerImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class PollsQuestionManagerImpl
	extends PrincipalBean implements PollsQuestionManager {

	// Business methods

	public PollsQuestion addQuestion(
			String portletId, String title, String description,
			int expMonth, int expDay, int expYear, boolean neverExpires,
			List choices)
		throws PortalException, SystemException {

		return PollsQuestionLocalManagerUtil.addQuestion(
			getUserId(), portletId, title, description, expMonth,
			expDay, expYear, neverExpires, choices);
	}

	public void checkQuestions() throws PortalException, SystemException {
		List questions = PollsQuestionUtil.findAll();

		Date now = new Date();

		for (int i = 0; i < questions.size(); i++) {
			PollsQuestion question = (PollsQuestion)questions.get(i);

			if (question.getExpirationDate() != null &&
				question.getExpirationDate().before(now)) {

				PollsQuestionUtil.remove(question.getQuestionId());
			}
		}
	}

	public void deleteQuestion(String questionId)
		throws PortalException, SystemException {

		if (!hasAdmin(questionId)) {
			throw new PrincipalException();
		}

		PollsQuestionLocalManagerUtil.deleteQuestion(questionId);
	}

	public PollsQuestion getQuestion(String questionId)
		throws PortalException, SystemException {

		return PollsQuestionUtil.findByPrimaryKey(questionId);
	}

	public List getQuestions(String portletId, String companyId)
		throws SystemException {

		return PollsQuestionUtil.findByP_G_C(portletId, companyId);
	}

	public List getQuestions(
			String portletId, String companyId, int begin,
			int end)
		throws SystemException {

		return PollsQuestionUtil.findByP_G_C(
			portletId, companyId, begin, end);
	}

	public int getQuestionsSize(
			String portletId, String companyId)
		throws SystemException {

		return PollsQuestionUtil.countByP_G_C(portletId, companyId);
	}

	public boolean hasVoted(String questionId)
		throws PortalException, SystemException {

		try {
			String userId = null;
			
			PollsVote vote = PollsVoteUtil.findByPrimaryKey(
				new PollsVotePK(questionId, userId));
		}
		catch (NoSuchVoteException nsve) {
			return false;
//		}catch(PrincipalException ue){
//			return false;
		}

		return true;
	}

	public PollsQuestion updateQuestion(
			String questionId, String title, String description, int expMonth,
			int expDay, int expYear, boolean neverExpires, List choices)
		throws PortalException, SystemException {

		if (!hasAdmin(questionId)) {
			throw new PrincipalException();
		}

		return PollsQuestionLocalManagerUtil.updateQuestion(
			getUserId(), questionId, title, description, expMonth, expDay,
			expYear, neverExpires, choices);
	}

	public void vote(String questionId, String choiceId)
		throws PortalException, SystemException {

		User user = getUser();

		PollsQuestion question = getQuestion(questionId);

		if (!question.getCompanyId().equals(user.getCompanyId())) {
			throw new PrincipalException();
		}

		PollsQuestionLocalManagerUtil.vote(
			user.getUserId(), questionId, choiceId);
	}

	// Permission methods

	public boolean hasAdmin(String questionId)
		throws PortalException, SystemException {

		PollsQuestion question = PollsQuestionUtil.findByPrimaryKey(questionId);

		try {
			if ((question.getUserId().equals(getUserId())) ||
				(getUser().getCompanyId().equals(question.getCompanyId()) &&
				APILocator.getRoleAPI().doesUserHaveRole(APILocator.getUserAPI().loadUserById(getUserId(),APILocator.getUserAPI().getSystemUser(),true), Role.POLLS_ADMIN))) {

				return true;
			}
			else {
				return false;
			}
		} catch (Exception e) {
			Logger.error(PollsQuestionManagerImpl.class,e.getMessage(),e);
			throw new SystemException(e);
		}
	}


}