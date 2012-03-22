package com.dotmarketing.portlets.campaigns.factories;


import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.campaigns.model.Campaign;
import com.dotmarketing.portlets.campaigns.model.Click;
import com.dotmarketing.portlets.campaigns.model.Recipient;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 *
 * @author  will
 */
public class RecipientFactory {

	public static Recipient getRecipientByCampaignAndSubscriber(Campaign c, User s) {
       	return (Recipient) InodeFactory.getChildOfClassbyCondition(c, Recipient.class, "email='" + s.getEmailAddress() + "'");
	}

	private static final String SELECT_RECIPIENTS_WITH_LINK = " SELECT parent from tree, click where child = inode and link like ?";

	/*public static final Recipient getRecipient(String id) {
		try {
			return getRecipient(Long.parseLong(id));
		}
		catch (Exception e) {
			return new Recipient();
		}
	}*/

	public static final Recipient getRecipient(String id) {
		HibernateUtil dh = new HibernateUtil(Recipient.class);
		try {
			return (Recipient) dh.load(id);
		} catch (DotHibernateException e) {
			Logger.error(RecipientFactory.class, e.getMessage(),e);
		}
		return null;
	}

	public static final java.util.List getOpenedRecipientsByCampaign(Campaign c) {
		
		
		return InodeFactory.getChildrenClassByCondition(c, Recipient.class, "opened is not null and last_result = '200'");
		
		

	}

	public static final java.util.List getUnopenedRecipientsByCampaign(Campaign c) {
		return InodeFactory.getChildrenClassByCondition(c, Recipient.class, "opened is null and last_result = '200'");
	}

	public static final java.util.List getRecipientsWithErrorsByCampaign(Campaign c) {
		return InodeFactory.getChildrenClassByCondition(c, Recipient.class, "last_result <> '200'");
	}

	
	public static final java.util.List getAllRecipientsByCampaign(Campaign c) {
		
		return InodeFactory.getChildrenClassByOrder(c, Recipient.class, "sent");
		

	}

	public static final java.util.List getWaitingRecipientsByCampaign(Campaign c) {
		
		return InodeFactory.getChildrenClassByCondition(c, Recipient.class, "sent is null");
		

	}

	public static final java.util.List getRecipientsByCampaignAndClick(Campaign q, Click c) {
		com.dotmarketing.common.db.DotConnect dbo = new com.dotmarketing.common.db.DotConnect();
		dbo.setSQL(SELECT_RECIPIENTS_WITH_LINK);
		dbo.addParam(c.getLink());
		java.util.List al = null ;
		try {
		  java.util.Iterator results = dbo.getResults().iterator();
		  StringBuffer nasty = new StringBuffer("0");
		  while (results.hasNext()) {
			java.util.HashMap hm = (java.util.HashMap) results.next();
			nasty.append("," + hm.get("parent"));
		  }

		  HibernateUtil dh = new HibernateUtil(Recipient.class);
//		  dh.setQuery("from inode in class com.dotmarketing.portlets.campaigns.model.Recipient where ? in inode.parents.elements and  inode in (   " + nasty + ")");
		  StringBuilder query = new StringBuilder(512);
		  query.ensureCapacity(128);
		
		  String tableName = null;
		  
			tableName = Recipient.class.newInstance().getType();
			query.append("from inode in class com.dotmarketing.portlets.campaigns.model.Recipient " +
						 "where inode in (select " + tableName + ".inode " +
						 			 "from " + tableName + " in class com.dotmarketing.portlets.campaigns.model.Recipient, " +
						 			 	  "tree in class com.dotmarketing.beans.Tree, " + 
						 			 	  "inode in class com.dotmarketing.beans.Inode " + 
						 			 "where tree.parent = ? and " + 
						 			 	   "tree.child = " + tableName + ".inode and " + 
						 			 	   "inode.inode = " + tableName + ".inode) and " +
						 	   "inode in (" + nasty + ")");
			
			dh.setQuery(query.toString());
			
//			dh.setParam(q.getInode());
			dh.setParam(q.getInode());
			al = dh.list();
		} catch (Exception e) {
			Logger.error(RecipientFactory.class, e.toString());
		}
		
		return al;

	}
	
	public static final List getRecipientsByUserId(String userId) {
		HibernateUtil dh = new HibernateUtil(Recipient.class);
		java.util.List al = null ;
		try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.campaigns.model.Recipient where user_id = ?");
			dh.setParam(userId);
			al = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(RecipientFactory.class, "getRecipientsByUserId failed:" + e,e);
		}	
		
		return al;
	}
	public static final List getRecipientsByUserId(String userId, String condition) {
		HibernateUtil dh = new HibernateUtil(Recipient.class);
		java.util.List al = null ;
		try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.campaigns.model.Recipient where user_id = ? and "+condition);
			dh.setParam(userId);
			al = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(RecipientFactory.class,"getRecipientsByUserId failed:" + e,e);
		}
		
		return al;
	}
}
