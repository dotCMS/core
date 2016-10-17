package com.dotmarketing.portlets.userclicks.factories;


import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;


/**
 *
 * @author  Rocco
 */
public class UserClickFactory {

	private static final String GET_TOP_USER_CLICKSTREAMS = "SELECT {clickstream.*} from clickstream where user_id = ? order by clickstream_id desc";
	private static final String COUNT_USER_CLICKS = "SELECT count(*) as test from clickstream where user_id = ?";

    public static java.util.List getTopUserClicks(String UserId){
    	HibernateUtil dh = new HibernateUtil(Clickstream.class);
    	List topClicks =null;
    	dh.setMaxResults(5);
    	try {
			dh.setSQLQuery(GET_TOP_USER_CLICKSTREAMS);
			dh.setParam(UserId);
			topClicks = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(UserClickFactory.class, "getTopUserClicks failed:" + e, e);
		}
    	return 	topClicks;
    }
    
    public static java.util.List getUserClicks(String UserId, int offset, int limit){
    	HibernateUtil dh = new HibernateUtil(Clickstream.class);
    	List userClicks=null;
    	try {
			dh.setQuery("from inode in class " + Clickstream.class.getName() + " where user_id = ? order by clickstream_id desc");
			dh.setParam(UserId);
			dh.setFirstResult(offset);
			dh.setMaxResults(limit);
			userClicks = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(UserClickFactory.class, "getUserClicks failed:" + e, e);
		} 
    	return userClicks;    	
  
    }
    
    public static java.util.List getAllUserClicks(String UserId){
    	HibernateUtil dh = new HibernateUtil(Clickstream.class);
    	List allUserClicks =null;
    	try {
			dh.setQuery("from inode in class " + Clickstream.class.getName() + " where user_id = ? order by clickstream_id desc");
			dh.setParam(UserId);
			allUserClicks = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(UserClickFactory.class, "getAllUserClicks failed:" + e, e);
		}
    	return allUserClicks; 	
  
    }
    
    public static int countUserClicks(String UserId){
    	DotConnect db = new DotConnect();
    	db.setSQL(COUNT_USER_CLICKS);
    	db.addParam(UserId);
    	return db.getInt("test");
	
    }
    

	
}
