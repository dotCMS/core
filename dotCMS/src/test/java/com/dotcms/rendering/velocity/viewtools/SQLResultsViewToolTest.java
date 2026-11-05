package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.liferay.util.JNDIUtil;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the data source rules of {@link SQLResultsViewTool}.
 */
class SQLResultsViewToolTest {

    private static final String DOTCMS_DB_FLAG = "ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB";
    private static final String DOTCMS_DB_REFUSED = DOTCMS_DB_FLAG + " is set to false";
    private static final String INVALID_DATA_SOURCE = "Invalid Data Source";
    private static final String AUTHOR_REFUSED = "User failed to execute SQL queries";
    private static final String ERROR_KEY = "dotConnectSQLError";
    private static final String HAS_ERROR_KEY = "hasDotConnectSQLError";

    private static final String POOL_NAME = "jdbc/dotCMSPool";
    private static final String EXTERNAL_NAME = "jdbc/reporting";
    private static final String DIRECTORY_URL = "ldap://directory.example:389/x";
    private static final String SQL = "select 1 as c";

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<Config> config;
    private MockedStatic<JNDIUtil> jndi;
    private MockedStatic<DbConnectionFactory> dbFactory;

    private DataSource dotcmsPool;
    private DataSource externalDb;
    private Object previousPool;
    private Contentlet author;
    private SQLResultsViewTool tool;

    /**
     * Loads, with the real APILocator, the classes these tests reach that keep an API in a static field. Loaded for the
     * first time while APILocator is mocked, they would keep a null API for every later test in the same JVM.
     */
    @BeforeAll
    static void loadClassesWithStaticApis() throws ClassNotFoundException {
        Class.forName(SQLUtil.class.getName());
    }

