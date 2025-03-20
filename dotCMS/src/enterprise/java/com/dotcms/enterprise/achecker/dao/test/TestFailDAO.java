/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.dao.test;


import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.achecker.dao.BaseDAO;


/**
 * DAO for "test_fail" table
 * @access	public
 
 * @package	DAO
 */

public class TestFailDAO extends BaseDAO {

	public TestFailDAO() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
	}

	/**
	 * Return check info of given check id
	 * @access  public
	 * @param   $checkID : check id
	 * @return  table rows
	 * @author  Cindy Qi Li
	 */
	public List<Map<String, Object>>  getFailStepsByID(int checkID) throws SQLException
	{
		//		$checkID = intval($checkID);
		String sql = "SELECT step_id, step 	FROM "+ tablePrefix+"test_fail WHERE check_id="+checkID +" 	ORDER BY step_id";

		return execute(sql);
	}

}
