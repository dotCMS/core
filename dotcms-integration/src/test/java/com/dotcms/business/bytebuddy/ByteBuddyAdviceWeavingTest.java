package com.dotcms.business.bytebuddy;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestCostApi;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.db.DbConnectionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Verifies that {@link ByteBuddyFactory} successfully wove its advice into methods
 * annotated with the dotCMS interception annotations ({@link WrapInTransaction},
 * {@link CloseDBIfOpened}, {@link RequestCost}). These tests fail if the agent
 * never installed (e.g. a regression of the Java 21+/25 container hang where
 * {@code ByteBuddyAgent.installExternal} blocked on {@code Process.waitFor()}),
 * because the annotated methods would execute as plain Java with no transaction
 * started, no connection cleanup, and no request-cost increment.
 *
 * <p>The subject classes live under {@code com.dotcms} so they match
 * {@link ByteBuddyFactory}'s package whitelist.</p>
 */
public class ByteBuddyAdviceWeavingTest extends IntegrationTestBase {

    private static HttpServletRequest priorRequest;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        priorRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
    }

    @AfterClass
    public static void cleanup() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(priorRequest);
        DbConnectionFactory.closeSilently();
    }

    /**
     * Sanity check: the static {@code agentLoaded} flag must be true after init,
     * proving {@link ByteBuddyFactory#init()} reached the {@code premain} phase.
     */
    @Test
    public void byteBuddyAgentIsLoaded() throws Exception {
        final Field field = ByteBuddyFactory.class.getDeclaredField("agentLoaded");
        field.setAccessible(true);
        final AtomicBoolean loaded = (AtomicBoolean) field.get(null);
        assertTrue("ByteBuddy agent should be loaded — ByteBuddyFactory.init() must have completed",
                loaded.get());
    }

    /**
     * Calling a {@code @WrapInTransaction}-annotated method must start a transaction.
     * Without bytecode weaving the body would run with whatever connection state
     * the caller had — here, no transaction.
     */
    @Test
    public void wrapInTransactionAdviceIsWoven() throws Exception {
        DbConnectionFactory.closeSilently();
        assertFalse("Pre-condition: no transaction before the call",
                DbConnectionFactory.inTransaction());

        final boolean[] inTxInside = new boolean[1];
        final TxSubject subject = new TxSubject();
        subject.transactional(() -> inTxInside[0] = DbConnectionFactory.inTransaction());

        assertTrue("Inside a @WrapInTransaction method, must be in a transaction "
                + "(advice was not woven if this fails)", inTxInside[0]);
        assertFalse("Transaction must be committed and closed after the call",
                DbConnectionFactory.inTransaction());
    }

    /**
     * Calling a {@code @CloseDBIfOpened}-annotated method that opens a connection
     * must result in the connection being closed on exit when it was opened by the
     * advice itself.
     */
    @Test
    public void closeDBIfOpenedAdviceIsWoven() throws Exception {
        DbConnectionFactory.closeSilently();
        assertFalse("Pre-condition: no connection before the call",
                DbConnectionFactory.connectionExists());

        final boolean[] connExistedInside = new boolean[1];
        final CloseDbSubject subject = new CloseDbSubject();
        subject.closeIfOpened(() -> {
            // Touch the connection so the advice's "newly opened" branch closes it on exit.
            DbConnectionFactory.getConnection();
            connExistedInside[0] = DbConnectionFactory.connectionExists();
        });

        assertTrue("Connection should be alive inside the @CloseDBIfOpened method",
                connExistedInside[0]);
        assertFalse("@CloseDBIfOpened must close the connection it opened "
                + "(advice was not woven if this fails)",
                DbConnectionFactory.connectionExists());
    }

    /**
     * Calling a {@code @RequestCost}-annotated method must increment the running
     * total stored on the current HttpServletRequest. Verifies the advice fires
     * end-to-end through {@link com.dotcms.business.interceptor.RequestCostHandler}
     * and {@link RequestCostApi}.
     */
    @Test
    public void requestCostAdviceIsWoven() throws Exception {
        final Map<String, Object> attributes = new HashMap<>();
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getAttribute(anyString()))
                .thenAnswer(inv -> attributes.get(inv.<String>getArgument(0)));
        Mockito.doAnswer(inv -> {
            attributes.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(request).setAttribute(anyString(), any());

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        try {
            final CostSubject subject = new CostSubject();
            subject.charge();

            final Object running = attributes.get(RequestCostApi.REQUEST_COST_RUNNING_TOTAL_ATTRIBUTE);
            assertNotNull("@RequestCost advice must have set the running-total attribute "
                    + "on the current request (advice was not woven if this is null)", running);
            assertEquals("Running total should equal the single charge price",
                    Price.TEN.price, ((Integer) running).intValue());
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(priorRequest);
        }
    }

    // ---------- Subject classes (must declare annotated methods so
    // ByteBuddyFactory.hasAnnotatedMethods() matches them for transformation) ----------

    @FunctionalInterface
    private interface Block {
        void run() throws Exception;
    }

    public static class TxSubject {
        @WrapInTransaction
        public void transactional(final Block block) throws Exception {
            block.run();
        }
    }

    public static class CloseDbSubject {
        @CloseDBIfOpened
        public void closeIfOpened(final Block block) throws Exception {
            block.run();
        }
    }

    public static class CostSubject {
        @RequestCost(Price.TEN)
        public void charge() {
            // body intentionally empty — the cost is added by the advice on entry
        }
    }
}
