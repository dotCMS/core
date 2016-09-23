package com.dotcms.api.system.event.verifier;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;

public class UserVerifier implements  PayloadVerifier{

    public UserVerifier(){}

    @Override
    public boolean verified(Payload payload, SessionWrapper session) {
        return SessionWrapper.class.cast(session).getUser().getUserId()
                .equals(payload.getVisibilityId());
    }
}
