/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.dao;



import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
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

	/**
	 * Creates a DAO that runs its queries through the given {@link DAO}. Mainly for tests, where
	 * the {@link DAO} can point at a throw-away database.
	 *
	 * @param dao the {@link DAO} that runs the queries
	 */
	public LanguageTextDAO(final DAO dao) {
		super(dao);
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
	 * Returns the message rows (variable {@code _msgs}) for the given term and language.
	 *
	 * @param term the language term; sent to the database as a bound value
	 * @param lang the language code, for example {@code eng}; sent to the database as a bound value
	 * @return the matching rows, keyed by lower-cased column name
	 * @throws SQLException if the query fails
	 */
	public List<Map<String, Object>>   getMsgByTermAndLang(String term, String lang)  throws SQLException 
	{
		String sql =  "SELECT * FROM "+tablePrefix+"language_text WHERE term = ? AND variable = '_msgs' AND language_code = ? ORDER BY variable";
		return execute(sql, term, lang);
  }

	/**
	 * Returns the text stored for the given term in the given language.
	 *
	 * @param term the language term; sent to the database as a bound value
	 * @param lang the language code, for example {@code eng}; sent to the database as a bound value
	 * @return the first matching row, or {@code null} when the term has no text in that language
	 * @throws Exception if the query fails
	 */
	public LanguageTextBean getByTermAndLang(String term, String lang)  throws Exception 
	{
		String sql = "SELECT * FROM "+tablePrefix+"language_text WHERE term = ? AND language_code = ? ORDER BY variable";

		return executeOne(LanguageTextBean.class, sql, term, lang);
  	}

	/**
	 * Returns the handbook rows of the given language whose text contains the given text,
	 * ignoring letter case. The text is matched with {@code LIKE}, so a {@code %} or {@code _} in
	 * it still acts as a wildcard. A {@code null} text is treated as an empty text and therefore
	 * matches every handbook row of the language.
	 *
	 * @param text the text to look for; sent to the database as a bound value
	 * @param lang the language code, for example {@code eng}; sent to the database as a bound value
	 * @return the matching rows
	 * @throws Exception if the query fails
	 */
	public List<LanguageTextBean>   getHelpByMatchingText(String text, String lang)  throws Exception 
	{
		String sql = "SELECT * FROM "+tablePrefix+"language_text WHERE term like 'AC_HELP_%' AND lower(cast(text as char)) like ? AND language_code = ? ORDER BY variable";
		final String searchText = text == null ? "" : text.toLowerCase(Locale.ROOT);
		return execute(LanguageTextBean.class, sql, "%" + searchText + "%", lang);
  	}

	/**
	 * Returns every text row of the given language, ordered by variable and term.
	 *
	 * @param lang the language code, for example {@code eng}; sent to the database as a bound value
	 * @return the rows of that language
	 * @throws Exception if the query fails
	 */
	public List<LanguageTextBean> getAllByLang(String lang)  throws Exception 
	{
		String sql = "SELECT * FROM "+tablePrefix+"language_text WHERE language_code = ? ORDER BY variable, term ASC";

		return execute(LanguageTextBean.class, sql, lang);
	}

	/**
	 * Returns the template rows (variable {@code _template}) of the given language.
	 *
	 * @param lang the language code, for example {@code eng}; sent to the database as a bound value
	 * @return the template rows of that language
	 * @throws Exception if the query fails
	 */
	public List<LanguageTextBean>  getAllTemplateByLang(String lang)  throws Exception 
	{
		String sql = "SELECT * FROM "+tablePrefix+"language_text WHERE language_code = ? AND variable = '_template' ORDER BY variable ASC";

		return execute(LanguageTextBean.class, sql, lang);
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