    @BeforeEach
    void setUp() throws Exception {
        apiLocator = mockStatic(APILocator.class);
        config = mockStatic(Config.class);
        jndi = mockStatic(JNDIUtil.class);
        // Real behavior, observed only through verify(): no stubbing on this mock.
        dbFactory = mockStatic(DbConnectionFactory.class, CALLS_REAL_METHODS);

        dotcmsPool = unavailableDataSource();
        externalDb = externalDataSourceCheckedOnce();
        previousPool = swapDotcmsPool(dotcmsPool);

        jndi.when(() -> JNDIUtil.lookup(any(), eq(POOL_NAME))).thenReturn(dotcmsPool);
        jndi.when(() -> JNDIUtil.lookup(any(), eq("java:comp/env/" + POOL_NAME))).thenReturn(dotcmsPool);
        jndi.when(() -> JNDIUtil.lookup(any(), eq(EXTERNAL_NAME))).thenReturn(externalDb);
        givenDotcmsDbAccess(false);

        author = mock(Contentlet.class);
        when(author.getIdentifier()).thenReturn("widget-identifier");

        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getVelocityContext()).thenReturn(mock(Context.class));
        tool = new SQLResultsViewTool() {
            @Override
            protected Contentlet getDbAccessorContentlet() {
                return author;
            }
        };
        tool.init(viewContext);
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            swapDotcmsPool(previousPool);
        } finally {
            dbFactory.close();
            jndi.close();
            config.close();
            apiLocator.close();
        }
    }

    // ---- name checks ----

    @Test
    void missingDataSourceReturnsError() {
        final List<HashMap<String, String>> result =
                assertDoesNotThrow(() -> tool.getSQLResults(null, SQL, 0, 10));

        assertSingleError(result, INVALID_DATA_SOURCE);
    }

    @Test
    void emptyDataSourceReturnsError() {
        assertSingleError(tool.getSQLResults("", SQL, 0, 10), INVALID_DATA_SOURCE);
    }

    @Test
    void urlDataSourceIsRefusedWithoutLookup() {
        final List<HashMap<String, String>> result = tool.getSQLResults(DIRECTORY_URL, SQL, 0, 10);

        assertSingleError(result, INVALID_DATA_SOURCE);
        jndi.verify(() -> JNDIUtil.lookup(any(), anyString()), never());
    }

    @Test
    void dataSourceIsNotResolvedWhenAuthorCannotRunQueries() {
        author = null;

        final List<HashMap<String, String>> result = tool.getSQLResults(EXTERNAL_NAME, SQL, 0, 10);

        assertSingleError(result, AUTHOR_REFUSED);
        jndi.verify(() -> JNDIUtil.lookup(any(), anyString()), never());
    }

    // ---- dotCMS database flag ----

    @Test
    void defaultNameIsRefusedWhenDotcmsDbAccessIsOff() throws SQLException {
        assertSingleError(tool.getSQLResults("default", SQL, 0, 10), DOTCMS_DB_REFUSED);
        verify(dotcmsPool, never()).getConnection();
    }

    @Test
    void poolNameIsRefusedWhenDotcmsDbAccessIsOff() throws SQLException {
        assertSingleError(tool.getSQLResults(POOL_NAME, SQL, 0, 10), DOTCMS_DB_REFUSED);
        verify(dotcmsPool, never()).getConnection();
    }

    @Test
    void prefixedPoolNameIsRefusedWhenDotcmsDbAccessIsOff() throws SQLException {
        assertSingleError(tool.getSQLResults("java:comp/env/" + POOL_NAME, SQL, 0, 10), DOTCMS_DB_REFUSED);
        verify(dotcmsPool, never()).getConnection();
    }

    @Test
    void externalDataSourceIsQueriedWhenDotcmsDbAccessIsOff() throws SQLException {
        final List<HashMap<String, String>> result = tool.getSQLResults(EXTERNAL_NAME, SQL, 0, 10);

        assertFalse(firstError(result).contains(DOTCMS_DB_REFUSED),
                "An external database must not be refused by the dotCMS database flag");
        verify(externalDb, atLeastOnce()).getConnection();
        dbFactory.verify(() -> DbConnectionFactory.getConnection(EXTERNAL_NAME));
    }

    @Test
    void separateDataSourceToDotcmsDatabaseIsRefusedWhenDotcmsDbAccessIsOff() throws Exception {
        final DataSource secondPool = dataSourceHoldingDotcmsData();
        jndi.when(() -> JNDIUtil.lookup(any(), eq("jdbc/secondPool"))).thenReturn(secondPool);

        assertSingleError(tool.getSQLResults("jdbc/secondPool", SQL, 0, 10), DOTCMS_DB_REFUSED);
        dbFactory.verify(() -> DbConnectionFactory.getConnection(anyString()), never());
    }

    @Test
    void containerDataSourceBehindThePoolIsRefusedWhenDotcmsDbAccessIsOff() throws Exception {
        final DataSource container = unavailableDataSource();
        final HikariDataSource pool = new HikariDataSource();
        pool.setDataSource(container);
        swapDotcmsPool(pool);
        jndi.when(() -> JNDIUtil.lookup(any(), eq("jdbc/container"))).thenReturn(container);

        assertSingleError(tool.getSQLResults("jdbc/container", SQL, 0, 10), DOTCMS_DB_REFUSED);
        dbFactory.verify(() -> DbConnectionFactory.getConnection(anyString()), never());
    }

    @Test
    void unresolvableDataSourceReturnsFixedError() {
        jndi.when(() -> JNDIUtil.lookup(any(), eq("jdbc/missing")))
                .thenThrow(new NamingException("Name [jdbc/missing] is not bound in this Context"));

        final List<HashMap<String, String>> result = tool.getSQLResults("jdbc/missing", SQL, 0, 10);

        assertSingleError(result, INVALID_DATA_SOURCE);
        assertFalse(firstError(result).contains("jdbc/missing"), "The error must not repeat the supplied name");
    }

    @Test
    void unreachableDataSourceIsRefusedWithoutQuerying() throws Exception {
        final DataSource down = unavailableDataSource();
        jndi.when(() -> JNDIUtil.lookup(any(), eq("jdbc/down"))).thenReturn(down);

        final List<HashMap<String, String>> result = tool.getSQLResults("jdbc/down", SQL, 0, 10);

        assertSingleError(result, INVALID_DATA_SOURCE);
        dbFactory.verify(() -> DbConnectionFactory.getConnection(anyString()), never());
    }

    @Test
    void dataSourceIsNotCheckedWhenDotcmsDbAccessIsOn() throws Exception {
        givenDotcmsDbAccess(true);
        final DataSource down = unavailableDataSource();
        jndi.when(() -> JNDIUtil.lookup(any(), eq("jdbc/down"))).thenReturn(down);

        final List<HashMap<String, String>> result = tool.getSQLResults("jdbc/down", SQL, 0, 10);

        assertFalse(firstError(result).contains(INVALID_DATA_SOURCE),
                "With access to the dotCMS database allowed, the query must be attempted");
        dbFactory.verify(() -> DbConnectionFactory.isDefaultDatabase(anyString()), never());
        dbFactory.verify(() -> DbConnectionFactory.getConnection("jdbc/down"));
    }

    @Test
    void defaultNameUsesInternalConnectionWhenDotcmsDbAccessIsOn() throws SQLException {
        givenDotcmsDbAccess(true);

        tool.getSQLResults("default", SQL, 0, 10);

        dbFactory.verify(() -> DbConnectionFactory.getConnection(anyString()), never());
        dbFactory.verify(DbConnectionFactory::getConnection);
        verify(externalDb, never()).getConnection();
    }

    @Test
    void poolNameIsQueriedThroughThatNameWhenDotcmsDbAccessIsOn() {
        givenDotcmsDbAccess(true);

        tool.getSQLResults(POOL_NAME, SQL, 0, 10);

        dbFactory.verify(() -> DbConnectionFactory.getConnection(POOL_NAME));
        dbFactory.verify(DbConnectionFactory::getConnection, never());
    }

    @Test
    void separateDataSourceToDotcmsDatabaseIsQueriedThroughThatNameWhenDotcmsDbAccessIsOn() throws Exception {
        givenDotcmsDbAccess(true);
        final DataSource secondPool = dataSourceHoldingDotcmsData();
        jndi.when(() -> JNDIUtil.lookup(any(), eq("jdbc/secondPool"))).thenReturn(secondPool);

        tool.getSQLResults("jdbc/secondPool", SQL, 0, 10);

        dbFactory.verify(() -> DbConnectionFactory.getConnection("jdbc/secondPool"));
        dbFactory.verify(DbConnectionFactory::getConnection, never());
    }

    // ---- helpers ----

    private void givenDotcmsDbAccess(final boolean allowed) {
        config.when(() -> Config.getBooleanProperty(eq(DOTCMS_DB_FLAG), any(Boolean.class))).thenReturn(allowed);
    }

    /** A data source whose database holds dotCMS data. */
    static DataSource dataSourceHoldingDotcmsData() throws SQLException {
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(true);
        final Statement statement = mock(Statement.class);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        final Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(statement);
        final DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        return dataSource;
    }

    /**
     * A data source to a database without dotCMS data that opens one connection, used by the check,
     * and is unavailable afterwards, so the query itself does not reach a database.
     */
    static DataSource externalDataSourceCheckedOnce() throws SQLException {
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(false);
        final Statement statement = mock(Statement.class);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        final Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(statement);
        final DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection())
                .thenReturn(connection)
                .thenThrow(new SQLException("unavailable in unit tests"));
        return dataSource;
    }

    static DataSource unavailableDataSource() throws SQLException {
        final DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("unavailable in unit tests"));
        return dataSource;
    }

    /** Replaces the dotCMS pool held by {@link DbConnectionFactory} and returns the previous one. */
    static Object swapDotcmsPool(final Object pool) throws Exception {
        final Field field = DbConnectionFactory.class.getDeclaredField("defaultDataSource");
        field.setAccessible(true);
        final Object previous = field.get(null);
        field.set(null, pool);
        return previous;
    }

    private static String firstError(final List<HashMap<String, String>> result) {
        return result.isEmpty() ? "" : String.valueOf(result.get(0).get(ERROR_KEY));
    }

    private static void assertSingleError(final List<HashMap<String, String>> result, final String expected) {
        assertEquals(1, result.size(), "Expected exactly one error row but got " + result);
        assertEquals("true", result.get(0).get(HAS_ERROR_KEY));
        assertTrue(firstError(result).contains(expected),
                "Expected an error containing '" + expected + "' but got '" + firstError(result) + "'");
    }
}
