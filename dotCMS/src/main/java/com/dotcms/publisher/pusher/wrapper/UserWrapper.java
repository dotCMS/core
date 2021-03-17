package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.Role;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 5/28/13
 */
public class UserWrapper {

    private User user;
    private Role userRole;

    private Operation operation;

    public UserWrapper ( User user, Role userRole ) {
        this.user = user;
        this.userRole = userRole;
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

	public Role getUserRole() {
		return userRole;
	}

	public void setUserRole(Role userRole) {
		this.userRole = userRole;
	}

}