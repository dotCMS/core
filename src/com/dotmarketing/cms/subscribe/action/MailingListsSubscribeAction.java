package com.dotmarketing.cms.subscribe.action;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.subscribe.struts.MailingListsSubscribeForm;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.mailinglists.factories.MailingListFactory;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 *
 * @author David
 * @version $Revision: 1.2 $
 *
 */
public class MailingListsSubscribeAction extends DispatchAction {

    public ActionForward unspecified(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

		return manageMailingList(mapping, lf, request, response);
    }

    /**
     * @param lf ActionForm, specifically MailingListsSubscribeForm action form
     * @return Updates the user mailing list subscriptions
     * @throws Exception
     */
    public ActionForward subscribe(ActionMapping mapping, ActionForm lf, HttpServletRequest request, HttpServletResponse response)
            throws Exception {

    	User u;
		MailingListsSubscribeForm form = (MailingListsSubscribeForm) lf;
		String[] inodes = form.getMailingListsInodes();

		String ui = request.getParameter("ui");
		if (UtilMethods.isSet(ui)) {
			String userId = null;
			try {
				userId = PublicEncryptionFactory.decryptString(ui);
			} catch (Exception e) {
				userId = ui;
				Logger.debug(this, e.toString());
			}

			java.util.Date now = new java.util.Date();
			u = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
			if ((u.getCreateDate() == null) || (u.getCreateDate().after(now))) {
				u = APILocator.getUserAPI().loadByUserByEmail(userId, APILocator.getUserAPI().getSystemUser(), false);
				if ((u.getCreateDate() == null) || (u.getCreateDate().after(now))) {
					ActionForward af = mapping.findForward("subscribePage");
			        return af;
				}
			}
		} else {
	    	ActionForward af = mapping.findForward("subscribePage");
	        return af;
		}

		HibernateUtil.startTransaction();

		if(UtilMethods.isSet(inodes))
			subscribeToMailingLists(u, inodes, false);
		else
			subscribeToMailingLists(u, inodes, true);

		HibernateUtil.commitTransaction();

    	ActionMessages message = new ActionMessages();
		message.add("message", new ActionMessage("message.myaccount.mailinglists.updated"));
		saveMessages(request, message);

		String referrer = null;
		if (request.getParameter("referrer") != null && !request.getParameter("referrer").toString().equalsIgnoreCase("")) {
			referrer = (String)request.getParameter("referrer");
		}

    	ActionForward af = mapping.findForward("subscribePage");
		if(UtilMethods.isSet(referrer) && !referrer.startsWith(af.getPath())) {
			af = new ActionForward(SecurityUtils.stripReferer(request, referrer));
			af.setRedirect(true);
		}

    	if (UtilMethods.isSet(ui)) {
    		request.setAttribute("ui", ui);
    	}

        return af;
    }

