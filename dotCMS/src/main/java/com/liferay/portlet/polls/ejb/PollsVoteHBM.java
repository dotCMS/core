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

package com.liferay.portlet.polls.ejb;

import java.util.Date;

/**
 * <a href="PollsVoteHBM.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.15 $
 *
 */
public class PollsVoteHBM {
	protected PollsVoteHBM() {
	}

	protected PollsVoteHBM(PollsVotePK pk) {
		_questionId = pk.questionId;
		_userId = pk.userId;
	}

	protected PollsVoteHBM(String questionId, String userId, String choiceId,
		Date voteDate) {
		_questionId = questionId;
		_userId = userId;
		_choiceId = choiceId;
		_voteDate = voteDate;
	}

	public PollsVotePK getPrimaryKey() {
		return new PollsVotePK(_questionId, _userId);
	}

	protected void setPrimaryKey(PollsVotePK pk) {
		_questionId = pk.questionId;
		_userId = pk.userId;
	}

	protected String getQuestionId() {
		return _questionId;
	}

	protected void setQuestionId(String questionId) {
		_questionId = questionId;
	}

	protected String getUserId() {
		return _userId;
	}

	protected void setUserId(String userId) {
		_userId = userId;
	}

	protected String getChoiceId() {
		return _choiceId;
	}

	protected void setChoiceId(String choiceId) {
		_choiceId = choiceId;
	}

	protected Date getVoteDate() {
		return _voteDate;
	}

	protected void setVoteDate(Date voteDate) {
		_voteDate = voteDate;
	}

	private String _questionId;
	private String _userId;
	private String _choiceId;
	private Date _voteDate;
}