package com.dotmarketing.db;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liferay.util.JNDIUtil;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/**
 * Unit tests for how {@link DbConnectionFactory} validates and recognises data source names.
 */
class DbConnectionFactoryDataSourceNameTest {

    private static final String DIRECTORY_URL = "ldap://directory.example:389/x";
    private static final String EXTERNAL_NAME = "jdbc/reporting";

    // ---- isLocalJndiName: accepted names ----

    @Test
    void plainResourceNameIsLocal() {
        assertTrue(DbConnectionFactory.isLocalJndiName(EXTERNAL_NAME));
    }

    @Test
    void compEnvPrefixedNameIsLocal() {
        assertTrue(DbConnectionFactory.isLocalJndiName("java:comp/env/jdbc/reporting"));
    }

    @Test
    void javaPrefixedNameIsLocal() {
        assertTrue(DbConnectionFactory.isLocalJndiName("java:jdbc/reporting"));
    }

    // ---- isLocalJndiName: rejected names ----

    @Test
    void directoryUrlIsNotLocal() {
        assertFalse(DbConnectionFactory.isLocalJndiName(DIRECTORY_URL));
    }

    @Test
    void remoteObjectUrlIsNotLocal() {
        assertFalse(DbConnectionFactory.isLocalJndiName("rmi://objects.example:1099/x"));
    }

    @Test
    void urlAfterJavaPrefixIsNotLocal() {
        assertFalse(DbConnectionFactory.isLocalJndiName("java:ldap://directory.example/x"));
    }

    @Test
    void bareSchemeIsNotLocal() {
        assertFalse(DbConnectionFactory.isLocalJndiName("dns:example.com"));
    }

