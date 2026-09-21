package com.dotcms.api.system.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Identifies the user and session behind a session-scoped system event, such as {@code SWITCH_SITE}.
 *
 * <p>Travels between cluster nodes as an event payload, so it has to survive a round trip through
 * Jackson. {@link Payload} records the concrete class name and the receiving node reconstructs into
 * it; without an explicit creator this class could be written but never read back. Because those
 * events are emitted during ordinary admin activity, that made cross-node delivery fail continuously
 * on a real cluster — 39 consecutive failed polls per node — and the batch it belonged to was lost
 * with it (issues #36827, #37249).
 */
public class UserSessionBean implements Serializable {

    private final String user;
    private final String sessionId;

    @JsonCreator
    public UserSessionBean(@JsonProperty("user") final String user,
                           @JsonProperty("sessionId") final String sessionId) {
        this.user = user;
        this.sessionId = sessionId;
    }

    public String getUser() {
        return user;
    }

    public String getSessionId() {
        return sessionId;
    }
}
