/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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


