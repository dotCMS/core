/**
 * 
 */
package com.dotmarketing.quartz.job;

import java.util.Set;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.BlockDirectiveCache;
import com.dotmarketing.business.BlockDirectiveCacheObject;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Logger;

/**
 * This job will clean up entries in the block cache that are past their ttl date
 * 
 * @author dotCMS Team
 * @since 
 * http://jira.dotmarketing.net/browse/DOTCMS-5769
 */
public class CleanBlockCacheScheduledTask implements Job {
	
	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	
	
	public CleanBlockCacheScheduledTask() {
		Logger.info(this.getClass(), "Init CleanBlockCacheScheduledTask");
	}
	
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		if(LicenseUtil.getLevel() <100){
			return;
		}
		long start = System.currentTimeMillis();
		Logger.info(this.getClass(), "Running CleanBlockCacheScheduledTask");
		BlockDirectiveCache bdc= CacheLocator.getBlockDirectiveCache();
		DotCacheAdministrator dca = CacheLocator.getCacheAdministrator();
		Set<String> keys= dca.getKeys(bdc.getPrimaryGroup());
		long x = 0;
		for(String key : keys){
			BlockDirectiveCacheObject bo = bdc.get(key);
			if(bo != null && bo.getCreated() + (bo.getTtl() * 1000) < System.currentTimeMillis()) {
				bdc.remove(key);
				x++;
			}
		}
		long end = System.currentTimeMillis();
		Logger.info(this.getClass(), "Ending CleanBlockCacheScheduledTask - cleaned " + x + " entries in " + (end-start)/1000 + " seconds");
	}


}
