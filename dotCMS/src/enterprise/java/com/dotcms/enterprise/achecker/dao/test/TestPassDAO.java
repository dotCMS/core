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
* DAO for "test_pass" table
* @access	public

* @package	DAO
*/

 
public class TestPassDAO extends BaseDAO {
	
	public TestPassDAO() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
	}
//	/**
//	* Create a new entry
//	* @access  public
//	* @param   $checkID
//	*          $nextCheckID
//	* @return  created row : if successful
//	*          false : if not successful
//	* @author  Cindy Qi Li
//	*/
//	public function Create($checkID, $nextCheckID)
//	{
//		$checkID = intval($checkID);
//		$nextCheckID = intval($nextCheckID);
//		
//		$sql = "INSERT INTO ".TABLE_PREFIX."test_pass (check_id, next_check_id) 
//		        VALUES (".$checkID.", ".$nextCheckID.")";
//		return $this->execute($sql);
//	}
//	
//	/**
//	* Delete by primary key
//	* @access  public
//	* @param   $checkID
//	*          $nextCheckID
//	* @return  true : if successful
//	*          false : if unsuccessful
//	* @author  Cindy Qi Li
//	*/
//	public function Delete($checkID, $nextCheckID)
//	{
//		$checkID = intval($checkID);
//		$nextCheckID = intval($nextCheckID);
//		
//		$sql = "DELETE FROM ".TABLE_PREFIX."test_pass 
//		         WHERE check_id=".$checkID." AND next_check_id=".$nextCheckID;
//		return $this->execute($sql);
//	}
//	
//	/**
//	* Delete next checks by given check ID
//	* @access  public
//	* @param   $checkID
//	* @return  true : if successful
//	*          false : if unsuccessful
//	* @author  Cindy Qi Li
//	*/
//	public function DeleteByCheckID($checkID)
//	{
//		$checkID = intval($checkID);
//		
//		$sql = "DELETE FROM ".TABLE_PREFIX."test_pass WHERE check_id=".$checkID;
//		return $this->execute($sql);
//	}
	
	/**
	* Return next check IDs by given check ID
	* @access  public
	* @param   $checkID
	* @return  table rows : if successful
	*          false : if unsuccessful
	* @author  Cindy Qi Li
	*/
	public List<Map<String, Object>> getNextChecksByCheckID(int checkID)   throws SQLException
	{
//		$checkID = intval($checkID);
		
		String sql = "SELECT * FROM "+tablePrefix+"checks WHERE check_id in (SELECT next_check_id FROM "+tablePrefix+"test_pass   WHERE check_id="+checkID+")"; 
		return execute(sql);
	}
	
}
