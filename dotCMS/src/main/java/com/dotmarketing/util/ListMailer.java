package com.dotmarketing.util;

import java.util.Iterator;
import java.util.Set;


/**
 * @author rocco
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ListMailer {
	
	private String fromEmail;
	private Set toList;
	private String ccEmail;
	private String bccEmail;
	private String subject;
	private String htmlMessage;
	
	
	public int SendMesageToList(){
		int numberSent = 0;
		
		if(fromEmail != null && toList!=null && htmlMessage!= null && subject != null){
			Iterator mli = toList.iterator();
			while(mli.hasNext()){
				
				Mailer mailer = new Mailer();
				mailer.setFromEmail(this.fromEmail);
				mailer.setToEmail((String) mli.next());
				if(this.bccEmail!=null){
					mailer.setBcc(this.bccEmail);
				}			
				if(this.ccEmail!=null){
					mailer.setBcc(this.ccEmail);
				}
				mailer.setSubject(subject);
				mailer.setHTMLAndTextBody(htmlMessage);
				mailer.sendMessage();
			}
		}			
			
		
		return numberSent;
	}

	/**
	 * Returns the bccEmail.
	 * @return String
	 */
	public String getBccEmail() {
		return bccEmail;
	}

	/**
	 * Returns the ccEmail.
	 * @return String
	 */
	public String getCcEmail() {
		return ccEmail;
	}

	/**
	 * Returns the fromEmail.
	 * @return String
	 */
	public String getFromEmail() {
		return fromEmail;
	}

	/**
	 * Returns the toList.
	 * @return java.util.List
	 */
	public java.util.Set getToList() {
		return toList;
	}

	/**
	 * Sets the bccEmail.
	 * @param bccEmail The bccEmail to set
	 */
	public void setBccEmail(String bccEmail) {
		this.bccEmail = bccEmail;
	}

	/**
	 * Sets the ccEmail.
	 * @param ccEmail The ccEmail to set
	 */
	public void setCcEmail(String ccEmail) {
		this.ccEmail = ccEmail;
	}

	/**
	 * Sets the fromEmail.
	 * @param fromEmail The fromEmail to set
	 */
	public void setFromEmail(String fromEmail) {
		this.fromEmail = fromEmail;
	}

	/**
	 * Sets the toList.
	 * @param toList The toList to set
	 */
	public void setToList(java.util.Set toList) {
		this.toList = toList;
	}

	/**
	 * Returns the htmlMessage.
	 * @return String
	 */
	public String getHtmlMessage() {
		return htmlMessage;
	}

	/**
	 * Returns the subject.
	 * @return String
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * Sets the htmlMessage.
	 * @param htmlMessage The htmlMessage to set
	 */
	public void setHtmlMessage(String htmlMessage) {
		this.htmlMessage = htmlMessage;
	}

	/**
	 * Sets the subject.
	 * @param subject The subject to set
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

}
