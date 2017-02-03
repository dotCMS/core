package com.dotmarketing.factories;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.ChallengeQuestion;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.util.Logger;

/**
 * This class was created to manage the challenge question search to db
 * @author Oswaldo
 *
 */
public class ChallengeQuestionFactory {

	/**
	 * This Methods return the list of challenge questions
	 * @author martin amaris
	 * @return List
	 */
	public static List<ChallengeQuestion> getChallengeQuestionList() {

		List<ChallengeQuestion> result = null;

		try {
			HibernateUtil dh = new HibernateUtil(ChallengeQuestion.class);
			dh.setQuery("from challenge_question in class com.dotmarketing.beans.ChallengeQuestion");
			result = dh.list();
		} catch (Exception e) {
			Logger.error(ChallengeQuestionFactory.class, e.toString());
		}

		return result;
	}
	
	/**
	 * This Method return a specified challenge question by id
	 * @author Armando Siem
	 * @return ChallengeQuestion
	 */
	public static ChallengeQuestion getChallengeQuestionById(long challengeQuestionId) {
		ChallengeQuestion result = null;

		try {
			HibernateUtil dh = new HibernateUtil(ChallengeQuestion.class);
			dh.setQuery("from challenge_question in class com.dotmarketing.beans.ChallengeQuestion where cquestionid = ?");
			dh.setParam(challengeQuestionId);
			result = (ChallengeQuestion) dh.load();
		} catch (Exception e) {
			Logger.error(ChallengeQuestionFactory.class, e.toString());
		}

		return result;
	}
}