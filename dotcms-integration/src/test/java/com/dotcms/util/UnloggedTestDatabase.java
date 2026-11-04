package com.dotcms.util;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

/** Opt-in preparation for disposable test databases; never used by application startup. */
public final class UnloggedTestDatabase {

    /** Prevents construction of this test utility. */
    private UnloggedTestDatabase() {
    }

    /**
     * Converts existing application tables atomically after schema initialization.
     * The SQL path is absent by default and supplied only by the test-unlogged Maven profile.
     *
     * @param suite the suite being prepared
     * @throws Exception if the target is unapproved or any conversion fails
     */
    public static void prepare(final String suite) throws Exception {
        final String sqlPath = Config.getStringProperty("test.unlogged.sql", "");
        if (sqlPath.isEmpty()) {
            return;
        }
        final long started = System.nanoTime();
        final String sql = Files.readString(Path.of(sqlPath), StandardCharsets.UTF_8);
        try (Connection connection = DbConnectionFactory.getConnection();
             Statement statement = connection.createStatement()) {
            if (!connection.getAutoCommit()) {
                throw new IllegalStateException("UNLOGGED preparation requires its own transaction");
            }
            statement.setQueryTimeout(120);
            statement.execute(sql);
            for (var warning = statement.getWarnings(); warning != null; warning = warning.getNextWarning()) {
                Logger.info(UnloggedTestDatabase.class, warning.getMessage());
            }
        }
        Logger.info(UnloggedTestDatabase.class, "UNLOGGED suite=" + suite + " preparation_ms="
                + (System.nanoTime() - started) / 1_000_000);
    }
}
