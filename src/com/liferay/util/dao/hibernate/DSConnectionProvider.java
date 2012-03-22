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

package com.liferay.util.dao.hibernate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.connection.ConnectionProvider;

import com.dotmarketing.db.DbConnectionFactory;

/**
 * <a href="DSConnectionProvider.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */
public class DSConnectionProvider implements ConnectionProvider {

	public void configure(Properties props) throws HibernateException {

		try {
			_ds = DbConnectionFactory.getDataSource();
		}
		catch (Exception e) {
			throw new HibernateException(e.getMessage());
		}
	}

	public Connection getConnection() throws SQLException {
		//This forces liferay to use our connection manager
		return DbConnectionFactory.getConnection();
	}

	public void closeConnection(Connection con) throws SQLException {
		//This condition is set to avoid closing connection when in middle of a transaction
		if(con != null && con.getAutoCommit())
			DbConnectionFactory.closeConnection();
	}

	public boolean isStatementCache() {
		return false;
	}

	public void close() {
	}

	private DataSource _ds;

}