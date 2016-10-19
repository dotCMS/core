package com.dotmarketing.cms.polls.business;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.util.Logger;
import com.liferay.portal.SystemException;
import com.liferay.portlet.polls.ejb.PollsChoiceManagerUtil;
import com.liferay.portlet.polls.ejb.PollsQuestionLocalManagerUtil;
import com.liferay.portlet.polls.ejb.PollsQuestionManagerUtil;
import com.liferay.portlet.polls.ejb.PollsVoteManagerUtil;
import com.liferay.portlet.polls.model.PollsQuestion;

/**
 * 
 * @author Roger
 * 
 * Liferay implementation of the PollsAPI class uses Liferay Domain objects and
 * com.liferay.portlet.polls.* methods.
 * 
 */
public class PollsAPILiferayImpl implements PollsAPI {

	/**
	 * Returns a list of PollQuestion objects
	 * This implementation uses the Liferay API's to obtain the list of PollQuestion
	 * objects
	 * 
	 * @return List of PollsQuestion
	 */
	@SuppressWarnings("unchecked")
	public List<PollsQuestion> getActiveQuestions(){
		return getActiveQuestions("createDate", -1);
	}
	
	/**
	 * Returns a list of PollQuestion objects
	 * This implementation uses the Liferay API's to obtain the list of PollQuestion
	 * objects
	 * @param orderBy Permitted values createDate, expirationDate and questionId
	 * @param direction value -1 descending order and 1 to ascending order
	 * 
	 * @return List of PollsQuestion
	 */
	public List<PollsQuestion> getActiveQuestions(String orderBy, int direction){
		List<PollsQuestion> questions = new ArrayList<PollsQuestion>();
		try{
			List results = PollsQuestionManagerUtil.getQuestions("25", PublicCompanyFactory.getDefaultCompanyId(), 0, 1000);
			for(PollsQuestion poll : (List<PollsQuestion>)results){
				if(!hasExpired(poll.getQuestionId())){
					questions.add(poll);
				}
			}
			Collections.sort(questions,new ComparateQuestions(direction,orderBy));

		}catch(Exception e){
			Logger.error(this, e.getMessage(),e);
		}
		return questions;
	}
	
	/**
	 * Returns a list of PollQuestion objects
	 * This implementation uses the Liferay API's to obtain the list of PollQuestion
	 * objects
	 * @param orderBy Permitted values createDate, expirationDate and questionId
	 * @param direction value -1 descending order and 1 to ascending order
	 * @param portletGroupIds array list of group ids configured with the poll portlet
	 * 
	 * @return List of PollsQuestion
	 */
	public List<PollsQuestion> getActiveQuestions(String orderBy, int direction, List<String> portletGroupIds){
		List<PollsQuestion> questions = new ArrayList<PollsQuestion>();
		try{
			//List results = PollsQuestionManagerUtil.getQuestions("25", "-1", PublicCompanyFactory.getDefaultCompanyId(), 0, 1000);
			List<PollsQuestion> results = new ArrayList<PollsQuestion>();
			for (String portletGroupId: portletGroupIds)
				results.addAll(PollsQuestionManagerUtil.getQuestions("25", PublicCompanyFactory.getDefaultCompanyId(), 0, 1000));
			for(PollsQuestion poll : (List<PollsQuestion>)results){
				if(!hasExpired(poll.getQuestionId())){
					questions.add(poll);
				}
			}
			Collections.sort(questions,new ComparateQuestions(direction,orderBy));

		}catch(Exception e){
			Logger.error(this, e.getMessage(),e);
		}
		return questions;
	}

