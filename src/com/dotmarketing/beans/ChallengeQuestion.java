package com.dotmarketing.beans;

import java.io.Serializable;

/**
 * Challenge Question Bean
 * @author  Armando
 */
public class ChallengeQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

	/** identifier field */
    private long challengeQuestionId;

    /** Challenge Question Text */
    private String challengeQuestionText;

    /**
     * Return Challenge Question Id Number
     * @return Challenge Question Id Number
     */
	public long getChallengeQuestionId() {
		return challengeQuestionId;
	}

	/**
     * Set Challenge Question Id Number
     * @param challengeQuestionId Number of the challenge question id to set
     */
	public void setChallengeQuestionId(long challengeQuestionId) {
		this.challengeQuestionId = challengeQuestionId;
	}

	/**
     * Return Challenge Question Text Information
     * @return Challenge Question Text Information
     */
	public String getChallengeQuestionText() {
		return challengeQuestionText;
	}

	/**
     * Set Challenge Question Text Information
     * @param challengeQuestionText Text with the challenge question text to set
     */
	public void setChallengeQuestionText(String challengeQuestionText) {
		this.challengeQuestionText = challengeQuestionText;
	}
}