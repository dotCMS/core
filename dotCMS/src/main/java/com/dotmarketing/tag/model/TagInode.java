package com.dotmarketing.tag.model;

import com.dotmarketing.util.InodeUtils;

import java.io.Serializable;
import java.util.Date;

public class TagInode implements Serializable {

    private static final long serialVersionUID = 1L;

	/** persistent field */
    private String inode;

    /** persistent field */
    private String tagId;

    /**
     * persistent field
     */
    private String fieldVarName;

	/** persistent field */
	private Date modDate;

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

    public String getFieldVarName() {
        return fieldVarName;
    }

    public void setFieldVarName(String fieldVarName) {
        this.fieldVarName = fieldVarName;
    }

	public String getInode() {
		if(InodeUtils.isSet(inode))
			return inode;

		return "";
	}

	public Date getModDate () {
		return modDate;
	}

	public void setModDate ( Date modDate ) {
		this.modDate = modDate;
	}

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
		if (fieldVarName == null) {
			if (other.fieldVarName != null)
				return false;
		} else if (!fieldVarName.equals(other.fieldVarName))
			return false;
		return true;
	}

}
