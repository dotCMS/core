package com.dotmarketing.cms.polls.ajax;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import uk.ltd.getahead.dwr.WebContextFactory;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.polls.business.PollsAPI;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.viewtools.PollsWebAPI;
import com.liferay.counter.ejb.CounterManagerUtil;
import com.liferay.portal.model.User;
import com.liferay.portlet.polls.model.PollsQuestion;
import com.liferay.util.servlet.SessionMessages;

public class PollsAjax {

	private PollsAPI pollsAPI = APILocator.getPollAPI();

	
	/**
	 * 
	 * @param questionId
	 * @param choiceId
	 * @param showVotes
	 * @return
	 */
	public VoteAnswer vote(String questionId, String choiceId, boolean showVotes) {

		HttpSession session = WebContextFactory.get().getSession();
		HttpServletRequest request = WebContextFactory.get()
				.getHttpServletRequest();

		User user = (User) session.getAttribute(WebKeys.CMS_USER);
		String userId = null;
		if (user != null) {
			userId = user.getUserId();
		} else {
			try {
				userId = Long
						.toString(CounterManagerUtil
								.increment(PollsQuestion.class.getName()
										+ ".anonymous"));
			} catch (Exception e2) {
			}
		}
		String voteUserId = "";

		boolean hasVoted = false;

		voteUserId = userId;

		if (pollsAPI.hasVoted(questionId)
				|| (session.getAttribute(PollsQuestion.class.getName() + "."
						+ questionId + "._voted") != null)) {
			hasVoted = true;
		}

		if (!hasVoted) {

			pollsAPI.vote(voteUserId, questionId, choiceId);

			SessionMessages.add(request, "vote_added");

			session.setAttribute(PollsQuestion.class.getName() + "."
					+ questionId + "._voted", new Boolean(true));
		}
		PollsAjax.VoteAnswer returnValue = displayPollResults(questionId,showVotes);
		return returnValue;
	}

	/**
	 * 
	 * @param questionId
	 * @param showVotes
	 * @return
	 */
	public PollsAjax.VoteAnswer displayPollResults(String questionId, boolean showVotes) {

		return  new PollsWebAPI().displayPollResults(questionId, showVotes);

	}
	
	public class VoteAnswer
	{
	 	public String questionId;
	 	public String htmlCode;
	 	
	 	public VoteAnswer()
	 	{
	 		questionId = "";
	 		htmlCode = "";	 		
	 	}
	 	
	 	public VoteAnswer(String questionId,String htmlCode)
	 	{
	 		this.questionId = questionId;
	 		this.htmlCode = htmlCode;
	 	}
	 	
		public String getQuestionId() {
			return questionId;
		}
		public void setQuestionId(String questionId) {
			this.questionId = questionId;
		}
		public String getHtmlCode() {
			return htmlCode;
		}
		public void setHtmlCode(String htmlCode) {
			this.htmlCode = htmlCode;
		}
	}
	

}