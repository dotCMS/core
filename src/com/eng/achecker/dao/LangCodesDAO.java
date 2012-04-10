package com.eng.achecker.dao;



import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.eng.achecker.model.LanguageCodeBean;


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
 * DAO for "lang_codes" table
 * @access	public
 * @author	Cindy Qi Li
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
