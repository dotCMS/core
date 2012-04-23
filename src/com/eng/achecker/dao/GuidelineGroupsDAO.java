package com.eng.achecker.dao;


import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.eng.achecker.utility.Utility;


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
* DAO for "guideline_groups" table
* @access	public
* @author	Cindy Qi Li
* @package	DAO
*/

public class GuidelineGroupsDAO extends BaseDAO {

	public GuidelineGroupsDAO() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
	}
	
	
	/**
	* Return group info of the given group id
	* @access  public
	* @param   $groupID : group id
	* @return  table row: if success
	*          false : if fail
	* @author  Cindy Qi Li
	 * @throws SQLException 
	*/
	public Map<String, Object> getGroupByID(Long groupID) throws SQLException
	{
		String sql = "SELECT * FROM " + tablePrefix + "guideline_groups WHERE group_id = " + groupID;
		return executeOne(sql);
	}

	/**
	* Return group info of the given check id and guideline id
	* @access  public
	* @param   $checkID : check id
	*          $guidelineID: guideline id
	* @return  table row: if success
	*          false : if fail
	* @author  Cindy Qi Li
	 * @throws SQLException 
	*/
	public Map<String, Object> getGroupByCheckIDAndGuidelineID(Long checkID, Long guidelineID) throws SQLException
	{
		String sql = "SELECT * FROM " + tablePrefix + "guideline_groups ";
		sql += "WHERE group_id in (SELECT gs.group_id ";
		sql += "FROM " + tablePrefix + "guideline_subgroups gs, ";
		sql += "" + tablePrefix + "subgroup_checks sc ";
		sql += "WHERE gs.subgroup_id = sc.subgroup_id AND sc.check_id=" + checkID + ") ";
		sql += "AND guideline_id = " + guidelineID;

		return executeOne(sql);
	}

	/**
	* Return array of groups info whose name is NOT null, and belong to the given guideline id
	* @access  public
	* @param   $guidelineID : guideline id
	* @return  group id rows : array of group ids, if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	 * @throws SQLException 
	*/
	public List<Map<String, Object>> getNamedGroupsByGuidelineID(Long guidelineID, String languageCode) throws SQLException
	{
		String sql = "SELECT * FROM " + tablePrefix + "guideline_groups gg, " + tablePrefix + "language_text l";
        sql += " WHERE gg.guideline_id = " + guidelineID;
        sql += " AND gg.name is not NULL ";
        sql += " AND gg.name = l.term ";
        sql += " AND l.language_code = '" + languageCode + "' ORDER BY l.text";

		return Utility.sortArrayByNumInField(execute(sql), "text");
	}

	/**
	* Return array of groups info whose name is null, and belong to the given guideline id
	* @access  public
	* @param   $guidelineID : guideline id
	* @return  group id rows : array of group ids, if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	 * @throws SQLException 
	*/
	public List<Map<String, Object>> getUnnamedGroupsByGuidelineID(Long guidelineID) throws SQLException
	{
		String sql = "SELECT * FROM " + tablePrefix + "guideline_groups ";
        sql += "WHERE guideline_id = " + guidelineID + " AND name is NULL";
		return execute(sql);
	}
	

	/**
	* Create a new guideline group
	* @access  public
	* @param   $guidelineID : guideline id
	*          $name
	*          $abbr
	*          $principle
	* @return  group_id : if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	*/
//	public function Create(Integer guidelineID, String name, String abbr, String principle)
//	{
//		global $addslashes;
//		
//		name = name.trim();	// $addslashes is not necessary as it's called in LanguageTextDAO->Create()
//		abbr = $addslashes(abbr.trim());
//		principle = $addslashes(trim($principle));
//		
//		$sql = "INSERT INTO ".TABLE_PREFIX."guideline_groups
//				(`guideline_id`, `abbr`, `principle`) 
//				VALUES
//				(".$guidelineID.", '".$abbr."', '".$principle."')";
//
//		if (!$this->execute($sql))
//		{
//			$msg->addError('DB_NOT_UPDATED');
//			return false;
//		}
//		else
//		{
//			$group_id = mysql_insert_id();
//
//			if ($name <> '')
//			{
//				$term = LANG_PREFIX_GUIDELINE_GROUPS_NAME.$group_id;
//				
//				require_once(AC_INCLUDE_PATH.'classes/DAO/LanguageTextDAO.class.php');
//				$langTextDAO = new LanguageTextDAO();
//				
//				if ($langTextDAO->Create($_SESSION['lang'], '_guideline',$term,$name,''))
//				{
//					$sql = "UPDATE ".TABLE_PREFIX."guideline_groups 
//					           SET name='".$term."' WHERE group_id=".$group_id;
//					$this->execute($sql);
//				}
//			}
//			return $group_id;
//		}
//	}
	
	/**
	* Update an existing guideline group
	* @access  public
	* @param   $groupID : group id
	*          $name
	*          $abbr
	*          $principle
	* @return  true : if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	*/
