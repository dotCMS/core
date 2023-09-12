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
import java.util.List;

import org.apache.commons.logging.Log;

import com.dotcms.enterprise.achecker.model.GuideLineBean;
import com.dotcms.enterprise.achecker.utility.Constants;
import com.dotcms.enterprise.achecker.utility.Utility;
import com.dotcms.enterprise.achecker.dao.BaseDAO;


/**
* DAO for "guidelines" table
* @access	public

* @package	DAO
*/

public class GuidelinesDAO extends BaseDAO {

	private static final Log LOG = org.apache.commons.logging.LogFactory.getLog( GuidelinesDAO.class);

	public GuidelinesDAO() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
	}


	
	/**
	* Return guideline info by given guideline id
	* @access  public
	* @param   $guidelineIDs : an array of guideline ids or one guide id
	* @return  table rows
	* @author  Cindy Qi Li
	 * @throws SQLException 
	*/
	public List<GuideLineBean> getGuidelineByIDs(List<Integer> guidelineIDs) throws Exception {
		String list = Utility.joinList(",", guidelineIDs);
		String sql = "select * from " + tablePrefix + "guidelines where guideline_id in (" + list + ") order by title";
		return execute ( GuideLineBean.class, sql);
	}

	/**
	* Return guideline info by given guideline abbreviation
	* @access  public
	* @param   $abbr : guideline abbreviation
	* @return  table rows
	* @author  Cindy Qi Li
	 * @throws SQLException 
	*/
	public  GuideLineBean  getGuidelineByAbbr(String abbr) throws  Exception {
		String sql = "select * from " + tablePrefix + "guidelines where abbr = '" + abbr + "'";
		return executeOne(GuideLineBean.class, sql);
  	}

  	/**
	* Return guideline info by given user id
	* @access  public
	* @param   $userID : user id
	* @return  table rows
	* @author  Cindy Qi Li
  	 * @throws SQLException 
	*/
	public List<GuideLineBean> getGuidelineByUserIDs(List<Long> userIDs) throws Exception {
		
		String sql = "select * ";
		sql += "from " + tablePrefix + "guidelines ";
		sql += "where user_id in (" + Utility.joinList(",", userIDs) + ") order by title";

	    return execute(GuideLineBean.class, sql);

	}

	/**
	* Return open-to-public guideline info by given user id
	* @access  public
	* @param   $userID : user id
	* @return  table rows
	* @author  Cindy Qi Li
	 * @throws SQLException 
	*/
	public List<GuideLineBean> getClosedEnabledGuidelinesByUserID(Long userID) throws  Exception {

		String sql = "select * ";
		
		sql += "from " + tablePrefix + "guidelines ";
		sql += "where user_id = " + userID + " ";
		sql += "and status = " + Constants.AC_STATUS_ENABLED + " ";
		sql += "and open_to_public = 0 ";
		sql += "order by title";

	    return execute(GuideLineBean.class, sql);
	    
  	}

	/**
	* Return open-to-public guideline info by given check id
	* @access  public
	* @param   $checkID
	* @return  table rows
	* @author  Cindy Qi Li
	 * @throws SQLException 
	*/
	public List<GuideLineBean> getEnabledGuidelinesByCheckID(Long checkID) throws Exception
	{
		String sql = "select *";
		sql += "		from " + tablePrefix + "guidelines";
		sql += "		where guideline_id in ";
		sql += "		     (SELECT distinct gg.guideline_id ";
		sql += "		        FROM " + tablePrefix + "guideline_groups gg, ";
		sql += "						" + tablePrefix + "guideline_subgroups gs, ";
		sql += "						" + tablePrefix + "subgroup_checks gc,";
		sql += "						" + tablePrefix + "checks c";
		sql += "		       WHERE gg.group_id = gs.group_id";
		sql += "				 AND gs.subgroup_id = gc.subgroup_id";
		sql += "				 AND gc.check_id = " + checkID + ")";
		sql += "			order by title";

	    return execute(GuideLineBean.class, sql);
  	}

  	/**
	* Return rows by guideline title
	* @access  public
	* @param   $title
	*          $ignoreCase: 1: ignore case; 0: don't ignore; set to 1 by default
	* @return  table rows
	* @author  Cindy Qi Li
  	 * @throws SQLException 
	*/
	public   GuideLineBean  getEnabledGuidelinesByAbbr(String abbr, boolean ignoreCase) throws  Exception {
		
		String sqlAbbr = "abbr = '" + abbr + "'";
		if (ignoreCase) {
			sqlAbbr = "lower(abbr) = '" + abbr.toLowerCase() + "'";
		}
		String sql = "select *";
		sql += "		from " + tablePrefix + "guidelines";
		sql += "		where " + sqlAbbr;
		sql += "		order by abbr";

	    return executeOne(GuideLineBean.class, sql);
	    
  	}

  	/**
	* Return open-to-public guideline info by given user id
	* @access  public
	* @param   none
	* @return  table rows
	* @author  Cindy Qi Li
  	 * @throws SQLException 
	*/
	public List<GuideLineBean> getOpenGuidelines() throws Exception
	{
		String sql = "select *";
		sql += "		from " + tablePrefix + "guidelines";
		sql += "		where open_to_public = 1";
		sql += "		order by title";

	    return execute(GuideLineBean.class, sql);
  	}

  	/**
	* Return customized guidelines
	* @access  public
	* @param   none
	* @return  table rows
	* @author  Cindy Qi Li
  	 * @throws SQLException 
	*/
	public List<GuideLineBean> getCustomizedGuidelines() throws  Exception
	{
		String sql = "select *";
		sql += "		from " + tablePrefix + "guidelines";
		sql += "		where user_id <> 0";
		sql += "		order by title";

    	return execute(GuideLineBean.class,  sql);
	}

	/**
	* Return standard guidelines
	* @access  public
	* @param   none
	* @return  table rows
	* @author  Cindy Qi Li
	 * @throws SQLException 
	*/
	public List<GuideLineBean> getStandardGuidelines() throws Exception
	{
		String sql = "select *";
		sql += "		from " + tablePrefix + "guidelines";
		sql += "		where user_id = 0";
		sql += "		order by title";

    	return execute(GuideLineBean.class, sql);
  	}
	// Simo: aggiunto per vedere quali check hanno il tag img, per il controllo visivo delle immagini
	// Restituisce una stringa con tutti gli id dei check, separati da virgola, che riguardano la guideline e l'elemento specificato
	public List<GuideLineBean> getCheckByTagAndGuideline(String tag, Long guideline) throws Exception
	{
		String sql = "select distinct c.check_id,c.html_tag";
		sql += "			from " + tablePrefix + "guidelines g, ";
		sql += "			     " + tablePrefix + "guideline_groups gg, ";
		sql += "			     " + tablePrefix + "guideline_subgroups gs, ";
		sql += "			     " + tablePrefix + "subgroup_checks gc,";
		sql += "			     " + tablePrefix + "checks c";
		sql += "			where g.guideline_id = '" + guideline + "'";
		sql += "			  and g.guideline_id = gg.guideline_id";
		sql += "			  and gg.group_id = gs.group_id";
		sql += "			  and gs.subgroup_id = gc.subgroup_id";
		sql += "			  and gc.check_id = c.check_id";
		sql += "			  and c.html_tag = '" + tag  + "'";
		sql += "			order by c.html_tag";
					
		// $check = ",";
		
		return execute(GuideLineBean.class, sql);
	}

  	/**
	* set guideline status
	* @access  public
	* @param   $guidelineID : guideline ID
	*          $status : guideline status
	* @return  true : if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	*/
