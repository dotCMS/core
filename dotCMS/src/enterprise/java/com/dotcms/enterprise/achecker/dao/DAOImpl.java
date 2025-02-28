/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.dao;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import com.dotcms.enterprise.achecker.utility.Utility;



/**
 * Root data access object
 * Each table has a DAO class, all inherits from this class
 * @access	public
 
 * @package	DAO
 */

public class DAOImpl implements DAO {

	private static final Log LOG = org.apache.commons.logging.LogFactory.getLog(DAOImpl.class);

	public DAOImpl() {
	}

	/**
	 * Execute SQL
	 * @access  protected
	 * @param   $sql : SQL statment to be executed
	 * @return  $rows: for 'select' sql, return retrived rows, 
	 *          true:  for non-select sql
	 *          false: if fail
	 * @author  Cindy Qi Li
	 * @throws SQLException 
	 */
	public List<Map<String, Object>> execute(String sql) throws SQLException {
		Statement st = null;
		try {
			sql = sql.trim();
			Connection conn  = ACheckerConnectionFactory.getConnection();
			st = conn.createStatement();
			ResultSet result = st.executeQuery(sql);
			List<Map<String, Object>> list = getObjects( result );
			st.close();
			return list;			
		} catch (Exception e) {
			LOG.error(e.getMessage() , e );
			throw new SQLException(e);
		}
	}

	private List<Map<String, Object>> getObjects( ResultSet result  ) throws SQLException{

		List<Map<String, Object>> list = new ArrayList<>();

		while (result.next()) {

			Map<String, Object> item = new HashMap<>();

			for ( int i = 1; i <= result.getMetaData().getColumnCount(); i ++ ) {

				String name = null;
				Object value = null;

				try {
					name = result.getMetaData().getColumnName(i);
					
					// FIX to work with h2db
					name = name.toLowerCase();
					
					value = result.getObject(i);

					if ( value instanceof Clob ) {
						String content = Utility.getClobContent((Clob) value);
						item.put(name, content);
					}
					else {
						if ( value instanceof String ) {
							String content = (String) value;
							item.put(name, content);
						}
						else {
							item.put(name, value);
						}
					}
				}
				catch (Exception t) {
					//LOG.error(t.getMessage() , t );
				}
			}
			list.add(item);
		}			
		return list;
	}
}
