package com.dotcms.rendering.velocity.viewtools;

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
import com.dotmarketing.util.Config;
import com.liferay.util.JNDIUtil;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the data source rules of {@link DotCMSMacroWebAPI#getSQLResults(String, String, int, int)}.
 */
class DotCMSMacroWebAPISQLResultsTest {

    private static final String DOTCMS_DB_FLAG = "ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB";
    private static final String POOL_NAME = "jdbc/dotCMSPool";
    private static final String EXTERNAL_NAME = "jdbc/reporting";
    private static final String DIRECTORY_URL = "ldap://directory.example:389/x";
    private static final String SQL = "select 1 as c";

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<Config> config;
    private MockedStatic<JNDIUtil> jndi;
    private MockedStatic<DbConnectionFactory> dbFactory;

    private DataSource dotcmsPool;
    private Object previousPool;
    private DotCMSMacroWebAPI macro;

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

        dotcmsPool = mock(DataSource.class);
        when(dotcmsPool.getConnection()).thenThrow(new SQLException("unavailable in unit tests"));
        previousPool = SQLResultsViewToolTest.swapDotcmsPool(dotcmsPool);
        jndi.when(() -> JNDIUtil.lookup(any(), eq(POOL_NAME))).thenReturn(dotcmsPool);
        // "default" is not a naming name: it is never bound.
        jndi.when(() -> JNDIUtil.lookup(any(), eq("default"))).thenThrow(new NamingException("not bound"));
        config.when(() -> Config.getBooleanProperty(eq(DOTCMS_DB_FLAG), any(Boolean.class))).thenReturn(false);

        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getVelocityContext()).thenReturn(mock(Context.class));
        macro = new DotCMSMacroWebAPI() {
            @Override
            protected boolean canUserEvaluate() {
                return true;
            }
        };
        macro.init(viewContext);
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            SQLResultsViewToolTest.swapDotcmsPool(previousPool);
        } finally {
            dbFactory.close();
            jndi.close();
            config.close();
            apiLocator.close();
        }
    }

    /**
     * The macro must ask the gate about the request it was initialised with: that request is
     * where the render reports who wrote the code being run, so a macro that asked about
     * anything else would be judging a different author. The override the other cases use
     * cannot see this, so this one goes through the real method.
     */
    @Test
    void theGateIsAskedAboutTheRequestTheMacroWasInitialisedWith() {
        final HttpServletRequest ownRequest = mock(HttpServletRequest.class);
        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getRequest()).thenReturn(ownRequest);
        final DotCMSMacroWebAPI plainMacro = new DotCMSMacroWebAPI();
        plainMacro.init(viewContext);
        // the switch has to be on, or the macro never gets as far as asking who wrote the code
        config.when(() -> Config.getBooleanProperty(eq("ENABLE_SCRIPTING"), any(Boolean.class)))
                .thenReturn(true);

        try (MockedStatic<RestrictedViewToolAccess> gate =
                mockStatic(RestrictedViewToolAccess.class)) {
            gate.when(() -> RestrictedViewToolAccess.renderingAuthorHasAccess(any()))
                    .thenReturn(false);

            final List<HashMap<String, String>> result =
                    plainMacro.getSQLResults(EXTERNAL_NAME, SQL, 0, 10);

            assertEquals(1, result.size(), "Expected one error row but got " + result);
            assertEquals("true", result.get(0).get("hasDotConnectSQLError"));
            gate.verify(() -> RestrictedViewToolAccess.renderingAuthorHasAccess(ownRequest));
            jndi.verify(() -> JNDIUtil.lookup(any(), anyString()), never());
        }
    }

    @Test
    void urlDataSourceIsRefusedWithoutLookup() {
        final List<HashMap<String, String>> result = macro.getSQLResults(DIRECTORY_URL, SQL, 0, 10);

        assertEquals(1, result.size(), "Expected one error row but got " + result);
        assertEquals("true", result.get(0).get("hasDotConnectSQLError"));
        assertEquals("getSQLResults macro was called with an invalid data source name",
                result.get(0).get("dotConnectSQLError"));
        jndi.verify(() -> JNDIUtil.lookup(any(), anyString()), never());
    }

    @Test
    void poolNameIsRefusedWhenDotcmsDbAccessIsOff() throws SQLException {
        macro.getSQLResults(POOL_NAME, SQL, 0, 10);

        dbFactory.verify(() -> DbConnectionFactory.getConnection(anyString()), never());
        dbFactory.verify(DbConnectionFactory::getConnection, never());
        verify(dotcmsPool, never()).getConnection();
    }

    @Test
    void unresolvableDataSourceReturnsFixedError() {
        jndi.when(() -> JNDIUtil.lookup(any(), eq("jdbc/missing")))
                .thenThrow(new NamingException("Name [jdbc/missing] is not bound in this Context"));

        final List<HashMap<String, String>> result = macro.getSQLResults("jdbc/missing", SQL, 0, 10);

        assertEquals(1, result.size(), "Expected one error row but got " + result);
        assertEquals("getSQLResults macro was called with an invalid data source name",
                result.get(0).get("dotConnectSQLError"));
    }

    @Test
    void unreachableDataSourceIsRefusedWithoutQuerying() throws SQLException {
        final DataSource down = SQLResultsViewToolTest.unavailableDataSource();
        jndi.when(() -> JNDIUtil.lookup(any(), eq("jdbc/down"))).thenReturn(down);

        final List<HashMap<String, String>> result = macro.getSQLResults("jdbc/down", SQL, 0, 10);

        assertEquals(1, result.size(), "Expected one error row but got " + result);
        assertEquals("getSQLResults macro was called with an invalid data source name",
                result.get(0).get("dotConnectSQLError"));
        dbFactory.verify(() -> DbConnectionFactory.getConnection(anyString()), never());
    }

    @Test
    void poolNameRefusalReturnsAnErrorRow() {
        final List<HashMap<String, String>> result = macro.getSQLResults(POOL_NAME, SQL, 0, 10);

        assertEquals(1, result.size(), "Expected one error row but got " + result);
        assertEquals("true", result.get(0).get("hasDotConnectSQLError"));
        assertTrue(result.get(0).get("dotConnectSQLError").contains(DOTCMS_DB_FLAG + " is set to false"));
    }

    @Test
    void dataSourceIsNotCheckedWhenDotcmsDbAccessIsOn() throws SQLException {
        config.when(() -> Config.getBooleanProperty(eq(DOTCMS_DB_FLAG), any(Boolean.class))).thenReturn(true);
        final DataSource down = SQLResultsViewToolTest.unavailableDataSource();
        jndi.when(() -> JNDIUtil.lookup(any(), eq("jdbc/down"))).thenReturn(down);

        final List<HashMap<String, String>> result = macro.getSQLResults("jdbc/down", SQL, 0, 10);

        assertFalse(String.valueOf(result.isEmpty() ? "" : result.get(0).get("dotConnectSQLError"))
                        .contains("invalid data source name"),
                "With access to the dotCMS database allowed, the query must be attempted");
        dbFactory.verify(() -> DbConnectionFactory.isDefaultDatabase(anyString()), never());
        dbFactory.verify(() -> DbConnectionFactory.getConnection("jdbc/down"));
    }

    @Test
    void unresolvableDataSourceReturnsFixedErrorWhenDotcmsDbAccessIsOn() {
        config.when(() -> Config.getBooleanProperty(eq(DOTCMS_DB_FLAG), any(Boolean.class))).thenReturn(true);
        jndi.when(() -> JNDIUtil.lookup(any(), eq("jdbc/missing")))
                .thenThrow(new NamingException("Name [jdbc/missing] is not bound in this Context"));

        final List<HashMap<String, String>> result = macro.getSQLResults("jdbc/missing", SQL, 0, 10);

        assertEquals(1, result.size(), "Expected one error row but got " + result);
        assertEquals("getSQLResults macro was called with an invalid data source name",
                result.get(0).get("dotConnectSQLError"));
        dbFactory.verify(() -> DbConnectionFactory.getConnection(anyString()), never());
    }

    @Test
    void defaultNameUsesInternalConnectionWhenDotcmsDbAccessIsOn() {
        config.when(() -> Config.getBooleanProperty(eq(DOTCMS_DB_FLAG), any(Boolean.class))).thenReturn(true);

        macro.getSQLResults("default", SQL, 0, 10);

        dbFactory.verify(() -> DbConnectionFactory.getConnection(anyString()), never());
        dbFactory.verify(DbConnectionFactory::getConnection);
    }

    @Test
    void poolNameIsQueriedThroughThatNameWhenDotcmsDbAccessIsOn() {
        config.when(() -> Config.getBooleanProperty(eq(DOTCMS_DB_FLAG), any(Boolean.class))).thenReturn(true);

        macro.getSQLResults(POOL_NAME, SQL, 0, 10);

        dbFactory.verify(() -> DbConnectionFactory.getConnection(POOL_NAME));
        dbFactory.verify(DbConnectionFactory::getConnection, never());
    }

    @Test
    void externalDataSourceIsQueriedWhenDotcmsDbAccessIsOff() throws SQLException {
        final DataSource externalDb = SQLResultsViewToolTest.externalDataSourceCheckedOnce();
        jndi.when(() -> JNDIUtil.lookup(any(), eq(EXTERNAL_NAME))).thenReturn(externalDb);

        macro.getSQLResults(EXTERNAL_NAME, SQL, 0, 10);

        dbFactory.verify(() -> DbConnectionFactory.getConnection(EXTERNAL_NAME));
        verify(externalDb, atLeastOnce()).getConnection();
    }
}
