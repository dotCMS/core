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

/**
 * <a href="PollsQuestionManagerUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.83 $
 *
 */
public class PollsQuestionManagerUtil {
	public static final String PORTLET_ID = "25";

	public static com.liferay.portlet.polls.model.PollsQuestion addQuestion(
		java.lang.String portletId,
		java.lang.String title, java.lang.String description, int expMonth,
		int expDay, int expYear, boolean neverExpires, java.util.List choices)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PollsQuestionManager pollsQuestionManager = PollsQuestionManagerFactory.getManager();

			return pollsQuestionManager.addQuestion(portletId, title,
				description, expMonth, expDay, expYear, neverExpires, choices);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static void checkQuestions()
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PollsQuestionManager pollsQuestionManager = PollsQuestionManagerFactory.getManager();
			pollsQuestionManager.checkQuestions();
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static void deleteQuestion(java.lang.String questionId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PollsQuestionManager pollsQuestionManager = PollsQuestionManagerFactory.getManager();
			pollsQuestionManager.deleteQuestion(questionId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portlet.polls.model.PollsQuestion getQuestion(
		java.lang.String questionId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PollsQuestionManager pollsQuestionManager = PollsQuestionManagerFactory.getManager();

			return pollsQuestionManager.getQuestion(questionId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static java.util.List getQuestions(java.lang.String portletId,
		java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		try {
			PollsQuestionManager pollsQuestionManager = PollsQuestionManagerFactory.getManager();

			return pollsQuestionManager.getQuestions(portletId, companyId);
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static java.util.List getQuestions(java.lang.String portletId,
		java.lang.String companyId, int begin, int end)
		throws com.liferay.portal.SystemException {
		try {
			PollsQuestionManager pollsQuestionManager = PollsQuestionManagerFactory.getManager();

			return pollsQuestionManager.getQuestions(portletId, companyId, begin, end);
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static int getQuestionsSize(java.lang.String portletId,
		java.lang.String companyId)
		throws com.liferay.portal.SystemException {
		try {
			PollsQuestionManager pollsQuestionManager = PollsQuestionManagerFactory.getManager();

			return pollsQuestionManager.getQuestionsSize(portletId, companyId);
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static boolean hasVoted(java.lang.String questionId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PollsQuestionManager pollsQuestionManager = PollsQuestionManagerFactory.getManager();

			return pollsQuestionManager.hasVoted(questionId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static com.liferay.portlet.polls.model.PollsQuestion updateQuestion(
		java.lang.String questionId, java.lang.String title,
		java.lang.String description, int expMonth, int expDay, int expYear,
		boolean neverExpires, java.util.List choices)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PollsQuestionManager pollsQuestionManager = PollsQuestionManagerFactory.getManager();

			return pollsQuestionManager.updateQuestion(questionId, title,
				description, expMonth, expDay, expYear, neverExpires, choices);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static void vote(java.lang.String questionId,
		java.lang.String choiceId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PollsQuestionManager pollsQuestionManager = PollsQuestionManagerFactory.getManager();
			pollsQuestionManager.vote(questionId, choiceId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}

	public static boolean hasAdmin(java.lang.String questionId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException {
		try {
			PollsQuestionManager pollsQuestionManager = PollsQuestionManagerFactory.getManager();

			return pollsQuestionManager.hasAdmin(questionId);
		}
		catch (com.liferay.portal.PortalException pe) {
			throw pe;
		}
		catch (com.liferay.portal.SystemException se) {
			throw se;
		}
		catch (Exception e) {
			throw new com.liferay.portal.SystemException(e);
		}
	}
}