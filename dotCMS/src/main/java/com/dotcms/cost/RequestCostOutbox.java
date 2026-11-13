package com.dotcms.cost;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Pending immutable request-cost bodies in the tenant's existing PostgreSQL database.
 * Admission and delivery use separate transaction locks so a slow receiver cannot hold admission up.
 */
final class RequestCostOutbox {

    static final int MAX_PAYLOAD_BYTES = 8 * 1024;
    static final int MAX_PENDING = 10_000;
    private static final int LOCK_NAMESPACE = 0x52434f53;
    private static final int LOG_INTERVAL_MS = 10 * 60 * 1000;

    /**
     * Commits a body before the monitor retires its captured accounting. Repeating an admission
     * after an uncertain commit is safe, including when the outbox is already full.
     *
     * @return true only after durable admission, or confirmation of an identical existing body
     */
    @CloseDBIfOpened
    boolean add(final String destination, final String payload) throws Exception {
        if (payload.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
            Logger.warnEvery(getClass(), "REQUEST_COST_OUTBOX_SIZE",
                    "Request cost outbox admission refused: body exceeds 8192 bytes", LOG_INTERVAL_MS);
            return false;
        }
        final String id = DigestUtils.sha256Hex(payload);
        return LocalTransaction.externalizeTransaction(() -> {
            new DotConnect().executeUpdate("SET LOCAL synchronous_commit = on");
            new DotConnect().executeUpdate("SET LOCAL lock_timeout = '2s'");
            new DotConnect().executeUpdate("SET LOCAL statement_timeout = '10s'");
            new DotConnect().setSQL("SELECT pg_advisory_xact_lock(?, ?)")
                    .addParam(LOCK_NAMESPACE).addParam(1).loadObjectResults();
            final List<Map<String, Object>> existing = new DotConnect()
                    .setSQL("SELECT payload FROM request_cost_outbox WHERE id = ?")
                    .addParam(id).loadObjectResults();
            if (!existing.isEmpty()) {
                if (!payload.equals(existing.get(0).get("payload"))) {
                    throw new DotDataException("Request cost outbox identity conflict");
                }
                return true;
            }
            final int pending = new DotConnect().setSQL("SELECT COUNT(*) AS pending FROM request_cost_outbox")
                    .loadInt("pending");
            if (pending >= MAX_PENDING) {
                Logger.warnEvery(getClass(), "REQUEST_COST_OUTBOX_FULL",
                        "Request cost outbox full: pending=" + pending + "; existing work retained",
                        LOG_INTERVAL_MS);
                return false;
            }
            new DotConnect().executeUpdate(
                    "INSERT INTO request_cost_outbox (id, destination, payload) VALUES (?, ?, ?)",
                    id, DigestUtils.sha256Hex(destination), payload);
            return true;
        });
    }

    /**
     * Delivers at most one due body. One nonblocking transaction lock bounds delivery to one
     * in-flight request per tenant database, including during rolling deployments.
     */
    @CloseDBIfOpened
    void deliver(final String destination, final ToIntFunction<String> sender) throws Exception {
        LocalTransaction.externalizeTransaction(() -> {
            new DotConnect().executeUpdate("SET LOCAL statement_timeout = '10s'");
            final Map<String, Object> lock = new DotConnect()
                    .setSQL("SELECT pg_try_advisory_xact_lock(?, ?) AS locked")
                    .addParam(LOCK_NAMESPACE).addParam(2).loadObjectResults().get(0);
            if (!Boolean.TRUE.equals(lock.get("locked"))) {
                return null;
            }
            final List<Map<String, Object>> due = new DotConnect().setSQL("""
                    SELECT id, payload, attempts FROM request_cost_outbox
                    WHERE destination = ? AND next_attempt <= clock_timestamp()
                    ORDER BY next_attempt, created_at, id LIMIT 1 FOR UPDATE SKIP LOCKED
                    """)
                    .addParam(DigestUtils.sha256Hex(destination)).loadObjectResults();
            if (due.isEmpty()) {
                return null;
            }
            final Map<String, Object> row = due.get(0);
            final String id = (String) row.get("id");
            final int status = sender.applyAsInt((String) row.get("payload"));
            if (isCompletionAck(status)) {
                new DotConnect().executeUpdate("DELETE FROM request_cost_outbox WHERE id = ?", id);
            } else {
                final int attempts = ((Number) row.get("attempts")).intValue();
                new DotConnect().executeUpdate("""
                        UPDATE request_cost_outbox SET attempts = LEAST(attempts + 1, 16),
                            next_attempt = clock_timestamp() + (? * INTERVAL '1 second') WHERE id = ?
                        """,
                        retryDelaySeconds(attempts, status), id);
                Logger.warnEvery(getClass(), "REQUEST_COST_PUSH_PENDING",
                        "Request cost delivery unconfirmed: HTTP " + status + "; body retained for retry",
                        LOG_INTERVAL_MS);
            }
            return null;
        });
    }

    /** Records tenant-wide pressure, including records paused by a destination URL change. */
    @CloseDBIfOpened
    void logStatistics() throws Exception {
        LocalTransaction.externalizeTransaction(() -> {
            new DotConnect().executeUpdate("SET LOCAL statement_timeout = '10s'");
            final Map<String, Object> stats = new DotConnect().setSQL("""
                    SELECT COUNT(*) AS pending, COALESCE(EXTRACT(EPOCH FROM
                        (clock_timestamp() - MIN(created_at)))::bigint, 0) AS oldest_seconds
                    FROM request_cost_outbox
                    """).loadObjectResults().get(0);
            Logger.info(getClass(), "Request cost outbox: pending=" + stats.get("pending")
                    + ", oldestSeconds=" + stats.get("oldest_seconds"));
            return null;
        });
    }

    /** Only the current receiver's completed-insertion response permits local retirement. */
    static boolean isCompletionAck(final int status) {
        return status == 201;
    }

    /** Backs off transient failures from five seconds to fifteen minutes, without expiry. */
    static long retryDelaySeconds(final int attempts, final int status) {
        if (status >= 400 && status < 500 && status != 408 && status != 429) {
            return 900;
        }
        return Math.min(900L, 5L << Math.min(Math.max(attempts, 0), 8));
    }
}
