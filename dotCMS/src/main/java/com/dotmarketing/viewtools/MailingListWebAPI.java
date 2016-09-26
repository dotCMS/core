package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.mailinglists.factories.MailingListFactory;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * this webapi lets execute mailing list methods called from liferay macros
 * @author Martin
 *
 */
public class MailingListWebAPI implements ViewTool {

	private static HttpServletRequest request;
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

	/**
	 * return a list of the all public mailing list
	 * @return list of the all public mailing list
	 */
	public List<MailingList> getAllPublicLists() {
		List<MailingList> l = MailingListFactory.getAllPublicLists();
		return l;
	}

	/**
	 * return a list of all the mailing list (private and public) where the user is susbcribed
	 * @param u user whom mailing lists are obtained
	 * @return list of all the mailing list (private and public) where the user is susbcribed
	 */
	public List<MailingList> getMailingListsBySubscriber(User u) {
		List<MailingList> l = MailingListFactory.getMailingListsBySubscriber(u);
		return l;
	}

	/**
	 * return a list of all the mailing list (private and public) where the user is susbcribed
	 * @param u user whom mailing lists are obtained
	 * @return list of all the mailing list (private and public) where the user is susbcribed
	 */
	public List<MailingList> getMailingListsBySubscriberEmail(String email) {
		User u = null;
		if(email != null)
			try {
				u = APILocator.getUserAPI().loadByUserByEmail(email, APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e) {
				Logger.info(this, "Unable to get user");
				Logger.debug(this, e.getMessage(), e);
				return new ArrayList<MailingList>();
			}
		if(u != null) 
			return MailingListFactory.getMailingListsBySubscriber(u);
		else
			return new ArrayList<MailingList>();
	}


	/**
	 * get the Unsubscribers Mailing List
	 * @return the Unsubscribers Mailing List
	 */
	public MailingList getUnsubscribersMailingList() {
		MailingList doNotSendList = MailingListFactory.getUnsubscribersMailingList(); 
		return doNotSendList;
	}
}
