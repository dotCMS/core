package com.dotmarketing.quartz.job;


import java.util.Date;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.business.journal.DistributedJournalAPI.DateType;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class DistReindexJournalCleanupThread implements Runnable, Job, StatefulJob {
	
    public DistReindexJournalCleanupThread() {
    }


	public void run() {
	    try {
            if(ESReindexationProcessStatus.inFullReindexation()) return;
        } catch (DotDataException e2) {
            Logger.warn(this, "can't determine if we're in full reindex",e2);
        }
	    
		int days = Config.getIntProperty("DIST_REINDEX_JOURNAL_CLEANUP_DAYS", 1);
		try 
    	{
			Logger.debug(this, "About to delete dist_reindex_journal entries older than "+ days +" day(s)");
    		HibernateUtil.startTransaction();
    		APILocator.getDistributedJournalAPI().distReindexJournalCleanup(days, false, false, DateType.DAY);
    		HibernateUtil.closeAndCommitTransaction();
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
			finally {
			    DbConnectionFactory.closeConnection();
			}
		}
		
	}

}
