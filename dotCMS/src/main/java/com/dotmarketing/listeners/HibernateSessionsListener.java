package com.dotmarketing.listeners;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;


public class HibernateSessionsListener implements ServletRequestListener {

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequestListener#requestDestroyed(javax.servlet.ServletRequestEvent)
	 */
	public void requestDestroyed(ServletRequestEvent arg0) {
		try {
			HibernateUtil.closeSession();
		} catch (DotHibernateException e) {
			Logger.debug(this, e.getMessage(),e);
		}
		DbConnectionFactory.closeConnection();

	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequestListener#requestInitialized(javax.servlet.ServletRequestEvent)
	 */
	public void requestInitialized(ServletRequestEvent arg0) {
		
	}

}
