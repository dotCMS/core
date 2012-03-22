package com.dotmarketing.portlets.fileUpload.model;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.util.InodeUtils;

/** @author Hibernate CodeGenerator */
public class FileUpload extends Inode implements Serializable {

    private static final long serialVersionUID = 1L;

	/** identifier field */
    private String inode;

	/** nullable persistent field */
	private String fileName;

	/** nullable persistent field */
	private long fileSize;

    /** nullable persistent field */
    private String caption;

    /** nullable persistent field */
    private int width;

    /** nullable persistent field */
    private int height;

    /** nullable persistent field */
    private int sortOrder;

    /** nullable persistent field */
    private String alignment;

    /** nullable persistent field */
    private String parent;

    /** nullable persistent field */
	private long maxsize;

    /** nullable persistent field */
	private long maxwidth;

    /** nullable persistent field */
	private long maxheight;

    /** nullable persistent field */
	private long minheight;

    /** default constructor */
    public FileUpload() {
    	super.setType("file_upload");
    }

    public String getInode() {
    	if(InodeUtils.isSet(this.inode))
    		return this.inode;
    	
    	return "";
    }

    public void setInode(String inode) {
        this.inode = inode;
    }
    public java.lang.String getFileName() {
        return this.fileName;
    }

    public void setFileName(java.lang.String fileName) {
        this.fileName = fileName;
    }
    public java.lang.String getCaption() {
        return this.caption;
    }

    public void setCaption(java.lang.String caption) {
        this.caption = caption;
    }
    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
    public java.lang.String getAlignment() {
        return this.alignment;
    }

    public void setAlignment(java.lang.String alignment) {
        this.alignment = alignment;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public boolean equals(Object other) {
        if ( !(other instanceof FileUpload) ) return false;
        FileUpload castOther = (FileUpload) other;
        return new EqualsBuilder()
            .append(this.inode, castOther.inode)
            .isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder()
            .append(inode)
            .toHashCode();
    }

	/**
	 * Returns the sortOrder.
	 * @return int
	 */
	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * Sets the sortOrder.
	 * @param sortOrder The sortOrder to set
	 */
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}
/**
	 * Returns the maxheight.
	 * @return long
	 */
	public long getMaxheight() {
		return maxheight;
	}

	/**
	 * Returns the maxsize.
	 * @return long
	 */
	public long getMaxsize() {
		return maxsize;
	}

	/**
	 * Returns the maxwidth.
	 * @return long
	 */
	public long getMaxwidth() {
		return maxwidth;
	}

	/**
	 * Returns the minheight.
	 * @return long
	 */
	public long getMinheight() {
		return minheight;
	}

	/**
	 * Returns the parent.
	 * @return long
	 */
	public String getParent() {
		return parent;
	}

	/**
	 * Sets the maxheight.
	 * @param maxheight The maxheight to set
	 */
	public void setMaxheight(long maxheight) {
		this.maxheight = maxheight;
	}

	/**
	 * Sets the maxsize.
	 * @param maxsize The maxsize to set
	 */
	public void setMaxsize(long maxsize) {
		this.maxsize = maxsize;
	}

	/**
	 * Sets the maxwidth.
	 * @param maxwidth The maxwidth to set
	 */
	public void setMaxwidth(long maxwidth) {
		this.maxwidth = maxwidth;
	}

	/**
	 * Sets the minheight.
	 * @param minheight The minheight to set
	 */
	public void setMinheight(long minheight) {
		this.minheight = minheight;
	}

	/**
	 * Sets the parent.
	 * @param parent The parent to set
	 */
	public void setParent(String parent) {
		this.parent = parent;
	}



	/**
	 * @return
	 */
	public long getFileSize() {
		return fileSize;
	}

	/**
	 * @param l
	 */
	public void setFileSize(long l) {
		fileSize = l;
	}

}
