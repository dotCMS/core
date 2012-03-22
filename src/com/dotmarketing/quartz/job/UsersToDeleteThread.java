package com.dotmarketing.quartz.job;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.beans.UsersToDelete;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

public class UsersToDeleteThread extends Thread implements Job {

	public UsersToDeleteThread() {
	}
	
	public UsersToDeleteThread(String name) {
		super(name);
	}

	@SuppressWarnings("unchecked")
	public void run() { 
		Logger.debug(this, "Running UsersToDeleteThread");
		HibernateUtil dh;
		
		try {
			dh = new HibernateUtil(UsersToDelete.class);
			
			StringBuilder query = new StringBuilder("from users_to_delete in class com.dotmarketing.beans.UsersToDelete");
			dh.setQuery(query.toString());
			List<UsersToDelete> usersToDelete = dh.list();
			
			if (usersToDelete != null) {
				Iterator<UsersToDelete> iterUsersToDelete = usersToDelete.iterator();
				UsersToDelete userToDelete;
				User user;
				Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
				UserAPI uAPI = APILocator.getUserAPI();
				User sysUser = uAPI.getSystemUser();
				
				for (; iterUsersToDelete.hasNext();) {
					try {
						userToDelete = iterUsersToDelete.next();
						if (comp.getAuthType().equals(Company.AUTH_TYPE_EA)) {
			            	user = uAPI.loadByUserByEmail(userToDelete.getUserId(), sysUser, false);
			            } else {
			            	user = uAPI.loadUserById(userToDelete.getUserId(), sysUser, false);
			            }
						
						user.setActive(false);
						uAPI.save(user, sysUser, false);
				        
				        HibernateUtil.delete(userToDelete);
					} catch (Exception e) {
						Logger.error(this, e.getMessage(), e);
					}
				}
			}
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}
		
		try {
			HibernateUtil.closeSession();
		} catch (DotHibernateException e) {
			Logger.error(this, e.getMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#destroy()
	 */
	public void destroy() {
		try {
			HibernateUtil.closeSession();
		} catch (DotHibernateException e) {
			Logger.error(this, e.getMessage(), e);
		}
	}
	
	public void execute(JobExecutionContext context) throws JobExecutionException {
		Logger.debug(this, "Running UsersToDeleteThread - " + new Date());
		
		run();
	
		try {
			HibernateUtil.closeSession();
		} catch (DotHibernateException e) {
			Logger.error(this, e.getMessage(), e);
		}
	}
}