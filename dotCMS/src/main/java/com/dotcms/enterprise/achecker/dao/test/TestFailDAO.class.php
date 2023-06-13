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
* DAO for "test_fail" table
* @access	public
* @author	Cindy Qi Li
* @package	DAO
*/

if (!defined('AC_INCLUDE_PATH')) exit;

require_once(AC_INCLUDE_PATH. 'classes/DAO/DAO.class.php');

class TestFailDAO extends DAO {

	/**
	* Return check info of given check id
	* @access  public
	* @param   $checkID : check id
	* @return  table rows
	* @author  Cindy Qi Li
	*/
	function getFailStepsByID($checkID)
	{
		$checkID = intval($checkID);
		$sql = "SELECT step_id, step
						FROM ". TABLE_PREFIX ."test_fail 
						WHERE check_id=". $checkID ."
						ORDER BY step_id";

    return $this->execute($sql);
  }

}
?>