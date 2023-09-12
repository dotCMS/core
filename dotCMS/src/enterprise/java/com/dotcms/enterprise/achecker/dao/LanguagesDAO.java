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


import com.dotcms.enterprise.achecker.utility.Constants;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;




/**
 * DAO for "config" table
 * @access	public
 
 * @package	DAO
 */



public class LanguagesDAO extends BaseDAO {
	
	public LanguagesDAO() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
	}

	//	/**
	//	* Insert table languages
	//	* @access  public
	//	* @param   $langCode, $charset, $regExp, $nativeName, $englishName, $status
	//	* @return  true / false
	//	* @author  Cindy Qi Li
	//	*/
	//	function Create($langCode, $charset, $regExp, $nativeName, $englishName, $status)
	//	{
	//		global $languageManager, $msg, $addslashes;
	//		
	//		// check if the required fields are filled
	//		if (!$this->ValidateFields($langCode, $charset, $nativeName, $englishName)) return false;
	//		
	//		// check if the language already exists
	//		if ($languageManager->exists($langCode)) $msg->addError('LANG_EXISTS');
	//		
	//		if ($msg->containsErrors()) return false;
	//		
	//		$langCode = $addslashes($langCode);
	//		$charset = $addslashes($charset);
	//		$regExp = $addslashes($regExp);
	//		$nativeName = $addslashes($nativeName);
	//		$englishName = $addslashes($englishName);
	//		$status = intval($status);
	//		
	//		$sql = "INSERT INTO ".TABLE_PREFIX."languages (language_code, charset, reg_exp, native_name, english_name, status) 
	//		        VALUES ('".$langCode."', '".$charset."', '".$regExp."', '".$nativeName."', '".$englishName."', ".$status.")";
	//		return $this->execute($sql);
	//	}
	//
	//	/**
	//	* Update a row
	//	* @access  public
	//	* @param   $langCode: required
	//	*          $charset: required
	//	* @return  true / false
	//	* @author  Cindy Qi Li
	//	*/
	//	function Update($langCode, $charset, $regExp, $nativeName, $englishName, $status)
	//	{
	//		global $addslashes;
	//		
	//		// check if the required fields are filled
	//		if (!$this->ValidateFields($langCode, $charset, $nativeName, $englishName)) return false;
	//		
	//		$langCode = $addslashes($langCode);
	//		$charset = $addslashes($charset);
	//		$regExp = $addslashes($regExp);
	//		$nativeName = $addslashes($nativeName);
	//		$englishName = $addslashes($englishName);
	//		$status = intval($status);
	//		
	//		$sql = "UPDATE ".TABLE_PREFIX."languages 
	//		           SET reg_exp='".$regExp."',
	//		               native_name = '".$nativeName."',
	//		               english_name = '".$englishName."',
	//		               status = ".$status."
	//		         WHERE language_code = '".$langCode."'
	//		           AND charset = '".$charset."'";
	//		return $this->execute($sql);
	//	}
	//
	//	/**
	//	* Delete a row
	//	* @access  public
	//	* @param   $langCode
	//	*          $charset
	//	* @return  true / false
	//	* @author  Cindy Qi Li
	//	*/
	//	function Delete($langCode)
	//	{
	//		global $addslashes;
	//		$langCode = $addslashes($langCode);
	//		
	//		$sql = "DELETE FROM ".TABLE_PREFIX."languages 
	//		         WHERE language_code = '".$langCode."'";
	//		if (!$this->execute($sql)) return false;
	//
	//		$sql = "DELETE FROM ".TABLE_PREFIX."language_text 
	//	             WHERE language_code = '".$langCode."'";
	//		
	//		return $this->execute($sql);
	//	}

	/**
	 * Return all languages
	 * @access  public
	 * @param   none
	 * @return  table rows
	 * @author  Cindy Qi Li
	 */
	public List<Map<String, Object>>  getAll() throws SQLException {
		String sql = "SELECT * FROM "+tablePrefix+"languages l     ORDER BY l.native_name";
		return execute(sql);
	}

	/**
	 * Return all enabled languages
	 * @access  public
	 * @param   none
	 * @return  table rows
	 * @author  Cindy Qi Li
	 */
	public List<Map<String, Object>>  getAllEnabled() throws SQLException
	{
		String sql = "SELECT * FROM "+tablePrefix+"languages l  WHERE status = "+Constants.AC_STATUS_ENABLED+"  ORDER BY l.native_name";
		return execute(sql);
	}

	/**
	 * Return language with given language code
	 * @access  public
	 * @param   $langCode
	 *          $charset
	 * @return  table rows
	 * @author  Cindy Qi Li
	 */
	public Map<String, Object> getByLangCodeAndCharset(String langCode, String charset) throws SQLException
	{
		//		global $addslashes;
		//		$langCode = $addslashes($langCode);
		//		$charset = $addslashes($charset);

		String sql = "SELECT * FROM "+tablePrefix+"languages l  WHERE l.language_code = '"+langCode+"' AND " +
		" l.charset='"+charset+"'ORDER BY l.native_name";

		List<Map<String, Object>> rows = execute(sql);
		if (rows != null && rows.size() > 0)
		{
			Map<String, Object> oneres = rows.get(0);
			return oneres;
		}
		return null;
	}

	/**
	 * Return all languages except the ones with language code in the given string 
	 * @access  public
	 * @param   $langCode : one language codes, for example: en
	 * @return  table rows
	 * @author  Cindy Qi Li
	 */
	public List<Map<String, Object>> getAllExceptLangCode(String langCode ) throws SQLException
	{
//		global $addslashes;
		if( langCode == null || StringUtils.isEmpty(langCode)){
			return this.getAll();
		}else {
			String sql = "SELECT * FROM "+tablePrefix+"languages WHERE language_code <> '"+langCode+"' ORDER BY native_name";
			return execute(sql);
		}
	}

//	/**
//	 * Return all languages except the ones with language code in the given string 
//	 * @access  public
//	 * @param   $langCode : one language codes, for example: en
//	 * @return  table rows
//	 * @author  Cindy Qi Li
//	 */
//	function ValidateFields(String langCode, Stringcharset, StringnativeName, String englishName)
//	{
//		global $msg;
//
//		$missing_fields = array();
//		
//
//		if ($langCode == '') {
//			$missing_fields[] = _AC('lang_code');
//		}
//		if ($charset == '') {
//			$missing_fields[] = _AC('charset');
//		}
//		if ($nativeName == '') {
//			$missing_fields[] = _AC('name_in_language');
//		}
//		if ($englishName == '') {
//			$missing_fields[] = _AC('name_in_english');
//		}
//
//		if ($missing_fields) {
//			$missing_fields = implode(', ', $missing_fields);
//			$msg->addError(array('EMPTY_FIELDS', $missing_fields));
//			return false;
//		}
//		return true;
//	}
}
