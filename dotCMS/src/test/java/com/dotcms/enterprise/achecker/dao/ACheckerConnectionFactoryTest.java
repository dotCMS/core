package com.dotcms.enterprise.achecker.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.enterprise.achecker.dao.ACheckerConnectionFactory.RestrictedCredentials;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the restricted database account the Accessibility Checker connection pool uses,
 * provisioned on throw-away H2 databases.
 */
class ACheckerConnectionFactoryTest {

    /** H2 error code for a statement that needs database administrator rights. */
    private static final int ADMIN_RIGHTS_REQUIRED = 90040;

    /** H2 error code for a statement the account has no rights for. */
    private static final int NOT_ENOUGH_RIGHTS = 90096;

    /** Statements that need database administrator rights. */
    private static final List<String> ADMINISTRATOR_ONLY_STATEMENTS = List.of(
            "CALL FILE_READ('nothing')",
            "SELECT * FROM CSVREAD('nothing')",
            "CALL LINK_SCHEMA('X', '', 'jdbc:h2:mem:x', 'sa', '', 'PUBLIC')",
            "RUNSCRIPT FROM 'nothing'");

    private final List<Connection> openConnections = new ArrayList<>();

    private String restrictedUrl;
    private Connection admin;
    private RestrictedCredentials credentials;
    private Connection restricted;

    @BeforeEach
    void setUp() throws SQLException {
        final String databaseUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL";
        // DB_CLOSE_DELAY is a database setting only an administrator may apply on sign-in,
        // so the read-only account connects without it while the admin connection keeps the database alive.
        restrictedUrl = databaseUrl;
        admin = open(databaseUrl + ";DB_CLOSE_DELAY=-1", "sa", "");
        createGuidelinesTable(admin);
        credentials = ACheckerConnectionFactory.provisionRestrictedUser(admin);
        restricted = open(restrictedUrl, credentials.user(), credentials.password());
    }

    @AfterEach
    void tearDown() throws SQLException {
        try {
            final List<Connection> others = new ArrayList<>(openConnections);
            others.remove(admin);
            closeAll(others);
        } finally {
            try {
                shutdown(admin);
            } finally {
                closeAll(Collections.singletonList(admin));
                openConnections.clear();
            }
        }
    }

    @Test
    void restrictedAccountCanReadTables() throws SQLException {
        // Given an in-memory H2 database with an AC_ table, provisioned through an admin connection
        // When a connection is opened with the returned credentials and a SELECT runs
        // Then the rows are returned
        try (Statement statement = restricted.createStatement();
                ResultSet rows = statement.executeQuery("SELECT guideline_id, abbr FROM AC_guidelines")) {
            assertTrue(rows.next());
            assertEquals(1, rows.getInt("guideline_id"));
            assertEquals("WCAG2-AA", rows.getString("abbr"));
        }
    }

    @Test
    void restrictedAccountCannotDefineAliases() throws SQLException {
        // Given a connection opened with the restricted credentials
        // When a CREATE ALIAS statement runs
        // Then an SQLException with H2 error code 90040 (admin rights required) is thrown
        try (Statement statement = restricted.createStatement()) {
            final SQLException error = assertThrows(SQLException.class,
                    () -> statement.execute("CREATE ALIAS ABS_VALUE FOR 'java.lang.Math.abs(int)'"));
            assertEquals(ADMIN_RIGHTS_REQUIRED, error.getErrorCode());
        }
    }

    @Test
    void administratorOnlyStatementsAreRefused() throws SQLException {
        // Given a connection opened with the restricted credentials
        // When each statement that needs administrator rights runs
        // Then every one fails with H2 error code 90040 (admin rights required)
        for (final String sql : ADMINISTRATOR_ONLY_STATEMENTS) {
            try (Statement statement = restricted.createStatement()) {
                final SQLException error = assertThrows(SQLException.class, () -> statement.execute(sql),
                        "Expected a refusal for: " + sql);
                assertEquals(ADMIN_RIGHTS_REQUIRED, error.getErrorCode(), "Unexpected error code for: " + sql);
            }
        }
    }

    @Test
    void restrictedAccountCannotWrite() throws SQLException {
        // Given a connection opened with the restricted credentials
        // When an INSERT into an AC_ table runs
        // Then an SQLException (not enough rights) is thrown
        try (Statement statement = restricted.createStatement()) {
            final SQLException error = assertThrows(SQLException.class,
                    () -> statement.execute("INSERT INTO AC_guidelines (guideline_id, abbr) VALUES (2, 'second')"));
            assertEquals(NOT_ENOUGH_RIGHTS, error.getErrorCode());
        }
        assertEquals(1, countGuidelines(admin));
    }

    @Test
    void provisioningIsRepeatable() throws SQLException {
        // Given a database already provisioned once
        // When provisioning runs a second time on the same database
        // Then it succeeds and the returned credentials still open a connection
        final RestrictedCredentials second = ACheckerConnectionFactory.provisionRestrictedUser(admin);

        assertEquals(credentials.user(), second.user());
        assertNotEquals(credentials.password(), second.password());
        final Connection reopened = open(restrictedUrl, second.user(), second.password());
        assertEquals(1, countGuidelines(reopened));
    }

