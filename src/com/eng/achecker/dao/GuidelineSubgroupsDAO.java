package com.eng.achecker.dao;


import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

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
* DAO for "guideline_subgroups" table
* @access	public
* @author	Cindy Qi Li
* @package	DAO
*/

public class GuidelineSubgroupsDAO extends BaseDAO {
	
	private static final Log LOG = org.apache.commons.logging.LogFactory.getLog( GuidelinesDAO.class);
	
	public GuidelineSubgroupsDAO() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
	}

	/**
	* Create a new guideline subgroup
	* @access  public
	* @param   $groupID : guideline group id
	*          $name
	*          $abbr
	* @return  subgroup_id : if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	*/
//	public function Create($groupID, $name, $abbr)
//	{
//		global $addslashes;
//		
//		$groupID = intval($groupID);
//		$name = trim($name);	// $addslashes is not necessary as it's called in LanguageTxetDAO->Create()
//		$abbr = $addslashes(trim($abbr));
//		
//		$sql = "INSERT INTO ".TABLE_PREFIX."guideline_subgroups
//				(`group_id`, `abbr`) 
//				VALUES
//				(".$groupID.", '".$abbr."')";
//
//		if (!$this->execute($sql))
//		{
//			$msg->addError('DB_NOT_UPDATED');
//			return false;
//		}
//		else
//		{
//			$subgroup_id = mysql_insert_id();
//
//			if ($name <> '')
//			{
//				$term = LANG_PREFIX_GUIDELINE_SUBGROUPS_NAME.$subgroup_id;
//				
//				require_once(AC_INCLUDE_PATH.'classes/DAO/LanguageTextDAO.class.php');
//				$langTextDAO = new LanguageTextDAO();
//				
//				if ($langTextDAO->Create($_SESSION['lang'], '_guideline',$term,$name,''))
//				{
//					$sql = "UPDATE ".TABLE_PREFIX."guideline_subgroups 
//					           SET name='".$term."' WHERE subgroup_id=".$subgroup_id;
//					$this->execute($sql);
//				}
//			}
//			return $subgroup_id;
//		}
//	}
	
	/**
	* Update an existing guideline subgroup
	* @access  public
	* @param   $subgroupID : subgroup id
	*          $name
	*          $abbr
	* @return  true : if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	*/
//	public function Update($subgroupID, $name, $abbr)
//	{
//		global $addslashes;
//		
//		$subgroupID = intval($subgroupID);
//		$name = trim($name);	// $addslashes is not necessary as it's called in LanguageTxetDAO->updateLang()
//		$abbr = $addslashes(trim($abbr));
//		
//		$sql = "UPDATE ".TABLE_PREFIX."guideline_subgroups
//				   SET abbr='".$abbr."' 
//				 WHERE subgroup_id = ".$subgroupID;
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
//				$term = LANG_PREFIX_GUIDELINE_SUBGROUPS_NAME.$subgroupID;
//				$this->updateLang($subgroupID, $term, $name, 'name');
//			}
//		}
//	}
	
	/**
	* Delete all entries of given subgroup ID
	* @access  public
	* @param   $subgroupID : subgroup id
	* @return  true : if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	*/
//	public function Delete($subgroupID)
//	{
//		require_once(AC_INCLUDE_PATH.'classes/DAO/SubgroupChecksDAO.class.php');
//		
//		$subgroupID = intval($subgroupID);
//		
//		// Delete all checks in this subgroup
//		$subgroupChecksDAO = new SubgroupChecksDAO();
//		if ($subgroupChecksDAO->DeleteBySubgroupID($subgroupID))
//		{
//			// delete language for subgroup name
//			$sql = "DELETE FROM ".TABLE_PREFIX."language_text 
//			         WHERE variable='_guideline' 
//			           AND term=(SELECT name 
//			                       FROM ".TABLE_PREFIX."guideline_subgroups
//			                      WHERE subgroup_id=".$subgroupID.")";
//			$this->execute($sql);
//				
//			// delete guideline_subgroups
//			$sql = "DELETE FROM ".TABLE_PREFIX."guideline_subgroups WHERE subgroup_id=".$subgroupID;
//			
//			return $this->execute($sql);
//		}
//		else
//			return false;
//	}

	/**
	* Add checks into guideline subgroup
	* @access  public
	* @param   $groupID : subgroup id
	*          $cids : array of check ids to be added into group
	* @return  true : if successful
	*          false : if unsuccessful
	* @author  Cindy Qi Li
	*/
