package com.dotmarketing.portlets.workflowmessages.struts;


import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.Logger;
import com.liferay.portal.util.Constants;

/** @author Hibernate CodeGenerator */
public class WorkflowMessageForm extends ValidatorForm {

    private static final long serialVersionUID = 1L;

	/** identifier field */
    private String parent;
    
    private String message;
    
	private Date requestDate;
	  
    private String requestUser;

	private long statusId;

	private long actionId;

	private String webasset;

	private String emailToUserId;
	
	private String assignedToUserId;
	
	private Date assignedToUserDate;

	private String notifyRoleId;

	private Date notifyRoleDate;
	
	private String oldInode;


    /** nullable persistent field */
    private String selectedwebasset;

    /** nullable persistent field */
    private String selectedwebassetPath;

	
	/*** WEB ASSET FIELDS FOR THE FORM ***/
    /** nullable persistent field */
    private String title;

    /** nullable persistent field */
    private String friendlyName;

    /** nullable persistent field */
    private boolean showOnMenu;

    /** nullable persistent field */
    private boolean internal;

    /** nullable persistent field */
    private int sortOrder;
	/*** WEB ASSET FIELDS FOR THE FORM ***/


    /** nullable persistent field */
    private String selectedparent;

    /** nullable persistent field */
    private String selectedparentPath;

    public WorkflowMessageForm() {
		requestDate = new Date();
		notifyRoleDate = new Date();
		assignedToUserDate = new Date();
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		
		if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.ADD)) {
			return super.validate(mapping, request);
		}
	
		return null;
	}

	/**
	 * Returns the message.
	 * @return String
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Returns the parent.
	 * @return String
	 */
	public String getParent() {
		return parent;
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
	 * Returns the selectedparent.
	 * @return String
	 */
	public String getSelectedparent() {
		return selectedparent;
	}

	/**
	 * Returns the selectedparentPath.
	 * @return String
	 */
	public String getSelectedparentPath() {
		return selectedparentPath;
	}

	/**
	 * Sets the message.
	 * @param message The message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Sets the parent.
	 * @param parent The parent to set
	 */
	public void setParent(String parent) {
		this.parent = parent;
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
	 * Sets the selectedparent.
	 * @param selectedparent The selectedparent to set
	 */
	public void setSelectedparent(String selectedparent) {
		this.selectedparent = selectedparent;
	}

	/**
	 * Sets the selectedparentPath.
	 * @param selectedparentPath The selectedparentPath to set
	 */
	public void setSelectedparentPath(String selectedparentPath) {
		this.selectedparentPath = selectedparentPath;
	}

	/**
	 * Returns the friendlyName.
	 * @return String
	 */
	public String getFriendlyName() {
		return friendlyName;
	}

	/**
	 * Returns the internal.
	 * @return boolean
	 */
	public boolean isInternal() {
		return internal;
	}

	/**
	 * Returns the showOnMenu.
	 * @return boolean
	 */
	public boolean isShowOnMenu() {
		return showOnMenu;
	}

	/**
	 * Returns the sortOrder.
	 * @return int
	 */
	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * Returns the title.
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the friendlyName.
	 * @param friendlyName The friendlyName to set
	 */
	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	/**
	 * Sets the internal.
	 * @param internal The internal to set
	 */
	public void setInternal(boolean internal) {
		this.internal = internal;
	}

	/**
	 * Sets the showOnMenu.
	 * @param showOnMenu The showOnMenu to set
	 */
	public void setShowOnMenu(boolean showOnMenu) {
		this.showOnMenu = showOnMenu;
	}

	/**
	 * Sets the sortOrder.
	 * @param sortOrder The sortOrder to set
	 */
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	/**
	 * Sets the title.
	 * @param title The title to set
	 */
	public void setTitle(String title) {		
		this.title = title;
	}

	/**
	 * Returns the selectedwebasset.
	 * @return String
	 */
	public String getSelectedwebasset() {
		return selectedwebasset;
	}

	/**
	 * Sets the selectedwebasset.
	 * @param selectedwebasset The selectedwebasset to set
	 */
	public void setSelectedwebasset(String selectedwebasset) {
		this.selectedwebasset = selectedwebasset;
	}

	/**
	 * Returns the selectedwebassetPath.
	 * @return String
	 */
	public String getSelectedwebassetPath() {
		return selectedwebassetPath;
	}

	/**
	 * Sets the selectedwebassetPath.
	 * @param selectedwebassetPath The selectedwebassetPath to set
	 */
	public void setSelectedwebassetPath(String selectedwebassetPath) {
		this.selectedwebassetPath = selectedwebassetPath;
	}

	/**
	 * Returns the webasset.
	 * @return String
	 */
	public String getWebasset() {
		return webasset;
	}

	/**
	 * Sets the webasset.
	 * @param webasset The webasset to set
	 */
	public void setWebasset(String webasset) {
		Logger.info(this, "setting web asset");
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
	public String getNotifyRoleId() {
		return notifyRoleId;
	}

	/**
	 * @param string
	 */
	public void setNotifyRoleId(String roleId) {
		this.notifyRoleId = roleId;
	}

	/**
	 * @return
	 */
	public Date getAssignedToUserDate() {
		return assignedToUserDate;
	}

	/**
	 * @return
	 */
	public Date getNotifyRoleDate() {
		return notifyRoleDate;
	}

	/**
	 * @param date
	 */
	public void setAssignedToUserDate(Date date) {
		assignedToUserDate = date;
	}

	/**
	 * @param date
	 */
	public void setNotifyRoleDate(Date date) {
		notifyRoleDate = date;
	}

	/**
	 * @return
	 */
	public String getEmailToUserId() {
		return emailToUserId;
	}

	/**
	 * @param string
	 */
	public void setEmailToUserId(String string) {
		emailToUserId = string;
	}

	/**
	 * @return
	 */
	public String getOldInode() {
		return oldInode;
	}

	/**
	 * @param l
	 */
	public void setOldInode(String l) {
		oldInode = l;
	}

}
