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