    @Test
    void upperCaseSchemeIsNotLocal() {
        assertFalse(DbConnectionFactory.isLocalJndiName("LDAP://directory.example/x"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingNameIsNotLocal(final String name) {
        assertFalse(DbConnectionFactory.isLocalJndiName(name));
    }

    @Test
    void prefixWithoutNameIsNotLocal() {
        assertFalse(DbConnectionFactory.isLocalJndiName("java:"));
    }

    @Test
    void leadingColonIsNotLocal() {
        assertFalse(DbConnectionFactory.isLocalJndiName(":x"));
    }

    // ---- getDataSource(String) ----

    @Test
    void nonLocalNameIsRejectedBeforeLookup() {
        try (MockedStatic<JNDIUtil> jndi = mockStatic(JNDIUtil.class)) {
            assertThrows(IllegalArgumentException.class,
                    () -> DbConnectionFactory.getDataSource(DIRECTORY_URL));
            jndi.verify(() -> JNDIUtil.lookup(any(), anyString()), never());
        }
    }

    @Test
    void rejectionDoesNotEchoTheName() {
        try (MockedStatic<JNDIUtil> ignored = mockStatic(JNDIUtil.class)) {
            final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> DbConnectionFactory.getDataSource(DIRECTORY_URL));
            assertFalse(String.valueOf(error.getMessage()).contains("directory.example"),
                    "The message must not repeat the supplied name");
        }
    }

    @Test
    void localNameIsLookedUpUnchanged() {
        final DataSource external = mock(DataSource.class);
        try (MockedStatic<JNDIUtil> jndi = mockStatic(JNDIUtil.class)) {
            jndi.when(() -> JNDIUtil.lookup(any(), eq(EXTERNAL_NAME))).thenReturn(external);

            assertSame(external, DbConnectionFactory.getDataSource(EXTERNAL_NAME));
            jndi.verify(() -> JNDIUtil.lookup(any(), eq(EXTERNAL_NAME)));
        }
    }

    // ---- isSameDataSource ----

    @Test
    void samePoolInstanceIsRecognised() {
        final DataSource pool = mock(DataSource.class);
        assertTrue(DbConnectionFactory.isSameDataSource(pool, pool));
    }

    @Test
    void wrappedContainerDataSourceIsRecognised() {
        final DataSource container = mock(DataSource.class);
        final HikariDataSource pool = new HikariDataSource();
        pool.setDataSource(container);

        assertTrue(DbConnectionFactory.isSameDataSource(container, pool));
    }

    @Test
    void otherDataSourceUnderPooledSetupIsNotRecognised() {
        final HikariDataSource pool = new HikariDataSource();
        pool.setDataSource(mock(DataSource.class));

        assertFalse(DbConnectionFactory.isSameDataSource(mock(DataSource.class), pool));
    }

    @Test
    void otherDataSourceIsNotRecognised() {
        assertFalse(DbConnectionFactory.isSameDataSource(mock(DataSource.class), mock(DataSource.class)));
    }

    @Test
    void nullCandidateIsNotRecognised() {
        assertFalse(DbConnectionFactory.isSameDataSource(null, mock(DataSource.class)));
    }

    // ---- isDefaultDataSourceName ----

    @ParameterizedTest
    @ValueSource(strings = {"jdbc/dotCMSPool", "java:jdbc/dotCMSPool", "java:comp/env/jdbc/dotCMSPool"})
    void poolNameIsRecognisedInEverySpelling(final String name) {
        assertTrue(DbConnectionFactory.isDefaultDataSourceName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {EXTERNAL_NAME, "jdbc/dotCMSPool2", "java:comp/env/jdbc/reporting"})
    void otherNamesAreNotThePoolName(final String name) {
        assertFalse(DbConnectionFactory.isDefaultDataSourceName(name));
    }

    // ---- holdsDotcmsData ----

    @Test
    void databaseHoldingDotcmsDataIsRecognised() throws SQLException {
        assertTrue(DbConnectionFactory.holdsDotcmsData(dataSourceAnswering(true)));
    }

    @Test
    void databaseWithoutDotcmsDataIsNotRecognised() throws SQLException {
        assertFalse(DbConnectionFactory.holdsDotcmsData(dataSourceAnswering(false)));
    }

    @Test
    void databaseGivingNoAnswerIsNotRecognised() throws SQLException {
        assertFalse(DbConnectionFactory.holdsDotcmsData(dataSourceAnswering(null)));
    }

    @Test
    void unreachableDatabaseIsNotRecognised() throws SQLException {
        final DataSource unreachable = mock(DataSource.class);
        when(unreachable.getConnection()).thenThrow(new SQLException("unavailable"));

        assertFalse(DbConnectionFactory.holdsDotcmsData(unreachable));
    }

    @Test
    void otherEngineThatCannotRunTheCheckIsNotRecognised() throws SQLException {
        final DataSource otherEngine = dataSourceAnswering(true, "MySQL");
        when(otherEngine.getConnection().createStatement().executeQuery(anyString()))
                .thenThrow(new SQLException("unsupported catalog"));

        assertFalse(DbConnectionFactory.holdsDotcmsData(otherEngine));
    }

    @Test
    void postgresDatabaseThatCannotRunTheCheckIsRecognised() throws SQLException {
        final DataSource postgres = dataSourceAnswering(true, "PostgreSQL");
        when(postgres.getConnection().createStatement().executeQuery(anyString()))
                .thenThrow(new SQLException("canceling statement due to statement timeout"));

        assertTrue(DbConnectionFactory.holdsDotcmsData(postgres));
    }

    @Test
    void answerIsReadOncePerDataSource() throws SQLException {
        final DataSource dataSource = dataSourceAnswering(false);

        DbConnectionFactory.holdsDotcmsData(dataSource);
        DbConnectionFactory.holdsDotcmsData(dataSource);

        verify(dataSource, times(1)).getConnection();
    }

    @Test
    void postgresAnswerFromAFailedCheckIsNotKept() throws SQLException {
        final DataSource postgres = dataSourceAnswering(false, "PostgreSQL");
        when(postgres.getConnection().createStatement().executeQuery(anyString()))
                .thenThrow(new SQLException("canceling statement due to statement timeout"));

        DbConnectionFactory.holdsDotcmsData(postgres);
        DbConnectionFactory.holdsDotcmsData(postgres);

        verify(postgres, times(3)).getConnection(); // once while stubbing above, then once per check
    }

    @Test
    void answerIsReturnedWhenClosingTheConnectionFails() throws SQLException {
        final DataSource dataSource = dataSourceAnswering(true);
        final Connection connection = dataSource.getConnection();
        doThrow(new SQLException("close failed")).when(connection).close();

        assertTrue(DbConnectionFactory.holdsDotcmsData(dataSource));
    }

    @Test
    void unreachableDatabaseIsCheckedAgainNextTime() throws SQLException {
        final DataSource unreachable = mock(DataSource.class);
        when(unreachable.getConnection()).thenThrow(new SQLException("unavailable"));

        DbConnectionFactory.holdsDotcmsData(unreachable);
        DbConnectionFactory.holdsDotcmsData(unreachable);

        verify(unreachable, times(2)).getConnection();
    }

    @Test
    void missingDataSourceIsNotRecognised() {
        assertFalse(DbConnectionFactory.holdsDotcmsData(null));
    }

    @Test
    void checkConnectionIsClosed() throws SQLException {
        final DataSource dataSource = dataSourceAnswering(true);

        DbConnectionFactory.holdsDotcmsData(dataSource);

        verify(dataSource.getConnection()).close();
    }

    /**
     * A data source whose database answers the dotCMS data check with the given value, or returns no
     * row when the value is {@code null}.
     */
    static DataSource dataSourceAnswering(final Boolean holdsDotcmsData) throws SQLException {
        return dataSourceAnswering(holdsDotcmsData, "PostgreSQL");
    }

    /** Same as {@link #dataSourceAnswering(Boolean)}, for a database that reports the given product name. */
    static DataSource dataSourceAnswering(final Boolean holdsDotcmsData, final String product) throws SQLException {
        final DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getDatabaseProductName()).thenReturn(product);
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(holdsDotcmsData != null);
        when(resultSet.getBoolean(1)).thenReturn(Boolean.TRUE.equals(holdsDotcmsData));
        final Statement statement = mock(Statement.class);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        final Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.getMetaData()).thenReturn(metaData);
        final DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        return dataSource;
    }
}
