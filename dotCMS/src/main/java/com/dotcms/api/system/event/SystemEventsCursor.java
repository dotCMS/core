package com.dotcms.api.system.event;

import java.util.Date;

/**
 * The delivery cursor of a single cluster node — how far that node has consumed the
 * {@code system_event} queue (issue #36827).
 *
 * <p>Replaces the {@code static volatile AtomicLong} the poller used to keep in memory. Because the
 * value is now durable, events committed while a node was down are still delivered when it returns,
 * instead of being skipped permanently by a mark that reset itself to "now" on every restart.
 */
public class SystemEventsCursor {

    private final String serverId;
    private final long lastEventDate;
    private final Date modDate;

    public SystemEventsCursor(final String serverId, final long lastEventDate, final Date modDate) {
        this.serverId = serverId;
        this.lastEventDate = lastEventDate;
        this.modDate = modDate;
    }

    /**
     * @return the node this cursor belongs to
     */
    public String getServerId() {
        return this.serverId;
    }

    /**
     * @return epoch millis of the start of the poll that last completed on this node
     */
    public long getLastEventDate() {
        return this.lastEventDate;
    }

    /**
     * @return when the cursor was last written; drives the "poller stalled" signal
     */
    public Date getModDate() {
        return this.modDate;
    }

    @Override
    public String toString() {
        return "SystemEventsCursor{serverId='" + this.serverId + "', lastEventDate="
                + this.lastEventDate + ", modDate=" + this.modDate + '}';
    }
}