    @Test
    void existingAdministratorAccountIsDemoted() throws SQLException {
        // Given a new database where the read-only account name already exists as an administrator
        // When provisioning runs
        // Then the account loses its administrator rights and cannot define aliases
        final String databaseUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL";
        // No DB_CLOSE_DELAY: this database is dropped once its last connection is closed in tearDown.
        final Connection otherAdmin = open(databaseUrl, "sa", "");
        try (Statement statement = otherAdmin.createStatement()) {
            statement.execute("CREATE USER " + ACheckerConnectionFactory.RESTRICTED_USER + " PASSWORD 'x' ADMIN");
        }

        final RestrictedCredentials provisioned = ACheckerConnectionFactory.provisionRestrictedUser(otherAdmin);

        final Connection demoted = open(databaseUrl, provisioned.user(), provisioned.password());
        try (Statement statement = demoted.createStatement()) {
            final SQLException error = assertThrows(SQLException.class,
                    () -> statement.execute("CREATE ALIAS ABS_VALUE FOR 'java.lang.Math.abs(int)'"));
            assertEquals(ADMIN_RIGHTS_REQUIRED, error.getErrorCode());
        }
    }

    @Test
    void fileDatabaseOpensThroughTheProductionUrls(@TempDir final Path dynamicContentPath) throws Exception {
        // Given a file database under the dynamic content path, opened by an administrator and provisioned
        // When the read-only account connects through the production pool URL
        // Then it can read the AC_ tables and cannot write to them
        final String urlPrefix = ACheckerConnectionFactory.databaseUrlPrefix(dynamicContentPath.toString());
        final String poolUrl = ACheckerConnectionFactory.restrictedPoolUrl(urlPrefix);
        for (final String adminOnlySetting : List.of("INIT=", "DB_CLOSE_DELAY", "LOCK_MODE", "IGNORECASE")) {
            assertFalse(poolUrl.contains(adminOnlySetting), "The pool URL must not carry " + adminOnlySetting);
        }
        assertTrue(ACheckerConnectionFactory.adminBootstrapUrl(urlPrefix, "/x/achecker.sql")
                .contains("INIT=RUNSCRIPT"));
        Files.createDirectories(dynamicContentPath.resolve("h2db").resolve("1"));

        final Connection fileAdmin = DriverManager.getConnection(urlPrefix + ";MODE=MySQL", "sa", "");
        Connection fileReader = null;
        try {
            createGuidelinesTable(fileAdmin);
            final RestrictedCredentials provisioned = ACheckerConnectionFactory.provisionRestrictedUser(fileAdmin);

            fileReader = DriverManager.getConnection(poolUrl, provisioned.user(), provisioned.password());

            assertEquals(1, countGuidelines(fileReader));
            try (Statement statement = fileReader.createStatement()) {
                final SQLException error = assertThrows(SQLException.class,
                        () -> statement.execute("INSERT INTO AC_guidelines (guideline_id, abbr) VALUES (2, 'second')"));
                assertEquals(NOT_ENOUGH_RIGHTS, error.getErrorCode());
            }
        } finally {
            try {
                if (fileReader != null) {
                    closeAll(List.of(fileReader));
                }
            } finally {
                try {
                    shutdown(fileAdmin);
                } finally {
                    closeAll(List.of(fileAdmin));
                }
            }
        }
    }

    @Test
    void credentialsToStringOmitsThePassword() {
        // Given credentials holding an account name and a password
        // When they are turned into a string
        // Then the account name is shown and the password is not
        final RestrictedCredentials holder = new RestrictedCredentials("ACHECKER_READER", "p4ssw0rd-marker");

        final String text = holder.toString();

        assertTrue(text.contains("ACHECKER_READER"));
        assertFalse(text.contains("p4ssw0rd-marker"));
    }

    private Connection open(final String url, final String user, final String password) throws SQLException {
        final Connection connection = DriverManager.getConnection(url, user, password);
        openConnections.add(connection);
        return connection;
    }

    private static void createGuidelinesTable(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE AC_guidelines (guideline_id INT PRIMARY KEY, abbr VARCHAR(100))");
            statement.execute("INSERT INTO AC_guidelines (guideline_id, abbr) VALUES (1, 'WCAG2-AA')");
        }
    }

    private static int countGuidelines(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet count = statement.executeQuery("SELECT COUNT(*) FROM AC_guidelines")) {
            count.next();
            return count.getInt(1);
        }
    }

    /** Shuts the database down through the given administrator connection, if it is still open. */
    private static void shutdown(final Connection adminConnection) throws SQLException {
        if (adminConnection == null || adminConnection.isClosed()) {
            return;
        }
        try (Statement statement = adminConnection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
    }

    /** Closes every connection, even when one of them fails to close, and reports the first failure. */
    private static void closeAll(final List<Connection> connections) throws SQLException {
        SQLException failure = null;
        for (final Connection connection : connections) {
            if (connection == null) {
                continue;
            }
            try {
                connection.close();
            } catch (final SQLException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
