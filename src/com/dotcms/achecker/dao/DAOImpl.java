package com.dotcms.achecker.dao;

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

import com.dotcms.achecker.utility.Utility;


/************************************************************************/
/* ACheckerImpl                                                             */
/************************************************************************/
/* Copyright (c) 2008 - 2011                                            */
/* Inclusive Design Institute                                           */
/*                                                                      */
/* This program is free software. You can redistribute it and/or        */
/* modify it under the terms of the GNU General Public License          */
/* as published by the Free Software Foundation.                        */
/************************************************************************/
// $Id$

/**
 * Root data access object
 * Each table has a DAO class, all inherits from this class
 * @access	public
 * @author	Cindy Qi Li
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

		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		while (result.next()) {

			Map<String, Object> item = new HashMap<String, Object>();

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


