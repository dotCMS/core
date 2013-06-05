package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotmarketing.beans.UserProxy;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;

import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 5/28/13
 */
public class UserWrapper {

    private User user;
    private UserProxy userProxy;
    private List<Address> addresses;

    private Operation operation;

    public UserWrapper ( User user, UserProxy userProxy, List<Address> addresses ) {
        this.user = user;
        this.userProxy = userProxy;
        this.addresses = addresses;
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

    public UserProxy getUserProxy () {
        return userProxy;
    }

    public void setUserProxy ( UserProxy userProxy ) {
        this.userProxy = userProxy;
    }

    public List<Address> getAddresses () {
        return addresses;
    }

    public void setAddresses ( List<Address> addresses ) {
        this.addresses = addresses;
    }

}