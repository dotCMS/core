package com.dotmarketing.quartz.job;

import com.dotcms.business.WrapInTransaction;
import java.util.Date;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

public class DistributionJournalCleanup implements Runnable, Job {

    public DistributionJournalCleanup() {
    }

	@WrapInTransaction
    public void run() 
    {
    	try 
    	{
    		APILocator.getDistributedJournalAPI().processJournalEntries();
    	}
    	catch (Exception e) 
    	{
    		Logger.error(this, "Error ocurred while trying to process journal entries.", e);
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
    	Logger.debug(this, "Running DistributionJournalCleanup - " + new Date());

    	try {
			run();
		} catch (Exception e) {
			Logger.info(this, e.toString());
		}
	}
}
