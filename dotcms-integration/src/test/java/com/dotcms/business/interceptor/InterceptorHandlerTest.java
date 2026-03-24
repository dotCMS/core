package com.dotcms.business.interceptor;

import com.dotcms.IntegrationTestBase;
import com.dotcms.enterprise.license.DotInvalidLicenseException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.db.DbConnectionFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration tests for the interceptor handler classes, verifying they
 * preserve behavioral compatibility with the original ByteBuddy advice
 * and LocalTransaction implementations.
 */
public class InterceptorHandlerTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    // ---- EnterpriseFeatureHandler ----

    @Test(expected = DotInvalidLicenseException.class)
    public void checkLicense_throws_DotInvalidLicenseException_when_insufficient() {
        // Level 0 < any required level should throw the correct exception type
        EnterpriseFeatureHandler.checkLicense(Integer.MAX_VALUE, "test error");
    }

    @Test
    public void checkLicense_does_not_throw_when_sufficient() {
        // Should not throw — current license level should be >= 0
        EnterpriseFeatureHandler.checkLicense(0, "should not throw");
    }

    // ---- WrapInTransactionHandler ----

    @Test
    public void wrapReturn_preserves_RuntimeException() {
        final IllegalStateException original = new IllegalStateException("test");
        try {
            WrapInTransactionHandler.wrapReturn(() -> {
                throw original;
            });
            fail("Should have thrown");
        } catch (IllegalStateException e) {
            assertSame("Should be the exact same exception instance", original, e);
        } catch (Exception e) {
            fail("Should have thrown IllegalStateException, got: " + e.getClass().getName());
        }
    }

    @Test
    public void wrapReturn_commits_on_success() throws Exception {
        DbConnectionFactory.closeSilently();

        final String result = WrapInTransactionHandler.wrapReturn(() -> {
            assertTrue("Should be in transaction",
                    DbConnectionFactory.inTransaction());
            return "ok";
        });

        assertEquals("ok", result);
        assertFalse("Connection should be closed after wrapReturn",
                DbConnectionFactory.connectionExists());
    }

    @Test
    public void wrapReturn_rolls_back_on_error() throws Exception {
        DbConnectionFactory.closeSilently();

        try {
            WrapInTransactionHandler.wrapReturn(() -> {
                assertTrue("Should be in transaction",
                        DbConnectionFactory.inTransaction());
                throw new RuntimeException("rollback me");
            });
            fail("Should have thrown");
        } catch (RuntimeException e) {
            assertEquals("rollback me", e.getMessage());
        }

        assertFalse("Connection should be closed after rollback",
                DbConnectionFactory.connectionExists());
    }

    // ---- ExternalTransactionHandler ----

    @Test
    public void externalizeTransaction_uses_separate_connection() throws Exception {
        DbConnectionFactory.closeSilently();

        // Start a parent connection — getConnection() returns a ManagedConnection wrapper
        // each time, so capture the Connection object for identity/equals comparison.
        final java.sql.Connection parentConn = DbConnectionFactory.getConnection();

        final java.sql.Connection innerConn = ExternalTransactionHandler.externalizeTransaction(() ->
                DbConnectionFactory.getConnection()
        );

        assertNotEquals("External transaction should use a different connection",
                parentConn, innerConn);

        // Parent connection should be restored — equals() compares the underlying
        // delegate connections, so a different ManagedConnection wrapper with the
        // same delegate (but owns=false after restore) will still match.
        assertEquals("Parent connection should be restored",
                parentConn, DbConnectionFactory.getConnection());

        DbConnectionFactory.closeSilently();
    }
}
