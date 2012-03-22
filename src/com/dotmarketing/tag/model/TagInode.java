package com.dotmarketing.tag.model;

import java.io.Serializable;

import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.InodeUtils;

public class TagInode implements Serializable {


    /**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/** persistent field */
    private String inode;

    /** persistent field */
    private String tagId;

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

	/**
	 * @return the userId
	 */
	public String getInode() {
		if(InodeUtils.isSet(inode))
			return inode;

		return "";
	}

	/**
	 * @param userId the userId to set
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int i = inode.hashCode();
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((tagId == null) ? 0 : tagId.hashCode());
		result = PRIME * result +  i;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TagInode other = (TagInode) obj;
		if (tagId == null) {
			if (other.tagId != null)
				return false;
		} else if (!tagId.equals(other.tagId))
			return false;
		if (!InodeUtils.isSet(inode) ) {
			if (InodeUtils.isSet(inode))
				return false;
		} else if (inode.equalsIgnoreCase(other.getInode()))
			return false;
		return true;
	}

}
