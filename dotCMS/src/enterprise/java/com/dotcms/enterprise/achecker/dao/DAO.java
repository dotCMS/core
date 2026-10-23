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

/**
 * Runs read-only queries against the Accessibility Checker database and returns each row as a map
 * keyed by lower-cased column name.
 */
public interface DAO {

	/**
	 * Runs a query that takes no values.
	 *
	 * @param sql the query to run
	 * @return one map per row, keyed by lower-cased column name
	 * @throws SQLException if the query fails
	 */
	public List<Map<String, Object>> execute(String sql) throws SQLException;

	/**
	 * Runs a query whose {@code ?} placeholders are filled, in order, with the given values. The
	 * values travel to the database separately from the query text, so they are always compared as
	 * plain data.
	 *
	 * @param sql the query to run, with one {@code ?} per value
	 * @param params the values for the placeholders, in order; strings, numbers and {@code null}
	 *               are supported
	 * @return one map per row, keyed by lower-cased column name
	 * @throws SQLException if the query fails
	 */
	List<Map<String, Object>> execute(String sql, Object... params) throws SQLException;

}
