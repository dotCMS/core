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
	 * Creates a DAO that runs its queries through the given {@link DAO}. Mainly for tests, where
	 * the {@link DAO} can point at a throw-away database.
	 *
	 * @param dao the {@link DAO} that runs the queries
	 */
	public LangCodesDAO(final DAO dao) {
		super(dao);
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
	 * Finds the language whose two-letter code (for example {@code en}) is the given one.
	 *
	 * @param code the two-letter language code; sent to the database as a bound value
	 * @return the matching language code row, or {@code null} when there is none
	 * @throws Exception if the query fails
	 */
	public  LanguageCodeBean getLangCodeBy2LetterCode(String code) throws Exception
	{
		String sql = "SELECT * FROM "+ tablePrefix +"lang_codes WHERE code_2letters = ?";
		return executeOne(LanguageCodeBean.class, sql, code);
	}

	/**
	 * Finds the language whose three-letter code (for example {@code eng}) is the given one.
	 *
	 * @param code the three-letter language code; sent to the database as a bound value
	 * @return the matching language code row, or {@code null} when there is none
	 * @throws Exception if the query fails
	 */
	public LanguageCodeBean getLangCodeBy3LetterCode(String code) throws Exception
	{
		String sql = "SELECT * FROM "+ tablePrefix +"lang_codes WHERE code_3letters = ?";
		return executeOne(LanguageCodeBean.class, sql, code);
	}

	/**
	 * Lists the languages written in the given direction.
	 *
	 * @param direction {@code rtl} or {@code ltr}; sent to the database as a bound value
	 * @return the language code rows with that direction
	 * @throws Exception if the query fails
	 */
	public  List<LanguageCodeBean>  getLangCodeByDirection(String direction ) throws Exception
	{
		String sql = "SELECT * FROM "+ tablePrefix +"lang_codes WHERE direction = ?";
		return execute(LanguageCodeBean.class, sql, direction);
	}

}
