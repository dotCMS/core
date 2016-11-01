package com.dotmarketing.beans;

import java.io.Serializable;

/**
 * UsersToDelete Bean
 * @author  Armando
 */
public class UsersToDelete implements Serializable {

    private static final long serialVersionUID = 1L;

	/** identifier field */
    private long id;

    /** User Id */
    private String userId;

    /**
     * Return UsersToDelete Id Number
     * @return UsersToDelete Id Number
     */
	public long getId() {
		return id;
	}

	/**
     * Set UsersToDelete Id Number
     * @param id Number of the UsersToDelete id to set
     */
	public void setId(long id) {
		this.id = id;
	}

	/**
     * Return User Id
     * @return User Id
     */
	public String getUserId() {
		return userId;
	}

	/**
     * Set User Id
     * @param userId with the User Id to set
     */
	public void setUserId(String userId) {
		this.userId = userId;
	}
}