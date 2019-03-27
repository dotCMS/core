package com.dotcms.api.system.event.verifier;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.rest.api.v1.system.websocket.WebSocketUserSessionData;

import java.util.List;

/**
 * Verified that the sessionUser user is one of the users in the lists by user id.
 * @author jsanca
 */
public class UsersVerifier implements  PayloadVerifier{

    public UsersVerifier(){}

    @Override
    public boolean verified(final Payload payload, final WebSocketUserSessionData userSessionData) {

        final List<String> userIdList = (List<String>)payload.getVisibilityValue();

        return userIdList.contains(userSessionData.getUser().getUserId());
    }
}
