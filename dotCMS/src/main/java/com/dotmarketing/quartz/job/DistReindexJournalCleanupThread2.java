package com.dotmarketing.quartz.job;

import java.util.Date;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.business.journal.DistributedJournalAPI.DateType;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class DistReindexJournalCleanupThread2 implements Runnable, Job {
	
    public DistReindexJournalCleanupThread2() {
    }


	public void run() {
	    
	    try {
            if(ESReindexationProcessStatus.inFullReindexation()) return;
        } catch (DotDataException e2) {
            Logger.warn(this, "can't determine if we're in full reindex",e2);
        }
	    
	    
		int minutes = Config.getIntProperty("DIST_REINDEX_JOURNAL_CLEANUP_MINUTES", 30);
		try 
    	{
			Logger.debug(this, "About to delete dist_reindex_journal entries older than "+ minutes +" minute(s)");
    		HibernateUtil.startTransaction();
    		APILocator.getDistributedJournalAPI().distReindexJournalCleanup(minutes, false, true, DateType.MINUTE);
    		HibernateUtil.closeAndCommitTransaction();
    	}
    	catch (Exception e) 
    	{
    		Logger.error(this, "Error ocurred while trying to delete dist_reindex_journal entries older than "+ minutes +" minute(s)", e);
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
		Logger.debug(this, "Running DistReindexJournalCleanupThread2 - " + new Date());

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
