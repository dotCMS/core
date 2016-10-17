package com.dotmarketing.portlets.campaigns.model;

import java.io.Serializable;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.util.InodeUtils;

/** @author Hibernate CodeGenerator */
public class Recipient extends Inode implements Serializable {


    private static final long serialVersionUID = 1L;

    private java.util.Date sent;
    private java.util.Date opened;

    /** nullable persistent field */
    private String name;
    
    /** nullable persistent field */
    private String lastname;

    /** nullable persistent field */
    private String email;

    /** nullable persistent field */
    private int lastResult;

    /** nullable persistent field */
    private String lastMessage;

    /** nullable persistent field */
    private String userId;
    
	/**
	 * @return Returns the email.
	 */
	public String getEmail() {
		return email;
	}
	/**
	 * @param email The email to set.
	 */
	public void setEmail(String email) {
		this.email = email;
	}
	/**
	 * @return Returns the lastMessage.
	 */
	public String getLastMessage() {
		return lastMessage;
	}
	/**
	 * @param lastMessage The lastMessage to set.
	 */
	public void setLastMessage(String lastMessage) {
		this.lastMessage = lastMessage;
	}
	/**
	 * @return Returns the lastResult.
	 */
	public int getLastResult() {
		return lastResult;
	}
	/**
	 * @param lastResult The lastResult to set.
	 */
	public void setLastResult(int lastResult) {
		this.lastResult = lastResult;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
    public Recipient() {
        super.setType("recipient");
    }

   


	/**
	 * Returns the inode.
	 * @return String
	 */
	public String getInode() {
		if(InodeUtils.isSet(inode))
			return inode;
		
		return "";
	}

	/**
	 * Returns the opened.
	 * @return java.util.Date
	 */
	public java.util.Date getOpened() {
		return opened;
	}

	/**
	 * Returns the sent.
	 * @return java.util.Date
	 */
	public java.util.Date getSent() {
		return sent;
	}

	/**
	 * Sets the inode.
	 * @param inode The inode to set
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}

	/**
	 * Sets the opened.
	 * @param opened The opened to set
	 */
	public void setOpened(java.util.Date opened) {
		this.opened = opened;
	}

	/**
	 * Sets the sent.
	 * @param sent The sent to set
	 */
	public void setSent(java.util.Date sent) {
		this.sent = sent;
	}
	
	/**
	 * Return the lastname
	 * @return lastname
	 */
	public String getLastname() {
		return lastname;
	}
	
	/**
	 * Set the lastname
	 * @param lastname
	 */
	public void setLastname(String lastname) {
		this.lastname = lastname;
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

}
