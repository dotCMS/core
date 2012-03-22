package com.dotmarketing.portlets.campaigns.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;

/** @author Hibernate CodeGenerator */
public class Campaign extends Inode implements Serializable, Permissionable {

	private static final long serialVersionUID = 1L;
	private String title;
	private java.util.Date CStartDate;
	private java.util.Date completedDate;
	private String fromEmail;
	private String fromName;
	private String subject;
	private String userId;
	private String message;
	private boolean active;
	private boolean locked;
	private String sendsPerHour;

    private String communicationInode;
    private boolean sendEmail;
    private String userFilterInode;
    private String sendTo;
    private boolean isRecurrent = false;
    private boolean wasSent = false;

	private java.util.Date expirationDate;

    private String parentCampaign;

    /**
	 * @return the wasSent
	 */
	public boolean getWasSent() {
		return wasSent;
	}

	/**
	 * @param isRecurrent the isRecurrent to set
	 */
	public void setWasSent(boolean wasSent) {
		this.wasSent = wasSent;
	}

	/**
	 * @return the isRecurrent
	 */
	public boolean getIsRecurrent() {
		return isRecurrent;
	}

	/**
	 * @param isRecurrent the isRecurrent to set
	 */
	public void setIsRecurrent(boolean isRecurrent) {
		this.isRecurrent = isRecurrent;
	}

	/**
	 * @return the userFilterInode
	 */
	public String getUserFilterInode() {
		return userFilterInode;
	}

	/**
	 * @param userFilterInode the userFilterInode to set
	 */
	public void setUserFilterInode(String userFilterInode) {
		this.userFilterInode = userFilterInode;
	}

	/**
	 * @return Returns the communicationInode.
	 */
	public String getCommunicationInode() {
		return communicationInode;
	}

	/**
	 * @param communicationInode The communicationInode to set.
	 */
	public void setCommunicationInode(String communicationInode) {
		this.communicationInode = communicationInode;
	}

	/**
	 * @return Returns the sendEmail.
	 */
	public boolean isSendEmail() {
		return sendEmail;
	}

	/**
	 * @param sendEmail The sendEmail to set.
	 */
	public void setSendEmail(boolean sendEmail) {
		this.sendEmail = sendEmail;
	}

	/**
	 * Returns the completedDate.
	 * @return java.util.Date
	 */
	public java.util.Date getCompletedDate() {
		return completedDate;
	}

	public Campaign() {
		super.setType("campaign");
		CStartDate = new java.util.Date();
	}
	/**
	 * Returns the fromEmail.
	 * @return String
	 */
	public String getFromEmail() {
		return fromEmail;
	}

	/**
	 * Returns the message.
	 * @return String
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Returns the CStartDate.
	 * @return java.util.Date
	 */
	public java.util.Date getCStartDate() {
		return CStartDate;
	}

	/**
	 * Returns the subject.
	 * @return String
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * Returns the title.
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns the userId.
	 * @return String
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Sets the completedDate.
	 * @param completedDate The completedDate to set
	 */
	public void setCompletedDate(java.util.Date completedDate) {
		this.completedDate = completedDate;
	}

	/**
	 * Sets the fromEmail.
	 * @param fromEmail The fromEmail to set
	 */
	public void setFromEmail(String fromEmail) {
		this.fromEmail = fromEmail;
	}

	/**
	 * Sets the message.
	 * @param message The message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Sets the CStartDate.
	 * @param startDate The startDate to set
	 */
	public void setCStartDate(java.util.Date CStartDate) {
		this.CStartDate = CStartDate;
	}

	/**
	 * Sets the subject.
	 * @param subject The subject to set
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * Sets the title.
	 * @param title The title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Sets the userId.
	 * @param userId The userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * Returns the active.
	 * @return boolean
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Sets the active.
	 * @param active The active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * Returns the fromName.
	 * @return String
	 */
	public String getFromName() {
		return fromName;
	}

	/**
	 * Sets the fromName.
	 * @param fromName The fromName to set
	 */
	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	/**
	 * Returns the locked.
	 * @return boolean
	 */
	public boolean isLocked() {
		return locked;
	}

	/**
	 * Sets the locked.
	 * @param locked The locked to set
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public String getSendsPerHour() {
		return sendsPerHour;
	}

	public void setSendsPerHour(String sendsPerHour) {
		this.sendsPerHour = sendsPerHour;
	}

	/**
	 * @return the sendTo
	 */
	public String getSendTo() {
		return sendTo;
	}

	/**
	 * @param sendTo the sendTo to set
	 */
	public void setSendTo(String sendTo) {
		this.sendTo = sendTo;
	}

	/**
	 * @return the expirationDate
	 */
	public java.util.Date getExpirationDate() {
		return expirationDate;
	}

	/**
	 * @param expirationDate the expirationDate to set
	 */
	public void setExpirationDate(java.util.Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	/**
	 * @return the parentCampaign
	 */
	public String getParentCampaign() {
		return parentCampaign;
	}

	/**
	 * @param parentCampaign the parentCampaign to set
	 */
	public void setParentCampaign(String parentCampaign) {
		this.parentCampaign = parentCampaign;
	}
	
	/**
	 * List of permissions it accepts
	 */
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
		accepted.add(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ));
		accepted.add(new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		return accepted;
	}

}
