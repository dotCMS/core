package com.dotmarketing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.Test;

/**
 * Tests {@link Logger#debug(Class, java.util.function.Supplier)}: a call that passes a
 * {@code Class} must be checked against, and written under, the logger of that class.
 */
public class LoggerDebugClassSupplierTest {

    /**
     * Given the logger of a class set to DEBUG,
     * When {@code Logger.debug} is called with that class and a message supplier,
     * Then exactly one event is written and its logger name is the name of that class.
     */
    @Test
    public void debug_with_class_and_supplier_logs_under_that_class() {
        final org.apache.logging.log4j.core.Logger target = targetLogger();
        final Level previousLevel = target.getLevel();
        final CapturingAppender appender = new CapturingAppender();
        appender.start();
        target.addAppender(appender);
        target.setLevel(Level.DEBUG);
        try {
            Logger.debug(LoggerDebugClassSupplierTest.class, () -> "supplied message");

            assertEquals(1, appender.loggerNames.size());
            assertEquals(LoggerDebugClassSupplierTest.class.getName(), appender.loggerNames.get(0));
            assertEquals("supplied message", appender.messages.get(0));
        } finally {
            target.removeAppender(appender);
            target.setLevel(previousLevel);
            appender.stop();
        }
    }

    /**
     * Given the logger of a class set to INFO,
     * When {@code Logger.debug} is called with that class and a message supplier,
     * Then the supplier is never invoked.
     */
    @Test
    public void debug_with_class_and_supplier_skips_supplier_when_debug_is_off() {
        final org.apache.logging.log4j.core.Logger target = targetLogger();
        final Level previousLevel = target.getLevel();
        target.setLevel(Level.INFO);
        try {
            final AtomicBoolean invoked = new AtomicBoolean(false);

            Logger.debug(LoggerDebugClassSupplierTest.class, () -> {
                invoked.set(true);
                return "should not be built";
            });

            assertFalse("supplier must not run when debug is disabled", invoked.get());
        } finally {
            target.setLevel(previousLevel);
        }
    }

    private static org.apache.logging.log4j.core.Logger targetLogger() {
        return (org.apache.logging.log4j.core.Logger) LogManager.getLogger(
                LoggerDebugClassSupplierTest.class.getName());
    }

    /**
     * Keeps the logger name and text of every event logged while it is attached.
     */
    private static final class CapturingAppender extends AbstractAppender {

        private final List<String> loggerNames = new ArrayList<>();
        private final List<String> messages = new ArrayList<>();

        private CapturingAppender() {
            super("LoggerDebugClassSupplierTest-appender", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public synchronized void append(final LogEvent event) {
            loggerNames.add(event.getLoggerName());
            messages.add(event.getMessage().getFormattedMessage());
        }
    }
}
