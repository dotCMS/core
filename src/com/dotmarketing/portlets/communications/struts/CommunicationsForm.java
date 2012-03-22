package com.dotmarketing.portlets.communications.struts;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.util.Constants;

/**
 * Communications Struts forms
 * @author Oswaldo
 *
 */
public class CommunicationsForm extends ValidatorForm {

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
	private String webModDate;
	private String inode;
	private String identifier;
	private String externalCommunicationIdentifier;
	
	/**
	 * Gets the COmmunication Identifier
	 * @return String
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Set the Communication identifier
	 * @param identifier
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Gets the Communication inode
	 * @return String
	 */
	public String getInode() {
		if(InodeUtils.isSet(inode))
			return inode;
		
		return "";
	}

	/**
	 * Set the communication inode
	 * @param inode
	 */
	public void setInode(String inode) {
		this.inode = inode;
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
	 * @param createDate
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
	 * @param htmlPageInode
	 */
	public void setHtmlPage(String htmlPageInode) {
		this.htmlPage = htmlPageInode;
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
	 * Returns the webCompletedDate.
	 * @return String
	 */
	public String getWebModDate() {
		return UtilMethods.dateToHTMLDate(modDate) + " " + UtilMethods.dateToHTMLTime(modDate);
	}
	
	/**
	 * Sets the webCompletedDate.
	 * @param webCompletedDate The webCompletedDate to set
	 */
	public void setWebModDate(String webModDate) {

		this.webModDate = webModDate;
		try {
			Logger.debug(this, "Setting Web Create Date " + webModDate);
			this.modDate = new SimpleDateFormat("MM/dd/yyyy HH:mm").parse(webModDate);			
			Logger.debug(this, "Setting Create Date " + this.webModDate);
		} catch(ParseException ex) {
		}
	}

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.ADD)) {
            //return super.validate(mapping, request);
        	ActionErrors ae = super.validate(mapping, request);

            Logger.debug(this, "action errors: " + ae);
            if(communicationType.equals("email")){
            	if(!UtilMethods.isSet(title)){
            		ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.communicationsTitle"));
            		
             	}
            	if(!UtilMethods.isSet(fromName)){
            		ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.communicationsFromName"));
            		
             	}
            	if(!UtilMethods.isSet(fromEmail)){
            		ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.communicationsFromEmail"));
            		
             	}
            	if(!UtilMethods.isSet(emailSubject)){
            		ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.communicationsSubject"));
            		
             	}
            	if(!InodeUtils.isSet(htmlPage) && !UtilMethods.isSet(textMessage)){
            		ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.communicationsTextMessage"));
            		
             	}
                
            	return ae;
            	
            }
            else if (communicationType.equals("alert")){
            	if(!UtilMethods.isSet(title)){
                	ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.communicationsTitle"));
                	
                }
            	if(!UtilMethods.isSet(textMessage)){
            		ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.communicationsTextMessage"));
                }
            	if(UtilMethods.isSet(textMessage) && (textMessage.length() > 2000)) {
                	ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.communicationsTextAlert.maximumLength"));
            	}
            	
            	return ae;
            }
            else {
            	if(!UtilMethods.isSet(title)){
                	ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.communicationsTitle"));
                	
                }
            	if(!InodeUtils.isSet(trackBackLinkInode)){
            		ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.communicationsTrackBackLinkInode"));
            		
             	}
            	
            	return ae;
            }
            
        }
        
        return null;
    }

	/**
	 * Get the External Communication Identifier of the communication
	 * @return String
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

}
