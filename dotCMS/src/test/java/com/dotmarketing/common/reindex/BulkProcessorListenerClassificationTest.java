package com.dotmarketing.common.reindex;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import java.io.IOException;
import java.net.ConnectException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for how {@link BulkProcessorListener#afterBulk(long, Throwable)} classifies a bulk
 * request that failed as a whole (#37269, AC-006).
 *
 * <h2>What is at stake</h2>
 * <p>When the request fails before the engine returns per-document results, the listener cannot
 * tell which document caused it, so every entry of the request is marked failed. Today each one
 * gets the culprit's raw message, so all of them look like the culprit and support ends up
 * inspecting every contentlet. If the cause is a content problem (a JSON processing error), each
 * entry must instead say it failed collaterally, and one ERROR line must list the identifiers of
 * the request. Transport problems (connection, timeouts, authentication) keep today's message.</p>
 *
 * <pre>
 *   ./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false \
 *       -Dtest=BulkProcessorListenerClassificationTest
 * </pre>
 */
public class BulkProcessorListenerClassificationTest {

    private static final String CONTENT_CAUSE =
            "String value length (20054016) exceeds the maximum allowed (20000000)";

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<Logger> logger;
    private ReindexQueueAPI queue;
    private BulkProcessorListener listener;
    private ReindexEntry first;
    private ReindexEntry second;

    @Before
    public void setUp() {
        queue = mock(ReindexQueueAPI.class);
        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getReindexQueueAPI).thenReturn(queue);
        logger = mockStatic(Logger.class);

        first = ReindexEntry.builder().id(1L).identToIndex("id-1").priority(0).build();
        second = ReindexEntry.builder().id(2L).identToIndex("id-2").priority(0).build();
        listener = new BulkProcessorListener();
        listener.workingRecords.put("id-1", first);
        listener.workingRecords.put("id-2", second);
    }

    @After
    public void tearDown() {
        logger.close();
        apiLocator.close();
    }

    /**
     * Given Scenario: The request fails with a Jackson constraint violation wrapped in a runtime
     * exception, as the Elasticsearch client throws it while building the request.
     * Expected Result: Every entry is marked with a message that says it failed collaterally, and
     * exactly one ERROR line names the identifiers of the request.
     */
    @Test
    public void test_contentCause_marksCollateral_andLogsIdentifiersOnce() throws Exception {
        listener.afterBulk(1L, new RuntimeException(CONTENT_CAUSE,
                new StreamConstraintsException(CONTENT_CAUSE)));

        verify(queue).markAsFailed(eq(first), argThat(BulkProcessorListenerClassificationTest::isCollateral));
        verify(queue).markAsFailed(eq(second), argThat(BulkProcessorListenerClassificationTest::isCollateral));
        logger.verify(() -> Logger.error(any(Class.class), anyString(), any(Throwable.class)),
                times(1));
        logger.verify(() -> Logger.error(any(Class.class),
                argThat((String message) -> message.contains("id-1") && message.contains("id-2")),
                any(Throwable.class)));
    }

    /**
     * Given Scenario: The request fails because the cluster cannot be reached (control).
     * Expected Result: Every entry keeps today's message, the exception's own text.
     */
    @Test
    public void test_connectionCause_keepsTodaysMessage() throws Exception {
        listener.afterBulk(1L, new ConnectException("Connection refused"));

        verify(queue).markAsFailed(first, "Connection refused");
        verify(queue).markAsFailed(second, "Connection refused");
    }

    /**
     * Given Scenario: The request fails with an {@link IOException} that is not a JSON processing
     * error. {@code JsonProcessingException} extends {@code IOException}, so a check on
     * {@code IOException} would wrongly call this a content problem.
     * Expected Result: Treated as transport — today's message, not the collateral one.
     */
    @Test
    public void test_plainIOException_isTransport() throws Exception {
        listener.afterBulk(1L, new IOException("Read timed out"));

        verify(queue).markAsFailed(first, "Read timed out");
        verify(queue).markAsFailed(second, "Read timed out");
    }

    /** A collateral message says so and still carries the underlying cause. */
    private static boolean isCollateral(final String message) {
        return message != null && message.toLowerCase().contains("collateral")
                && message.contains("20054016");
    }
}
