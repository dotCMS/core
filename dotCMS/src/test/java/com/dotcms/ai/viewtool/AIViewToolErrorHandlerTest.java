package com.dotcms.ai.viewtool;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.json.JSONObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AIViewToolErrorHandler}, the single place where the dotAI viewtools turn
 * a caught exception into a template-facing payload (#37154).
 *
 * <p>Two properties are pinned here. First, the payload never carries anything derived from the
 * exception: not the trace, not the message, not a class name. Second, the full exception is
 * logged at ERROR under the logger of the class that caught it, so operators keep the detail that
 * templates no longer receive.</p>
 *
 * <p>The log assertion attaches a log4j2 appender to the logger named after the class this test
 * passes as {@code source}. Note that {@code com.dotmarketing.util.Logger} treats any class whose
 * name contains {@code viewtool} as a Velocity class and routes it through {@code velocityError},
 * which writes to the same named logger and appends the thread name to the message.</p>
 *
 * @author hassandotcms
 */
public class AIViewToolErrorHandlerTest {

    private static final Level ORIGINAL_LEVEL = Level.ERROR;

    private Logger sourceLogger;
    private CapturingAppender appender;
    private Level previousLevel;

    @Before
    public void attachAppender() {
        sourceLogger = (Logger) LogManager.getLogger(AIViewToolErrorHandlerTest.class);
        previousLevel = sourceLogger.getLevel();
        sourceLogger.setLevel(ORIGINAL_LEVEL);
        appender = new CapturingAppender();
        appender.start();
        sourceLogger.addAppender(appender);
    }

    @After
    public void detachAppender() {
        sourceLogger.removeAppender(appender);
        appender.stop();
        sourceLogger.setLevel(previousLevel);
    }

    /**
     * Scenario: a plain exception is handled
     * Given any runtime exception with an ordinary message
     * When the handler processes it
     * Then the payload holds exactly one key, "error", with the fixed generic message
     */
    @Test
    public void test_handle_plainException_returnsOnlyFixedError() {
        final RuntimeException cause = new RuntimeException("provider timed out after 30s");

        final JSONObject payload = AIViewToolErrorHandler.handle(AIViewToolErrorHandlerTest.class, cause);

        assertSafePayload(payload);
    }

    /**
     * Scenario: a wrapped exception whose message carries a class name is handled
     * Given a DotRuntimeException built the way OpenAIImageAPIImpl builds it, with the cause
     *       concatenated into the message, so getMessage() contains "java.net.ConnectException"
     * When the handler processes it
     * Then the payload still holds only the fixed generic message and no class name
     */
    @Test
    public void test_handle_wrappedExceptionWithClassNameMessage_returnsOnlyFixedError() {
        final ConnectException root = new ConnectException("Connection refused");
        final DotRuntimeException cause = new DotRuntimeException("Error generating image:" + root, root);
        assertTrue("precondition: wrapped message carries a class name",
                cause.getMessage().contains("java.net.ConnectException"));

        final JSONObject payload = AIViewToolErrorHandler.handle(AIViewToolErrorHandlerTest.class, cause);

        assertSafePayload(payload);
        assertFalse(payload.getString("error").contains("ConnectException"));
        assertFalse(payload.getString("error").contains("Connection refused"));
    }

    /**
     * Scenario: an exception with a null message is handled
     * Given a RuntimeException constructed with a null message
     * When the handler processes it
     * Then the payload holds the fixed generic message, not "null" and not an empty string
     */
    @Test
    public void test_handle_nullMessage_returnsOnlyFixedError() {
        final RuntimeException cause = new RuntimeException((String) null);

        final JSONObject payload = AIViewToolErrorHandler.handle(AIViewToolErrorHandlerTest.class, cause);

        assertSafePayload(payload);
        assertFalse("null".equals(payload.getString("error")));
    }

    /**
     * Scenario: the failure is logged server-side with the exception attached
     * Given any exception
     * When the handler processes it with this class as the source
     * Then exactly one ERROR event is emitted on this class's logger, carrying the same exception
     *      instance as its throwable, with a fixed message that does not repeat the exception text
     */
    @Test
    public void test_handle_logsOneErrorEventWithThrowable() {
        final RuntimeException cause = new RuntimeException("secret-ish provider detail 0xDEADBEEF");

        AIViewToolErrorHandler.handle(AIViewToolErrorHandlerTest.class, cause);

        assertEquals("expected exactly one log event, got " + appender.events, 1, appender.events.size());
        final CapturedEvent event = appender.events.get(0);
        assertEquals(Level.ERROR, event.level);
        assertSame("logged throwable must be the caught exception itself", cause, event.thrown);
        assertTrue("log message must carry the fixed text, was: " + event.message,
                event.message.contains(AIViewToolErrorHandler.LOG_MESSAGE));
        assertFalse("log message must not repeat the exception message", event.message.contains("0xDEADBEEF"));
        assertEquals(AIViewToolErrorHandlerTest.class.getName(), event.loggerName);
    }

    private static void assertSafePayload(final JSONObject payload) {
        assertNotNull(payload);
        assertEquals("payload must carry exactly one key, got " + payload.keySet(), 1, payload.size());
        assertTrue(payload.containsKey("error"));
        assertFalse(payload.containsKey("stackTrace"));
        final String error = payload.getString("error");
        assertEquals(AIViewToolErrorHandler.GENERIC_ERROR_MESSAGE, error);
        for (final String marker : new String[] {"Exception", "\tat ", ".java:", "com.dotcms"}) {
            assertFalse("payload leaks internal detail (" + marker.trim() + "): " + error, error.contains(marker));
        }
    }

    /**
     * Snapshot of a log event. log4j2 may reuse mutable event objects, so fields are copied at
     * append time rather than holding the event.
     */
    private static final class CapturedEvent {
        private final Level level;
        private final String message;
        private final Throwable thrown;
        private final String loggerName;

        private CapturedEvent(final LogEvent event) {
            this.level = event.getLevel();
            this.message = event.getMessage().getFormattedMessage();
            this.thrown = event.getThrown();
            this.loggerName = event.getLoggerName();
        }

        @Override
        public String toString() {
            return level + " [" + loggerName + "] " + message;
        }
    }

    private static final class CapturingAppender extends AbstractAppender {

        private final List<CapturedEvent> events = new ArrayList<>();

        private CapturingAppender() {
            super("AIViewToolErrorHandlerTest-appender", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(final LogEvent event) {
            events.add(new CapturedEvent(event));
        }
    }

}
