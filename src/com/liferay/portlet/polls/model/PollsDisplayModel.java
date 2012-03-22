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
import com.liferay.portlet.polls.ejb.PollsDisplayPK;
import com.liferay.util.GetterUtil;
import com.liferay.util.Xss;

/**
 * <a href="PollsDisplayModel.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.9 $
 *
 */
public class PollsDisplayModel extends BaseModel {
	public static boolean CACHEABLE = GetterUtil.get(PropsUtil.get(
				"value.object.cacheable.com.liferay.portlet.polls.model.PollsDisplay"),
			VALUE_OBJECT_CACHEABLE);
	public static int MAX_SIZE = GetterUtil.get(PropsUtil.get(
				"value.object.max.size.com.liferay.portlet.polls.model.PollsDisplay"),
			VALUE_OBJECT_MAX_SIZE);
	public static boolean XSS_ALLOW_BY_MODEL = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portlet.polls.model.PollsDisplay"),
			XSS_ALLOW);
	public static boolean XSS_ALLOW_LAYOUTID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portlet.polls.model.PollsDisplay.layoutId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_USERID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portlet.polls.model.PollsDisplay.userId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_PORTLETID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portlet.polls.model.PollsDisplay.portletId"),
			XSS_ALLOW_BY_MODEL);
	public static boolean XSS_ALLOW_QUESTIONID = GetterUtil.get(PropsUtil.get(
				"xss.allow.com.liferay.portlet.polls.model.PollsDisplay.questionId"),
			XSS_ALLOW_BY_MODEL);
	public static long LOCK_EXPIRATION_TIME = GetterUtil.getLong(PropsUtil.get(
				"lock.expiration.time.com.liferay.portlet.polls.model.PollsDisplayModel"));

	public PollsDisplayModel() {
	}

	public PollsDisplayModel(PollsDisplayPK pk) {
		_userId = pk.userId;
		_portletId = pk.portletId;
		setNew(true);
	}

	public PollsDisplayModel(String userId, String portletId,
		String questionId) {
		_userId = userId;
		_portletId = portletId;
		_questionId = questionId;
	}

	public PollsDisplayPK getPrimaryKey() {
		return new PollsDisplayPK(_userId, _portletId);
	}

	public String getUserId() {
		return _userId;
	}

	public void setUserId(String userId) {
		if (((userId == null) && (_userId != null)) ||
				((userId != null) && (_userId == null)) ||
				((userId != null) && (_userId != null) &&
				!userId.equals(_userId))) {
			if (!XSS_ALLOW_USERID) {
				userId = Xss.strip(userId);
			}

			_userId = userId;
			setModified(true);
		}
	}

	public String getPortletId() {
		return _portletId;
	}

	public void setPortletId(String portletId) {
		if (((portletId == null) && (_portletId != null)) ||
				((portletId != null) && (_portletId == null)) ||
				((portletId != null) && (_portletId != null) &&
				!portletId.equals(_portletId))) {
			if (!XSS_ALLOW_PORTLETID) {
				portletId = Xss.strip(portletId);
			}

			_portletId = portletId;
			setModified(true);
		}
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

	public BaseModel getProtected() {
		return null;
	}

	public void protect() {
	}

	public Object clone() {
		return new PollsDisplay(getUserId(), getPortletId(), getQuestionId());
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return -1;
		}

		PollsDisplay pollsDisplay = (PollsDisplay)obj;
		PollsDisplayPK pk = pollsDisplay.getPrimaryKey();

		return getPrimaryKey().compareTo(pk);
	}

	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		PollsDisplay pollsDisplay = null;

		try {
			pollsDisplay = (PollsDisplay)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		PollsDisplayPK pk = pollsDisplay.getPrimaryKey();

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

	private String _layoutId;
	private String _userId;
	private String _portletId;
	private String _questionId;
}