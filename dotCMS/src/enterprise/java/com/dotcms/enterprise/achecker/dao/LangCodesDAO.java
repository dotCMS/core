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
import java.util.Map;

import com.dotcms.enterprise.achecker.model.LanguageCodeBean;
import com.dotcms.enterprise.achecker.dao.BaseDAO;


/**
 * DAO for "lang_codes" table
 * @access	public
 
 * @package	DAO
 */


public class LangCodesDAO extends BaseDAO {

	public LangCodesDAO() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
	}
	/**
	 * Return all rows
	 * @access  public
	 * @param   none
	 * @return  table rows
	 * @author  Cindy Qi Li
	 */
	public List<Map<String, Object>> getAll() throws SQLException {
		String sql = "SELECT * FROM "+ tablePrefix +"lang_codes ORDER BY description";

		return execute(sql);
	}

	/**
	 * Return lang code info of the given 2 letters code
	 * @access  public
	 * @param   $code : 2 letters code
	 * @return  table rows
	 * @author  Cindy Qi Li
	 */
	public  LanguageCodeBean getLangCodeBy2LetterCode(String code) throws Exception
	{
		try{
 			String sql = "SELECT * FROM "+ tablePrefix +"lang_codes WHERE code_2letters = '"+code+"'";
			LanguageCodeBean rows = executeOne( LanguageCodeBean.class ,  sql);
			return rows;
		}catch (Exception e) {
			throw new SQLException(e);
		}
	}

	/**
	 * Return lang code info of the given 3 letters code
	 * @access  public
	 * @param   $code : 3 letters code
	 * @return  table rows
	 * @author  Cindy Qi Li
	 */
	public LanguageCodeBean getLangCodeBy3LetterCode(String code) throws Exception
	{
		//		global $addslashes;
		//		$code = $addslashes($code);
		String sql = "SELECT * FROM "+ tablePrefix +"lang_codes WHERE code_3letters = '"+code+"'";
		LanguageCodeBean  rows = executeOne(LanguageCodeBean.class , sql);
		return rows;
		 
	}

	/**
	 * Return array of all the 2-letter & 3-letter language codes with given direction
	 * @access  public
	 * @param   $direction : 'rtl' or 'ltr'
	 * @return  table rows
	 * @author  Cindy Qi Li
	 */
	public  List<LanguageCodeBean>  getLangCodeByDirection(String direction ) throws Exception
	{
		//		global $addslashes;
		//		$direction = $addslashes($direction);
		//		$rtn_array = array();
		String sql = "SELECT * FROM "+ tablePrefix +"lang_codes WHERE direction = '"+direction+"'";
		List<LanguageCodeBean> rows = execute(LanguageCodeBean.class, sql);
		return rows;
	}

}
