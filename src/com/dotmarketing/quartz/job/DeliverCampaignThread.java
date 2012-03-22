package com.dotmarketing.quartz.job;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.cms.factories.PublicAddressFactory;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.EmailFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.campaigns.factories.CampaignFactory;
import com.dotmarketing.portlets.campaigns.factories.RecipientFactory;
import com.dotmarketing.portlets.campaigns.model.Campaign;
import com.dotmarketing.portlets.campaigns.model.Recipient;
import com.dotmarketing.portlets.communications.model.Communication;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.mailinglists.factories.MailingListFactory;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.portlets.user.factories.UserCommentsFactory;
import com.dotmarketing.portlets.user.model.UserComment;
import com.dotmarketing.portlets.userfilter.factories.UserFilterFactory;
import com.dotmarketing.portlets.userfilter.model.UserFilter;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;

public class DeliverCampaignThread implements Runnable, StatefulJob {

	private Campaign campaign;
	private User subscriber;
	private Recipient recipient;
	private StringBuffer message;
	private String alternateTextMessage;
	private JobExecutionContext context;

	/**
	 * @return the context
	 */
	public JobExecutionContext getContext() {
		return context;
	}
	/**
	 * @param context the context to set
	 */
	public void setContext(JobExecutionContext context) {
		this.context = context;
	}
	/**
	 * @return Returns the alternateTextMessage.
	 */
	public String getAlternateTextMessage() {
		return alternateTextMessage;
	}
	/**
	 * @param alternateTextMessage The alternateTextMessage to set.
	 */
	public void setAlternateTextMessage(String alternateTextMessage) {
		this.alternateTextMessage = alternateTextMessage;
	}
	private boolean html;




	public DeliverCampaignThread() {
	}



