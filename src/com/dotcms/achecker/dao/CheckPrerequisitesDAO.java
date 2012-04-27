package com.dotcms.achecker.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.dotcms.achecker.dao.BaseDAO;

/************************************************************************/
/* ACheckerImplImpl                                                             */
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
* DAO for "check_prerequisites" table
* @access	public
* @author	Cindy Qi Li
* @package	DAO
*/

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
	* @author  Cindy Qi Li
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
	* @author  Cindy Qi Li
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
	* @author  Cindy Qi Li
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
	* @author  Cindy Qi Li
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

