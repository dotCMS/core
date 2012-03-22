package com.dotmarketing.cms.polls.business;

import java.util.List;

import com.liferay.portlet.polls.model.PollsQuestion;

/**
 * @author Roger
 * 
 */

public interface PollsAPI {
	
	/**
	 * Returns a list of PollQuestion objects
	 * This implementation uses the Liferay API's to obtain the list of PollQuestion
	 * objects
	 * 
	 * @return List of PollsQuestion
	 */
	public List getActiveQuestions();
	
	/**
	 * Returns a list of PollQuestion objects
	 * This implementation uses the Liferay API's to obtain the list of PollQuestion
	 * objects
	 * @param orderBy Permitted values createDate, expirationDate and questionId
	 * @param direction value -1 descending order and 1 to ascending order
	 * 
	 * @return List of PollsQuestion
	 */
	public List<PollsQuestion> getActiveQuestions(String orderBy, int direction);
	
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
	public List<PollsQuestion> getActiveQuestions(String orderBy, int direction, List<String> portletGroupIds);

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
	public PollsQuestion getQuestion(String questionId);

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
	public List getChoices(String questionId);

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
	public void vote(String voteUserId, String questionId, String choiceId);

	/**
	 * Returns the integer representation of the total vote count for a question
	 * given a question id.
	 * 
	 * @param questionId
	 *            a valid question id
	 * @return the total vote count for a given question id.
	 */
	public int getTotalVotes(String questionId);

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
	public int getChoiceVotes(String questionId, String choiceId);

	/**
	 * Returns whether a question has expired or not.
	 * 
	 * @param questionId
	 *            a valid question id
	 * @return if the question has expired.
	 */
	public boolean hasExpired(String questionId);

	/**
	 * Returns whether a question exists or not.
	 * 
	 * @param questionId
	 *            a valid question id
	 * @return if the question exists.
	 */
	public boolean questionExists(String questionId);

	/**
	 * Returns whether a vote has been casted on a question given a question id.
	 * 
	 * @param questionId
	 *            a valid question id
	 * @return if a vote has already been casted for the given question id.
	 */
	public boolean hasVoted(String questionId);
	
	/**
	 * Returns a list of inactive PollQuestion objects
	 * This implementation uses the Liferay API's to obtain the list of PollQuestion
	 * objects
	 * 
	 * @return List of PollsQuestion
	 */
	public List getInactiveQuestions();
	
	/**
	 * Returns a list of inactive PollQuestion objects
	 * This implementation uses the Liferay API's to obtain the list of PollQuestion
	 * objects
	 * @param orderBy Permitted values createDate, expirationDate and questionId
	 * @param direction value -1 descending order and 1 to ascending order
	 * 
	 * @return List of PollsQuestion
	 */
	public List<PollsQuestion> getInactiveQuestions(String orderBy, int direction);
	
}