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
 