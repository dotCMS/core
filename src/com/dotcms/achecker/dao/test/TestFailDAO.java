package com.dotcms.achecker.dao.test;


import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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
 * DAO for "test_fail" table
 * @access	public
 * @author	Cindy Qi Li
 * @package	DAO
 */

public class TestFailDAO extends BaseDAO {

	public TestFailDAO() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		super();
	}

	/**
	 * Return check info of given check id
	 * @access  public
	 * @param   $checkID : check id
	 * @return  table rows
	 * @author  Cindy Qi Li
	 */
	public List<Map<String, Object>>  getFailStepsByID(int checkID) throws SQLException
	{
		//		$checkID = intval($checkID);
		String sql = "SELECT step_id, step 	FROM "+ tablePrefix+"test_fail WHERE check_id="+checkID +" 	ORDER BY step_id";

		return execute(sql);
	}

}
