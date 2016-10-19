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
 * <a href="PollsVoteManager.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.51 $
 *
 */
public interface PollsVoteManager {
	public int countByVoteDate(java.lang.String companyId,
		java.util.Date voteDate, boolean before)
		throws com.liferay.portal.SystemException, java.rmi.RemoteException;

	public com.liferay.portlet.polls.model.PollsVote getVote(
		com.liferay.portlet.polls.ejb.PollsVotePK pk)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException, java.rmi.RemoteException;

	public com.liferay.portlet.polls.model.PollsVote getVote(
		java.lang.String questionId)
		throws com.liferay.portal.PortalException, 
			com.liferay.portal.SystemException, java.rmi.RemoteException;

	public int getVotesSize(java.lang.String questionId)
		throws com.liferay.portal.SystemException, java.rmi.RemoteException;

	public int getVotesSize(java.lang.String questionId,
		java.lang.String choiceId)
		throws com.liferay.portal.SystemException, java.rmi.RemoteException;
}