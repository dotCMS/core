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