//	public function addChecks($subgroupID, $cids)
//	{
//		global $msg;
//		
//		require_once(AC_INCLUDE_PATH.'classes/DAO/SubgroupChecksDAO.class.php');
//		
//		$subgroupID = intval($subgroupID);
//		
//		if ($subgroupID == 0)
//		{
//			$msg->addError('MISSING_GID');
//			return false;
//		}
//		
//		$subgroupChecksDAO = new SubgroupChecksDAO();
//		
//		if (is_array($cids))
//		{
//			foreach ($cids as $cid) {
//				$cid = intval($cid);
//				if ($cid > 0){
//					$subgroupChecksDAO->Create($subgroupID, $cid);
//				}
//			}
//		}
//		
//		return true;
//	}
	
	/**
	* Return subgroup info of the given group id
	* @access  public
	* @param   $subgroupID : subgroup id
	* @return  table row: if success
	*          false : if fail
	* @author  Cindy Qi Li
	 * @throws SQLException 
	*/
	public Map<String, Object> getSubgroupByID(Long subgroupID) throws SQLException
	{		
		String sql = "SELECT * FROM " + tablePrefix + "guideline_subgroups ";
        sql += "         WHERE subgroup_id = " + subgroupID;
        return executeOne(sql);
	}
	
	/**
	* Return subgroup info of the given check id and guideline id
	* @access  public
	* @param   $checkID : check id
	*          $guidelineID: guideline id
	* @return  table row: if success
	*          false : if fail
	* @author  Cindy Qi Li
	 * @throws SQLException 
	*/
	public List<Map<String, Object>> getSubgroupByCheckIDAndGuidelineID(Long checkID, Long guidelineID) throws SQLException
	{
		String sql = "SELECT * FROM " + tablePrefix + "guideline_subgroups ";
		sql += "         WHERE subgroup_id in (SELECT subgroup_id ";
		sql += "                                 FROM " + tablePrefix + "subgroup_checks";
		sql += "                                WHERE check_id=" + checkID + ")";
		sql += "           AND group_id in (SELECT group_id ";
		sql += "                                   FROM " + tablePrefix + "guideline_groups gg";
		sql += "                                   WHERE gg.guideline_id = " + guidelineID + ")";

		return execute(sql);
	}
	
	/**
	* Return array of subgroups info whose name is NOT null, and belong to the given group id
	* @access  public
	* @param   $groupID : group id
	* @return  subgroup id rows : array of subgroup ids, if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	 * @throws SQLException 
	*/
	public List<Map<String, Object>> getNamedSubgroupByGroupID(Long groupID, String languageCode) throws SQLException
	{
		String sql = "SELECT * FROM " + tablePrefix + "guideline_subgroups gs, " + tablePrefix + "language_text l";
        sql += "         WHERE gs.group_id = " + groupID;
        sql += "           AND gs.name is not NULL";
        sql += "           AND gs.name = l.term";
        sql += "           AND l.language_code = '" + languageCode + "'";
        sql += "         ORDER BY l.text";

		return Utility.sortArrayByNumInField(execute(sql), "text");
	}
	
	/**
	* Return array of subgroups info whose name is null, and belong to the given group id
	* @access  public
	* @param   $groupID : group id
	* @return  subgroup id rows : array of subgroup ids, if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	 * @throws SQLException 
	*/
	public List<Map<String, Object>> getUnnamedSubgroupByGroupID(Long groupID) throws SQLException
	{
		String sql = "SELECT * FROM " + tablePrefix + "guideline_subgroups";
        sql += "         WHERE group_id = " + groupID;
        sql += "           AND name is NULL";

		return execute(sql);
	}

	/**
	 * insert/update guideline subgroup term into language_text and update according record in table "guideline_subgroups"
	 * @access  private
	 * @param   $subgroupID
	 *          $term      : term to create/update into 'language_text' table
	 *          $text      : text to create/update into 'language_text' table
	 *          $fieldName : field name in table 'guideline_groups' to update
	 * @return  true    if update successfully
	 *          false   if update unsuccessful
	 * @author  Cindy Qi Li
	 */
//	private function updateLang($subgroupID, $term, $text, $fieldName)
//	{
//		global $addslashes;
//		
//		require_once(AC_INCLUDE_PATH.'classes/DAO/LanguageTextDAO.class.php');
//		$langTextDAO = new LanguageTextDAO();
//		
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
//			$sql = "UPDATE ".TABLE_PREFIX."guideline_subgroups SET ".$fieldName."='".$term."' WHERE subgroup_id=".$subgroupID;
//			$this->execute($sql);
//		}
//		
//		return true;
//	}

}