//	public function setStatus($guidelineID, $status)
//	{
//		$guidelineID = intval($guidelineID);
//		$status = intval($status);
//		
//		$sql = "update ". TABLE_PREFIX ."guidelines
//				set status = " . $status . "
//				where guideline_id=".$guidelineID;
//
//    return $this->execute($sql);
//  }

	/**
	* set open_to_public
	* @access  public
	* @param   $guidelineID : guideline ID
	*          $open_to_public : open to public flag
	* @return  true : if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	*/
//	public function setOpenToPublicFlag($guidelineID, $open_to_public)
//	{
//		$guidelineID = intval($guidelineID);
//		$open_to_public = intval($open_to_public);
//		
//		$sql = "update ". TABLE_PREFIX ."guidelines
//				set open_to_public = " . $open_to_public . "
//				where guideline_id=".$guidelineID;
//
//    return $this->execute($sql);
//  }

	/**
	 * Validate fields preparing for insert and update
	 * @access  private
	 * @param   $title  
	 *          $abbr
	 *          $create_new: flag to indicate if this is creating new record or update.
	 *                       true is to create new record, false is update record.
	 *                       if update record, only check abbr uniqueness when abbr is modified.
	 *          $guidelineID: must be given at updating record, when $create_new == false
	 * @return  true    if all fields are valid
	 *          false   if any field is not valid
	 * @author  Cindy Qi Li
	 * @throws SQLException 
	 */
