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
 * DAO for "check_examples" table
 * @access	public
 
 * @package	DAO
 */


public class CheckExamplesDAO extends BaseDAO {

	public CheckExamplesDAO() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
	}
	//	/**
	//	* return rows with given check id and type
	//	* @access  public
	//	* @param   $checkID
	//	*          $type
	//	* @return  table rows
	//	* @author  Cindy Qi Li
	//	*/
	//	public function Create($checkID, $type, $description, $content)
	//	{
	//		global $addslashes;
	//
	//		$checkID = intval($checkID);
	//		$type = $addslashes($type);
	//		$description = $addslashes(trim($description));
	//		$content = $addslashes(trim($content));
	//		
	//		// don't insert if no desc and content
	//		if ($description == '' && $content == '') return true;
	//		
	//		if (!$this->isFieldsValid($checkID, $type)) return false;
	//		
	//		$sql = "INSERT INTO ".TABLE_PREFIX."check_examples
	//				(`check_id`, `type`, `description`, `content`) 
	//				VALUES
	//				(".$checkID.",".$type.",'".$description."', ".
	//		         "'".$content."')";
	//
	//		if (!$this->execute($sql))
	//		{
	//			$msg->addError('DB_NOT_UPDATED');
	//			return false;
	//		}
	//		else
	//		{
	//			return mysql_insert_id();
	//		}
	//	}

	/**
	 * return rows with given check id and type
	 * @access  public
	 * @param   $checkID
	 *          $type
	 * @return  table rows
	 * @author  Cindy Qi Li
	 */
	public  List<Map<String, Object>>  getByCheckIDAndType(int checkID, int type) throws SQLException
	{
		//	global $addslashes;
		//	$addslashes($type)	
		String sql  = "SELECT * FROM "+tablePrefix+"check_examples	WHERE check_id = "+checkID +"   AND type = "+type;
		return execute(sql);
	}

//	/**
//	 * Delete all entries with given check id
//	 * @access  public
//	 * @param   $checkID
//	 * @return  true : if successful
//	 *          false : if not successful
//	 * @author  Cindy Qi Li
//	 */
//	public function DeleteByCheckID($checkID)
//	{
//		$sql = "DELETE FROM ".TABLE_PREFIX."check_examples
//		WHERE check_id = ".intval($checkID);
//
//			return $this->execute($sql);
//	}

//	/**
//	 * Validate fields preparing for insert and update
//	 * @access  private
//	 * @param   $checkID  
//	 *          $type
//	 * @return  true    if all fields are valid
//	 *          false   if any field is not valid
//	 * @author  Cindy Qi Li
//	 */
//	private function isFieldsValid($checkID, $type)
//	{
//		global $msg;
//
//		$missing_fields = array();
//
//		if ($checkID == '')
//		{
//			$missing_fields[] = _AC('check_id');
//		}
//		if ($type <> AC_CHECK_EXAMPLE_FAIL && $type <> AC_CHECK_EXAMPLE_PASS)
//		{
//			$missing_fields[] = _AC('example_type');
//		}
//
//		if ($missing_fields)
//		{
//			$missing_fields = implode(', ', $missing_fields);
//			$msg->addError(array('EMPTY_FIELDS', $missing_fields));
//		}
//
//		if (!$msg->containsErrors())
//			return true;
//		else
//			return false;
//	}
	
	
	public  List<Map<String, Object>>  getByType( int type) throws SQLException
	{
		//	global $addslashes;
		//	$addslashes($type)	
		String sql  = "SELECT * FROM "+tablePrefix+"check_examples	WHERE   type = "+type;
		return execute(sql);
	}
	


}
