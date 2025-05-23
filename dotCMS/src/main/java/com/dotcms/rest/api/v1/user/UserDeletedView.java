package com.dotcms.rest.api.v1.user;

/**
 * User Deleted view
 * @author jsanca
 */
public class UserDeletedView {

    private final String status;
    private final String deletedUser;
    private final String reassignedTo;

    public UserDeletedView(String status, String deletedUser, String reassignedTo) {
        this.status = status;
        this.deletedUser = deletedUser;
        this.reassignedTo = reassignedTo;
    }

    public String getStatus() {
        return status;
    }

    public String getDeletedUser() {
        return deletedUser;
    }

    public String getReassignedTo() {
        return reassignedTo;
    }
}
