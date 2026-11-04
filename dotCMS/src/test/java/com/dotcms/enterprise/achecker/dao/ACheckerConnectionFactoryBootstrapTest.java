package com.dotcms.enterprise.achecker.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the bootstrap of the Accessibility Checker database through
 * {@link ACheckerConnectionFactory}, run on a file database under a temporary directory with a
 * small bootstrap script in place of {@code achecker.sql}.
 */
class ACheckerConnectionFactoryBootstrapTest {

    /** H2 error code for a statement the account has no rights for. */
    private static final int NOT_ENOUGH_RIGHTS = 90096;

    /** H2 error code for a wrong user name or password. */
    private static final int WRONG_USER_OR_PASSWORD = 28000;

    private static final String BOOTSTRAP_SCRIPT =
            "CREATE TABLE IF NOT EXISTS AC_guidelines(guideline_id INT, abbr VARCHAR(100));\n"
                    + "DELETE FROM AC_guidelines;\n"
                    + "INSERT INTO AC_guidelines VALUES (7,'WCAG2-AA');\n";

    @TempDir
    Path dynamicContentPath;

    private String bootstrapScriptPath;

    @BeforeEach
    void writeBootstrapScript() throws IOException {
        Files.createDirectories(dynamicContentPath.resolve("h2db").resolve("1"));
        final Path script = dynamicContentPath.resolve("bootstrap.sql");
        Files.write(script, BOOTSTRAP_SCRIPT.getBytes(StandardCharsets.UTF_8));
        bootstrapScriptPath = script.toString();
    }

    @AfterEach
    void resetFactory() {
        ACheckerConnectionFactory.resetForTests();
    }

    @Test
    void firstConnectionUsesTheReadOnlyAccount() throws SQLException {
        // Given a database directory and a bootstrap script
        // When the first connection is requested
        // Then it belongs to the read-only account, can read the loaded rows and cannot write
        final Connection connection =
                ACheckerConnectionFactory.getConnection(dynamicContentPath.toString(), bootstrapScriptPath);

        assertEquals(ACheckerConnectionFactory.RESTRICTED_USER, currentUser(connection));
        assertEquals("WCAG2-AA", guidelineAbbr(connection, 7));
        try (Statement statement = connection.createStatement()) {
            final SQLException error = assertThrows(SQLException.class,
                    () -> statement.execute("INSERT INTO AC_guidelines VALUES (8, 'second')"));
            assertEquals(NOT_ENOUGH_RIGHTS, error.getErrorCode());
        }
    }

    @Test
    void missingBootstrapScriptFailsClosedAndRetries() throws SQLException {
        // Given a bootstrap script path that does not exist
        // When a connection is requested
        // Then the fixed "not available" message is reported and no connection is handed out
        final String missingScriptPath = dynamicContentPath.resolve("missing.sql").toString();

        final SQLException error = assertThrows(SQLException.class,
                () -> ACheckerConnectionFactory.getConnection(dynamicContentPath.toString(), missingScriptPath));

        assertEquals(ACheckerConnectionFactory.UNAVAILABLE_MESSAGE, error.getMessage());
        assertNull(error.getCause());

        // And a later request with a valid script bootstraps the database
        final Connection connection =
                ACheckerConnectionFactory.getConnection(dynamicContentPath.toString(), bootstrapScriptPath);

        assertEquals(ACheckerConnectionFactory.RESTRICTED_USER, currentUser(connection));
        assertEquals("WCAG2-AA", guidelineAbbr(connection, 7));
    }

    @Test
    void discardedPoolIsRecreated() throws SQLException {
        // Given a database that was bootstrapped once
        final Connection first =
                ACheckerConnectionFactory.getConnection(dynamicContentPath.toString(), bootstrapScriptPath);
        assertNotNull(first);

        // When the shared connection and the pool are discarded and a connection is requested again
        ACheckerConnectionFactory.resetForTests();
        assertTrue(first.isClosed());
        final Connection second =
                ACheckerConnectionFactory.getConnection(dynamicContentPath.toString(), bootstrapScriptPath);

        // Then a working read-only connection is handed out from a new pool
        assertFalse(second.isClosed());
        assertEquals(ACheckerConnectionFactory.RESTRICTED_USER, currentUser(second));
        assertEquals("WCAG2-AA", guidelineAbbr(second, 7));
    }

    @Test
    void poolThatCannotOpenConnectionsIsReplaced() throws SQLException {
        // Given a bootstrapped database whose current pool can no longer sign in
        ACheckerConnectionFactory.getConnection(dynamicContentPath.toString(), bootstrapScriptPath);
        final String urlPrefix = ACheckerConnectionFactory.databaseUrlPrefix(dynamicContentPath.toString());
        final JdbcConnectionPool unusablePool = JdbcConnectionPool.create(
                ACheckerConnectionFactory.restrictedPoolUrl(urlPrefix),
                ACheckerConnectionFactory.RESTRICTED_USER, "not-the-password");
        final SQLException signInError = assertThrows(SQLException.class, unusablePool::getConnection);
        assertEquals(WRONG_USER_OR_PASSWORD, signInError.getErrorCode());
        ACheckerConnectionFactory.usePoolForTests(unusablePool);

        // When a connection is requested
        final Connection connection =
                ACheckerConnectionFactory.getConnection(dynamicContentPath.toString(), bootstrapScriptPath);

        // Then the unusable pool is disposed and a working read-only connection comes from a new one
        assertThrows(IllegalStateException.class, unusablePool::getConnection);
        assertEquals(ACheckerConnectionFactory.RESTRICTED_USER, currentUser(connection));
        assertEquals("WCAG2-AA", guidelineAbbr(connection, 7));
    }

    @Test
    void bootstrapConnectionIsClosed() throws SQLException {
        // Given a bootstrapped database
        ACheckerConnectionFactory.getConnection(dynamicContentPath.toString(), bootstrapScriptPath);

        // When the open sessions are listed by an administrator
        final JdbcDataSource admin = new JdbcDataSource();
        admin.setURL(ACheckerConnectionFactory.databaseUrlPrefix(dynamicContentPath.toString())
                + ";FILE_LOCK=NO;MODE=MySQL");
        admin.setUser("sa");
        admin.setPassword("sa");
        try (Connection adminConnection = admin.getConnection();
                Statement statement = adminConnection.createStatement();
                ResultSet sessions = statement.executeQuery(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS WHERE USER_NAME = 'SA'")) {

            // Then the only administrator session is the one opened by this test
            assertTrue(sessions.next());
            assertEquals(1, sessions.getInt(1));
        }
    }

    private static String currentUser(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet user = statement.executeQuery("SELECT CURRENT_USER")) {
            assertTrue(user.next());
            return user.getString(1);
        }
    }

    private static String guidelineAbbr(final Connection connection, final int guidelineId) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT abbr FROM AC_guidelines WHERE guideline_id = " + guidelineId)) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }
}
