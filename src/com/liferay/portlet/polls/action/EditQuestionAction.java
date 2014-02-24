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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotcms.util.SecurityUtils;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.polls.NoSuchQuestionException;
import com.liferay.portlet.polls.QuestionChoiceException;
import com.liferay.portlet.polls.QuestionDescriptionException;
import com.liferay.portlet.polls.QuestionExpirationDateException;
import com.liferay.portlet.polls.QuestionTitleException;
import com.liferay.portlet.polls.ejb.PollsQuestionManagerUtil;
import com.liferay.portlet.polls.model.PollsChoice;
import com.liferay.util.ParamUtil;
import com.liferay.util.Validator;
import com.liferay.util.servlet.SessionErrors;

/**
 * <a href="EditQuestionAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.11 $
 *
 */
public class EditQuestionAction extends PortletAction {

	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
		throws Exception {

		try {
			ActionUtil.getQuestion(req);
		}
		catch (Exception e) {
			if (e != null &&
				e instanceof NoSuchQuestionException) {

				SessionErrors.add(req, e.getClass().getName());

				setForward(req, "portlet.polls.error");
			}
			else {
				req.setAttribute(PageContext.EXCEPTION, e);

				setForward(req, Constants.COMMON_ERROR);
			}
		}

		String cmd = req.getParameter(Constants.CMD);

		if ((cmd != null) &&
			(cmd.equals(Constants.ADD) || cmd.equals(Constants.UPDATE))) {

			try {
				_updateQuestion(config, req, res);
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof PrincipalException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.polls.error");
				}
				else if (e != null &&
						 e instanceof QuestionChoiceException ||
						 e instanceof QuestionDescriptionException ||
						 e instanceof QuestionExpirationDateException ||
						 e instanceof QuestionTitleException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.polls.edit_question");
				}
				else {
					req.setAttribute(PageContext.EXCEPTION, e);

					setForward(req, Constants.COMMON_ERROR);
				}
			}
		}
		else if (cmd != null && cmd.equals(Constants.DELETE)) {
			try {
				_deleteQuestion(req, res);
			}
			catch (Exception e) {
				if (e != null &&
					e instanceof NoSuchQuestionException ||
					e instanceof PrincipalException) {

					SessionErrors.add(req, e.getClass().getName());

					setForward(req, "portlet.polls.error");
				}
				else {
					req.setAttribute(PageContext.EXCEPTION, e);

					setForward(req, Constants.COMMON_ERROR);
				}
			}
		}
		else {
			setForward(req, "portlet.polls.edit_question");
		}
	}

	private void _deleteQuestion(ActionRequest req, ActionResponse res)
		throws Exception {

		String questionId = ParamUtil.getString(req, "question_id");

		PollsQuestionManagerUtil.deleteQuestion(questionId);

		// Send redirect

		res.sendRedirect(SecurityUtils.stripReferer(ParamUtil.getString(req, "redirect")));
	}

	private void _updateQuestion(
			PortletConfig config, ActionRequest req, ActionResponse res)
		throws Exception {

		String questionId = req.getParameter("question_id");

		String title = ParamUtil.getString(req, "question_title");
		String description = ParamUtil.getString(req, "question_desc");
		int expMonth = ParamUtil.getInteger(req, "question_exp_month");
		int expDay = ParamUtil.getInteger(req, "question_exp_day");
		int expYear = ParamUtil.getInteger(req, "question_exp_year");
		boolean neverExpires = ParamUtil.get(
			req, "question_never_expires", false);

		// Add choices

		List choices = new ArrayList();

		Enumeration enu = req.getParameterNames();

		while (enu.hasMoreElements()) {
			String param = (String)enu.nextElement();

			if (param.startsWith("choice_desc_")) {
				try {
					String id = param.substring(
						param.lastIndexOf("_") + 1, param.length());

					String choiceId = req.getParameter("choice_id_" + id);

					String choiceDesc = req.getParameter(param);

					PollsChoice choice =
						new PollsChoice(null, choiceId, choiceDesc);

					choices.add(choice);
				}
				catch (Exception exc) {
				}
			}
		}

		if (Validator.isNull(questionId)) {

			// Add question

			PollsQuestionManagerUtil.addQuestion(
				config.getPortletName(),
				title, description, expMonth, expDay, expYear, neverExpires,
				choices);
		}
		else {

			// Update question

			PollsQuestionManagerUtil.updateQuestion(
				questionId, title, description, expMonth, expDay, expYear,
				neverExpires, choices);
		}

		// Send redirect

		res.sendRedirect(SecurityUtils.stripReferer(ParamUtil.getString(req, "redirect")));
	}

}