    /**
     *
     * This action creates the submit user email as a dotCMS account (if the email doesn't exists) and then subscribes the created/found
     * user account to the given mailing lists
     *
     * @param lf ActionForm, specifically MailingListsSubscribeForm action form
     * @return Updates the user mailing list subscriptions
     * @throws Exception
     */
    public ActionForward createAndSubscribe(ActionMapping mapping, ActionForm lf, HttpServletRequest request, HttpServletResponse response)
            throws Exception {

		MailingListsSubscribeForm form = (MailingListsSubscribeForm) lf;
		String[] inodes = form.getMailingListsInodes();

		User user = null;

		try {
			user = APILocator.getUserAPI().loadByUserByEmail(form.getEmailAddress(), APILocator.getUserAPI().getSystemUser(), false);
		} catch (NoSuchUserException e) {
			Logger.debug(this,"No User Found");
		}

		User defaultUser = APILocator.getUserAPI().getDefaultUser();
		Date today = new Date();

		String referrer = null;
		if (request.getParameter("referrer") != null && !request.getParameter("referrer").toString().equalsIgnoreCase("")) {
			referrer = (String)request.getParameter("referrer");
		}

		ActionErrors errors = form.validateCreateAndSubscribe(mapping, request);

		if(errors.size() > 0) {
			saveMessages(request.getSession(), errors);
	    	ActionForward af = mapping.findForward("subscribePage");
			if(UtilMethods.isSet(referrer) && !referrer.startsWith(af.getPath())) {
				af = new ActionForward(SecurityUtils.stripReferer(request, referrer));
				af.setRedirect(true);
			}
			return af;
		}

		HibernateUtil.startTransaction();
		if(user == null) {
			//### CREATE USER ###
			user = APILocator.getUserAPI().createUser(null, null);
			user.setEmailAddress(form.getEmailAddress());
			user.setFirstName(form.getName() == null ? "" : form.getName());
			user.setLastName(form.getLastName() == null ? "" : form.getLastName());
			user.setPasswordEncrypted(true);
			user.setPassword(PublicEncryptionFactory.getRandomEncryptedPassword());
			user.setComments("");
			user.setGreeting("Welcome, " + user.getFullName() + "!");
			user.setCreateDate(today);
			user.setActive(true);
			APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
		}


		subscribeToMailingLists(user, inodes, form.isUnsubscribeFromAll());
		HibernateUtil.commitTransaction();

    	ActionMessages message = new ActionMessages();
		message.add("message", new ActionMessage("message.myaccount.mailinglists.updated"));
		saveMessages(request.getSession(), message);

		String returnURL = null;
		if (request.getParameter("return") != null && !request.getParameter("return").toString().equalsIgnoreCase("")) {
			returnURL = (String)request.getParameter("return");
		}

    	ActionForward af = mapping.findForward("subscribePage");
		if(UtilMethods.isSet(returnURL) && !returnURL.startsWith(af.getPath())) {
			af = new ActionForward(SecurityUtils.stripReferer(request, returnURL));
			af.setRedirect(true);
		}

        return af;

    }

    private void subscribeToMailingLists(User u, String[] mailingListsInodes, boolean unsubscribeFromAll)throws Exception {

    	MailingList ml;
    	UserProxy s = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(u,APILocator.getUserAPI().getSystemUser(), false);

    	if (InodeUtils.isSet(s.getInode()))  {
			List<MailingList> mailingLists = MailingListFactory.getAllPublicLists();
			Iterator<MailingList> it = mailingLists.iterator();
			while (it.hasNext()) {
				ml = (MailingList) it.next();
				MailingListFactory.deleteUserFromMailingList(ml, s);
			}
			ml = MailingListFactory.getUnsubscribersMailingList();
			MailingListFactory.deleteUserFromMailingList(ml, s);
		} else  {
			HibernateUtil.save(s);
		}

		if(unsubscribeFromAll) {
			List<MailingList> mailingListsByUser = MailingListFactory.getMailingListsByUser(u);
			if(UtilMethods.isSet(mailingListsByUser)) {
				for (int i = 0; i < mailingListsByUser.size(); i++) {
					ml = (MailingList) mailingListsByUser.get(i);
					MailingListFactory.deleteSubscriberFromMailingList(ml, s);
				}
			}
		} else if(mailingListsInodes != null) {
			for (int i = 0; i < mailingListsInodes.length; i++) {
				ml = (MailingList) InodeFactory.getInode(mailingListsInodes[i], MailingList.class);
				MailingListFactory.addMailingSubscriber(ml, s, true);
				HibernateUtil.save(ml);
			}
		}

    }

    /**
     * @param lf ActionForm, specifically MailingListsSubscribeForm action form
     * @return this method get the encrypted user id and passes it through to the mailing list suscription page
     * @throws Exception
     */
	public ActionForward manageMailingList(ActionMapping mapping, ActionForm lf, HttpServletRequest request, HttpServletResponse response)
    throws Exception {
    	String ui = request.getParameter("ui");

    	if (UtilMethods.isSet(ui)) {
    		request.setAttribute("ui", ui);
    	}

        return mapping.findForward("subscribePage");
    }
}