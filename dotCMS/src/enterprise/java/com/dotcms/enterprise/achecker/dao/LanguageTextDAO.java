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

import com.dotcms.enterprise.achecker.model.LanguageTextBean;
import com.dotcms.enterprise.achecker.dao.BaseDAO;



/**
* DAO for "language_text" table
* @access	public

* @package	DAO
*/
 

public class LanguageTextDAO extends BaseDAO {

	public LanguageTextDAO() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
	}

//	/**
//	* Create a new entry
//	* @access  public
//	* @param   $language_code : language code
//	*          $variable: '_msgs', '_template', '_check', '_guideline', '_test'
//	*          $term
//	*          $text
//	*          $context
//	* @return  table rows
//	* @author  Cindy Qi Li
//	*/
//	function Create($language_code, $variable, $term, $text, $context)
//	{
//		global $addslashes;
//		
//		$sql = "INSERT INTO ".TABLE_PREFIX."language_text
//		        (`language_code`, `variable`, `term`, `text`, `revised_date`, `context`)
//		        VALUES
//		        ('".$addslashes($language_code)."', 
//		         '".$addslashes($variable)."', 
//		         '".$addslashes($term)."', 
//		         '".$addslashes($text)."', 
//		         now(), 
//		         '".$addslashes($context)."')";
//
//		return $this->execute($sql);
//	}
//
//	/**
//	* Insert new record if not exists, replace the existing one if already exists. 
//	* Record is identified by primary key: $language_code, variable, $term
//	* @access  public
//	* @param   $language_code : language code
//	*          $variable: '_msgs', '_template', '_check', '_guideline', '_test'
//	*          $term
//	*          $text
//	*          $context
//	* @return  table rows
//	* @author  Cindy Qi Li
//	*/
//	function Replace($language_code, $variable, $term, $text, $context)
//	{
//		global $addslashes;
//		
//		$sql = "REPLACE INTO ".TABLE_PREFIX."language_text
//		        (`language_code`, `variable`, `term`, `text`, `revised_date`, `context`)
//		        VALUES
//		        ('".$addslashes($language_code)."', 
//		         '".$addslashes($variable)."', 
//		         '".$addslashes($term)."', 
//		         '".$addslashes($text)."', 
//		         now(), 
//		         '".$addslashes($context)."')";
//		        
//		return $this->execute($sql);
//	}
//	
//	/**
//	* Delete a record by $variable and $term
//	* @access  public
//	* @param   $language_code : language code
//	*          $variable: '_msgs', '_template', '_check', '_guideline', '_test'
//	*          $term
//	* @return  true / false
//	* @author  Cindy Qi Li
//	*/
//	function DeleteByVarAndTerm($variable, $term)
//	{
//		global $addslashes;
//		
//		$sql = "DELETE FROM ".TABLE_PREFIX."language_text
//		        WHERE `variable` = '".$addslashes($variable)."'
//		          AND `term` = '".$addslashes($term)."'";
//		        
//		return $this->execute($sql);
//	}
	
	/**
	* Return message text of given term and language
	* @access  public
	* @param   term : language term
	*          lang : language code
	* @return  table rows
	* @author  Cindy Qi Li
	*/
	public List<Map<String, Object>>   getMsgByTermAndLang(String term, String lang)  throws SQLException 
	{
//		global $addslashes;
//		
//		$term = $addslashes($term);
//		$lang = $addslashes($lang);
		
		String sql =  "SELECT * FROM "+tablePrefix+"language_text WHERE term='" +term +"'  AND variable=_msgs  AND language_code='"+lang+"' ORDER BY variable";
		return execute(sql);
  }

	/**
	* Return text of given term and language
	* @access  public
	* @param   term : language term
	*          lang : language code
	* @return  table rows
	* @author  Cindy Qi Li
	*/
	public LanguageTextBean getByTermAndLang(String term, String lang)  throws Exception 
	{
//		global $addslashes;
//		
//		$term = $addslashes($term);
//		$lang = $addslashes($lang);
		
		String sql = "SELECT * FROM "+tablePrefix+"language_text 	WHERE term='"+term +"' AND language_code='"+lang+"' ORDER BY variable";

		return executeOne(LanguageTextBean.class,  sql);
  	}

	/**
	* Return rows of handbook rows by matching given text and language
	* @access  public
	* @param   term : language term
	*          lang : language code
	* @return  table rows
	* @author  Cindy Qi Li
	*/
	public List<LanguageTextBean>   getHelpByMatchingText(String text, String lang)  throws Exception 
	{
//		global $addslashes;
		text = text.toLowerCase();
//		$text = $addslashes(strtolower($text));
//		$lang = $addslashes($lang);
		
		String sql = "SELECT * FROM "+tablePrefix+"language_text WHERE term like 'AC_HELP_%' AND lower(cast(text as char)) like '%"+text+"%' AND language_code='"+lang+"' ORDER BY variable";
		return execute(LanguageTextBean.class ,sql);
  	}

  	/**
	* Return all template info of given language
	* @access  public
	* @param   lang : language code
	* @return  table rows
	* @author  Cindy Qi Li
	*/
	public List<LanguageTextBean> getAllByLang(String lang)  throws Exception 
	{
//		global $addslashes;
//		
//		$lang = $addslashes($lang);
		
		String sql = "SELECT * FROM "+tablePrefix+"language_text WHERE language_code='"+lang+"' ORDER BY variable, term ASC";

		return execute(LanguageTextBean.class,  sql);
	}

  	/**
	* Return all template info of given language
	* @access  public
	* @param   lang : language code
	* @return  table rows
	* @author  Cindy Qi Li
	*/
	public List<LanguageTextBean>  getAllTemplateByLang(String lang)  throws Exception 
	{
//		global $addslashes;
//		
//		$lang = $addslashes($lang);
		
		String sql = "SELECT * FROM "+tablePrefix+"language_text 	WHERE language_code='"+lang+"' AND variable='_template'  ORDER BY variable ASC";

		return execute(LanguageTextBean.class , sql);
	}

//	/**
//	* Update text based on given primary key
//	* @access  public
//	* @param   $languageCode : language_text.language_code
//	*          $variable : language_text.variable
//	*          $term : language_text.term
//	*          $text : text to update into language_text.text
//	* @return  true : if successful
//	*          false: if unsuccessful
//	* @author  Cindy Qi Li
//	*/
//	function setText(String languageCode, String variable, String term, String text)
//	{
//		global $addslashes;
//		
//		$languageCode = $addslashes($languageCode);
//		$variable = $addslashes($variable);
//		$term = $addslashes($term);
//		$text = $addslashes($text);
//		
//		$sql = "UPDATE ".TABLE_PREFIX."language_text 
//		           SET text='".$text."',
//		               revised_date = now()
//		         WHERE language_code = '".$languageCode."' 
//		           AND variable='".$variable."' 
//		           AND term = '".$term."'";
//
//		return $this->execute($sql);
//  }
}
 