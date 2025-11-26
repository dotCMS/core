/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.achecker.dao.BaseDAO;



public class CheckPrerequisitesDAO extends BaseDAO {

	public CheckPrerequisitesDAO() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
	}

	/**
	* Create a new entry
	* @access  public
	* @param   $checkID
	*          $prerequisiteCheckID
	* @return  created row : if successful
	*          false : if not successful

	*/
//	public function Create($checkID, $prerequisiteCheckID)
//	{
//		$sql = "INSERT INTO ".TABLE_PREFIX."check_prerequisites (check_id, prerequisite_check_id) 
//		        VALUES (".intval($checkID).", ".intval($prerequisiteCheckID).")";
//		return $this->execute($sql);
//	}
	
	/**
	* Delete by primary key
	* @access  public
	* @param   $checkID
	*          $prerequisiteCheckID
	* @return  true : if successful
	*          false : if unsuccessful

	*/
//	public function Delete($checkID, $prerequisiteCheckID)
//	{
//		$sql = "DELETE FROM ".TABLE_PREFIX."check_prerequisites 
//		         WHERE check_id=".intval($checkID)." AND prerequisite_check_id=".intval($prerequisiteCheckID);
//		return $this->execute($sql);
//	}
	
	/**
	* Delete prerequisites by given check ID
	* @access  public
	* @param   $checkID
	* @return  true : if successful
	*          false : if unsuccessful

	*/
//	public function DeleteByCheckID($checkID)
//	{
//		$sql = "DELETE FROM ".TABLE_PREFIX."check_prerequisites WHERE check_id=".intval($checkID);
//		return $this->execute($sql);
//	}
	
	/**
	* Return prerequisite check IDs by given check ID
	* @access  public
	* @param   $checkID
	* @return  table rows : if successful
	*          false : if unsuccessful

	 * @throws SQLException 
	*/
	public List<Map<String, Object>> getPreChecksByCheckID(Long checkID) throws SQLException
	{
		String sql = "SELECT * FROM " + tablePrefix + "checks ";
		sql += "         WHERE check_id in (SELECT prerequisite_check_id ";
		sql += "                              FROM " + tablePrefix + "check_prerequisites ";
		sql += "                             WHERE check_id=" + checkID + ")";

		return execute(sql);
	}
	
}
