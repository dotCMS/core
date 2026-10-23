package com.dotcms.saml.service.impl;

import com.dotcms.saml.service.external.SamlException;
import com.dotcms.saml.service.internal.AssertionReplayStore;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

/**
 * {@link AssertionReplayStore} backed by a table in the dotCMS database, so a used assertion is rejected by
 * every node of a cluster and after a restart.
 *
 * The bundle creates the table itself on first use. Each check deletes expired rows, then inserts the
 * assertion key with {@code ON CONFLICT DO NOTHING}: one inserted row means first use, none means replay.
 * It runs on its own auto-commit connection, so the record is committed straight away and two nodes
 * checking the same assertion at once can't both succeed. Any database error is reported as a
 * {@link SamlException}, so the caller rejects the response rather than skipping the check.
 *
 * @author dotCMS
 */
public class DatabaseAssertionReplayStore implements AssertionReplayStore {

    static final String TABLE = "dotsaml_assertion_replay";

    static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE
            + " (assertion_key VARCHAR(64) NOT NULL PRIMARY KEY, expires_at BIGINT NOT NULL)";
    static final String CREATE_INDEX = "CREATE INDEX IF NOT EXISTS idx_" + TABLE + "_expires_at ON "
            + TABLE + " (expires_at)";
    static final String DELETE_EXPIRED = "DELETE FROM " + TABLE + " WHERE expires_at < ?";
    static final String INSERT = "INSERT INTO " + TABLE + " (assertion_key, expires_at) VALUES (?, ?)"
            + " ON CONFLICT (assertion_key) DO NOTHING";

    /** The table only has to be created once per JVM. */
    private static final AtomicBoolean TABLE_READY = new AtomicBoolean(false);

    /** Opens a connection; closing it is the store's job. */
    @FunctionalInterface
    interface ConnectionSource {
        Connection open() throws SQLException;
    }

    private final ConnectionSource connectionSource;
    private final LongSupplier     clock;
    private final AtomicBoolean    tableReady;

    public DatabaseAssertionReplayStore() {

        this(() -> DbConnectionFactory.getDataSource().getConnection(), System::currentTimeMillis, TABLE_READY);
    }

    DatabaseAssertionReplayStore(final ConnectionSource connectionSource, final LongSupplier clock,
                                 final AtomicBoolean tableReady) {

        this.connectionSource = connectionSource;
        this.clock            = clock;
        this.tableReady       = tableReady;
    }

    @Override
    public boolean markUsed(final String key, final long expiresAt) {

        try (Connection connection = this.connectionSource.open()) {

            connection.setAutoCommit(true);
            this.ensureTable(connection);

            try (PreparedStatement delete = connection.prepareStatement(DELETE_EXPIRED)) {

                delete.setLong(1, this.clock.getAsLong());
                delete.executeUpdate();
            }

            try (PreparedStatement insert = connection.prepareStatement(INSERT)) {

                insert.setString(1, hash(key));
                insert.setLong(2, expiresAt);
                return insert.executeUpdate() == 1;
            }
        } catch (SQLException e) {

            Logger.error(DatabaseAssertionReplayStore.class, "Could not check the SAML assertion replay table: "
                    + e.getMessage(), e);
            throw new SamlException("Could not check whether the SAML assertion has already been used", e);
        }
    }

    private void ensureTable(final Connection connection) throws SQLException {

        if (this.tableReady.get()) {
            return;
        }

        synchronized (this.tableReady) {

            if (this.tableReady.get()) {
                return;
            }

            try (Statement statement = connection.createStatement()) {

                statement.execute(CREATE_TABLE);
                statement.execute(CREATE_INDEX);
            } catch (SQLException e) {

                // another node may be creating it at the same moment; if the table really is missing,
                // the insert below fails and the response is rejected
                Logger.warn(DatabaseAssertionReplayStore.class, "Could not create " + TABLE + ": " + e.getMessage());
                return;
            }

            this.tableReady.set(true);
        }
    }

    /** Fixed-length key, whatever the length of the assertion ID. */
    static String hash(final String key) {

        try {

            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hex = new StringBuilder(digest.length * 2);
            for (final byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {

            throw new SamlException("SHA-256 is not available", e);
        }
    }
}
