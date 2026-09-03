package com.dotcms.api.system.event;

import com.dotmarketing.exception.DotDataException;

import java.util.Optional;

/**
 * Reads and writes the per-node delivery cursor for the {@code system_event} queue (issue #36827).
 *
 * <p>This is deliberately kept <b>off</b> {@link SystemEventsAPI}. Publishing and subscribing are the
 * seams a future move of this queue onto a different transport would cut at; cursor bookkeeping is an
 * implementation detail of the database-backed poller and must not leak into either.
 */
public interface SystemEventsCursorAPI {

    /**
     * Returns the stored cursor for a node, if it has one.
     *
     * <p>An empty result is a meaningful state, not an error: a node that has never polled seeds at
     * "now" rather than replaying the retained backlog.
     *
     * @param serverId the node
     * @return the cursor, or empty when the node has never stored one
     */
    Optional<SystemEventsCursor> findByServerId(String serverId) throws DotDataException;

    /**
     * Stores the cursor for a node. Upsert — a node holds at most one cursor.
     *
     * @param serverId      the node
     * @param lastEventDate epoch millis of the start of the poll that last completed
     */
    void save(String serverId, long lastEventDate) throws DotDataException;
}
