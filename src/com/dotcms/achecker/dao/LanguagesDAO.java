package com.dotcms.achecker.dao;


import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.dotcms.achecker.utility.Constants;
import com.dotcms.achecker.dao.BaseDAO;


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
 * DAO for "config" table
 * @access	public
 * @author	Cindy Qi Li
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
