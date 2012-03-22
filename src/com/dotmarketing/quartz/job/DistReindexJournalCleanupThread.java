package com.dotmarketing.quartz.job;


import java.util.Date;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.business.journal.DistributedJournalAPI.DateType;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class DistReindexJournalCleanupThread implements Runnable, Job {
	
    public DistReindexJournalCleanupThread() {
    }


	public void run() {
		int days = Config.getIntProperty("DIST_REINDEX_JOURNAL_CLEANUP_DAYS", 1);
		try 
    	{
			Logger.debug(this, "About to delete dist_reindex_journal entries older than "+ days +" day(s)");
    		HibernateUtil.startTransaction();
    		APILocator.getDistributedJournalAPI().distReindexJournalCleanup(days, false, false, DateType.DAY);
    		HibernateUtil.commitTransaction();
    	}
    	catch (Exception e) 
    	{
    		Logger.error(this, "Error ocurred while trying to delete dist_reindex_journal entries older than "+ days +" day(s)", e);
    		try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.error(this, e1.getMessage(), e1);
			}
    	}
    	finally
    	{
    		try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, e.getMessage(), e);
			}
    		DbConnectionFactory.closeConnection();
    	}
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#destroy()
	 */
	public void destroy() {
	}
	

	public void execute(JobExecutionContext context) throws JobExecutionException {
		Logger.debug(this, "Running DistReindexJournalCleanupThread - " + new Date());

    	try {
			run();
		} catch (Exception e) {
			Logger.info(this, e.toString());
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, e.getMessage(), e);
			}
		}
		
	}

}
