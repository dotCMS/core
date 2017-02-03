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


/**
 * <a href="PollsDisplayHBM.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class PollsDisplayHBM {
	protected PollsDisplayHBM() {
	}

	protected PollsDisplayHBM(PollsDisplayPK pk) {
		_userId = pk.userId;
		_portletId = pk.portletId;
	}

	protected PollsDisplayHBM(String userId, String portletId,
		String questionId) {
		_userId = userId;
		_portletId = portletId;
		_questionId = questionId;
	}

	public PollsDisplayPK getPrimaryKey() {
		return new PollsDisplayPK(_userId, _portletId);
	}

	protected void setPrimaryKey(PollsDisplayPK pk) {
		_userId = pk.userId;
		_portletId = pk.portletId;
	}

	protected String getUserId() {
		return _userId;
	}

	protected void setUserId(String userId) {
		_userId = userId;
	}

	protected String getPortletId() {
		return _portletId;
	}

	protected void setPortletId(String portletId) {
		_portletId = portletId;
	}

	protected String getQuestionId() {
		return _questionId;
	}

	protected void setQuestionId(String questionId) {
		_questionId = questionId;
	}

	private String _userId;
	private String _portletId;
	private String _questionId;
}