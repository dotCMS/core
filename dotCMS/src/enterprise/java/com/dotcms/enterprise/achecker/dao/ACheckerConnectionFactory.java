/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/
package com.dotcms.enterprise.achecker.dao;

import java.io.File;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;

import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;

/**
 * Provides connections to the embedded H2 database that holds the Accessibility Checker guidelines,
 * checks and language texts.
 * <p>
 * The database is bootstrapped once per JVM through a single administrator connection, which loads
 * {@code achecker.sql} and provisions a read-only account. Every connection handed out afterwards
 * comes from a pool that signs in with that read-only account, so callers can query the
 * {@code AC_} tables but cannot change data or run statements that need database administrator
 * rights. If the bootstrap fails, no connection is handed out and the next call tries again.
 */
public class ACheckerConnectionFactory {

	/** Name of the read-only database account the connection pool signs in with. */
	static final String RESTRICTED_USER = "ACHECKER_READER";

	/** Message of the exception thrown when no connection to the database can be handed out. */
	static final String UNAVAILABLE_MESSAGE = "Accessibility Checker database is not available";

	private static final String DATABASE_RELATIVE_PATH = "h2db/1/achecker_db1";

	private static final String BOOTSTRAP_SCRIPT_NAME = "achecker.sql";

	private static final int PASSWORD_BYTES = 32;

	private static final int MAX_POOL_CONNECTIONS = 10;

	private static final int POOL_LOGIN_TIMEOUT_SECONDS = 3;

	private static final SecureRandom PASSWORD_RANDOM = new SecureRandom();

	private static JdbcConnectionPool pool = null;

	private static Connection connection = null;

	/**
	 * Returns the shared connection to the Accessibility Checker database, opening it on first use.
	 * <p>
	 * The connection belongs to the read-only account, never to the database administrator. On the
	 * first call the database is bootstrapped (see the class description); if that fails, this
	 * method throws and a later call tries the bootstrap again.
	 * <p>
	 * If the existing read-only pool can no longer hand out a connection, for example because the
	 * account's password was changed by another process or a pooled session broke, the pool is
	 * discarded and the bootstrap runs once more in the same call. Only when that second attempt
	 * also fails does this method throw.
	 *
	 * @return an open connection signed in with the read-only account; never {@code null}
	 * @throws SQLException with a fixed message when the database cannot be reached or bootstrapped
	 */
	public static synchronized Connection getConnection() throws SQLException {
		if (isConnectionOpen()) {
			return connection;
		}
		final String dynamicContentPath;
		final String bootstrapScriptPath;
		try {
			dynamicContentPath = ConfigUtils.getDynamicContentPath();
			bootstrapScriptPath = ConfigUtils.getACheckerPath() + File.separator + BOOTSTRAP_SCRIPT_NAME;
		} catch (final RuntimeException e) {
			throw unavailable(e);
		}
		return getConnection(dynamicContentPath, bootstrapScriptPath);
	}

	/**
	 * Returns the shared connection, opening it on first use from the database under the given
	 * directory and bootstrapping it with the given script. Behaves exactly like
	 * {@link #getConnection()}, which calls it with the dotCMS locations; tests call it with
	 * temporary ones.
	 *
	 * @param dynamicContentPath the directory the database file lives under
	 * @param bootstrapScriptPath the location of the script that loads the database
	 * @return an open connection signed in with the read-only account; never {@code null}
	 * @throws SQLException with a fixed message when the database cannot be reached or bootstrapped
	 */
	static synchronized Connection getConnection(final String dynamicContentPath, final String bootstrapScriptPath)
			throws SQLException {
		try {
			if (!isConnectionOpen()) {
				connection = openConnection(dynamicContentPath, bootstrapScriptPath);
				Logger.debug(ACheckerConnectionFactory.class, "Accessibility Checker database connection opened");
			}
			return connection;
		} catch (final Exception e) {
			throw unavailable(e);
		}
	}

