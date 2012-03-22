package com.dotmarketing.quartz.job;

import java.util.Date;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.util.Logger;

public class ContentReindexerThread implements Runnable, Job {

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	
    public ContentReindexerThread() {
    }

    public void run() 
    {
    	
        Logger.info(this, "Trying to reindex all the content");
        try{
        	conAPI.reindex();
            Logger.info(this, "Reindexation is in progress");
        }catch (Exception e) {
        	Logger.info(this, "Unable to reindex. Probably because another reindexation is in progress");
		}finally{
    		try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(ContentReindexerThread.class, e.getMessage(), e);
			}
    		DbConnectionFactory.closeConnection();
    	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#destroy()
     */
    public void destroy() 
    {
        
    }
    
    public void execute(JobExecutionContext context) throws JobExecutionException {
    	Logger.debug(this, "Running ContentReindexerThread - " + new Date());

    	try {
			run();
		} catch (Exception e) {
			Logger.info(this, e.toString());
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(ContentReindexerThread.class, e.getMessage(), e);
			}
		}
	}
}