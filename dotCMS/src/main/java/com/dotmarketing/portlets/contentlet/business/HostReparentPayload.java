package com.dotmarketing.portlets.contentlet.business;

import java.io.Serializable;

/**
 * Event payload fired by {@link HostAPIImpl} when a {@link com.dotmarketing.beans.Host} is
 * reparented (i.e. its {@code Identifier.hostId} changes because a new parent host or folder is
 * selected).
 *
 * <p>Both {@link #oldTopLevelHostId} and {@link #newTopLevelHostId} are the UUIDs of
 * <em>top-level</em> hosts (hosts whose {@code Identifier.hostId == SYSTEM_HOST_ID}). When a host
 * moves within the same top-level tree both fields are equal; the double-invalidation that results
 * from that is harmless.</p>
 *
 * <p>Consumers — notably {@link NestedHostPatternCache} — use this payload to know which cache
 * buckets must be evicted after the reparent.</p>
 *
 * @see NestedHostPatternCache#invalidate(String)
 */
public final class HostReparentPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    /** UUID of the top-level host that owned the reparented host <em>before</em> the move. */
    private final String oldTopLevelHostId;

    /** UUID of the top-level host that owns the reparented host <em>after</em> the move. */
    private final String newTopLevelHostId;

    /**
     * Constructs a new {@code HostReparentPayload}.
     *
     * @param oldTopLevelHostId UUID of the top-level host before the move; must not be {@code null}
     * @param newTopLevelHostId UUID of the top-level host after the move; must not be {@code null}
     */
    public HostReparentPayload(final String oldTopLevelHostId, final String newTopLevelHostId) {
        if (oldTopLevelHostId == null) {
            throw new IllegalArgumentException("oldTopLevelHostId must not be null");
        }
        if (newTopLevelHostId == null) {
            throw new IllegalArgumentException("newTopLevelHostId must not be null");
        }
        this.oldTopLevelHostId = oldTopLevelHostId;
        this.newTopLevelHostId = newTopLevelHostId;
    }

    /**
     * Returns the UUID of the top-level host that owned the moved host before the reparent.
     *
     * @return non-null UUID string
     */
    public String getOldTopLevelHostId() {
        return oldTopLevelHostId;
    }

    /**
     * Returns the UUID of the top-level host that owns the moved host after the reparent.
     *
     * @return non-null UUID string
     */
    public String getNewTopLevelHostId() {
        return newTopLevelHostId;
    }

    @Override
    public String toString() {
        return "HostReparentPayload{"
                + "oldTopLevelHostId='" + oldTopLevelHostId + '\''
                + ", newTopLevelHostId='" + newTopLevelHostId + '\''
                + '}';
    }
}