//	public function Update($groupID, $name, $abbr, $principle)
//	{
//		global $addslashes;
//		
//		$groupID = intval($groupID);
//		$name = trim($name);	// $addslashes is not necessary as it's called in LanguageTextDAO->updateLang()
//		$abbr = $addslashes(trim($abbr));
//		$principle = $addslashes(trim($principle));
//		
//		$sql = "UPDATE ".TABLE_PREFIX."guideline_groups
//				   SET abbr='".$abbr."', 
//				       principle = '".$principle."' 
//				 WHERE group_id = ".$groupID;
//
//		if (!$this->execute($sql))
//		{
//			$msg->addError('DB_NOT_UPDATED');
//			return false;
//		}
//		else
//		{
//			if ($name <> '')
//			{
//				$term = LANG_PREFIX_GUIDELINE_GROUPS_NAME.$groupID;
//				$this->updateLang($groupID, $term, $name, 'name');
//			}
//		}
//	}
	
	/**
	* Delete all entries of given group ID
	* @access  public
	* @param   $groupID : group id
	* @return  true : if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	*/
//	public function Delete($groupID)
//	{
//		require_once(AC_INCLUDE_PATH.'classes/DAO/GuidelineSubgroupsDAO.class.php');
//		
//		$groupID = intval($groupID);
//		
//		// Delete all subgroups
//		$guidelineSubgroupsDAO = new GuidelineSubgroupsDAO();
//		$sql = "SELECT subgroup_id FROM ".TABLE_PREFIX."guideline_subgroups
//		         WHERE group_id = ".$groupID;
//		$rows = $this->execute($sql);
//		
//		if (is_array($rows))
//		{
//			foreach ($rows as $row)
//				$guidelineSubgroupsDAO->Delete($row['subgroup_id']);
//		}
//		
//		// delete language for group name
//		$sql = "DELETE FROM ".TABLE_PREFIX."language_text 
//		         WHERE variable='_guideline' 
//		           AND term=(SELECT name 
//		                       FROM ".TABLE_PREFIX."guideline_groups
//		                      WHERE group_id=".$groupID.")";
//		$this->execute($sql);
//			
//		// delete guideline_groups
//		$sql = "DELETE FROM ".TABLE_PREFIX."guideline_groups WHERE group_id=".$groupID;
//			
//		return $this->execute($sql);
//	}

	/**
	* Add checks into guideline group
	* @access  public
	* @param   $groupID : guideline group id
	*          $cids : array of check ids to be added into guideline group
	* @return  true : if successful
	*          false : if unsuccessful
	* @author  Cindy Qi Li
	*/
//	public function addChecks($groupID, $cids)
//	{
//		require_once(AC_INCLUDE_PATH.'classes/DAO/GuidelineSubgroupsDAO.class.php');
//		require_once(AC_INCLUDE_PATH.'classes/DAO/SubgroupChecksDAO.class.php');
//		
//		$groupID = intval($groupID);
//		if ($groupID == 0)
//		{
//			$msg->addError('MISSING_GID');
//			return false;
//		}
//		
//		$guidelineSubgroupsDAO = new GuidelineSubgroupsDAO();
//		$subgroups = $guidelineSubgroupsDAO->getUnnamedSubgroupByGroupID($groupID);
//		
//		if (is_array($subgroups))
//			$subgroup_id = $subgroups[0]['subgroup_id'];
//		else
//			$subgroup_id = $guidelineSubgroupsDAO->Create($groupID, '','');
//		
//		if ($subgroup_id)
//		{
//			$subgroupChecksDAO = new SubgroupChecksDAO();
//			
//			if (is_array($cids))
//			{
//				foreach ($cids as $cid) {
//					$cid = intval($cid);
//					
//					if ($cid > 0) {
//						$subgroupChecksDAO->Create($subgroup_id, $cid);
//					}
//				}
//			}
//		}
//		else return false;
//		
//		return true;
//	}

	/**
	 * insert/update guideline group term into language_text and update according record in table "guideline_groups"
	 * @access  private
	 * @param   $groupID
	 *          $term      : term to create/update into 'language_text' table
	 *          $text      : text to create/update into 'language_text' table
	 *          $fieldName : field name in table 'guideline_groups' to update
	 * @return  true    if update successfully
	 *          false   if update unsuccessful
	 * @author  Cindy Qi Li
	 */
//	private function updateLang($groupID, $term, $text, $fieldName)
//	{
//		global $addslashes;
//		
//		require_once(AC_INCLUDE_PATH.'classes/DAO/LanguageTextDAO.class.php');
//		$langTextDAO = new LanguageTextDAO();
//		$langs = $langTextDAO->getByTermAndLang($term, $_SESSION['lang']);
//
//		if (is_array($langs))
//		{// term already exists. Only need to update modified text
//			if ($langs[0]['text'] <> $addslashes($text)) $langTextDAO->setText($_SESSION['lang'], '_guideline',$term,$text);
//		}
//		else
//		{
//			$langTextDAO->Create($_SESSION['lang'], '_guideline',$term,$text,'');
//			
//			$sql = "UPDATE ".TABLE_PREFIX."guideline_groups SET ".$fieldName."='".$term."' WHERE group_id=".$groupID;
//			$this->execute($sql);
//		}
//		
//		return true;
//	}
	
}

