package com.dotcms.api.system.event.verifier;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.api.system.event.UserSessionBean;
import com.dotcms.rest.api.v1.system.websocket.WebSocketUserSessionData;

/**
 * Verified that the sessionUser and session applies to the current sessionUser.
 * @author jsanca
 */
public class UserSessionVerifier implements  PayloadVerifier {

    public UserSessionVerifier(){}

    @Override
    public boolean verified(final Payload payload, final WebSocketUserSessionData userSessionData) {

        final UserSessionBean userSessionBean  =
                (UserSessionBean)payload.getVisibilityValue();

        if  (userSessionData.getUser().getUserId()
                .equals(userSessionBean.getUser())) {

            if (null != userSessionData.getUserSessionId()) {
                return (userSessionData.getUserSessionId()
                        .equals(userSessionBean.getSessionId()));
            }

            return true;
        }

        return false;
    } // verified.
}
