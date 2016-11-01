package com.dotmarketing.portlets.campaigns.model;

import java.io.Serializable;

import com.dotmarketing.util.InodeUtils;

/** @author Hibernate CodeGenerator */
public class Click extends com.dotmarketing.beans.Inode implements Serializable {

	private static final long serialVersionUID = 1L;

	/** nullable persistent field */
	private String link;

	/** nullable persistent field */
	private int clickCount;

	/** default constructor */
	public Click() {
		super.setType("click");
	}

	public String getInode() {
		if(InodeUtils.isSet(this.inode))
    		return this.inode;
    	
    	return "";
	}

	public void setInode(String inode) {
		this.inode = inode;
	}

	public int getClickCount() {
		return this.clickCount;
	}

	public void setClickCount(int clickCount) {
		this.clickCount = clickCount;
	}
	public void setClickCount(Object clickCount) {
		try {
			this.clickCount = Integer.parseInt((String) clickCount);
		} catch (Exception e) {
		}
	}


	/**
	 * Returns the link.
	 * @return String
	 */
	public String getLink() {
		return link;
	}

	/**
	 * Sets the link.
	 * @param link The link to set
	 */
	public void setLink(String link) {
		this.link = link;
	}

}