	public void run() {
		
		JobDataMap data = context.getJobDetail().getJobDataMap();
		String inode = data.getString("inode");
		List<Campaign> campaigns;
		RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
		
		/**
		 * if inode is set on Job Context, there's a recurrent campaign, otherwise
		 * pulls a list of all waiting recipients, iterates through that list and
		 * calls the sendNewsletter method for each one. After sending, the
		 * recipient sent property is updated to the current time
		 *
		 */
		if (InodeUtils.isSet(inode)) {
			Campaign c = CampaignFactory.getCampaign(inode);
			campaigns = new ArrayList<Campaign>();
			if (c.isActive() && 
					!(UtilMethods.isSet(c.getExpirationDate()) && c.getExpirationDate().before(new Date()))) {
				// create new child campaign and added to campaign array
				try {
					Campaign childCampaign = new Campaign();
					BeanUtils.copyProperties(childCampaign, c);
					childCampaign.setParentCampaign(c.getInode());
					childCampaign.setInode(null);
					childCampaign.setIsRecurrent(false);
					childCampaign.setLocked(true);
					HibernateUtil.saveOrUpdate(childCampaign);

					MailingList ml = (MailingList) InodeFactory.getChildOfClass(c, MailingList.class);
					HTMLPage page = (HTMLPage) InodeFactory.getChildOfClass(c, HTMLPage.class);
					Communication comm = (Communication) InodeFactory.getChildOfClass(c, Communication.class);
					UserFilter userfilter = (UserFilter) InodeFactory.getChildOfClass(c, UserFilter.class);

					if(InodeUtils.isSet(ml.getInode())){
						relationshipAPI.addRelationship(childCampaign.getInode(), ml.getInode(), "child");
					}
					if(InodeUtils.isSet(page.getInode())){
						relationshipAPI.addRelationship(childCampaign.getInode(), page.getInode(), "child");
					}
					if (InodeUtils.isSet(comm.getInode())) {
						relationshipAPI.addRelationship(childCampaign.getInode(), comm.getInode(), "child");
					}
					if (InodeUtils.isSet(userfilter.getInode())) {
						relationshipAPI.addRelationship(childCampaign.getInode(), userfilter.getInode(), "child");
					}
					HibernateUtil.saveOrUpdate(childCampaign);


					campaigns.add(childCampaign);



					c.setLocked(false);
				}
				catch (Exception ex) {
					Logger.debug(this, ex.getMessage());
				}
			}
		}
		else {
			//get a list of waiting queues
			campaigns = CampaignFactory.getWaitingCampaigns();
		}

		StringBuffer message = null;

		if (campaigns.size() == 0)
		{
			campaigns = null;
			return;
		}

		try {

			Logger.debug(DeliverCampaignThread.class, "GOING to deliver campaigns");
			Iterator<Campaign> campIter = campaigns.iterator();
			
			//### LOOP THE CAMPAIGNS ###
			while (campIter.hasNext())
			{
				//Obtain the campaign
				Campaign c = (Campaign) campIter.next();
				Communication comm = (Communication) InodeFactory.getChildOfClass(c, Communication.class);
				
				if ((comm == null) || (!InodeUtils.isSet(comm.getInode()))) {
					Logger.info(DeliverCampaignThread.class, "I didn't find a communication for campaign inode=" + c.getInode());
					
					c.setCompletedDate(new java.util.Date());
					c.setLocked(false);
					message = null;
					HibernateUtil.saveOrUpdate(c);
					
					continue;
				}

				Logger.debug(DeliverCampaignThread.class, "got campaign:" + c.getTitle());
				//Mailing list
				String campaingSendTo = c.getSendTo();

				MailingList ml = null;
				UserFilter uf = null;
				List<UserProxy> subscribers =  null;
				if ((campaingSendTo != null) && campaingSendTo.equalsIgnoreCase("mailingList")) {
					ml = (MailingList) InodeFactory.getChildOfClass(c,MailingList.class);
					if (!InodeUtils.isSet(ml.getInode()))
					{
						Logger.info(DeliverCampaignThread.class, "I didn't find a mailing list for campaign inode=" + c.getInode());
						
						c.setCompletedDate(new java.util.Date());
						c.setLocked(false);
						message = null;
						HibernateUtil.saveOrUpdate(c);
						
						continue;
					}
					else
					{
						Logger.debug(DeliverCampaignThread.class, "got mailingList:" + ml.getTitle());
						//Get the subscribers
						subscribers = MailingListFactory.getMailingListSubscribers(ml);
						Logger.debug(DeliverCampaignThread.class, "Got subscribers:" + subscribers.size());
					}
				}
				else if ((campaingSendTo != null) && campaingSendTo.equalsIgnoreCase("userFilter")) {
					uf = (UserFilter) InodeFactory.getChildOfClass(c,UserFilter.class);
					if (!InodeUtils.isSet(uf.getInode()))
					{
						Logger.info(DeliverCampaignThread.class, "I didn't find an user filter for campaign inode=" + c.getInode());
						
						c.setCompletedDate(new java.util.Date());
						c.setLocked(false);
						message = null;
						HibernateUtil.saveOrUpdate(c);
						
						continue;
					}
					else
					{
						Logger.debug(DeliverCampaignThread.class, "got user filter:" + uf.getUserFilterTitle());
						//Get the subscribers
						try {
							subscribers = UserFilterFactory.getUserProxiesFromFilter(uf);
						}
						catch (Exception e) {
							Logger.info(DeliverCampaignThread.class, "Error getting subscriber from user filter for campaign inode=" + c.getInode());
							
							c.setCompletedDate(new java.util.Date());
							c.setLocked(false);
							message = null;
							HibernateUtil.saveOrUpdate(c);
							
							continue;
						}
						Logger.debug(DeliverCampaignThread.class, "Got subscribers:" + subscribers.size());
					}
				}

				//Unsubscriber mailing list
				MailingList unSubscribers = MailingListFactory.getUnsubscribersMailingList();

				//do we have an html page?
				String alternateTextMessage = null;
				HTMLPageAPI pageAPI = APILocator.getHTMLPageAPI();
				HTMLPage htmlPage = (HTMLPage) pageAPI.loadWorkingPageById(comm.getHtmlPage(), APILocator.getUserAPI().getSystemUser(), false);
				String serverName;
				if (htmlPage != null && UtilMethods.isSet(htmlPage.getTitle())) {
					html = true;
					Logger.debug(DeliverCampaignThread.class, "Got htmlPage:"+ htmlPage.getTitle());

					// get the newsletter and the attachments
					Identifier id = APILocator.getIdentifierAPI().find(htmlPage);

					serverName = APILocator.getHostAPI().find(id.getHostId(), APILocator.getUserAPI().getSystemUser(), false).getHostname();

					//rewrite the urls
					try {
						Logger.debug(DeliverCampaignThread.class, "Retrieving page from url " + "http://"+ serverName + UtilMethods.encodeURIComponent(id.getURI()));
						message = new StringBuffer(UtilMethods.escapeUnicodeCharsForHTML(UtilMethods.getURL("http://"+ serverName + UtilMethods.encodeURIComponent(id.getURI())).toString()));
						Logger.debug(DeliverCampaignThread.class, "Page retrieved " + message);
						message = EmailFactory.alterBodyHTML(message, serverName);
						Logger.debug(DeliverCampaignThread.class, "Page altered " + message);
						alternateTextMessage = "If you are having trouble reading this message, click here: "+ "http://"+ serverName + UtilMethods.encodeURIComponent(id.getURI());
					}catch(Exception e){
						/**
						 * This condition was included to avoid send  
						 * campaigns without content
						 * */
						
						Logger.info(DeliverCampaignThread.class, "Error generating message for campaign inode=" + c.getInode() + " and htmlPage inode=" + htmlPage.getInode());
						
						c.setCompletedDate(new java.util.Date());
						c.setLocked(false);
						message = null;
						HibernateUtil.saveOrUpdate(c);
						
						continue;
					}

				} else {
					serverName = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false).getHostname();
					html = false;
					if (comm.getTextMessage() != null){

						message = new StringBuffer(UtilMethods.escapeUnicodeCharsForHTML(comm.getTextMessage()));
						message = EmailFactory.alterBodyHTML(message, serverName);
					}
					else
						message = new StringBuffer("");
				}
				//add tracking shim
				message.append("<img src='http://"+ serverName + "/imageShim?r=<rId>' width='1' height='1'>");

				// big loop over recipients
				Iterator<UserProxy> subscriberIterator = subscribers.iterator();
				String send_per_hour = c.getSendsPerHour();
				User user = new User();
				try {
					user = APILocator.getUserAPI().loadUserById(c.getUserId(),APILocator.getUserAPI().getSystemUser(),false);
				} catch (Exception e) {
					Logger.error(DeliverCampaignThread.class, "deliverCampaigns Failed - getting campaign owner: "+ c.getUserId()+ " " + e.getMessage());
				}


				//### LOOP THE SUBSCRIBERS OF EACH CAMPAIGN ###
				while (subscriberIterator.hasNext())
				{
					UserProxy userProxy = (UserProxy) subscriberIterator.next();
					try {
						// validting permissions over subscriber
						try {
							_checkUserPermissions(userProxy, user, PERMISSION_READ);
						}catch(Exception e){
							continue;
						}
	
						User member = new User();
						try {
							member = APILocator.getUserAPI().loadUserById(userProxy.getUserId(), APILocator.getUserAPI().getSystemUser(), false);
						} catch (Exception e) {
							Logger.error(DeliverCampaignThread.class, "deliverCampaigns Failed - getting subscriber: " + userProxy.getUserId() + " " + e.getMessage());
							
						}
	
						Recipient recipient = RecipientFactory.getRecipientByCampaignAndSubscriber(c, member);
						recipient.setUserId(member.getUserId());
						if (EmailFactory.isSubscribed(unSubscribers,member))
						{
							recipient.setLastMessage("On "+MailingListFactory.getUnsubscribersMailingList().getTitle());
							recipient.setLastResult(500);
							userProxy.setLastMessage("On "+MailingListFactory.getUnsubscribersMailingList().getTitle());
							userProxy.setLastResult(500);
							c.addChild(recipient);
							continue;
						}
	
						//if this recipient already got the email
						List<UserComment> l = UserCommentsFactory.getUserCommentsByComm(userProxy.getInode(), comm.getInode());
	
						if (InodeUtils.isSet(recipient.getInode()) || l.size() > 0)
						{
							Logger.debug(EmailFactory.class,"Already got this email:" + recipient.getEmail());
							if (!c.isSendEmail()) {
								continue;
							}
						}
	
						recipient.setEmail(member.getEmailAddress());
						recipient.setName(member.getFirstName());
						recipient.setLastname(member.getLastName());
	
						recipient.setSent(new java.util.Date());
						HibernateUtil.saveOrUpdate(recipient);
	
						setRecipient(recipient);
						setCampaign(c);
						setSubscriber(member);
						setAlternateTextMessage(alternateTextMessage);
						setMessage(message);
						setHtml(true);
						c.addChild(recipient);
						
						if (!comm.getCommunicationType().equalsIgnoreCase("alert")) {
							if(!sendEmail()) {
								//If got errors sending the email then we mark it as a bounce
								//from the mailing list
								MailingListFactory.markAsBounceFromMailingList(ml, userProxy);
							}
						}
						else {
							//for alert communications only
							recipient.setLastResult(200);
							recipient.setLastMessage("Alert created");
						}
						HibernateUtil.saveOrUpdate(recipient);
	
						long sleepTime = 10;
						if(UtilMethods.isSet(send_per_hour) && !send_per_hour.equals("unlimited"))
						{
							sleepTime = 3600000 / Integer.parseInt(send_per_hour);
						}
						try
						{
							Thread.sleep(sleepTime);
						}
						catch(Exception t)
						{
							Logger.error(this,t.getMessage(),t);
						}
					} catch (Exception e) {
						Logger.info(DeliverCampaignThread.class, "Failed to sent campaign inode=" + c.getInode() + " to userProxy inode=" + userProxy.getInode() + " e=" + e);
					}
				}
				c.setWasSent(true);
				c.setCompletedDate(new java.util.Date());
				c.setLocked(false);
				message = null;
				HibernateUtil.saveOrUpdate(c);
				Logger.debug(DeliverCampaignThread.class, "Campaign sent: " + c.getTitle());
			}
		} catch (Exception e) { }
		finally {
			try {
			   CampaignFactory.unlockAllCampaigns();
		       HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(DeliverCampaignThread.class, e.getMessage(), e);
			}
			DbConnectionFactory.closeConnection();
		}
	}

	public boolean sendEmail() throws DotDataException, DotSecurityException {
		//add it to the campaign's children
		//campaign.addChild(recipient);


		Campaign campaign = getCampaign();
		Communication comm = (Communication) InodeFactory.getChildOfClass(campaign, Communication.class);
		Mailer Mailer = new Mailer();

		//get subject and message
		String subject = comm.getEmailSubject();

		User tempUser = new User();
		try {
			BeanUtils.copyProperties(tempUser, subscriber);
		} catch (Exception e) {
		}
		tempUser.setFirstName(recipient.getName());
		subject = replaceTextVar(subject, recipient, tempUser, comm);

		Mailer.setRecipientId(recipient.getInode());
		Mailer.setFromName(replaceTextVar(comm.getFromName(), recipient, subscriber, comm));
		Logger.debug(DeliverCampaignThread.class, "comm.getFromName()="+comm.getFromName());
		Mailer.setFromEmail(replaceTextVar(comm.getFromEmail(), recipient, subscriber, comm));
		Logger.debug(DeliverCampaignThread.class, "comm.getFromEmail()="+comm.getFromEmail());
		Mailer.setToEmail(recipient.getEmail());

		if(UtilMethods.isSet(recipient.getLastname()))
			Mailer.setToName(recipient.getName()+" "+recipient.getLastname());
		else
			Mailer.setToName(recipient.getName());

		Mailer.setSubject(subject);

		String finalMessageStr = message.toString();

		Logger.debug(DeliverCampaignThread.class, "sendEmail: message = " + message);

		finalMessageStr = replaceTextVar(finalMessageStr, recipient, subscriber, comm);

		if (html) {
			//personalize
			Mailer.setHTMLBody(finalMessageStr);
			if (alternateTextMessage != null) {
				Mailer.setTextBody(alternateTextMessage);
			}
		}
		else {
			//personalize
			Mailer.setTextBody(finalMessageStr);
		}

		boolean sent = Mailer.sendMessage();
		if (!sent) {

			recipient.setLastResult(500);
			recipient.setLastMessage(Mailer.getErrorMessage());
		}
		else{
			recipient.setLastResult(200);
			recipient.setLastMessage("Mail Sent");

			//add the comment to the user record
			Date today = new Date();
			String commentComment = "Campaign: "+comm.getTitle() + " sent on " + UtilMethods.dateToHTMLDate(today);
			UserComment comment = new UserComment();

			comment.setComment(commentComment);
			comment.setDate(today);
			comment.setMethod(UserComment.METHOD_MARKETING_LIST);
			comment.setTypeComment(UserComment.TYPE_OUTGOING);
			comment.setType("mailing_list");
			comment.setSubject(commentComment);
			comment.setCommentUserId(subscriber.getUserId());
			comment.setUserId(subscriber.getUserId());
			comment.setCommunicationId(comm.getInode());
			UserCommentsFactory.saveUserComment(comment);
		}
		HibernateUtil.commitTransaction();

		return sent;
	}


	/**
	 * Returns the campaign.
	 * @return Campaign
	 */
	public Campaign getCampaign() {
		return campaign;
	}

	/**
	 * Sets the campaign.
	 * @param campaign The campaign to set
	 */
	public void setCampaign(Campaign campaign) {
		this.campaign = campaign;
	}

	/**
	 * Returns the s.
	 * @return Subscriber
	 */
	public User getSubscriber() {
		return subscriber;
	}

	/**
	 * Sets the subscriber.
	 * @param subscriber The subscriber to set
	 */
	public void setSubscriber(User subscriber) {
		this.subscriber = subscriber;
	}

	/**
	 * Returns the html.
	 * @return boolean
	 */
	public boolean isHtml() {
		return html;
	}

	/**
	 * Sets the html.
	 * @param html The html to set
	 */
	public void setHtml(boolean html) {
		this.html = html;
	}

	/**
	 * Returns the message.
	 * @return StringBuffer
	 */
	public StringBuffer getMessage() {
		return message;
	}

	/**
	 * Sets the message.
	 * @param message The message to set
	 */
	public void setMessage(StringBuffer message) {
		this.message = message;
	}


	/**
	 * Returns the recipient.
	 * @return Recipient
	 */
	public Recipient getRecipient() {
		return recipient;
	}



	/**
	 * Sets the recipient.
	 * @param recipient The recipient to set
	 */
	public void setRecipient(Recipient recipient) {
		this.recipient = recipient;
	}

	public void execute(JobExecutionContext context) throws JobExecutionException {
		Logger.debug(DeliverCampaignThread.class, "Running DeliverCampaignThread - " + new Date());
		this.context = context;
		try {
			run();
		} catch (Exception e) {
			Logger.info(DeliverCampaignThread.class, e.toString());
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(DeliverCampaignThread.class, e.getMessage(), e);
			}
		}
	}

	public void _checkUserPermissions(Inode webAsset, User user, int permission) throws ActionException, DotDataException {
		// Checking permissions
		if (!InodeUtils.isSet(webAsset.getInode()))
			return;
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		if (!perAPI.doesUserHavePermission(webAsset, permission, user)) {
			Logger.debug(DeliverCampaignThread.class, "_checkUserPermissions: user does not have permissions ( " + permission + " ) over this asset: " + webAsset);
			throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
	}

	public static String replaceTextVar(String text, Recipient recipient, User subscriber, Communication comm) {
		String finalMessageStr = text;

		Address address = new Address();
		try {
			List<Address> adds = PublicAddressFactory.getAddressesByUserId(subscriber.getUserId());
			if (adds != null && adds.size() > 0) {
				address = (Address) adds.get(0);
			}
		}
		catch(Exception e) {
			Logger.error(DeliverCampaignThread.class, "deliverCampaigns Failed" + e.getMessage());
		}

		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/rId(>|(&gt;))", "");
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))rId(\")?( )*/*( )*(>|(&gt;))", recipient.getInode() + "");
		//Variables replacement from subscriber object
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varName(>|(&gt;))", "");
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varName(\")?( )*/*( )*(>|(&gt;))", (subscriber.getFirstName()!=null) ? subscriber.getFirstName() : "");

		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varEmail(>|(&gt;))", "");
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varEmail(\")?( )*/*( )*(>|(&gt;))", (subscriber.getEmailAddress()!=null) ? subscriber.getEmailAddress() : "");

		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varMiddleName(>|(&gt;))", "");
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varMiddleName(\")?( )*/*( )*(>|(&gt;))", (subscriber.getMiddleName()!=null) ? subscriber.getMiddleName() : "");

		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varLastName(>|(&gt;))", "");
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varLastName(\")?( )*/*( )*(>|(&gt;))", (subscriber.getLastName()!=null) ? subscriber.getLastName() : "");

		UserProxy userproxy;
		try {
			userproxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(subscriber,APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(DeliverCampaignThread.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varLastMessage(>|(&gt;))", "");
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varLastMessage(\")?( )*/*( )*(>|(&gt;))", (userproxy.getLastMessage()!=null) ? userproxy.getLastMessage() : "");

		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varAddress1(>|(&gt;))", "");
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varAddress1(\")?( )*/*( )*(>|(&gt;))", (address.getStreet1()!=null) ? address.getStreet1() : "");

		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varAddress2(>|(&gt;))", "");
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varAddress2(\")?( )*/*( )*(>|(&gt;))", (address.getStreet2()!=null) ? address.getStreet2() : "");

		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varPhone(>|(&gt;))", "");
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varPhone(\")?( )*/*( )*(>|(&gt;))", (address.getPhone()!=null) ? address.getPhone() : "");

		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varState(>|(&gt;))", "");
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varState(\")?( )*/*( )*(>|(&gt;))", (address.getState()!=null) ? address.getState() : "");

		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varCity(>|(&gt;))", "");
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varCity(\")?( )*/*( )*(>|(&gt;))", (address.getCity()!=null) ? address.getCity() : "");

		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varCountry(>|(&gt;))", "");
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varCountry(\")?( )*/*( )*(>|(&gt;))", (address.getCountry()!=null) ? address.getCountry() : "");

		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varZip(>|(&gt;))", "");
		finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varZip(\")?( )*/*( )*(>|(&gt;))", (address.getZip()!=null) ? address.getZip() : "");

		//gets default company to get locale
		Company comp = PublicCompanyFactory.getDefaultCompany();

		try {
			String var1 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var1");
			if (var1!=null) var1 = var1.replaceAll(" ","_");

			String var2 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var2");
			if (var2!=null) var2 = var2.replaceAll(" ","_");

			String var3 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var3");
			if (var3!=null) var3 = var3.replaceAll(" ","_");

			String var4 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var4");
			if (var4!=null) var4 = var4.replaceAll(" ","_");

			String var5 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var5");
			if (var5!=null) var5 = var5.replaceAll(" ","_");

			String var6 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var6");
			if (var6!=null) var6 = var6.replaceAll(" ","_");

			String var7 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var7");
			if (var7!=null) var7 = var7.replaceAll(" ","_");

			String var8 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var8");
			if (var8!=null) var8 = var8.replaceAll(" ","_");

			String var9 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var9");
			if (var9!=null) var9 = var9.replaceAll(" ","_");

			String var10 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var10");
			if (var10!=null) var10 = var10.replaceAll(" ","_");

			String var11 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var11");
			if (var11!=null) var11 = var11.replaceAll(" ","_");

			String var12 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var12");
			if (var12!=null) var12 = var12.replaceAll(" ","_");

			String var13 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var13");
			if (var13!=null) var13 = var13.replaceAll(" ","_");

			String var14 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var14");
			if (var14!=null) var14 = var14.replaceAll(" ","_");

			String var15 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var15");
			if (var15!=null) var15 = var15.replaceAll(" ","_");

			String var16 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var16");
			if (var16!=null) var16 = var16.replaceAll(" ","_");

			String var17 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var17");
			if (var17!=null) var17 = var17.replaceAll(" ","_");

			String var18 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var18");
			if (var18!=null) var18 = var18.replaceAll(" ","_");

			String var19 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var19");
			if (var19!=null) var19 = var19.replaceAll(" ","_");

			String var20 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var20");
			if (var20!=null) var20 = var20.replaceAll(" ","_");

			String var21 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var21");
			if (var21!=null) var21 = var21.replaceAll(" ","_");

			String var22 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var22");
			if (var22!=null) var22 = var22.replaceAll(" ","_");

			String var23 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var23");
			if (var23!=null) var23 = var23.replaceAll(" ","_");

			String var24 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var24");
			if (var24!=null) var24 = var24.replaceAll(" ","_");

			String var25 = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var25");
			if (var25!=null) var25 = var25.replaceAll(" ","_");

			//additional variables
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var1+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var1 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar1()!=null) ? userproxy.getVar1() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var2+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var2 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar2()!=null) ? userproxy.getVar2() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var3+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var3 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar3()!=null) ? userproxy.getVar3() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var4+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var4 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar4()!=null) ? userproxy.getVar4() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var5+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var5 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar5()!=null) ? userproxy.getVar5() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var6+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var6 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar6()!=null) ? userproxy.getVar6() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var7+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var7 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar7()!=null) ? userproxy.getVar7() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var8+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var8 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar8()!=null) ? userproxy.getVar8() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var9+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var9 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar9()!=null) ? userproxy.getVar9() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var10+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var10 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar10()!=null) ? userproxy.getVar10() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var11+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var11 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar11()!=null) ? userproxy.getVar11() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var12+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var12 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar12()!=null) ? userproxy.getVar12() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var13+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var13 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar13()!=null) ? userproxy.getVar13() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var14+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var14 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar14()!=null) ? userproxy.getVar14() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var15+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var15 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar15()!=null) ? userproxy.getVar15() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var16+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var16 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar16()!=null) ? userproxy.getVar16() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var17+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var17 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar17()!=null) ? userproxy.getVar17() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var18+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var18 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar18()!=null) ? userproxy.getVar18() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var19+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var19 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar19()!=null) ? userproxy.getVar19() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var20+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var20 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar20()!=null) ? userproxy.getVar20() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var21+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var21 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar21()!=null) ? userproxy.getVar21() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var22+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var22 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar22()!=null) ? userproxy.getVar22() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var23+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var23 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar23()!=null) ? userproxy.getVar23() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var24+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var24 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar24()!=null) ? userproxy.getVar24() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var25+"(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var25 + "(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar25()!=null) ? userproxy.getVar25() : "");

			//additional variables
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var1(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var1(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar1()!=null) ? userproxy.getVar1() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var2(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var2(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar2()!=null) ? userproxy.getVar2() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var3(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var3(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar3()!=null) ? userproxy.getVar3() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var4(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var4(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar4()!=null) ? userproxy.getVar4() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var5(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var5(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar5()!=null) ? userproxy.getVar5() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var6(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var6(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar6()!=null) ? userproxy.getVar6() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var7(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var7(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar7()!=null) ? userproxy.getVar7() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var8(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var8(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar8()!=null) ? userproxy.getVar8() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var9(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var9(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar9()!=null) ? userproxy.getVar9() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var10(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var10(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar10()!=null) ? userproxy.getVar10() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var11(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var11(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar11()!=null) ? userproxy.getVar11() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var12(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var12(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar12()!=null) ? userproxy.getVar12() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var13(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var13(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar13()!=null) ? userproxy.getVar13() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var14(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var14(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar14()!=null) ? userproxy.getVar14() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var15(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var15(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar15()!=null) ? userproxy.getVar15() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var16(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var16(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar16()!=null) ? userproxy.getVar16() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var17(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var17(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar17()!=null) ? userproxy.getVar17() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var18(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var18(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar18()!=null) ? userproxy.getVar18() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var19(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var19(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar19()!=null) ? userproxy.getVar19() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var20(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var20(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar20()!=null) ? userproxy.getVar20() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var21(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var21(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar21()!=null) ? userproxy.getVar21() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var22(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var22(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar22()!=null) ? userproxy.getVar22() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var23(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var23(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar23()!=null) ? userproxy.getVar23() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var24(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var24(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar24()!=null) ? userproxy.getVar24() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var25(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var25(\")?( )*/*( )*(>|(&gt;))", (userproxy.getVar25()!=null) ? userproxy.getVar25() : "");

			//Replacing the subscriptions link with the subscriptions dotCMS action path
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/subcriptionsLink(>|(&gt;))", "");

			//Case when the subscriptions link is prepended with the host 
			finalMessageStr = finalMessageStr.replaceAll("(?i)(http[s]?://[^/]+)/?(<|(&lt;))subscriptionsLink(\")?( )*/*( )*(>|(&gt;))", "$1/dotCMS/subscribe?dispatch=manageMailingList&ui="+PublicEncryptionFactory.encryptString(subscriber.getUserId()));

			//Case when the subscriptions link is alone then we prepend the default host to it  
			Host host;
			try {
				host = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);
			} catch (DotDataException e) {
				Logger.error(DeliverCampaignThread.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			} catch (DotSecurityException e) {
				Logger.error(DeliverCampaignThread.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
			String hostName = host.getHostname();
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))subscriptionsLink(\")?( )*/*( )*(>|(&gt;))", hostName+"/dotCMS/subscribe?dispatch=manageMailingList&ui="+PublicEncryptionFactory.encryptString(subscriber.getUserId()));
			
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/fromName(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))fromName(\")?( )*/*( )*(>|(&gt;))", (comm.getFromName()!=null) ? comm.getFromName() : "");
			
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/fromEmail(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))fromEmail(\")?( )*/*( )*(>|(&gt;))", (comm.getFromEmail()!=null) ? comm.getFromEmail() : "");

		} catch(LanguageException le) {
			Logger.error(DeliverCampaignThread.class, le.getMessage());
		}

		return finalMessageStr;
	}
}