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

package com.liferay.util.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.NamingException;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;

/**
 * <a href="DataAccess.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.7 $
 *
 */
public class DataAccess {

	public static Connection getConnection(String location)
		throws NamingException, SQLException {

		//This forces liferay to use our connection manager
		Connection con = DbConnectionFactory.getConnection();

		return con;
	}

	public static void cleanUp(Connection con) {
		cleanUp(con, null, null);
	}

	public static void cleanUp(Connection con, Statement s) {
		cleanUp(con, s, null);
	}

	public static void cleanUp(Connection con, Statement s, ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		}
		catch (SQLException sqle) {
			Logger.error(DataAccess.class,sqle.getMessage(),sqle);
		}

		try {
			if (s != null) {
				s.close();
			}
		}
		catch (SQLException sqle) {
			Logger.error(DataAccess.class,sqle.getMessage(),sqle);
		}

		try {
			//This condition is required if the connection is in the middle of a transaction then can't be closed
			//however started the transaction is responsible to close the connection
			if (con != null && con.getAutoCommit()) {
				DbConnectionFactory.closeConnection();
			}
		}
		catch (SQLException sqle) {
			Logger.error(DataAccess.class,sqle.getMessage(),sqle);
		}
	}

}