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

import com.dotcms.enterprise.achecker.dao.DAOImpl;


/**
* DAO for "subgroup_checks" table
* @access	public

* @package	DAO
*/

public class SubgroupChecksDAO extends DAOImpl {

	public SubgroupChecksDAO() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
	}

	/**
	* Create a new entry of subgroup_id <=> check_id relationship
	* @access  public
	* @param   $groupID : guideline subgroup id
	*          $checkID : check ID
	* @return  true : if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	*/
//	public function Create($subgroupID, $checkID)
//	{
//		$subgroupID = intval($subgroupID);
//		$checkID = intval($checkID);
//		
//		$sql = "INSERT INTO ".TABLE_PREFIX."subgroup_checks
//				(`subgroup_id`, `check_id`) 
//				VALUES
//				(".$subgroupID.",".$checkID.")";
//
//		if (!$this->execute($sql))
//		{
//			$msg->addError('DB_NOT_UPDATED');
//			return false;
//		}
//		else
//		{
//			return true;
//		}
//	}
	
	/**
	* Delete given check, identified by check ID, from given guideline
	* @access  public
	* @param   $type: "guideline", "group", "subgroup"
	*          $typeID : guideline id if type = "guideline"
	*                    group id if type = "group"
	*                    subgroup id if type = "subgroup"
	*          $checkID : check ID to delete
	* @return  true : if successful
	*          false : if unsuccessful
	* @author  Cindy Qi Li
	*/
//	public function deleteChecksByTypeAndID($type, $typeID, $checkID)
//	{
//		$typeID = intval($typeID);
//		$checkID = intval($checkID);
//		
//		if ($type == "guideline")
//		{
//			$sql = "DELETE FROM ".TABLE_PREFIX."subgroup_checks
//			         WHERE subgroup_id in (SELECT distinct subgroup_id 
//			                                 FROM ".TABLE_PREFIX."guideline_groups gg, "
//			                                       .TABLE_PREFIX."guideline_subgroups gs
//			                                 WHERE gg.guideline_id=".$typeID."
//			                                   AND gg.group_id = gs.group_id)
//			           AND check_id = ".$checkID;
//		}
//		
//		if ($type == "group")
//		{
//			$sql = "DELETE FROM ".TABLE_PREFIX."subgroup_checks
//			         WHERE subgroup_id in (SELECT distinct subgroup_id 
//			                                 FROM ".TABLE_PREFIX."guideline_groups gg, "
//			                                       .TABLE_PREFIX."guideline_subgroups gs
//			                                 WHERE gg.group_id=".$typeID."
//			                                   AND gg.group_id = gs.group_id)
//			           AND check_id = ".$checkID;
//		}
//		
//		if ($type == "subgroup")
//		{
//			$sql = "DELETE FROM ".TABLE_PREFIX."subgroup_checks
//		             WHERE subgroup_id = ".$typeID."
//		               AND check_id = ".$checkID;
//		}
//		
//		return $this->execute($sql);
//	}
	
	/**
	* Delete all entries with given check id
	* @access  public
	* @param   $checkID
	* @return  true : if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	*/
//	public function DeleteByCheckID($checkID)
//	{
//		$checkID = intval($checkID);
//		$sql = "DELETE FROM ".TABLE_PREFIX."subgroup_checks
//				WHERE check_id = ".$checkID;
//
//		return $this->execute($sql);
//	}
	
	/**
	* Delete all entries with given subgroup id
	* @access  public
	* @param   $subgroupID
	* @return  no returns
	*          Note that this function is called by GuidelineGroupsDAO->DeleteByGroupID, 
	*          return true or false from this function forces the caller return too.
	* @author  Cindy Qi Li
	*/
//	public function DeleteBySubgroupID($subgroupID)
//	{
//		$subgroupID = intval($subgroupID);
//		
//		$sql = "DELETE FROM ".TABLE_PREFIX."subgroup_checks
//				WHERE subgroup_id = ".$subgroupID;
//
//		return $this->execute($sql);
//	}
	
}
