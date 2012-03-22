package com.dotmarketing.portlets.campaigns.struts;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

public class CampaignForm extends ValidatorForm {

    private static final long serialVersionUID = 1L;
	private String title;
    private java.util.Date CStartDate;
    private java.util.Date completedDate;
    private boolean active;
	private String mailingList;
    private String fromName;
    private String fromEmail;
    private String subject;
    private String userId;
    private String message;
    private String webCompletedDate;
    private String webStartDate;
    private String sendsPerHour;
    
    private String inode;

    private String htmlPage;
    
    private String selectedHtmlPage;
    
    private String communicationInode;
    private boolean sendEmail;
    
    private String sendTo = "mailingList";
    private String userFilterInode;
    private boolean isRecurrent = false;

    /****** begin recurrent scheduler variables ******/
    private String jobName;
    private String jobGroup;
    private String jobDescription;
    
    private String javaClass;
    
    private boolean haveStartDate;
    private String startDate;
    private boolean haveEndDate;
    private String endDate;
    
    private boolean atInfo;
    private String at;
    private int atTimeHour;
    private int atTimeMinute;
    private int atTimeSecond;
    private int betweenFromHour;
    private int betweenFromMinute;
    private int betweenFromSecond;
    private int betweenToHour;
    private int betweenToMinute;
    private int betweenToSecond;
    
    private boolean everyInfo;
    private String every;
    private int everyDateMonth;
    private int everyDateDay;
    private int everyDateYear;
    private boolean isMonday;
    private boolean isTuesday;
    private boolean isWednesday;
    private boolean isThusday;
    private boolean isFriday;
    private boolean isSaturday;
    private boolean isSunday;
    
    private boolean eachInfo;
    private int eachHours;
    private int eachMinutes;
    
    private boolean displayRecurrence; 
    
    /****** end recurrent scheduler variables ******/

	private java.util.Date expirationDate;
    private String webExpirationDate;

    private String parentCampaign;

    public boolean isDisplayRecurrence() {
		return displayRecurrence;
	}

