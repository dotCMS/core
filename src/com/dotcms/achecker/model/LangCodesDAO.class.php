<?php
/************************************************************************/
/* AChecker                                                             */
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
* DAO for "lang_codes" table
* @access	public
* @author	Cindy Qi Li
* @package	DAO
*/

if (!defined('AC_INCLUDE_PATH')) exit;

require_once(AC_INCLUDE_PATH. 'classes/DAO/DAO.class.php');

class LangCodesDAO extends DAO {

	/**
	* Return all rows
	* @access  public
	* @param   none
	* @return  table rows
	* @author  Cindy Qi Li
	*/
	public function GetAll()
	{
		$sql = "SELECT * FROM ". TABLE_PREFIX ."lang_codes ORDER BY description";
		
		return $this->execute($sql);
	}
	
	/**
	* Return lang code info of the given 2 letters code
	* @access  public
	* @param   $code : 2 letters code
	* @return  table rows
	* @author  Cindy Qi Li
	*/
	public function GetLangCodeBy2LetterCode($code)
	{
		global $addslashes;
		
		$code = $addslashes($code);
		
		$sql = "SELECT * FROM ". TABLE_PREFIX ."lang_codes 
					WHERE code_2letters = '".$code ."'";
		
		return $this->execute($sql);
	}

	/**
	* Return lang code info of the given 3 letters code
	* @access  public
	* @param   $code : 3 letters code
	* @return  table rows
	* @author  Cindy Qi Li
	*/
	public function GetLangCodeBy3LetterCode($code)
	{
		global $addslashes;

		$code = $addslashes($code);
		
		$sql = "SELECT * FROM ". TABLE_PREFIX ."lang_codes 
					WHERE code_3letters = '".$code ."'";
		
		if ($rows = $this->execute($sql))
		{
			return $rows[0];
		}
		else
			return false;
	}

	/**
	* Return array of all the 2-letter & 3-letter language codes with given direction
	* @access  public
	* @param   $direction : 'rtl' or 'ltr'
	* @return  table rows
	* @author  Cindy Qi Li
	*/
	public function GetLangCodeByDirection($direction)
	{
		global $addslashes;
		
		$direction = $addslashes($direction);
		
		$rtn_array = array();
		$sql = "SELECT * FROM ". TABLE_PREFIX ."lang_codes 
					WHERE direction = '".$direction ."'";
		
		$rows = $this->execute($sql);
		
		if (is_array($rows))
		{
			foreach ($rows as $row)
			{
				array_push($rtn_array, $row['code_3letters']);
				array_push($rtn_array, $row['code_2letters']);
			}
		}
		return $rtn_array;
	}

}
?>