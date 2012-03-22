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

package com.liferay.portlet.polls.model;

import com.liferay.portal.model.BaseModel;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portlet.polls.ejb.PollsChoicePK;
import com.liferay.util.GetterUtil;
import com.liferay.util.Xss;

/**
 * <a href="PollsChoiceModel.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.74 $
 *
 */
public class PollsChoiceModel extends BaseModel {
	public static boolean CACHEABLE = GetterUtil.get(PropsUtil.get(
				"value.object.cacheable.com.liferay.portlet.polls.model.PollsChoice"),
			VALUE_OBJECT_CACHEABLE);
	public static int MAX_SIZE = GetterUtil.get(PropsUtil.get(
				"value.object.max.size.com.liferay.portlet.polls.model.PollsChoice"),
			VALUE_OBJECT_MAX_SIZE);
	public static boolean XSS_ALLOW_BY_MODEL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portlet.polls.model.PollsChoice"),
			XSS_ALLOW);
	public static boolean XSS_ALLOW_QUESTIONID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portlet.polls.model.PollsChoice.questionId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_CHOICEID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portlet.polls.model.PollsChoice.choiceId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_DESCRIPTION = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portlet.polls.model.PollsChoice.description"),
			XSS_ALLOW_BY_MODEL);
	public static long LOCK_EXPIRATION_TIME = GetterUtil.getLong(PropsUtil.get(
				"lock.expiration.time.com.liferay.portlet.polls.model.PollsChoiceModel"));

	public PollsChoiceModel() {
	}

	public PollsChoiceModel(PollsChoicePK pk) {
		_questionId = pk.questionId;
		_choiceId = pk.choiceId;
		setNew(true);
	}

	public PollsChoiceModel(String questionId, String choiceId,
		String description) {
		_questionId = questionId;
		_choiceId = choiceId;
		_description = description;
	}

	public PollsChoicePK getPrimaryKey() {
		return new PollsChoicePK(_questionId, _choiceId);
	}

	public String getQuestionId() {
		return _questionId;
	}

	public void setQuestionId(String questionId) {
		if (((questionId == null) && (_questionId != null)) ||
				((questionId != null) && (_questionId == null)) ||
				((questionId != null) && (_questionId != null) &&
				!questionId.equals(_questionId))) {
			if (!XSS_ALLOW_QUESTIONID) {
				questionId = Xss.strip(questionId);
			}

			_questionId = questionId;
			setModified(true);
		}
	}

	public String getChoiceId() {
		return _choiceId;
	}

	public void setChoiceId(String choiceId) {
		if (((choiceId == null) && (_choiceId != null)) ||
				((choiceId != null) && (_choiceId == null)) ||
				((choiceId != null) && (_choiceId != null) &&
				!choiceId.equals(_choiceId))) {
			if (!XSS_ALLOW_CHOICEID) {
				choiceId = Xss.strip(choiceId);
			}

			_choiceId = choiceId;
			setModified(true);
		}
	}

	public String getDescription() {
		return _description;
	}

	public void setDescription(String description) {
		if (((description == null) && (_description != null)) ||
				((description != null) && (_description == null)) ||
				((description != null) && (_description != null) &&
				!description.equals(_description))) {
			if (!XSS_ALLOW_DESCRIPTION) {
				description = Xss.strip(description);
			}

			_description = description;
			setModified(true);
		}
	}

	public BaseModel getProtected() {
		return null;
	}

	public void protect() {
	}

	public Object clone() {
		return new PollsChoice(getQuestionId(), getChoiceId(), getDescription());
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		PollsChoice pollsChoice = (PollsChoice)obj;
		int value = 0;
		value = getChoiceId().compareTo(pollsChoice.getChoiceId());

		if (value != 0) {
			return value;
		}

		return 0;
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		PollsChoice pollsChoice = null;

		try {
			pollsChoice = (PollsChoice)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		PollsChoicePK pk = pollsChoice.getPrimaryKey();

		if (getPrimaryKey().equals(pk)) {
			return true;
		}
		else {
			return false;
		}
	}

	public int hashCode() {
		return getPrimaryKey().hashCode();
	}

	private String _questionId;
	private String _choiceId;
	private String _description;
}