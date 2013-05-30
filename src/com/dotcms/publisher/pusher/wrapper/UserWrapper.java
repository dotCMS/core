package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.liferay.portal.model.User;

/**
 * @author Jonathan Gamba
 *         Date: 5/28/13
 */
public class UserWrapper {

    private Operation operation;
    private User user;

    public UserWrapper ( User user, Operation operation ) {
        this.user = user;
        this.operation = operation;
    }

    public Operation getOperation () {
        return operation;
    }

    public void setOperation ( Operation operation ) {
        this.operation = operation;
    }

    public User getUser () {
        return user;
    }

    public void setUser ( User user ) {
        this.user = user;
    }

}