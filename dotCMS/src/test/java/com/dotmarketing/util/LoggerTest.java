package com.dotmarketing.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public class LoggerTest {

    /**
     * The {@code Supplier} overloads exist so message building is skipped when the level is
     * disabled. Verifies the supplier is only evaluated when debug logging is enabled.
     */
    @Test
    public void debug_supplier_is_lazy() {
        final String loggerName = LoggerTest.class.getName();
        try {
            Logger.setLevel(loggerName, "INFO");
            final AtomicBoolean evaluated = new AtomicBoolean(false);
            Logger.debug(this, () -> {
                evaluated.set(true);
                return "should not be built";
            });
            assertFalse("supplier must not be evaluated when debug is disabled", evaluated.get());

            Logger.setLevel(loggerName, "DEBUG");
            Logger.debug(this, () -> {
                evaluated.set(true);
                return "should be built";
            });
            assertTrue("supplier must be evaluated when debug is enabled", evaluated.get());
        } finally {
            Logger.setLevel(loggerName, "INFO");
        }
    }

    @Test
    public void info_supplier_is_lazy() {
        final String loggerName = LoggerTest.class.getName();
        try {
            Logger.setLevel(loggerName, "WARN");
            final AtomicBoolean evaluated = new AtomicBoolean(false);
            Logger.info(this, () -> {
                evaluated.set(true);
                return "should not be built";
            });
            assertFalse("supplier must not be evaluated when info is disabled", evaluated.get());

            Logger.setLevel(loggerName, "INFO");
            Logger.info(this, () -> {
                evaluated.set(true);
                return "should be built";
            });
            assertTrue("supplier must be evaluated when info is enabled", evaluated.get());
        } finally {
            Logger.setLevel(loggerName, "INFO");
        }
    }
}
