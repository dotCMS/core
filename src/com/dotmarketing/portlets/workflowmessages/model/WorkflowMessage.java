package com.dotmarketing.portlets.workflowmessages.model;

import java.io.Serializable;
import java.util.Date;

import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;

/** @author Hibernate CodeGenerator */
public class WorkflowMessage extends WebAsset implements Serializable, Comparable {

    
    private static final long serialVersionUID = 1L;

	/** identifier field */    
    private String inode;

    /** identifier field */
    private String parent;
    
    private String message;
    
	private Date requestDate;
	  
    private String requestUser;

	private long statusId;

	private long actionId;

	private String assignedToUserId;

	private Date assignedToUserDate;

	private String notifyRoleId;

	private Date notifyRoleDate;

	
    /** identifier field */
    private long webasset;


    /** default constructor */
    public WorkflowMessage() {
		super.setType("workflow_messages");
		requestDate = new java.util.Date();
    }

    public String getInode() {
    	if(InodeUtils.isSet(this.inode))
    		return this.inode;
    	
    	return "";
    }

	/**
	 * Returns the parent.
	 * @return long
	 */
	public String getParent() {
		return parent;
	}

	/**
	 * Sets the parent.
	 * @param parent The parent to set
	 */
	public void setParent(String parent) {
		this.parent = parent;
	}
	


	/**
	 * Sets the inode.
	 * @param inode The inode to set
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}

	//Every Web Asset should implement this method!!!
	public void copy(WorkflowMessage newWorkflowMessage) {
	    this.setParent(newWorkflowMessage.getParent());
	    super.copy(newWorkflowMessage);
	}

	public int compareTo(Object compObject){
		if(!(compObject instanceof WorkflowMessage))return -1;
		
		WorkflowMessage workflowMessage = (WorkflowMessage) compObject;
		return (workflowMessage.getTitle().compareTo(this.getTitle()));
	}

    public String getURI(Folder folder) {
    	String folderPath = "";
    	try {
			folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
		} catch (Exception e) {
			Logger.error(this, e.getMessage());
			throw new DotRuntimeException(e.getMessage(),e);
		} 
    	return  folderPath  + this.getInode();
    }

	/**
	 * Returns the message.
	 * @return String
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Returns the requestDate.
	 * @return Date
	 */
	public Date getRequestDate() {
		return requestDate;
	}

	/**
	 * Returns the requestUser.
	 * @return String
	 */
	public String getRequestUser() {
		return requestUser;
	}

	/**
	 * Sets the message.
	 * @param message The message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Sets the requestDate.
	 * @param requestDate The requestDate to set
	 */
	public void setRequestDate(Date requestDate) {
		this.requestDate = requestDate;
	}

	/**
	 * Sets the requestUser.
	 * @param requestUser The requestUser to set
	 */
	public void setRequestUser(String requestUser) {
		this.requestUser = requestUser;
	}

	/**
	 * Returns the webasset.
	 * @return long
	 */
	public long getWebasset() {
		return webasset;
	}

	/**
	 * Sets the webasset.
	 * @param webasset The webasset to set
	 */
	public void setWebasset(long webasset) {
		this.webasset = webasset;
	}




	/**
	 * @return
	 */
	public String getAssignedToUserId() {
		return assignedToUserId;
	}


	/**
	 * @param string
	 */
	public void setAssignedToUserId(String string) {
		assignedToUserId = string;
	}

	/**
	 * @return
	 */
	public long getActionId() {
		return actionId;
	}

	/**
	 * @return
	 */
	public long getStatusId() {
		return statusId;
	}

	/**
	 * @param l
	 */
	public void setActionId(long l) {
		actionId = l;
	}

	/**
	 * @param l
	 */
	public void setStatusId(long l) {
		statusId = l;
	}

	/**
	 * @return
	 */
	public Date getNotifyRoleDate() {
		return notifyRoleDate;
	}

	/**
	 * @return
	 */
	public String getNotifyRoleId() {
		return notifyRoleId;
	}

	/**
	 * @param date
	 */
	public void setNotifyRoleDate(Date date) {
		notifyRoleDate = date;
	}

	/**
	 * @param string
	 */
	public void setNotifyRoleId(String string) {
		notifyRoleId = string;
	}


	/**
	 * @return
	 */
	public Date getAssignedToUserDate() {
		return assignedToUserDate;
	}

	/**
	 * @param date
	 */
	public void setAssignedToUserDate(Date date) {
		assignedToUserDate = date;
	}

}
