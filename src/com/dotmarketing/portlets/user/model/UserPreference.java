package com.dotmarketing.portlets.user.model;

import java.io.Serializable;

/** @author Hibernate CodeGenerator */
public class UserPreference implements Serializable {

    private static final long serialVersionUID = 1L;

	/** persistent field */
    private long id;

    /** persistent field */
    private String userId;

    /** persistent field */
    private String preference;

    /** persistent field */
    private String value;

    /** full constructor */
    public UserPreference(String userId, String preference, String value) {
    	this.userId = userId;
    	this.preference = preference;
    	this.value = value;
    }

    /** default constructor */
    public UserPreference() {
    }

    
	public boolean equals(Object other) {

        if (!(other instanceof UserPreference)) {
            return false;
        }

        UserPreference castOther = ( UserPreference ) other;

        return ((this.getUserId() == castOther.getUserId())
        		&& (this.getPreference() == castOther.getPreference())
        		&& (this.getValue() == castOther.getValue()));
    }

	/**
	 * Returns the id.
	 * @return long
	 */
	public long getId() {
		return id;
	}

	/**
	 * Returns the preference.
	 * @return String
	 */
	public String getPreference() {
		return preference;
	}

	/**
	 * Returns the userId.
	 * @return String
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Returns the value.
	 * @return String
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Sets the preference.
	 * @param preference The preference to set
	 */
	public void setPreference(String preference) {
		this.preference = preference;
	}

	/**
	 * Sets the userId.
	 * @param userId The userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * Sets the value.
	 * @param value The value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

}
