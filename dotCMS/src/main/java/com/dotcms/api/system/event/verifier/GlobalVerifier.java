package com.dotcms.api.system.event.verifier;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.rest.api.v1.system.websocket.WebSocketUserSessionData;

/**
 * Simple {@link PayloadVerifier} used to verify GLOBAL {@link com.dotcms.api.system.event.Visibility} for notifications,
 * as it GLOBAL NO validation is made and will always return true.
 *
 * @author Jonathan Gamba
 *         11/15/16
 */
public class GlobalVerifier implements PayloadVerifier {

    @Override
    public boolean verified(Payload payload, WebSocketUserSessionData userSessionData) {
        return true;
    }
}