//	private function isFieldsValid($title, $abbr, $create_new, $guidelineID = 0)
//	{
//		global $msg;
//		
//		// check missing fields
//		$missing_fields = array();
//
//		if ($title == '')
//		{
//			$missing_fields[] = _AC('title');
//		}
//		if ($abbr == '')
//		{
//			$missing_fields[] = _AC('abbr');
//		}
//		if ($missing_fields)
//		{
//			$missing_fields = implode(', ', $missing_fields);
//			$msg->addError(array('EMPTY_FIELDS', $missing_fields));
//		}
//		
//		if (!$create_new)
//		{
//			$current_grow = $this->getGuidelineByIDs($guidelineID);
//		}
//		
//		if ($create_new || (!$create_new && $current_grow[0]['abbr'] <> $abbr))
//		{
//			// abbr must be unique
//			$sql = "SELECT * FROM ".TABLE_PREFIX."guidelines WHERE abbr='".$abbr."'";
//	
//			if (is_array($this->execute($sql)))
//			{
//				$msg->addError('ABBR_EXISTS');
//			}
//		}
//			
//		if (!$msg->containsErrors())
//			return true;
//		else
//			return false;
//	}
	

	
	/**
	* Create a new guideline
	* @access  public
	* @param   $userID : user id
	*          $title
	*          $abbr
	*          $long_name
	*          $published_date
	*          $earlid
	*          $preamble
	*          $status
	*          $open_to_public
	* @return  guidelineID : if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	*/
	/*
	public void create(Integer userID, String title, String abbr, String long_name, Date published_date, String earlid, String preamble, Integer status, Integer open_to_public) {

		global $addslashes;
		
		$userID = intval($userID);
		$title = $addslashes(trim($title));	
		$abbr = $addslashes(trim($abbr));	
		$long_name = trim($long_name);   // $addslashes is not necessary as it's called in LanguageTextDAO->Create()
		$earlid = $addslashes(trim($earlid));
		$preamble = $addslashes(trim($preamble));
		if ($published_date == '' || is_null($published_date)) $published_date = '0000-00-00';
		
		if (!$this->isFieldsValid($title, $abbr, true)) return false;
		
		$sql = "INSERT INTO ".TABLE_PREFIX."guidelines
				(`user_id`, `title`, `abbr`, `published_date`,  
				 `earlid`, `preamble`, `status`, `open_to_public`) 
				VALUES
				(".$userID.",'".$title."', '".$abbr."', '".$published_date."',
				 '".$earlid."','".$preamble."', ".$status.",".$open_to_public.")";

		if (!$this->execute($sql))
		{
			$msg->addError('DB_NOT_UPDATED');
			return false;
		}
		else
		{
			$guidelineID = mysql_insert_id();

			if ($long_name <> '')
			{
				$term = LANG_PREFIX_GUIDELINES_LONG_NAME.$guidelineID;

				require_once(AC_INCLUDE_PATH.'classes/DAO/LanguageTextDAO.class.php');
				$langTextDAO = new LanguageTextDAO();
				
				if ($langTextDAO->Create($_SESSION['lang'], '_guideline',$term,$long_name,''))
				{
					$sql = "UPDATE ".TABLE_PREFIX."guidelines SET long_name='".$term."' WHERE guideline_id=".$guidelineID;
					$this->execute($sql);
				}
			}
			return $guidelineID;
		}
	}
	*/
	
	/**
	* Update an existing guideline
	* @access  public
	* @param   $guidelineID
	*          $userID : user id
	*          $title
	*          $abbr
	*          $long_name
	*          $published_date
	*          $earlid
	*          $preamble
	*          $status
	*          $open_to_public
	* @return  true : if successful
	*          false : if not successful
	* @author  Cindy Qi Li
	*/
	/*
	public function update($guidelineID, $userID, $title, $abbr, $long_name, $published_date, $earlid, $preamble, $status, $open_to_public)
	{
		global $addslashes;
		
		$guidelineID = intval($guidelineID);
		$userID = intval($userID);
		$title = $addslashes(trim($title));	
		$abbr = $addslashes(trim($abbr));	
		$long_name = trim($long_name);   // $addslashes is not necessary as it's called in LanguageTextDAO->setText()
		$earlid = $addslashes(trim($earlid));
		$preamble = $addslashes(trim($preamble));
		
		if (!$this->isFieldsValid($title, $abbr, false, $guidelineID)) return false;
		
		$sql = "UPDATE ".TABLE_PREFIX."guidelines
				   SET `user_id`=".$userID.", 
				       `title` = '".$title."', 
				       `abbr` = '".$abbr."', 
				       `published_date` = '".$published_date."',  
				       `earlid` = '".$earlid."', 
				       `preamble` = '".$preamble."', 
				       `status` = ".$status.", 
				       `open_to_public` = ".$open_to_public." 
				 WHERE guideline_id = ".$guidelineID;

		if (!$this->execute($sql))
		{
			$msg->addError('DB_NOT_UPDATED');
			return false;
		}
		else
		{
			// find language term to update	
			$rows = $this->getGuidelineByIDs($guidelineID);
			$term = $rows[0]['long_name'];
			
			require_once(AC_INCLUDE_PATH.'classes/DAO/LanguageTextDAO.class.php');
			$langTextDAO = new LanguageTextDAO();
			
			if ($langTextDAO->setText($_SESSION['lang'],'_guideline',$term,$long_name))
				return true;
			else
				return false;
		}
	}
	*/
	
	/**
	* Add checks into guideline
	* @access  public
	* @param   $guidelineID : guideline id
	*          $cids : array of check ids to be added into guideline
	* @return  true : if successful
	*          false : if unsuccessful
	* @author  Cindy Qi Li
	*/
	/*
	public function addChecks($guidelineID, $cids)
	{
		require_once(AC_INCLUDE_PATH.'classes/DAO/GuidelineGroupsDAO.class.php');
		require_once(AC_INCLUDE_PATH.'classes/DAO/GuidelineSubgroupsDAO.class.php');
		require_once(AC_INCLUDE_PATH.'classes/DAO/SubgroupChecksDAO.class.php');
		
		$guidelineID = intval($guidelineID);
		if ($guidelineID == 0)
		{
			$msg->addError('MISSING_GID');
			return false;
		}
		
		$guidelineGroupsDAO = new GuidelineGroupsDAO();
		$groups = $guidelineGroupsDAO->getUnnamedGroupsByGuidelineID($guidelineID);
		
		if (is_array($groups))
			$group_id = $groups[0]['group_id'];
		else
			$group_id = $guidelineGroupsDAO->Create($guidelineID, '','','');
		
		if ($group_id)
		{
			$guidelineSubgroupsDAO = new GuidelineSubgroupsDAO();
			$subgroups = $guidelineSubgroupsDAO->getUnnamedSubgroupByGroupID($group_id);
			
			if (is_array($subgroups))
				$subgroup_id = $subgroups[0]['subgroup_id'];
			else
				$subgroup_id = $guidelineSubgroupsDAO->Create($group_id, '','');
			
			if ($subgroup_id)
			{
				$subgroupChecksDAO = new SubgroupChecksDAO();
				
				if (is_array($cids))
				{
					foreach ($cids as $cid) {
						$cid = intval($cid);
						
						if ($cid > 0) {
							$subgroupChecksDAO->Create($subgroup_id, $cid);
						}
					}
				}
			}
			else return false;
		}
		else return false;
		
		return true;
	}
	*/
	
	/**
	* Delete guideline by ID
	* @access  public
	* @param   $guidelineID : guideline id
	* @return  true : if successful
	*          false : if unsuccessful
	* @author  Cindy Qi Li
	*/
	/*
	public function Delete($guidelineID)
	{
		require_once(AC_INCLUDE_PATH.'classes/DAO/GuidelineGroupsDAO.class.php');
		
		$guidelineID = intval($guidelineID);
		
		// Delete all subgroups
		$guidelineGroupsDAO = new GuidelineGroupsDAO();
		$sql = "SELECT group_id FROM ".TABLE_PREFIX."guideline_groups
		         WHERE guideline_id = ".$guidelineID;
		$rows = $this->execute($sql);
		
		if (is_array($rows))
		{
			foreach ($rows as $row)
				$guidelineGroupsDAO->Delete($row['group_id']);
		}
		
		// delete language for long name
		$sql = "DELETE FROM ".TABLE_PREFIX."language_text 
		         WHERE variable='_guideline' 
		           AND term=(SELECT long_name 
		                       FROM ".TABLE_PREFIX."guidelines
		                      WHERE guideline_id=".$guidelineID.")";
		$this->execute($sql);
		
		$sql = "DELETE FROM ".TABLE_PREFIX."guidelines WHERE guideline_id=".$guidelineID;
		
		return $this->execute($sql);
	}
	*/
	
}

