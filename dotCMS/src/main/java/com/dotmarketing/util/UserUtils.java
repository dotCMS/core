package com.dotmarketing.util;

import com.dotmarketing.factories.ChallengeQuestionFactory;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;

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
	public static List getChallengeQuestionList() {

		return ChallengeQuestionFactory.getChallengeQuestionList();
	}

	static final String ACCEPTABLE_PASSWORD_CHARS = "!#%+23456789:=?@ABCDEFGHJKLMNPRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

	/**
	 * Generates a passwords that might be any random combination of 16 chars lenght
	 * with any of these chars !#%+23456789:=?@ABCDEFGHJKLMNPRSTUVWXYZabcdefghijkmnopqrstuvwxyz
	 * @return
	 */
	public static String generateSecurePassword(){
	   	return RandomStringUtils.random(16, ACCEPTABLE_PASSWORD_CHARS.toCharArray());
	}

}