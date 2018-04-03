package com.dotcms.api.system.event;

/**
 * Encapsulates the payload for the user session
 * @author jsanca
 */
public class UserSessionPayloadBuilder {

    /**
     * Builds a Payload for the user session destroyed
     * @param userId String
     * @param sessionId String
     * @return Payload
     */
    public static Payload build(final String userId, final String sessionId) {

        return new Payload(new Long(System.currentTimeMillis()),
                Visibility.USER_SESSION,
                new UserSessionBean(userId, sessionId));
    }
}


