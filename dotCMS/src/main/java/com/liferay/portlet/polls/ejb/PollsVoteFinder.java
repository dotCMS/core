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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.liferay.portal.SystemException;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.polls.model.PollsVote;
import com.liferay.util.dao.DataAccess;

/**
 * <a href="PollsVoteFinder.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.12 $
 *
 */
public class PollsVoteFinder {

	/**
	 * Count all PollsVotes associated with the specified company id created
	 * before or after the specified date.
	 *
	 * @return		the count of all PollsVotes associated with the specified
	 *				company id before or after the specified date
	 */
	protected static int countByVoteDate(
			String companyId, Date voteDate, boolean before)
		throws SystemException {

		return findByCountDate(
			companyId, new Timestamp(voteDate.getTime()), before);
	}

	/**
	 * Count all PollsVotes associated with the specified company id created
	 * before or after the specified date.
	 *
	 * @return		the count of all PollsVotes associated with the specified
	 *				company id before or after the specified date
	 */
	protected static int findByCountDate(
			String companyId, Timestamp voteDate, boolean before)
		throws SystemException {

		int count = 0;

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getConnection(Constants.DATA_SOURCE);

			StringBuffer query = new StringBuffer();
			query.append("SELECT questionId ");
			query.append("FROM PollsQuestion WHERE ");
			query.append("companyId = ?");

			ps = con.prepareStatement(query.toString());

			ps.setString(1, companyId);

			rs = ps.executeQuery();

			List questions = new ArrayList();

			while (rs.next()) {
				questions.add(rs.getString(1));

			}

			Iterator itr = questions.iterator();

			while (itr.hasNext()) {
				String questionId = (String)itr.next();

				String comparator = ">";
				if (before) {
					comparator = "<";
				}

				query = new StringBuffer();
				query.append("SELECT COUNT(*) ");
				query.append("FROM PollsVote WHERE ");
				query.append("questionId = ? AND ");
				query.append("voteDate ").append(comparator).append(" ?");

				ps = con.prepareStatement(query.toString());

				ps.setString(1, questionId);
				ps.setTimestamp(2, voteDate);

				rs = ps.executeQuery();

				while (rs.next()) {
					count += rs.getInt(1);
				}
			}
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}

		return count;
	}

	/**
	 * Find all PollsVotes associated with the specified company id created
	 * before or after the specified date.
	 *
	 * @return		a list of all PollsVotes associated with the specified
	 *				company id before or after the specified date
	 */
	protected static List findByVoteDate(
			String companyId, Date voteDate, boolean before)
		throws SystemException {

		return findByVoteDate(
			companyId, new Timestamp(voteDate.getTime()), before);
	}

	/**
	 * Find all PollsVotes associated with the specified company id created
	 * before or after the specified date.
	 *
	 * @return		a list of all PollsVotes associated with the specified
	 *				company id before or after the specified date
	 */
	protected static List findByVoteDate(
			String companyId, Timestamp voteDate, boolean before)
		throws SystemException {

		List list = new ArrayList();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getConnection(Constants.DATA_SOURCE);

			StringBuffer query = new StringBuffer();
			query.append("SELECT questionId ");
			query.append("FROM PollsQuestion WHERE ");
			query.append("companyId = ?");

			ps = con.prepareStatement(query.toString());

			ps.setString(1, companyId);

			rs = ps.executeQuery();

			List questions = new ArrayList();

			while (rs.next()) {
				questions.add(rs.getString(1));

			}

			Iterator itr = questions.iterator();

			while (itr.hasNext()) {
				String questionId = (String)itr.next();

				String comparator = ">";
				if (before) {
					comparator = "<";
				}

				query = new StringBuffer();
				query.append("SELECT userId ");
				query.append("FROM PollsVote WHERE ");
				query.append("questionId = ? AND ");
				query.append("voteDate ").append(comparator).append(" ?");

				ps = con.prepareStatement(query.toString());

				ps.setString(1, questionId);
				ps.setTimestamp(2, voteDate);

				rs = ps.executeQuery();

				while (rs.next()) {
					PollsVote vote = PollsVoteUtil.findByPrimaryKey(
						new PollsVotePK(questionId, rs.getString(1)));

					list.add(vote);
				}
			}
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}

		return list;
	}

}