	public void setDisplayRecurrence(boolean displayRecurrence) {
		this.displayRecurrence = displayRecurrence;
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
	 * Returns the completedDate.
	 * @return java.util.Date
	 */
	public java.util.Date getCompletedDate() {
		return completedDate;
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
	 * Returns the webCompletedDate.
	 * @return String
	 */
	public String getWebCompletedDate() {
		return UtilMethods.dateToHTMLDate(completedDate) + " " + UtilMethods.dateToHTMLTime(completedDate);
	}

	/**
	 * Returns the webStartDate.
	 * @return String
	 */
	public String getWebStartDate() {
		return UtilMethods.dateToHTMLDate(CStartDate) + " " + UtilMethods.dateToHTMLTime(CStartDate);

	}

	/**
	 * Sets the webCompletedDate.
	 * @param webCompletedDate The webCompletedDate to set
	 */
	public void setWebCompletedDate(String webCompletedDate) {

		this.webCompletedDate = webCompletedDate;
		try {
			Logger.debug(this, "Setting Web Completed Date " + webCompletedDate);
			this.completedDate = new SimpleDateFormat("MM/dd/yyyy HH:mm").parse(webCompletedDate);			
			Logger.debug(this, "Setting Completed Date " + this.completedDate);
		} catch(ParseException ex) {
		}
	}

	/**
	 * Sets the webStartDate.
	 * @param webStartDate The webStartDate to set
	 */
	public void setWebStartDate(String webStartDate) {

		this.webStartDate = webStartDate;
		try {
			Logger.debug(this, "Setting Web Start Date " + webStartDate);
			this.CStartDate = new SimpleDateFormat("MM/dd/yyyy HH:mm").parse(webStartDate);			
			Logger.debug(this, "Setting Start Date " + this.CStartDate);
		} catch(ParseException ex) {
		}
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
	 * Sets the inode.
	 * @param inode The inode to set
	 */
	public void setInode(String inode) {
		this.inode = inode;
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
	 * Returns the mailingList.
	 * @return String
	 */
	public String getMailingList() {
		return mailingList;
	}

	/**
	 * Sets the mailingList.
	 * @param mailingList The mailingList to set
	 */
	public void setMailingList(String mailingList) {
		this.mailingList = mailingList;
	}

	/**
	 * Returns the htmlPage.
	 * @return String
	 */
	public String getHtmlPage() {
		return htmlPage;
	}



	/**
	 * Sets the htmlPage.
	 * @param htmlPage The htmlPage to set
	 */
	public void setHtmlPage(String htmlPage) {
		this.htmlPage = htmlPage;
	}


	/**
	 * Returns the selectedHtmlPage.
	 * @return String
	 */
	public String getSelectedHtmlPage() {
		return selectedHtmlPage;
	}

	/**
	 * Sets the selectedHtmlPage.
	 * @param selectedHtmlPage The selectedHtmlPage to set
	 */
	public void setSelectedHtmlPage(String selectedHtmlPage) {
		this.selectedHtmlPage = selectedHtmlPage;
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

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.ADD)) {

        	ActionErrors ae = super.validate(mapping, request);
        	
        	if (!UtilMethods.isSet(mailingList) && !UtilMethods.isSet(userFilterInode)) {
        		ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("Please-select-a-Mailing-List"));
        	}
        	
        	if((UtilMethods.isSet(webExpirationDate)) && (expirationDate == null)) {
        		ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.campaign.error.expiration.date.incomplete"));
			}
        	if(expirationDate != null && (expirationDate.before(new Date()))) {
        		ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.campaign.error.wrong.expiration.date"));
        	}

        	return ae;
        }
        return null;
    }

	public String getSendsPerHour() {
		return sendsPerHour;
	}

	public void setSendsPerHour(String sendsPerHour) {
		this.sendsPerHour = sendsPerHour;
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
	 * @param isRecurrent the isRecurrent to set
	 */
	public void setRecurrent(boolean isRecurrent) {
		this.isRecurrent = isRecurrent;
	}

    /****** begin recurrent scheduler getters/setters ******/

    public int getBetweenFromHour() {
		return betweenFromHour;
	}

	public void setBetweenFromHour(int betweenFromHour) {
		this.betweenFromHour = betweenFromHour;
	}

	public int getBetweenFromMinute() {
		return betweenFromMinute;
	}

	public void setBetweenFromMinute(int betweenFromMinute) {
		this.betweenFromMinute = betweenFromMinute;
	}

	public int getBetweenFromSecond() {
		return betweenFromSecond;
	}

	public void setBetweenFromSecond(int betweenFromSecond) {
		this.betweenFromSecond = betweenFromSecond;
	}

	public String getAt() {
		return at;
	}

	public void setAt(String at) {
		this.at = at;
	}

	public boolean isAtInfo() {
		return atInfo;
	}

	public void setAtInfo(boolean atInfo) {
		this.atInfo = atInfo;
	}

	public int getAtTimeHour() {
		return atTimeHour;
	}

	public void setAtTimeHour(int atTimeHour) {
		this.atTimeHour = atTimeHour;
	}

	public int getAtTimeMinute() {
		return atTimeMinute;
	}

	public void setAtTimeMinute(int atTimeMinute) {
		this.atTimeMinute = atTimeMinute;
	}

	public int getAtTimeSecond() {
		return atTimeSecond;
	}

	public void setAtTimeSecond(int atTimeSecond) {
		this.atTimeSecond = atTimeSecond;
	}

	public String getEndDate() {
		return endDate;
	}
	
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	
	public String getJavaClass() {
		return javaClass;
	}
	
	public void setJavaClass(String javaClass) {
		this.javaClass = javaClass;
	}
	
	public String getJobDescription() {
		return jobDescription;
	}
	
	public void setJobDescription(String jobDescription) {
		this.jobDescription = jobDescription;
	}
	
	public String getJobName() {
		return jobName;
	}
	
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	
	public String getStartDate() {
		return startDate;
	}
	
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getJobGroup() {
		return jobGroup;
	}

	public void setJobGroup(String jobGroup) {
		this.jobGroup = jobGroup;
	}

	public boolean isHaveEndDate() {
		return haveEndDate;
	}

	public void setHaveEndDate(boolean haveEndDate) {
		this.haveEndDate = haveEndDate;
	}

	public boolean isHaveStartDate() {
		return haveStartDate;
	}

	public void setHaveStartDate(boolean haveStartDate) {
		this.haveStartDate = haveStartDate;
	}

	public int getBetweenToHour() {
		return betweenToHour;
	}

	public void setBetweenToHour(int betweenToHour) {
		this.betweenToHour = betweenToHour;
	}

	public int getBetweenToMinute() {
		return betweenToMinute;
	}

	public void setBetweenToMinute(int betweenToMinute) {
		this.betweenToMinute = betweenToMinute;
	}

	public int getBetweenToSecond() {
		return betweenToSecond;
	}

	public void setBetweenToSecond(int betweenToSecond) {
		this.betweenToSecond = betweenToSecond;
	}

	public String getEvery() {
		return every;
	}

	public void setEvery(String every) {
		this.every = every;
	}

	public int getEveryDateDay() {
		return everyDateDay;
	}

	public void setEveryDateDay(int everyDateDay) {
		this.everyDateDay = everyDateDay;
	}

	public int getEveryDateMonth() {
		return everyDateMonth;
	}

	public void setEveryDateMonth(int everyDateMonth) {
		this.everyDateMonth = everyDateMonth;
	}

	public int getEveryDateYear() {
		return everyDateYear;
	}

	public void setEveryDateYear(int everyDateYear) {
		this.everyDateYear = everyDateYear;
	}

	public boolean isEveryInfo() {
		return everyInfo;
	}

	public void setEveryInfo(boolean everyInfo) {
		this.everyInfo = everyInfo;
	}

	public boolean isFriday() {
		return isFriday;
	}

	public void setFriday(boolean isFriday) {
		this.isFriday = isFriday;
	}

	public boolean isMonday() {
		return isMonday;
	}

	public void setMonday(boolean isMonday) {
		this.isMonday = isMonday;
	}

	public boolean isSaturday() {
		return isSaturday;
	}

	public void setSaturday(boolean isSaturday) {
		this.isSaturday = isSaturday;
	}

	public boolean isSunday() {
		return isSunday;
	}

	public void setSunday(boolean isSunday) {
		this.isSunday = isSunday;
	}

	public boolean isThusday() {
		return isThusday;
	}

	public void setThusday(boolean isThusday) {
		this.isThusday = isThusday;
	}

	public boolean isTuesday() {
		return isTuesday;
	}

	public void setTuesday(boolean isTuesday) {
		this.isTuesday = isTuesday;
	}

	public boolean isWednesday() {
		return isWednesday;
	}

	public void setWednesday(boolean isWednesday) {
		this.isWednesday = isWednesday;
	}

	public int getEachHours() {
		return eachHours;
	}

	public void setEachHours(int eachHours) {
		this.eachHours = eachHours;
	}

	public boolean isEachInfo() {
		return eachInfo;
	}

	public void setEachInfo(boolean eachInfo) {
		this.eachInfo = eachInfo;
	}

	public int getEachMinutes() {
		return eachMinutes;
	}

	public void setEachMinutes(int eachMinutes) {
		this.eachMinutes = eachMinutes;
	}
    /****** end recurrent scheduler getters/setters ******/

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
	 * @return the webExpirationDate
	 */
	public String getWebExpirationDate() {
		return UtilMethods.dateToHTMLDate(expirationDate) + " " + UtilMethods.dateToHTMLTime(expirationDate);
	}

	/**
	 * @param webExpirationDate the webExpirationDate to set
	 */
	public void setWebExpirationDate(String webExpirationDate) {
		this.webExpirationDate = webExpirationDate;
		try {
			Logger.debug(this, "Setting Web Expiration Date " + webExpirationDate);
			this.expirationDate = new SimpleDateFormat("MM/dd/yyyy HH:mm").parse(webExpirationDate);			
			Logger.debug(this, "Setting Expiration Date " + this.expirationDate);
		} catch(ParseException ex) {
		}
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
	

}
