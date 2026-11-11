package com.dotmarketing.util;

import java.util.List;

import com.dotmarketing.beans.ChallengeQuestion;
import com.dotmarketing.db.HibernateUtil;

/**
 * This class was created to manage the challenge question search to db
 * @author Martin Amaris
 * @author Oswaldo Gallango
 *
 */
public class UserUtils {

	/**
	 * This method return a list of challenge question list
	 * @return List
	 */
	public static List<ChallengeQuestion> getChallengeQuestionList() {
		try {
			HibernateUtil dh = new HibernateUtil(ChallengeQuestion.class);
			dh.setQuery("from challenge_question in class com.dotmarketing.beans.ChallengeQuestion");
			return dh.list();
		} catch (Exception e) {
			Logger.error(UserUtils.class, e.toString());
			return null;
		}
	}
}