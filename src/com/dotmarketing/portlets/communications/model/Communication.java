package com.dotmarketing.portlets.communications.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;

/**
 * This class is to get the communication object of 
 * the communication manager portlet
 * 
 * @author Oswaldo
 *
 */
public class Communication extends Inode implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private String title;
	private String trackBackLinkInode;
	private String communicationType;
	private String fromEmail;
	private String fromName;
	private String emailSubject;
	private String htmlPage;
	private String textMessage;
	private java.util.Date modDate;
	private String modifiedBy;
	private String externalCommunicationIdentifier;
	
	
	public Communication() {
		super.setType("communication");
		this.modDate = new java.util.Date();
		this.communicationType = "email";
		this.trackBackLinkInode= "";
		this.htmlPage = "";
		this.fromEmail = "";
		this.fromName = "";
		this.emailSubject = "";
		this.textMessage = "";
		this.title = "";
		this.modifiedBy = "";
	}
	/**
	 * Get the modification date of the communication
	 * @return java.util.Date
	 */
	public java.util.Date getModDate() {
		return modDate;
	}
	
	/**
	 * Set the communication modification date
	 * @param modDate
	 */
	public void setModDate(java.util.Date modDate) {
		this.modDate = modDate;
	}
	
	/**
	 * Get the userId of the user that modify the communication
	 * @return String
	 */
	public String getModifiedBy() {
		return modifiedBy;
	}
	
	/**
	 * Set the userId of the user that modify the communication
	 * @param createdBy
	 */
	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}
	
	/**
	 * Get the email subject of the communication only apply to email communication
	 * @return String
	 */
	public String getEmailSubject() {
		return emailSubject;
	}
	
	/**
	 * Set the email subject of the communication only apply to email communication
	 * @param emailSubject
	 */
	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}
	
	/**
	 * Get the email of the user that create the communication only apply to email communication
	 * @return String
	 */
	public String getFromEmail() {
		return fromEmail;
	}
	
	/**
	 * Set the email of the user that create the comunication only apply to email communication
	 * @param fromEmail
	 */
	public void setFromEmail(String fromEmail) {
		this.fromEmail = fromEmail;
	}
	
	/**
	 * Get the name of the user that create the communication only apply to email communication
	 * @return String
	 */
	public String getFromName() {
		return fromName;
	}
	
	/**
	 * Set the name of the user that create the communication only apply to email communication
	 * @param fromName
	 */
	public void setFromName(String fromName) {
		this.fromName = fromName;
	}
	
	/**
	 * Get the html inode of the email communication
	 * @return String
	 */
	public String getHtmlPage() {
		return htmlPage;
	}
	
	/**
	 * Set the email html page inode
	 * @param htmlPageId
	 */
	public void setHtmlPage(String htmlPageId) {
		this.htmlPage = htmlPageId;
	}
	
	/**
	 * Get the email text Message of the communication
	 * @return String
	 */
	public String getTextMessage() {
		return textMessage;
	}
	
	/**
	 * Set the email text message of the communication
	 * @param textMessage
	 */
	public void setTextMessage(String textMessage) {
		this.textMessage = textMessage;
	}
	
	/**
	 * Get the communication title
	 * @return String
	 */
	public String getTitle() {
		return title;
	}
	
	/**
	 * Set the communication title
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	
	/**
	 * Get the page inode link of the offline communication
	 * @return String
	 */
	public String getTrackBackLinkInode() {
		return trackBackLinkInode;
	}
	
	/**
	 * Set the inode of the offline communication 
	 * @param trackBackLinkInode
	 */
	public void setTrackBackLinkInode(String trackBackLinkInode) {
		this.trackBackLinkInode = trackBackLinkInode;
	}
	
	/**
	 * Get the type of communication email or offline
	 * @return String
	 */
	public String getCommunicationType() {
		return communicationType;
	}
	
	/**
	 * Set the communication type
	 */
	public void setCommunicationType(String communicationType) {
		this.communicationType = communicationType;
	}
	
	/**
	 * Get the External Communication Identifier of the communication
	 * @return Strin
	 */
	public String getExternalCommunicationIdentifier() {
		return externalCommunicationIdentifier;
	}
	
	/**
	 * Set the External Communication Identifier
	 * @param modDate
	 */
	public void setExternalCommunicationIdentifier(
			String externalCommunicationIdentifier) {
		this.externalCommunicationIdentifier = externalCommunicationIdentifier;
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
