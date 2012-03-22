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

import java.rmi.RemoteException;

/**
 * <a href="PollsQuestionManagerSoap.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class PollsQuestionManagerSoap {
	public static com.liferay.portlet.polls.model.PollsQuestionModel addQuestion(
		java.lang.String portletId,
		java.lang.String title, java.lang.String description, int expMonth,
		int expDay, int expYear, boolean neverExpires, java.util.List choices)
		throws RemoteException {
		try {
			com.liferay.portlet.polls.model.PollsQuestion returnValue = PollsQuestionManagerUtil.addQuestion(portletId,
					title, description, expMonth, expDay, expYear,
					neverExpires, choices);

			return returnValue;
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static void checkQuestions() throws RemoteException {
		try {
			PollsQuestionManagerUtil.checkQuestions();
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static void deleteQuestion(java.lang.String questionId)
		throws RemoteException {
		try {
			PollsQuestionManagerUtil.deleteQuestion(questionId);
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static com.liferay.portlet.polls.model.PollsQuestionModel getQuestion(
		java.lang.String questionId) throws RemoteException {
		try {
			com.liferay.portlet.polls.model.PollsQuestion returnValue = PollsQuestionManagerUtil.getQuestion(questionId);

			return returnValue;
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static com.liferay.portlet.polls.model.PollsQuestionModel[] getQuestions(
		java.lang.String portletId, java.lang.String companyId) throws RemoteException {
		try {
			java.util.List returnValue = PollsQuestionManagerUtil.getQuestions(portletId,
					companyId);

			return (com.liferay.portlet.polls.model.PollsQuestion[])returnValue.toArray(new com.liferay.portlet.polls.model.PollsQuestion[0]);
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static com.liferay.portlet.polls.model.PollsQuestionModel[] getQuestions(
		java.lang.String portletId, java.lang.String companyId, int begin, int end)
		throws RemoteException {
		try {
			java.util.List returnValue = PollsQuestionManagerUtil.getQuestions(portletId,
					companyId, begin, end);

			return (com.liferay.portlet.polls.model.PollsQuestion[])returnValue.toArray(new com.liferay.portlet.polls.model.PollsQuestion[0]);
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static int getQuestionsSize(java.lang.String portletId,
		java.lang.String companyId)
		throws RemoteException {
		try {
			int returnValue = PollsQuestionManagerUtil.getQuestionsSize(portletId,
					companyId);

			return returnValue;
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static boolean hasVoted(java.lang.String questionId)
		throws RemoteException {
		try {
			boolean returnValue = PollsQuestionManagerUtil.hasVoted(questionId);

			return returnValue;
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static com.liferay.portlet.polls.model.PollsQuestionModel updateQuestion(
		java.lang.String questionId, java.lang.String title,
		java.lang.String description, int expMonth, int expDay, int expYear,
		boolean neverExpires, java.util.List choices) throws RemoteException {
		try {
			com.liferay.portlet.polls.model.PollsQuestion returnValue = PollsQuestionManagerUtil.updateQuestion(questionId,
					title, description, expMonth, expDay, expYear,
					neverExpires, choices);

			return returnValue;
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static void vote(java.lang.String questionId,
		java.lang.String choiceId) throws RemoteException {
		try {
			PollsQuestionManagerUtil.vote(questionId, choiceId);
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public static boolean hasAdmin(java.lang.String questionId)
		throws RemoteException {
		try {
			boolean returnValue = PollsQuestionManagerUtil.hasAdmin(questionId);

			return returnValue;
		}
		catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}
}