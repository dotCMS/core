package com.dotmarketing.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.campaigns.factories.RecipientFactory;
import com.dotmarketing.portlets.campaigns.model.Campaign;
import com.dotmarketing.portlets.campaigns.model.Recipient;
import com.dotmarketing.portlets.communications.model.Communication;
import com.dotmarketing.quartz.job.DeliverCampaignThread;
import com.liferay.portal.model.User;

public class AlertMessagesWebAPI implements ViewTool {

	private HttpServletRequest request;
	Context ctx;

	/**
	 * @param  obj  the ViewContext that is automatically passed on view tool initialization, either in the request or the application
	 * @return      
	 * @see         ViewTool, ViewContext
	 */
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		ctx = context.getVelocityContext();
	}

	public boolean hasAlertMessages (String userId) {
		if (this.getAlertMessagesCount(userId) > 0) {
			return true;
		}
		return false;
	}

	public List getAlertMessages (String userId) {
		Date now = new Date();
		if (UtilMethods.isSet(request)) {
			request.getSession().setAttribute(WebKeys.PENDING_ALERT_SEEN, "true");
		}
		String condition = " opened is null and last_message = 'Alert created'";
		String pendingAlertExpDay = Config.getStringProperty("PENDING_ALERTS_EXPIRATION_DAYS");
		if (UtilMethods.isSet(pendingAlertExpDay) && !pendingAlertExpDay.equalsIgnoreCase("*")) {
			condition += " and sent >= (CURRENT_DATE - integer '" + pendingAlertExpDay + "')";
		}

		List<Recipient> recipients = RecipientFactory.getRecipientsByUserId(userId, condition);
		HashMap<String, Object> campaignRecipient;
		List<HashMap<String, Object>> campaigns = new ArrayList<HashMap<String, Object>>();
		
		List communications = new ArrayList();

		Iterator itRecipient = recipients.iterator();
		Recipient r;
		while(itRecipient.hasNext()){
			r = (Recipient) itRecipient.next();
			Campaign campaign = (Campaign) InodeFactory.getParentOfClass(r, Campaign.class);
			if (InodeUtils.isSet(campaign.getInode())) {
				if (UtilMethods.isSet(campaign.getExpirationDate()) && now.after(campaign.getExpirationDate())) {
					break;
				}
				campaignRecipient = new HashMap<String, Object>();
				campaignRecipient.put("campaign", campaign);
				campaignRecipient.put("recipient", r);
				campaigns.add(campaignRecipient);
			}
		}
		
		User user = null;
		try {
			user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e1) {
			Logger.error(AlertMessagesWebAPI.class,e1.getMessage(), e1);
		} 
		
		Iterator itCampaign = campaigns.iterator();
		Campaign c;
		while(itCampaign.hasNext()) {
			campaignRecipient = (HashMap<String, Object>) itCampaign.next();
			c = (Campaign) campaignRecipient.get("campaign");
			r = (Recipient) campaignRecipient.get("recipient");
			Communication communication = (Communication) InodeFactory.getChildOfClass(c, Communication.class);
			if (InodeUtils.isSet(communication.getInode()) && (communication.getCommunicationType().equalsIgnoreCase("alert"))) {
				String[] comm = new String[2];
				if(UtilMethods.isSet(user)){
					comm[0] = DeliverCampaignThread.replaceTextVar(communication.getTitle(), r, user, communication);
					comm[1] = DeliverCampaignThread.replaceTextVar(communication.getTextMessage(), r, user, communication);
				}
				communications.add(comm);
			}
		}
		return communications;
	}
	
	public int getAlertMessagesCount (String userId) {
		Date now = new Date();
		int alertMessagesCount = 0;
		if (UtilMethods.isSet(request)) {
			request.getSession().setAttribute(WebKeys.PENDING_ALERT_SEEN, "true");
		}
		String condition = " opened is null and last_message = 'Alert created'";
		String pendingAlertExpDay = Config.getStringProperty("PENDING_ALERTS_EXPIRATION_DAYS");
		if (UtilMethods.isSet(pendingAlertExpDay) && !pendingAlertExpDay.equalsIgnoreCase("*")) {
			condition += " and sent >= (CURRENT_DATE - integer '" + pendingAlertExpDay + "')";
		}

		List<Recipient> recipients = RecipientFactory.getRecipientsByUserId(userId, condition);
		List<Campaign> campaigns = new ArrayList<Campaign>();
		List communications = new ArrayList();

		Iterator itRecipient = recipients.iterator();
		while(itRecipient.hasNext()){
			Recipient r = (Recipient) itRecipient.next();
			Campaign campaign = (Campaign) InodeFactory.getParentOfClass(r, Campaign.class);
			if (InodeUtils.isSet(campaign.getInode())) {
				if (UtilMethods.isSet(campaign.getExpirationDate()) && now.after(campaign.getExpirationDate())) {
					break;
				}
				campaigns.add(campaign);
			}
		}
		
		Iterator itCampaign = campaigns.iterator();
		while(itCampaign.hasNext()) {
			Campaign c = (Campaign) itCampaign.next();
			Communication communication = (Communication) InodeFactory.getChildOfClass(c, Communication.class);
			if (InodeUtils.isSet(communication.getInode()) && (communication.getCommunicationType().equalsIgnoreCase("alert"))) {
				alertMessagesCount++;
			}
		}
		return alertMessagesCount;
	}

	public void readAlertMessages (String userId) throws DotHibernateException {
		String condition = " opened is null and last_message = 'Alert created'";
		String pendingAlertExpDay = Config.getStringProperty("PENDING_ALERTS_EXPIRATION_DAYS");
		if (UtilMethods.isSet(pendingAlertExpDay) && !pendingAlertExpDay.equalsIgnoreCase("*")) {
			condition += " and sent >= (CURRENT_DATE - integer '" + pendingAlertExpDay + "')";
		}

		List<Recipient> recipients = RecipientFactory.getRecipientsByUserId(userId, condition);
		Iterator itRecipient = recipients.iterator();
		Date now = new Date();
		while(itRecipient.hasNext()){
			Recipient r = (Recipient) itRecipient.next();

			Campaign campaign = (Campaign) InodeFactory.getParentOfClass(r, Campaign.class);
			if (InodeUtils.isSet(campaign.getInode())) {
				if (UtilMethods.isSet(campaign.getExpirationDate()) && now.after(campaign.getExpirationDate())) {
					break;
				}
				Communication communication = (Communication) InodeFactory.getChildOfClass(campaign, Communication.class);
				if (InodeUtils.isSet(communication.getInode()) && (communication.getCommunicationType().equalsIgnoreCase("alert"))) {
					r.setOpened(now);
					r.setLastMessage("Alert Displayed");
					
					HibernateUtil.saveOrUpdate(r);
				}
			}
		}
		if (UtilMethods.isSet(request)) {
			request.getSession().setAttribute(WebKeys.PENDING_ALERT_SEEN, "true");
		}
	}
	
	public List viewReadMessages (String userId) {
		String condition = " opened is not null";
		String seenAlertExpDay = Config.getStringProperty("SEEN_ALERTS_EXPIRATION_DAYS");
		if (UtilMethods.isSet(seenAlertExpDay) && !seenAlertExpDay.equalsIgnoreCase("*")) {
			condition += " sent >= (CURRENT_DATE - integer '" + seenAlertExpDay + "')";
		}

		List<Recipient> recipients = RecipientFactory.getRecipientsByUserId(userId, condition);
		List<Campaign> campaigns = new ArrayList<Campaign>();
		List communications = new ArrayList();

		Iterator itRecipient = recipients.iterator();
		
		User user = null;
		try {
			user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e1) {
			Logger.error(AlertMessagesWebAPI.class,e1.getMessage(), e1);
		}
		
		while(itRecipient.hasNext()){
			Recipient r = (Recipient) itRecipient.next();
			Campaign campaign = (Campaign) InodeFactory.getParentOfClass(r, Campaign.class);
			if (InodeUtils.isSet(campaign.getInode())) {
				Communication communication = (Communication) InodeFactory.getChildOfClass(campaign, Communication.class);
				if (InodeUtils.isSet(communication.getInode()) && (communication.getCommunicationType().equalsIgnoreCase("alert"))) {
					String[] comm = new String[3];
					if(UtilMethods.isSet(user)){
						comm[0] = DeliverCampaignThread.replaceTextVar(communication.getTitle(), r, user, communication);
						comm[1] = DeliverCampaignThread.replaceTextVar(communication.getTextMessage(), r, user, communication);
					}
					comm[2] = UtilMethods.dateToPrettyHTMLDate2(r.getOpened());
					communications.add(comm);
				}
			}
		}

		return communications;
	}
}
