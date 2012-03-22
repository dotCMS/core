package com.dotmarketing.portlets.workflowmessages.factories;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.workflowmessages.model.Action;
import com.dotmarketing.util.Logger;



/**
 *
 * @author Rocco
 */
public class ActionFactory {
    
	// public static final long ACTION_PUBLISH = 5; 
	
	public static Action getActionById(long actionId) {
		HibernateUtil dh = new HibernateUtil(Action.class);
		Action myAction = null;
		try {
			dh.setQuery(
				"from action in class com.dotmarketing.portlets.workflowmessages.model.Action where id = ?");
			dh.setParam(actionId);
			myAction = (Action) dh.load();
		} catch (DotHibernateException e) {
			Logger.error(ActionFactory.class,e.getMessage(),e);
		}
		return myAction;
	}

	public static java.util.List getActionsByAntiStatusId(long statusId) {
		HibernateUtil dh = new HibernateUtil(Action.class);
		List actionList =null;
		try {
			dh.setQuery(
				"from action in class com.dotmarketing.portlets.workflowmessages.model.Action where anti_status_id = ?");
			dh.setParam(statusId);
			actionList = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(ActionFactory.class,e.getMessage(),e);
		}
		return actionList;
	}

}
