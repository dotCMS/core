/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.dao;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.achecker.dao.DAO;
import com.dotcms.enterprise.achecker.dao.DAOFactory;
import com.dotmarketing.util.Logger;

/**
 * Base class of the Accessibility Checker table DAOs. Runs queries through a {@link DAO} and can
 * turn each returned row into a bean, as long as the bean has a public constructor taking the row
 * {@link Map}.
 * <p>
 * Each query method comes in two forms: one taking only the query text, kept for queries built
 * from trusted values such as numeric ids, and one taking the values for the query's {@code ?}
 * placeholders. Request-supplied text must always go through the second form.
 */
public class BaseDAO {

	public static final String DEFAULT_PREFIX = "AC_";

	private static final Object[] NO_PARAMS = new Object[0];

	protected String tablePrefix;

	protected DAO dao;

	/**
	 * Creates a DAO backed by the default {@link DAO} implementation from {@link DAOFactory}.
	 * If that implementation cannot be created the failure is logged and later queries fail.
	 */
	public BaseDAO() {
		try{
			dao = DAOFactory.getDAO();
			this.tablePrefix = DEFAULT_PREFIX;
		}catch (Exception e) {
			Logger.error(BaseDAO.class, "Accessibility Checker data access could not be initialized: "
					+ e.getClass().getName());
		}
	}

	/**
	 * Creates a DAO that runs its queries through the given {@link DAO}, using the default table
	 * prefix.
	 *
	 * @param dao the {@link DAO} that runs the queries
	 */
	public BaseDAO(final DAO dao) {
		this.dao = dao;
		this.tablePrefix = DEFAULT_PREFIX;
	}

	/**
	 * Runs a query that takes no values and returns all rows.
	 *
	 * @param sql the query to run
	 * @return one map per row, keyed by lower-cased column name
	 * @throws SQLException if the query fails
	 */
	public List<Map<String, Object>> execute(String sql) throws SQLException {
		return dao.execute(sql);
	}

	/**
	 * Runs a query whose {@code ?} placeholders are filled, in order, with the given values, and
	 * returns all rows.
	 *
	 * @param sql the query to run, with one {@code ?} per value
	 * @param params the values for the placeholders, in order
	 * @return one map per row, keyed by lower-cased column name
	 * @throws SQLException if the query fails
	 */
	public List<Map<String, Object>> execute(final String sql, final Object... params) throws SQLException {
		return dao.execute(sql, params);
	}

	/**
	 * Runs a query that takes no values and turns every row into a bean of the given class.
	 *
	 * @param clazz the bean class; it must have a public constructor taking the row {@link Map}
	 * @param sql the query to run
	 * @return one bean per row
	 * @throws SQLException if the query fails or a row cannot be turned into a bean
	 */
	public <T> List<T> execute(Class<T> clazz, String sql) throws SQLException{
		return execute(clazz, sql, NO_PARAMS);
	}

	/**
	 * Runs a query whose {@code ?} placeholders are filled, in order, with the given values, and
	 * turns every row into a bean of the given class.
	 *
	 * @param clazz the bean class; it must have a public constructor taking the row {@link Map}
	 * @param sql the query to run, with one {@code ?} per value
	 * @param params the values for the placeholders, in order
	 * @return one bean per row
	 * @throws SQLException if the query fails or a row cannot be turned into a bean
	 */
	public <T> List<T> execute(final Class<T> clazz, final String sql, final Object... params) throws SQLException {
		final List<Map<String, Object>> rows = dao.execute(sql, params);
		final List<T> result = new ArrayList<>();
		for (final Map<String, Object> row : rows) {
			result.add(toBean(clazz, row));
		}
		return result;
	}

	/**
	 * Runs a query that takes no values and turns the first row into a bean of the given class.
	 *
	 * @param clazz the bean class; it must have a public constructor taking the row {@link Map}
	 * @param sql the query to run
	 * @return the bean for the first row, or {@code null} when the query returns no rows
	 * @throws SQLException if the query fails or the row cannot be turned into a bean
	 */
	public <T> T executeOne(Class<T> clazz, String sql)  throws SQLException  {
		return executeOne(clazz, sql, NO_PARAMS);
	}

	/**
	 * Runs a query whose {@code ?} placeholders are filled, in order, with the given values, and
	 * turns the first row into a bean of the given class.
	 *
	 * @param clazz the bean class; it must have a public constructor taking the row {@link Map}
	 * @param sql the query to run, with one {@code ?} per value
	 * @param params the values for the placeholders, in order
	 * @return the bean for the first row, or {@code null} when the query returns no rows
	 * @throws SQLException if the query fails or the row cannot be turned into a bean
	 */
	public <T> T executeOne(final Class<T> clazz, final String sql, final Object... params) throws SQLException {
		final Map<String, Object> row = executeOne(sql, params);
		return row == null ? null : toBean(clazz, row);
	}

	/**
	 * Runs a query that takes no values and returns its first row.
	 *
	 * @param sql the query to run
	 * @return the first row, keyed by lower-cased column name, or {@code null} when there is none
	 * @throws SQLException if the query fails
	 */
	public Map<String, Object> executeOne(String sql) throws SQLException {
		return executeOne(sql, NO_PARAMS);
	}

	/**
	 * Runs a query whose {@code ?} placeholders are filled, in order, with the given values, and
	 * returns its first row.
	 *
	 * @param sql the query to run, with one {@code ?} per value
	 * @param params the values for the placeholders, in order
	 * @return the first row, keyed by lower-cased column name, or {@code null} when there is none
	 * @throws SQLException if the query fails
	 */
	public Map<String, Object> executeOne(final String sql, final Object... params) throws SQLException {
		final List<Map<String, Object>> results = execute(sql, params);
		return results.isEmpty() ? null : results.get(0);
	}

	public String getTablePrefix() {
		return tablePrefix;
	}

	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	/**
	 * Builds a bean of the given class from one result row through the class's public constructor
	 * taking the row {@link Map}. A failure is logged with the bean class and the exception type.
	 *
	 * @param clazz the bean class
	 * @param row the row, keyed by lower-cased column name
	 * @return the new bean
	 * @throws SQLException with a generic message if the bean cannot be built
	 */
	private static <T> T toBean(final Class<T> clazz, final Map<String, Object> row) throws SQLException {
		try {
			final Constructor<T> constructor = clazz.getConstructor(Map.class);
			return constructor.newInstance(row);
		} catch (ReflectiveOperationException | RuntimeException e) {
			Logger.error(BaseDAO.class, "Accessibility Checker row could not be read into "
					+ clazz.getName() + ": " + e.getClass().getName());
			throw new SQLException("Accessibility Checker row could not be read", e);
		}
	}

}
