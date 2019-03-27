package com.dotcms.api.system.event;

import com.dotcms.rest.api.v1.system.websocket.WebSocketUserSessionData;

/**
 * This contract is used to verified a payload.
 * If the payload is valid, will return true, otherwise false.
 * {@link Visibility}'s verifier
 */
public interface PayloadVerifier {

    /**
     * Returns true if the payload is valid.
     * @param payload {@link Payload}
     * @param userSessionData {@link WebSocketUserSessionData}
     * @return boolean
     */
    public boolean verified (Payload payload, WebSocketUserSessionData userSessionData);
}