	/**
	 * Closes the shared connection and disposes the read-only pool, so the next connection request
	 * bootstraps the database again. Only meant for tests, which need a clean state between runs.
	 */
	static synchronized void resetForTests() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (final SQLException e) {
			Logger.warn(ACheckerConnectionFactory.class,
					"Unable to close the Accessibility Checker database connection: " + e.getClass().getName());
		} finally {
			connection = null;
			if (pool != null) {
				discardPool();
			}
		}
	}

	/**
	 * Replaces the read-only pool and closes the shared connection, so the next connection request
	 * goes through the given pool first. Only meant for tests, which use it to check what happens
	 * when the current pool can no longer open connections.
	 *
	 * @param replacement the pool the next connection request should use
	 */
	static synchronized void usePoolForTests(final JdbcConnectionPool replacement) {
		resetForTests();
		pool = replacement;
	}

	/**
	 * Tells whether the shared connection exists and is still open. A connection whose state
	 * cannot be read is treated as closed, so a new one is opened.
	 *
	 * @return {@code true} if the shared connection can be handed out as it is
	 */
	private static boolean isConnectionOpen() {
		try {
			return connection != null && !connection.isClosed();
		} catch (final SQLException e) {
			return false;
		}
	}

	/**
	 * Logs why no connection could be opened and builds the exception reported to the caller. The
	 * ERROR line carries only the exception type; the full exception is written at DEBUG level.
	 *
	 * @param cause the failure raised while opening the connection
	 * @return a new exception with the fixed "not available" message and no cause attached
	 */
	private static SQLException unavailable(final Exception cause) {
		Logger.error(ACheckerConnectionFactory.class,
				"Unable to open the Accessibility Checker database: " + cause.getClass().getName());
		Logger.debug(ACheckerConnectionFactory.class,
				"Accessibility Checker database bootstrap failure details", cause);
		return new SQLException(UNAVAILABLE_MESSAGE);
	}

	/**
	 * Creates or refreshes the read-only account the connection pool uses.
	 * <p>
	 * The account receives a freshly generated random password on every call, loses any database
	 * administrator rights and is allowed to read every table in the {@code PUBLIC} schema. Calling
	 * this method again on the same database is safe: the account is kept and only its password
	 * changes. The password is never logged, and the given connection is left open.
	 *
	 * @param admin an open connection with database administrator rights
	 * @return the account name and the password that was just set for it
	 * @throws SQLException if any of the account statements fails
	 */
	public static RestrictedCredentials provisionRestrictedUser(final Connection admin) throws SQLException {
		final String password = generatePassword();
		try (PreparedStatement createUser = admin.prepareStatement(
				"CREATE USER IF NOT EXISTS " + RESTRICTED_USER + " PASSWORD ?")) {
			createUser.setString(1, password);
			createUser.execute();
		}
		try (PreparedStatement setPassword = admin.prepareStatement(
				"ALTER USER " + RESTRICTED_USER + " SET PASSWORD ?")) {
			setPassword.setString(1, password);
			setPassword.execute();
		}
		try (Statement statement = admin.createStatement()) {
			statement.execute("ALTER USER " + RESTRICTED_USER + " ADMIN FALSE");
			statement.execute("GRANT SELECT ON SCHEMA PUBLIC TO " + RESTRICTED_USER);
		}
		return new RestrictedCredentials(RESTRICTED_USER, password);
	}

	/**
	 * Builds the JDBC URL prefix that points at the Accessibility Checker database file.
	 *
	 * @param dynamicContentPath the dotCMS dynamic content directory the database lives under
	 * @return the URL prefix shared by the administrator and the read-only connections
	 */
	static String databaseUrlPrefix(final String dynamicContentPath) {
		final String location = new File(dynamicContentPath + File.separator + DATABASE_RELATIVE_PATH).getAbsolutePath();
		return ("jdbc:h2:split:nio:" + location).replace("\\", "/");
	}

	/**
	 * Builds the URL for the administrator connection that loads the bootstrap script when it opens.
	 *
	 * @param urlPrefix the prefix returned by {@link #databaseUrlPrefix(String)}
	 * @param bootstrapScriptPath the location of {@code achecker.sql}
	 * @return the administrator bootstrap URL
	 */
	static String adminBootstrapUrl(final String urlPrefix, final String bootstrapScriptPath) {
		return (urlPrefix + ";LOCK_MODE=0;DB_CLOSE_ON_EXIT=TRUE;IGNORECASE=FALSE;FILE_LOCK=NO;MODE=MySQL;"
				+ "INIT=RUNSCRIPT FROM '" + bootstrapScriptPath + "' CHARSET 'utf8'").replace("\\", "/");
	}

	/**
	 * Builds the URL the read-only connection pool uses. It carries only settings a read-only
	 * account may apply when signing in, so it has no bootstrap script and no database-wide settings.
	 *
	 * @param urlPrefix the prefix returned by {@link #databaseUrlPrefix(String)}
	 * @return the read-only pool URL
	 */
	static String restrictedPoolUrl(final String urlPrefix) {
		return urlPrefix + ";DB_CLOSE_ON_EXIT=TRUE;FILE_LOCK=NO;MODE=MySQL";
	}

	/**
	 * Opens a new read-only connection. An existing pool is tried first; if it fails, it is
	 * discarded and the database is bootstrapped again, which provisions a fresh password and pool.
	 *
	 * @param dynamicContentPath the directory the database file lives under
	 * @param bootstrapScriptPath the location of the script that loads the database
	 * @return an open connection from the read-only pool
	 * @throws SQLException if the bootstrap fails
	 */
	private static Connection openConnection(final String dynamicContentPath, final String bootstrapScriptPath)
			throws SQLException {
		if (pool != null) {
			try {
				return pool.getConnection();
			} catch (final SQLException | RuntimeException e) {
				Logger.warn(ACheckerConnectionFactory.class,
						"Accessibility Checker connection pool could not open a connection, bootstrapping again: "
								+ e.getClass().getName());
				Logger.debug(ACheckerConnectionFactory.class,
						"Accessibility Checker connection pool failure details", e);
				discardPool();
			}
		}
		return bootstrap(dynamicContentPath, bootstrapScriptPath);
	}

	/**
	 * Closes every connection of the current read-only pool and forgets it, so the next connection
	 * request bootstraps a new pool.
	 */
	private static void discardPool() {
		try {
			pool.dispose();
		} catch (final RuntimeException e) {
			Logger.warn(ACheckerConnectionFactory.class,
					"Unable to dispose the Accessibility Checker connection pool: " + e.getClass().getName());
		} finally {
			pool = null;
		}
	}

	/**
	 * Loads the database through one administrator connection, provisions the read-only account and
	 * opens the read-only pool. The pool is only kept when its first connection opens; the
	 * administrator connection is always closed before returning.
	 *
	 * @param dynamicContentPath the directory the database file lives under
	 * @param bootstrapScriptPath the location of the script that loads the database
	 * @return the first connection from the read-only pool
	 * @throws SQLException if the database cannot be loaded or the read-only account cannot sign in
	 */
	private static Connection bootstrap(final String dynamicContentPath, final String bootstrapScriptPath)
			throws SQLException {
		final String urlPrefix = databaseUrlPrefix(dynamicContentPath);

		final JdbcDataSource adminDataSource = new JdbcDataSource();
		adminDataSource.setURL(adminBootstrapUrl(urlPrefix, bootstrapScriptPath));
		adminDataSource.setUser("sa");
		adminDataSource.setPassword("sa");

		JdbcConnectionPool restrictedPool = null;
		final Connection admin = adminDataSource.getConnection();
		try {
			final RestrictedCredentials credentials = provisionRestrictedUser(admin);
			restrictedPool = JdbcConnectionPool.create(
					restrictedPoolUrl(urlPrefix), credentials.user(), credentials.password());
			restrictedPool.setMaxConnections(MAX_POOL_CONNECTIONS);
			restrictedPool.setLoginTimeout(POOL_LOGIN_TIMEOUT_SECONDS);
			final Connection firstConnection = restrictedPool.getConnection();
			pool = restrictedPool;
			return firstConnection;
		} catch (final SQLException | RuntimeException e) {
			if (restrictedPool != null) {
				restrictedPool.dispose();
			}
			throw e;
		} finally {
			closeAdminConnection(admin);
		}
	}

	/**
	 * Closes the administrator connection used for the bootstrap. A failure to close it is logged
	 * with the exception type only and does not stop the bootstrap.
	 *
	 * @param admin the administrator connection
	 */
	private static void closeAdminConnection(final Connection admin) {
		try {
			admin.close();
		} catch (final SQLException e) {
			Logger.warn(ACheckerConnectionFactory.class,
					"Unable to close the Accessibility Checker bootstrap connection: " + e.getClass().getName());
		}
	}

	/**
	 * Generates a random password for the read-only account: 32 random bytes written as 64
	 * hexadecimal characters, so it never contains a quote or other special character.
	 *
	 * @return a new random password
	 */
	private static String generatePassword() {
		final byte[] randomBytes = new byte[PASSWORD_BYTES];
		PASSWORD_RANDOM.nextBytes(randomBytes);
		return HexFormat.of().formatHex(randomBytes);
	}

	/**
	 * Closes the shared connection handed out by {@link #getConnection()}, if one is open. The
	 * shared reference is always cleared, even when closing fails, so the next call to
	 * {@link #getConnection()} opens a fresh connection from the read-only pool.
	 *
	 * @throws SQLException if the connection could not be closed
	 */
	public static synchronized void closeConnection() throws SQLException {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (final Exception e) {
			Logger.error(ACheckerConnectionFactory.class,
					"Unable to close the Accessibility Checker database connection: " + e.getClass().getName(), e);
			throw new SQLException(e.toString());
		} finally {
			connection = null;
		}
	}

	/**
	 * Returns the major version of the embedded H2 database engine behind the Accessibility Checker
	 * database, opening the shared connection if needed.
	 *
	 * @return the database major version
	 * @throws Exception if the database cannot be reached or its metadata cannot be read
	 */
	public static int getDbVersion() throws Exception {
		int version = 0;
		try {
			Connection con = getConnection();
			DatabaseMetaData meta = con.getMetaData();
			version = meta.getDatabaseMajorVersion();
		} catch (Exception e) {
			Logger.error(ACheckerConnectionFactory.class,
					"Unable to read the Accessibility Checker database version: " + e.getClass().getName(), e);
			throw new Exception(e.toString());
		}
		return version;
	}

	/**
	 * Name and password of the read-only database account. The password is never included in
	 * {@link #toString()}.
	 */
	public static final class RestrictedCredentials {

		private final String user;

		private final String password;

		/**
		 * Creates the credentials holder.
		 *
		 * @param user the account name
		 * @param password the account password
		 */
		public RestrictedCredentials(final String user, final String password) {
			this.user = user;
			this.password = password;
		}

		/**
		 * @return the account name
		 */
		public String user() {
			return user;
		}

		/**
		 * @return the account password
		 */
		public String password() {
			return password;
		}

		@Override
		public String toString() {
			return "RestrictedCredentials{user=" + user + "}";
		}
	}
}
