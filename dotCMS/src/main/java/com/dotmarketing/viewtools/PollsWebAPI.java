package com.dotmarketing.viewtools;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.polls.ajax.PollsAjax;
import com.dotmarketing.cms.polls.business.PollsAPI;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portlet.polls.model.PollsChoice;
import com.liferay.portlet.polls.model.PollsQuestion;

public class PollsWebAPI implements ViewTool {

	private HttpServletRequest request;

	private PollsAPI pollsAPI;
	private GlobalVariableWebAPI globalVars;
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		pollsAPI = APILocator.getPollAPI();
		globalVars = new GlobalVariableWebAPI();
		globalVars.init(context);
	}

	/**
	 * 
	 * @param questionId
	 * @return
	 */
	public boolean hasExpired(String questionId) {

		return pollsAPI.hasExpired(questionId);

	}

	/**
	 * 
	 * @param questionId
	 * @return
	 */
	public boolean questionExists(String questionId) {

		return pollsAPI.questionExists(questionId);

	}

	
	
	/**
	 * 
	 * @param questionId
	 * @param request
	 * @return
	 */
	public PollsAjax.VoteAnswer displayPoll(String questionId) {

		HttpSession session = request.getSession();

		StringBuffer htmlResult = new StringBuffer(512);
		htmlResult.ensureCapacity(128);

		boolean allowViewResults = (Boolean) request.getAttribute("allowViewResults");
		boolean showVotes = (Boolean) request.getAttribute("showVotes");

		PollsAjax.VoteAnswer result = new PollsAjax().new VoteAnswer();

		boolean hasVoted = false;

		if (pollsAPI.hasVoted(questionId)
				|| (session.getAttribute(PollsQuestion.class.getName() + "."
						+ questionId + "._voted") != null)) {
			hasVoted = true;
		}

		if (!hasVoted) {
			PollsQuestion question = pollsAPI.getQuestion(questionId);
			List choices = pollsAPI.getChoices(questionId);
			Iterator itr = choices.iterator();
	
			htmlResult.append("<table class='poll-answer-table' id='answer" + questionId + "'>");
			while (itr.hasNext()) {
				PollsChoice choice = (PollsChoice) itr.next();
				
				htmlResult.append("<tr><td class='poll-radio'>");
				htmlResult
						.append("<input type='radio' name='question" + questionId + "' id='choice-" + choice.getChoiceId() + "'  value='"
						+ choice.getChoiceId() + "'/>");
				htmlResult.append("</td><td class='poll-label'>");
				htmlResult.append("<label for='choice-"+ choice.getChoiceId() +"'>" + choice.getDescription() + "</label>");
				htmlResult.append("</td></tr>");
			}
			
			


			htmlResult.append("<tr><td colspan='2' class='poll-buttons'>");
			htmlResult.append("<input type=button onclick=\"vote('" + questionId + "');\" value='"+globalVars.get("Vote")+"' class='poll-vote'>");
			htmlResult.append("</td></tr>");
			if (allowViewResults) {
				htmlResult.append("<tr><td colspan='2' class='poll-view-results'>");
					htmlResult.append("<a href='#' onclick=\"showResults('" + questionId + "');\">"+globalVars.get("View-Results")+"</a>");
				htmlResult.append("</td></tr>");
			}
			htmlResult.append("</table>");

			result = new PollsAjax().new VoteAnswer(questionId,htmlResult.toString());
			

		} else {

			result = displayPollResults(questionId, showVotes);
		}

		return result;

	}

	/**
	 * 
	 * @param questionId
	 * @param showVotes
	 * @return
	 */
	public PollsAjax.VoteAnswer displayPollResults(String questionId, boolean showVotes) {
		PollsAPI pollsAPI = APILocator.getPollAPI();
		NumberFormat numberFormat = NumberFormat.getNumberInstance();
		NumberFormat percentFormat = NumberFormat.getPercentInstance();
		StringBuffer htmlResult = new StringBuffer(512);
		htmlResult.ensureCapacity(128);

		int totalVotes = pollsAPI.getTotalVotes(questionId);
		PollsQuestion question = pollsAPI.getQuestion(questionId);

		List choices = pollsAPI.getChoices(questionId);

		htmlResult.append("<div id='result" + questionId + "'>");
		//htmlResult.append("<h2>" + question.getDescription() + "</h2>");
		htmlResult.append("<table class='poll-result-table'>");

		for (int i = 0; i < choices.size(); i++) {
			PollsChoice choice = (PollsChoice) choices.get(i);

			int choiceVotes = pollsAPI.getChoiceVotes(questionId, choice
					.getChoiceId());

			double votesPercent = 0.0;
			if (totalVotes > 0) {
				votesPercent = (double) choiceVotes / totalVotes;
			}
			
			htmlResult.append("<tr>");
				htmlResult.append("<td  colspan=4 class='poll-question'>");
					htmlResult.append(choice.getDescription());
				htmlResult.append("</td>");
			htmlResult.append("</tr>");
			htmlResult.append("<tr>");
				htmlResult.append("<td class='poll-percents'>");
					htmlResult.append(percentFormat.format(votesPercent));
				htmlResult.append("</td>");
				if (showVotes) {
					htmlResult.append("<td class='poll-votes'>");
					htmlResult.append(numberFormat.format(choiceVotes));
					htmlResult.append("</td>");
				}
				htmlResult.append("<td class='poll-bars'>");
					
				htmlResult.append("<div class='poll-result-bar poll-result-bar-" + i + "' style='width:" + percentFormat.format(votesPercent) + ";'><img src='/html/images/shim.gif' width='1' height='1'></div>");
					
				htmlResult.append("</td>");
			htmlResult.append("</tr>");


		}
		htmlResult.append("<tr>");
			htmlResult.append("<td colspan='4' class='poll-responses'>");
				if(totalVotes ==0){
					htmlResult.append("No responses");
				}
				else if(totalVotes ==1){
					htmlResult.append( globalVars.get("Total")+": " + totalVotes + " " + globalVars.get("responses"));
				}
				else{
					htmlResult.append("Total: " + totalVotes + " responses");
				}
				
			htmlResult.append("</td>");
		htmlResult.append("</tr>");
		htmlResult.append("</table>");
		htmlResult.append("</div>");

		return new PollsAjax().new VoteAnswer(questionId,htmlResult.toString());
	}

	/**
	 * Return a list of active PollsQuestion
	 * @return List<PollsQuestion>
	 */
	@SuppressWarnings("unchecked")
	public List<PollsQuestion> displayActivePolls() {		

		return pollsAPI.getActiveQuestions();
	}
	
	/**
	 * Return a list of active PollsQuestion ordered
	 * @param orderBy Permitted values createDate, expirationDate and questionId
	 * @param direction value -1 descending order and 1 to ascending order
	 *  
	 * @return List<PollsQuestion>
	 */
	@SuppressWarnings("unchecked")
	public List<PollsQuestion> displayActivePolls(String orderBy, int direction) {		

		return pollsAPI.getActiveQuestions(orderBy, direction);
	}
	
	/**
	 * Return a list of active PollsQuestion ordered
	 * @param orderBy Permitted values createDate, expirationDate and questionId
	 * @param direction value -1 descending order and 1 to ascending order
	 * @param portletGroupIds array list of group ids configured with the poll portlet
	 *  
	 * @return List<PollsQuestion>
	 */
	@SuppressWarnings("unchecked")
	public List<PollsQuestion> displayActivePolls(String orderBy, int direction, List<String> portletGroupIds) {		

		return pollsAPI.getActiveQuestions(orderBy, direction, portletGroupIds);
	}
	
	/**
	 * Return a list of active PollsQuestion with the given questionID
	 * @param questionId the questionId to check
	 *  
	 * @return List<PollsQuestion>
	 */
	@SuppressWarnings("unchecked")
	public List<PollsQuestion> displayActivePolls(String questionId) 
	{		
		List<PollsQuestion> pollsQuestions = new ArrayList<PollsQuestion>();
		PollsQuestion question = pollsAPI.getQuestion(questionId);
		if(UtilMethods.isSet(question))
		{
			pollsQuestions.add(question);
		}
		return pollsQuestions; 
	}

	/**
	 * get the hashmap list of question to be passed to the buildRSS macro to generate the rss page
	 * @param rssDetailPage The URL to the Polls rss detail page
	 * @param orderBy Permitted values createDate, expirationDate and questionId
	 * @param direction value -1 descending order and 1 to ascending order
	 * @return
	 */
	public List<Map<String,Object>> getRSSPollsMapList(String rssDetailPage, String orderBy, int direction) {	

		List<Map<String,Object>> contentList = new ArrayList<Map<String,Object>>();
		List<PollsQuestion> polls = pollsAPI.getActiveQuestions(orderBy, direction);
		for(PollsQuestion poll : polls){
			Map<String,Object> item = new HashMap<String,Object>();
			item.put("guid", poll.getQuestionId());
			item.put("title", poll.getTitle());
			item.put("description", poll.getDescription());
			item.put("link", rssDetailPage+"?questionId="+poll.getQuestionId());
			item.put("pubdate", poll.getModifiedDate());		
			contentList.add(item);	 	
		}
		return contentList;
	}
	
	/**
	 * Return a list of inactive PollsQuestion
	 * @return List<PollsQuestion>
	 */
	@SuppressWarnings("unchecked")
	public List<PollsQuestion> displayInactivePolls() {		

		return pollsAPI.getInactiveQuestions();
	}
	
	/**
	 * Return a list of inactive PollsQuestion ordered
	 * @param orderBy Permitted values createDate, expirationDate and questionId
	 * @param direction value -1 descending order and 1 to ascending order
	 *  
	 * @return List<PollsQuestion>
	 */
	@SuppressWarnings("unchecked")
	public List<PollsQuestion> displayInactivePolls(String orderBy, int direction) {		

		return pollsAPI.getInactiveQuestions(orderBy, direction);
	}
	
	/**
	 * Return a list of inactive PollsQuestion ordered
	 * @param orderBy Permitted values createDate, expirationDate and questionId
	 * @param direction value -1 descending order and 1 to ascending order
	 * @param portletGroupIds array list of group ids configured with the poll portlet
	 *  
	 *  @deprecated
	 * @return List<PollsQuestion>
	 */
	@SuppressWarnings("unchecked")
	public List<PollsQuestion> displayInactivePolls(String orderBy, int direction, List<String> portletGroupIds) {		

		return pollsAPI.getInactiveQuestions(orderBy, direction);
	}
}