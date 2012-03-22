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

package com.liferay.portlet.polls.action;

import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.RenderRequest;
import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.RenderRequestImpl;
import com.liferay.portlet.polls.ejb.PollsChoiceManagerUtil;
import com.liferay.portlet.polls.ejb.PollsQuestionManagerUtil;
import com.liferay.portlet.polls.model.PollsQuestion;
import com.liferay.util.Validator;

/**
 * <a href="ActionUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class ActionUtil {

	public static void getQuestion(ActionRequest req) throws Exception {
		HttpServletRequest httpReq =
			((ActionRequestImpl)req).getHttpServletRequest();

		getQuestion(httpReq);
	}

	public static void getQuestion(RenderRequest req) throws Exception {
		HttpServletRequest httpReq =
			((RenderRequestImpl)req).getHttpServletRequest();

		getQuestion(httpReq);
	}

	public static void getQuestion(HttpServletRequest req) throws Exception {
		String questionId = req.getParameter("question_id");

		// Find question

		PollsQuestion question = null;

		if (Validator.isNotNull(questionId)) {
			question = PollsQuestionManagerUtil.getQuestion(questionId);
		}

		// Find choices

		List choices = null;

		if (question != null) {
			choices = PollsChoiceManagerUtil.getChoices(questionId);
		}

		req.setAttribute(WebKeys.POLLS_QUESTION, question);
		req.setAttribute(WebKeys.POLLS_CHOICES, choices);
	}

}