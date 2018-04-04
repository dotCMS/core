package com.dotcms.api.system.event.verifier;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.rest.api.v1.system.websocket.WebSocketUserSessionData;

/**
 * Verified that the sessionUser user is the same of the payload visibilityValue
 */
public class UserVerifier implements  PayloadVerifier {

    public UserVerifier(){}

    @Override
    public boolean verified(final Payload payload,
                            final WebSocketUserSessionData userSessionData) {

        return userSessionData.getUser().getUserId()
                .equals(payload.getVisibilityValue());
    }
}
