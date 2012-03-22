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

import java.io.Serializable;

import com.liferay.util.StringPool;

/**
 * <a href="PollsChoicePK.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.78 $
 *
 */
public class PollsChoicePK implements Comparable, Serializable {
	public String questionId;
	public String choiceId;

	public PollsChoicePK() {
	}

	public PollsChoicePK(String questionId, String choiceId) {
		this.questionId = questionId;
		this.choiceId = choiceId;
	}

	public String getQuestionId() {
		return questionId;
	}

	public void setQuestionId(String questionId) {
		this.questionId = questionId;
	}

	public String getChoiceId() {
		return choiceId;
	}

	public void setChoiceId(String choiceId) {
		this.choiceId = choiceId;
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		PollsChoicePK pk = (PollsChoicePK)obj;
		int value = 0;
		value = questionId.compareTo(pk.questionId);

		if (value != 0) {
			return value;
		}

		value = choiceId.compareTo(pk.choiceId);

		if (value != 0) {
			return value;
		}

		return 0;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		PollsChoicePK pk = null;

		try {
			pk = (PollsChoicePK)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		if ((questionId.equals(pk.questionId)) &&
				(choiceId.equals(pk.choiceId))) {
			return true;
		}
		else {
			return false;
		}
	}

	public int hashCode() {
		return (questionId + choiceId).hashCode();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(StringPool.OPEN_CURLY_BRACE);
		sb.append("questionId");
		sb.append(StringPool.EQUAL);
		sb.append(questionId);
		sb.append(StringPool.COMMA);
		sb.append(StringPool.SPACE);
		sb.append("choiceId");
		sb.append(StringPool.EQUAL);
		sb.append(choiceId);
		sb.append(StringPool.CLOSE_CURLY_BRACE);

		return sb.toString();
	}
}