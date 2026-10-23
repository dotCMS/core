/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.dao;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.dotcms.enterprise.achecker.utility.Utility;
import com.dotmarketing.util.Logger;



/**
 * Default {@link DAO} implementation. Every query is sent as a prepared statement on a connection
 * obtained from a {@link ConnectionSupplier}; by default that is the shared Accessibility Checker
 * connection from {@link ACheckerConnectionFactory}.
 * <p>
 * The connection is shared, so this class never closes it; it only closes the statements and
 * result sets it opens.
 */
public class DAOImpl implements DAO {

	/** Message used both in the log and in the exception thrown when a query fails. */
	static final String QUERY_FAILED_MESSAGE = "Accessibility Checker query failed";

	private static final Object[] NO_PARAMS = new Object[0];

	/**
	 * Provides the connection a query runs on. Unlike {@link java.util.function.Supplier}, it may
	 * throw a checked exception, matching {@link ACheckerConnectionFactory#getConnection()}.
	 */
	@FunctionalInterface
	public interface ConnectionSupplier {

		/**
		 * Returns the connection to run the next query on. The caller does not close it.
		 *
		 * @return an open connection
		 * @throws Exception if no connection can be obtained
		 */
		Connection get() throws Exception;
	}

	private final ConnectionSupplier connectionSupplier;

	/**
	 * Creates a DAO that runs its queries on the shared Accessibility Checker connection.
	 */
	public DAOImpl() {
		this(ACheckerConnectionFactory::getConnection);
	}

	/**
	 * Creates a DAO that runs its queries on the connections handed out by the given supplier.
	 * Mainly useful in tests, where the supplier can return a connection to a throw-away database.
	 *
	 * @param connectionSupplier where each query gets its connection from; the connection is never
	 *                           closed by this class
	 */
	public DAOImpl(final ConnectionSupplier connectionSupplier) {
		this.connectionSupplier = connectionSupplier;
	}

	/**
	 * Runs a query that takes no values. Same as {@link #execute(String, Object...)} with no
	 * values.
	 *
	 * @param sql the query to run
	 * @return one map per row, keyed by lower-cased column name
	 * @throws SQLException if the query fails; the message is generic and does not repeat the query
	 */
	@Override
	public List<Map<String, Object>> execute(final String sql) throws SQLException {
		return execute(sql, NO_PARAMS);
	}

	/**
	 * Runs a query whose {@code ?} placeholders are filled, in order, with the given values, and
	 * returns all rows. Text columns stored as CLOBs are read into strings.
	 * <p>
	 * When the query fails, the log line and the thrown exception only carry the exception type,
	 * the SQL state and the vendor error code. Neither the query text nor the values are included,
	 * and the original exception is not attached, because database messages usually repeat both.
	 *
	 * @param sql the query to run, with one {@code ?} per value
	 * @param params the values for the placeholders, in order; strings, numbers and {@code null}
	 *               are supported
	 * @return one map per row, keyed by lower-cased column name
	 * @throws SQLException if the query fails, with the generic message "Accessibility Checker query
	 *                      failed" and the original SQL state and error code
	 */
	@Override
	public List<Map<String, Object>> execute(final String sql, final Object... params) throws SQLException {
		try {
			final Connection conn = connectionSupplier.get();
			try (PreparedStatement statement = conn.prepareStatement(sql.trim())) {
				bindParams(statement, params);
				try (ResultSet result = statement.executeQuery()) {
					return getObjects(result);
				}
			}
		} catch (Exception e) {
			throw queryFailure(e);
		}
	}

	/**
	 * Fills the statement's {@code ?} placeholders, in order, with the given values.
	 *
	 * @param statement the statement to fill
	 * @param params the values, in placeholder order; {@code null} means there are none
	 * @throws SQLException if a value cannot be set
	 */
	private static void bindParams(final PreparedStatement statement, final Object... params) throws SQLException {
		if (params == null) {
			return;
		}
		for (int i = 0; i < params.length; i++) {
			statement.setObject(i + 1, params[i]);
		}
	}

	/**
	 * Logs a failed query and builds the exception reported to the caller. The ERROR line carries
	 * only the exception type, SQL state and error code; the original exception, whose message may
	 * repeat the query, is only written at DEBUG level and is not attached to the returned exception.
	 *
	 * @param cause the exception raised while running the query
	 * @return a new exception with the generic message and the original SQL state and error code
	 */
	private SQLException queryFailure(final Exception cause) {
		final String sqlState = cause instanceof SQLException ? ((SQLException) cause).getSQLState() : null;
		final int errorCode = cause instanceof SQLException ? ((SQLException) cause).getErrorCode() : 0;
		Logger.error(DAOImpl.class, QUERY_FAILED_MESSAGE + ": " + cause.getClass().getName()
				+ " (SQL state: " + sqlState + ", error code: " + errorCode + ")");
		Logger.debug(DAOImpl.class, QUERY_FAILED_MESSAGE + " details", cause);
		return new SQLException(QUERY_FAILED_MESSAGE, sqlState, errorCode);
	}

	/**
	 * Reads every row of the result into a map keyed by lower-cased column name. The column names
	 * are read once, before the first row. A column that cannot be read is left out of its row and
	 * reported at WARN level with its name and the exception type only.
	 *
	 * @param result the result set to read; it is not closed here
	 * @return one map per row
	 * @throws SQLException if the result metadata or the next row cannot be read
	 */
	private List<Map<String, Object>> getObjects( ResultSet result  ) throws SQLException{

		final ResultSetMetaData metaData = result.getMetaData();
		final int columnCount = metaData.getColumnCount();
		final String[] columnNames = new String[columnCount];
		for (int i = 1; i <= columnCount; i++) {
			// H2 reports column names in upper case; callers look them up in lower case
			columnNames[i - 1] = metaData.getColumnName(i).toLowerCase(Locale.ROOT);
		}

		List<Map<String, Object>> list = new ArrayList<>();

		while (result.next()) {

			Map<String, Object> item = new HashMap<>();

			for ( int i = 1; i <= columnCount; i ++ ) {

				final String name = columnNames[i - 1];
				Object value = null;

				try {
					value = result.getObject(i);

					if ( value instanceof Clob ) {
						String content = Utility.getClobContent((Clob) value);
						item.put(name, content);
					}
					else {
						if ( value instanceof String ) {
							String content = (String) value;
							item.put(name, content);
						}
						else {
							item.put(name, value);
						}
					}
				}
				catch (Exception t) {
					Logger.warn(DAOImpl.class, "Unable to read Accessibility Checker column '" + name + "': "
							+ t.getClass().getName());
				}
			}
			list.add(item);
		}
		return list;
	}
}
