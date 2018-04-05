package com.dotcms.api.system.event;

import java.io.Serializable;

public class UserSessionBean implements Serializable {

    private final String user;
    private final String sessionId;

    public UserSessionBean(final String user,
                                    final String sessionId) {
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
