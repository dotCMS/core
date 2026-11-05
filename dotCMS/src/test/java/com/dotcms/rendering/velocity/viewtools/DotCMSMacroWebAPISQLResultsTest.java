package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void poolNameUsesInternalConnectionWhenDotcmsDbAccessIsOn() {
        config.when(() -> Config.getBooleanProperty(eq(DOTCMS_DB_FLAG), any(Boolean.class))).thenReturn(true);

        macro.getSQLResults(POOL_NAME, SQL, 0, 10);

        dbFactory.verify(() -> DbConnectionFactory.getConnection(anyString()), never());
        dbFactory.verify(DbConnectionFactory::getConnection);
    }

    @Test
    void externalDataSourceIsQueriedWhenDotcmsDbAccessIsOff() throws SQLException {
        final DataSource externalDb = SQLResultsViewToolTest.unavailableDataSource();
        jndi.when(() -> JNDIUtil.lookup(any(), eq(EXTERNAL_NAME))).thenReturn(externalDb);

        macro.getSQLResults(EXTERNAL_NAME, SQL, 0, 10);

        dbFactory.verify(() -> DbConnectionFactory.getConnection(EXTERNAL_NAME));
        verify(externalDb, atLeastOnce()).getConnection();
    }
}