	/**
	 * Returns a PollQuestion object given a question id. The questionId
	 * argument must specify a valid question id.
	 * <p>
	 * This implementation uses the Liferay API's to obtain a PollQuestion
	 * object given a valid question id.
	 * 
	 * @param questionId
	 *            a valid question id
	 * @return the PollQuestion
	 */
	public PollsQuestion getQuestion(String questionId) {

		PollsQuestion question = null;
		try {
			question = PollsQuestionManagerUtil.getQuestion(questionId);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(),e);
		}
		return question;
	}

	/**
	 * Returns a List object containing string representations of the choices of
	 * a question, given a valid question id. The questionId argument must
	 * specify a valid question id.
	 * <p>
	 * This implementation uses the Liferay API's to obtain the list of choices.
	 * 
	 * @param questionId
	 *            a valid question id
	 * @return the List of choices
	 */
	public List getChoices(String questionId) {

		List choices = null;
		try {
			choices = PollsChoiceManagerUtil.getChoices(questionId);
		} catch (SystemException e) {
			Logger.error(this, e.getMessage(),e);
		}
		return choices;

	}

	/**
	 * Performs a vote operation using Liferay API's given a valid user,
	 * question id an choice id.
	 * 
	 * @param voteUserId
	 *            a user id
	 * @param questionId
	 *            a valid question id
	 * @param choiceId
	 *            a valid choice id
	 */
	public void vote(String voteUserId, String questionId, String choiceId) {
		try {
			PollsQuestionLocalManagerUtil
			.vote(voteUserId, questionId, choiceId);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(),e);
		}
	}

	/**
	 * Returns the integer representation of the total vote count for a question
	 * given a question id.
	 * 
	 * @param questionId
	 *            a valid question id
	 * @return the total vote count for a given question id.
	 */
	public int getTotalVotes(String questionId) {

		int totalVotes = 0;
		try {
			totalVotes = PollsVoteManagerUtil.getVotesSize(questionId);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(),e);
		}

		return totalVotes;
	}

	/**
	 * Returns the integer representation of the total vote count for a choice
	 * given a question id and a choice id.
	 * 
	 * @param questionId
	 *            a valid question id
	 * @param choiceId
	 *            a valid choice id
	 * @return the total vote count for a given question id and choice id.
	 */
	public int getChoiceVotes(String questionId, String choiceId) {

		int choiceVotes = 0;
		try {
			choiceVotes = PollsVoteManagerUtil.getVotesSize(questionId,
					choiceId);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(),e);
		}
		return choiceVotes;

	}

	/**
	 * Returns whether a question has expired or not.
	 * 
	 * @param questionId
	 *            a valid question id
	 * @return if the question has expired.
	 */
	public boolean hasExpired(String questionId) {

		PollsQuestion question = null;
		boolean hasExpired = false;
		try {
			question = PollsQuestionManagerUtil.getQuestion(questionId);
		} catch (Exception e) {
			Logger.error(PollsAPILiferayImpl.class, e.getMessage());
		}
		Date expirationDate = question.getExpirationDate();
		if (expirationDate != null) {
			Date today = new Date();
			if (expirationDate.before(today)) {
				hasExpired = true;
			}
		}
		return hasExpired;

	}

	/**
	 * Returns whether a question exists or not.
	 * 
	 * @param questionId
	 *            a valid question id
	 * @return if the question exists.
	 */
	public boolean questionExists(String questionId) {

		PollsQuestion question = getQuestion(questionId);
		if (question == null) {
			return false;
		} else {
			return true;
		}

	}

	/**
	 * Returns whether a vote has been casted on a question given a question id.
	 * 
	 * @param questionId
	 *            a valid question id
	 * @return if a vote has already been casted for the given question id.
	 */
	public boolean hasVoted(String questionId) {

		boolean hasVoted = false;

		try {
			hasVoted = PollsQuestionManagerUtil.hasVoted(questionId);

		} catch (Exception e) {
			Logger.error(this, e.getMessage(),e);
		}

		return hasVoted;
	}

	/**
	 * Compare two BigIdeaForm
	 * @author Oswaldo Gallango
	 *
	 */
	private class ComparateQuestions implements Comparator{
		/**
		 * Evaluate the comparation of the two object
		 */
		private int direction = 1;
		private String orderBy = "createDate";
		public ComparateQuestions(){

		}

		public ComparateQuestions(int direction, String orderBy){
			this.direction = direction;
			this.orderBy = orderBy;
		}

		@SuppressWarnings("unchecked")
		public int compare( Object object1, Object object2 ) throws ClassCastException {
			final int BEFORE = -1;
			final int EQUAL = 0;
			final int AFTER = 1;

			PollsQuestion object11 = (PollsQuestion)object1;
			PollsQuestion object22 = (PollsQuestion)object2;

			if ( object1 == object2 ) return EQUAL;

			if(orderBy.equals("createDate")){
				if (object11.getCreateDate().before(object22.getCreateDate())) return BEFORE * direction;
				if (object11.getCreateDate().after(object22.getCreateDate())) return AFTER * direction;
			}
			if(orderBy.equals("expirationDate")){
				if (object11.getExpirationDate().before(object22.getExpirationDate())) return BEFORE * direction;
				if (object11.getExpirationDate().after(object22.getExpirationDate())) return AFTER * direction;
			}
			if(orderBy.equals("questionId")){
				return object11.getQuestionId().compareTo(object22.getQuestionId());
			}

			assert object1.equals(object2) : "compareTo inconsistent with equals.";

			return EQUAL;
		}

	}
	
	/**
	 * Returns a list of inactive PollQuestion objects
	 * This implementation uses the Liferay API's to obtain the list of PollQuestion
	 * objects
	 * 
	 * @return List of PollsQuestion
	 */
	@SuppressWarnings("unchecked")
	public List<PollsQuestion> getInactiveQuestions(){
		return getInactiveQuestions("createDate", -1);
	}
	
	/**
	 * Returns a list of inactive PollQuestion objects
	 * This implementation uses the Liferay API's to obtain the list of PollQuestion
	 * objects
	 * @param orderBy Permitted values createDate, expirationDate and questionId
	 * @param direction value -1 descending order and 1 to ascending order
	 * 
	 * @return List of PollsQuestion
	 */
	public List<PollsQuestion> getInactiveQuestions(String orderBy, int direction){
		List<PollsQuestion> questions = new ArrayList<PollsQuestion>();
		try{
			List results = PollsQuestionManagerUtil.getQuestions("25", PublicCompanyFactory.getDefaultCompanyId(), 0, 1000);
			for(PollsQuestion poll : (List<PollsQuestion>)results){
				if(hasExpired(poll.getQuestionId())){
					questions.add(poll);
				}
			}
			Collections.sort(questions,new ComparateQuestions(direction,orderBy));

		}catch(Exception e){
			Logger.error(this, e.getMessage(),e);
		}
		return questions;
	}
	
}