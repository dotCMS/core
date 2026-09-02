package com.dotcms.business.interceptor;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Static service locator for interceptor SPI implementations. Both ByteBuddy advice classes
 * and CDI interceptors resolve their dependencies through this provider.
 *
 * <p>By default, no-op implementations are installed so that the annotation/handler layer
 * compiles and runs without any core dependency. The core module registers the real
 * implementations at startup via {@link #init(DatabaseConnectionOps, TransactionOps, LicenseOps, InterceptorLogger)}.</p>
 *
 * <p>When the annotations and handlers are eventually extracted to a utility module, this
 * class and the SPI interfaces move with them. Only the core implementations stay behind.</p>
 */
public final class InterceptorServiceProvider {

    private static final AtomicReference<DatabaseConnectionOps> DB_OPS =
            new AtomicReference<>(NoOpDatabaseConnectionOps.INSTANCE);
    private static final AtomicReference<TransactionOps> TX_OPS =
            new AtomicReference<>(NoOpTransactionOps.INSTANCE);
    private static final AtomicReference<LicenseOps> LICENSE_OPS =
            new AtomicReference<>(NoOpLicenseOps.INSTANCE);
    private static final AtomicReference<InterceptorLogger> LOGGER =
            new AtomicReference<>(JulInterceptorLogger.INSTANCE);

    private InterceptorServiceProvider() {
        // Utility class
    }

    /**
     * Registers the real (core) implementations. Must be called once at application startup,
     * typically from {@code ByteBuddyFactory.init()} or a servlet context listener.
     */
    public static void init(final DatabaseConnectionOps dbOps,
                            final TransactionOps txOps,
                            final LicenseOps licenseOps,
                            final InterceptorLogger logger) {
        DB_OPS.set(dbOps);
        TX_OPS.set(txOps);
        LICENSE_OPS.set(licenseOps);
        LOGGER.set(logger);
    }

    public static DatabaseConnectionOps getDatabaseOps() {
        return DB_OPS.get();
    }

    public static TransactionOps getTransactionOps() {
        return TX_OPS.get();
    }

    public static LicenseOps getLicenseOps() {
        return LICENSE_OPS.get();
    }

    public static InterceptorLogger getLogger() {
        return LOGGER.get();
    }

    // ---- No-op defaults ----

    private enum NoOpDatabaseConnectionOps implements DatabaseConnectionOps {
        INSTANCE;

        @Override public boolean connectionExists() { return false; }
        @Override public Connection getConnection() { return null; }
        @Override public void setConnection(Connection connection) { }
        @Override public Connection newConnection() { return null; }
        @Override public void closeAndCommit() { }
        @Override public void closeSilently() { }
        @Override public void setAutoCommit(boolean autoCommit) { }
        @Override public void closeConnection() { }
    }

    private enum NoOpTransactionOps implements TransactionOps {
        INSTANCE;

        @Override public boolean startLocalTransactionIfNeeded() { return false; }
        @Override public void commitTransaction() { }
        @Override public void rollbackTransaction() { }
        @Override public void closeSessionSilently() { }
        @Override public void startTransaction() { }
        @Override public Object getSession() { return null; }
        @Override public void setSession(Object session) { }
        @Override public Object createNewSession(Connection connection) { return null; }
        @Override public void handleTransactionInterruption(Connection connection, StackTraceElement[] threadStack) { }
        @Override public void throwException(Throwable t) throws Exception {
            if (t instanceof RuntimeException) { throw (RuntimeException) t; }
            if (t instanceof Exception) { throw (Exception) t; }
            throw new RuntimeException(t);
        }
        @Override public String getConfigProperty(String key, String defaultValue) { return defaultValue; }
    }

    private enum NoOpLicenseOps implements LicenseOps {
        INSTANCE;

        /** No-op: all features enabled by default (max level). */
        @Override public int getLicenseLevel() { return Integer.MAX_VALUE; }
    }

    /** Falls back to {@code java.util.logging} when the dotCMS Logger is not available. */
    private enum JulInterceptorLogger implements InterceptorLogger {
        INSTANCE;

        @Override
        public void info(Class<?> clazz, String message) {
            java.util.logging.Logger.getLogger(clazz.getName()).log(Level.INFO, message);
        }

        @Override
        public void debug(Class<?> clazz, String message) {
            java.util.logging.Logger.getLogger(clazz.getName()).log(Level.FINE, message);
        }

        @Override
        public void warn(Class<?> clazz, String message) {
            java.util.logging.Logger.getLogger(clazz.getName()).log(Level.WARNING, message);
        }

        @Override
        public void error(Class<?> clazz, String message, Throwable t) {
            java.util.logging.Logger.getLogger(clazz.getName()).log(Level.SEVERE, message, t);
        }
    }
}
