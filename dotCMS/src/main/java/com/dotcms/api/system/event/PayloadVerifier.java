package com.dotcms.api.system.event;

import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;

/**
 * {@link Visibility}'s verifier
 */
public interface PayloadVerifier {

    public boolean verified (Payload payload, SessionWrapper session);
}
