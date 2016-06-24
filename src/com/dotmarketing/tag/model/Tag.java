package com.dotmarketing.tag.model;

import java.io.Serializable;
import java.util.Date;

import com.dotmarketing.tag.model.Tag;

/**
 * Tags are a method of labeling content with one or more terms so that content
 * can be found and extracted dynamically for display on a page. Tags can be
 * single words or phrases of multiple words separated by spaces. The
 * <i>Persona</i> objects are types of tags as well.
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public class Tag implements Serializable {

    /**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/** persistent field */
    private String userId;

    /** persistent field */
    private String tagName;

    /** persistent field */
    private String tagId;

	/** persistent field */
	private boolean persona;

	/** persistent field */
	private Date modDate;

    /** persistent field */
    private String hostId;

	/**
	 * @return the tagName
	 */
	public String getTagName() {
		return tagName;
	}

	/**
	 * @param tagName the tagName to set
	 */
	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @return the tagId
	 */
	public String getTagId() {
		return tagId;
	}

	/**
	 * @param tagId the tagId to set
	 */
	public void setTagId(String tagId) {
		this.tagId = tagId;
	}

	public Date getModDate () {
		return modDate;
	}

	public void setModDate ( Date modDate ) {
		this.modDate = modDate;
	}

	public boolean isPersona () {
		return persona;
	}

	public void setPersona ( boolean persona ) {
		this.persona = persona;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((tagName == null) ? 0 : tagName.hashCode());
		result = PRIME * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Tag other = (Tag) obj;
		if (tagName == null) {
			if (other.tagName != null)
				return false;
		} else if (!tagName.equals(other.tagName))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	/**
	 * @param hostId the hostId to set
	 */
	public void setHostId(String hostId) {
		this.hostId = hostId;
	}

	/**
	 * @return the hostId
	 */
	public String getHostId() {
		return hostId;
	}

	@Override
	public String toString() {
		return "Tag [userId=" + userId + ", tagName=" + tagName + ", tagId=" + tagId + ", persona=" + persona + ", modDate="
				+ modDate + ", hostId=" + hostId + "]";
	}
	
}
