package com.dotmarketing.common.reindex;

import com.dotcms.UnitTestBase;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.api.system.event.PayloadVerifierFactory;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.verifier.RoleVerifier;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.index.domain.IndexBulkListener;
import com.dotcms.content.index.domain.IndexBulkProcessor;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ServletContext;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Locale;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ReindexThread}
 * @author jsanca
 */
public class ReindexThreadUnitTest extends UnitTestBase {

    private boolean testGenerateNotification = false;
    private PayloadVerifier originalRoleVerifier;

    @Test()
    public void testGenerateNotification() throws Exception {

        final NotificationAPI notificationAPI = mock(NotificationAPI.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final ServletContext context = mock(ServletContext.class);
        final ReindexQueueAPI jAPI = mock(ReindexQueueAPI.class);
        final ContentletIndexAPI indexApi = mock(ContentletIndexAPI.class);
        final Locale locale = new Locale.Builder().setLanguage("en").setRegion("US").build();

        String cmsAdminRoleId = UUID.randomUUID().toString();

        //Getting the original version of the Verifier in order to restore it after the test
        PayloadVerifierFactory payloadVerifierFactory = PayloadVerifierFactory.getInstance();
        Payload payload = new Payload(Visibility.ROLE, cmsAdminRoleId);
        this.originalRoleVerifier = payloadVerifierFactory.getVerifier(payload);

        //Mocking the Notification Visibility
        PayloadVerifier roleVerifier = new RoleVerifier(roleAPI);
        payloadVerifierFactory.register(Visibility.ROLE, roleVerifier);

        final ReindexThread reindexThread = new ReindexThread(jAPI, notificationAPI, userAPI, roleAPI, indexApi);
        final String identToIndex = "index1";
        final String msg = "Could not re-index record with the Identifier '"
                + identToIndex
                + "'. The record is in a bad state or can be associated to orphaned records. You can try running the Fix Assets Inconsistencies tool and re-start the reindex.";

        //Mock the system user
        final User user = new User();
        user.setLocale(locale);
        user.setUserId("admin@dotcms.com");
        when(userAPI.getSystemUser()).thenReturn(user);

        //Mock the CMS Admin Role
        final Role cmsAdminRole = new Role();
        cmsAdminRole.setId(cmsAdminRoleId);
        cmsAdminRole.setName("CMS Administrator");
        cmsAdminRole.setRoleKey("CMS Administrator");
        when(roleAPI.loadCMSAdminRole()).thenReturn(cmsAdminRole);

        this.initMessages();
        Config.CONTEXT = context;
        try {
            when(context.getInitParameter(WebKeys.COMPANY_ID)).thenReturn(RestUtilTest.DEFAULT_COMPANY);

            doAnswer(new Answer<Void>() { // if this method is called, should fail

                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {

                    testGenerateNotification = true;
                    return null;
                }
            }).when(notificationAPI).generateNotification(
                    new I18NMessage("notification.reindex.error.title"),
                    new I18NMessage("notification.reindexing.error.processrecord", msg, identToIndex),
                    null,
                    NotificationLevel.INFO,
                    NotificationType.GENERIC,
                    Visibility.ROLE,
                    cmsAdminRoleId,
                    user.getUserId(),
                    locale
            );

            //Execute the notification call
            reindexThread.sendNotification
                    ("notification.reindexing.error.processrecord",
                            new Object[] {identToIndex}, msg, false);

            //Validate
            assertTrue(this.testGenerateNotification);
        } finally {
            Config.CONTEXT = null;
        }
    }

    /**
     * Verifies that {@code runReindexLoop} creates a <em>fresh</em>
     * {@link BulkProcessorListener} for every batch, eliminating the TOCTOU race
     * where {@code putAll(batch N+1)} could interleave with {@code afterBulk(batch N)}
     * reading {@code workingRecords} on the BulkProcessor callback thread.
     *
     * <p>The test runs the actual loop (via reflection) against fully mocked dependencies
     * and uses a {@link CountDownLatch} to let exactly two batches complete before
     * stopping the thread.</p>
     */
    @Test
    public void testPerBatchListenerIsolation() throws Exception {
        // — mocks —
        final ReindexQueueAPI queueApi       = mock(ReindexQueueAPI.class);
        final ContentletIndexAPI indexApi    = mock(ContentletIndexAPI.class);
        final IndexBulkProcessor mockProc    = mock(IndexBulkProcessor.class);

        final ReindexEntry e1 = mock(ReindexEntry.class);
        final ReindexEntry e2 = mock(ReindexEntry.class);
        final Map<String, ReindexEntry> batch1 = Map.of("id1", e1);
        final Map<String, ReindexEntry> batch2 = Map.of("id2", e2);

        // Alternate batch1 / batch2 so the queue is never empty and
        // finalizeReIndex() (which uses static helpers) is never reached.
        final AtomicInteger callCount = new AtomicInteger();
        when(queueApi.findContentToReindex())
                .thenAnswer(inv -> (callCount.getAndIncrement() % 2 == 0) ? batch1 : batch2);

        // close() inside try-with-resources must be a no-op
        doAnswer(inv -> null).when(mockProc).close();
        when(indexApi.createBulkProcessor(any(IndexBulkListener.class))).thenReturn(mockProc);

        // Count down once per batch so the test thread knows when to stop
        final CountDownLatch twoBatches = new CountDownLatch(2);
        doAnswer(inv -> { twoBatches.countDown(); return null; })
                .when(indexApi).appendToBulkProcessor(any(), any());

        // — instantiate (sets the static singleton used by stopThread) —
        final ReindexThread thread = new ReindexThread(
                queueApi,
                mock(NotificationAPI.class),
                mock(UserAPI.class),
                mock(RoleAPI.class),
                indexApi);
        setStateRunning(thread);

        // — run the private loop in a background thread —
        final Method runLoop = ReindexThread.class.getDeclaredMethod("runReindexLoop");
        runLoop.setAccessible(true);
        final Thread loopThread = new Thread(() -> {
            try {
                runLoop.invoke(thread);
            } catch (Exception ignored) {
                // InvocationTargetException is expected if stopThread races with invoke
            }
        });
        loopThread.start();

        // — wait for two complete batches, then stop —
        assertTrue("Two batches should complete within 5 s",
                twoBatches.await(5, TimeUnit.SECONDS));
        ReindexThread.stopThread();
        loopThread.join(3_000);
        assertFalse("runReindexLoop must exit after stopThread()", loopThread.isAlive());

        // — verify per-batch isolation —
        final ArgumentCaptor<IndexBulkListener> captor =
                ArgumentCaptor.forClass(IndexBulkListener.class);
        verify(indexApi, atLeast(2)).createBulkProcessor(captor.capture());

        final List<IndexBulkListener> captured = captor.getAllValues();
        final BulkProcessorListener l0 = (BulkProcessorListener) captured.get(0);
        final BulkProcessorListener l1 = (BulkProcessorListener) captured.get(1);

        assertNotSame(
                "Each batch must receive a fresh listener — reusing listeners is the TOCTOU hazard",
                l0, l1);
        assertEquals("First listener must contain only batch-1 entries",
                Set.of("id1"), l0.workingRecords.keySet());
        assertEquals("Second listener must contain only batch-2 entries",
                Set.of("id2"), l1.workingRecords.keySet());
    }

    @After
    public void restore() {
        //Restore the original version of the Verifier
        if (originalRoleVerifier != null) {
            PayloadVerifierFactory payloadVerifierFactory = PayloadVerifierFactory.getInstance();
            payloadVerifierFactory.register(Visibility.ROLE, this.originalRoleVerifier);
        }
    }

    /**
     * Uses reflection to set the private {@code state} field of a {@link ReindexThread}
     * instance to {@code ThreadState.RUNNING}, bypassing the private enum visibility.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setStateRunning(final ReindexThread thread) throws Exception {
        final Field stateField = ReindexThread.class.getDeclaredField("state");
        stateField.setAccessible(true);
        final AtomicReference<Object> stateRef =
                (AtomicReference<Object>) stateField.get(thread);

        for (final Class<?> inner : ReindexThread.class.getDeclaredClasses()) {
            if (inner.isEnum() && "ThreadState".equals(inner.getSimpleName())) {
                stateRef.set(Enum.valueOf((Class<Enum>) inner, "RUNNING"));
                return;
            }
        }
        throw new IllegalStateException("ThreadState.RUNNING not found via reflection");
    }
}
