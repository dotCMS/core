/**
 *
 */
package com.dotmarketing.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.cluster.common.ClusterServerActionThread;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;

/**
 * This job will register the current date (db date) into db every configured period of time.
 * The idea here is to act as a heart beat of the server for clustering purposes
 *
 */
public class ServerHeartbeatJob implements Job {





    @CloseDBIfOpened
	public void execute(JobExecutionContext ctx) throws JobExecutionException {

	    Logger.debug(this.getClass(), ()-> "Running ServerHeartbeatJob");
	    
	    // Skip execution if shutdown is in progress to prevent component reinitialization
	    if (com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted()) {
	        Logger.info(this.getClass(), "Shutdown in progress - skipping ServerHeartbeatJob execution");
	        return;
	    }
	    
		try{

			LicenseManager.getInstance().takeLicenseFromRepoIfNeeded();
			LicenseUtil.updateLicenseHeartbeat();

			ClusterFactory.rewireClusterIfNeeded();
			ClusterServerActionThread.createThread();


		} catch (Exception e) {

			Logger.error(getClass(), "Could not get ServerUptime", e);
			new DotRuntimeException(e);
		}
		finally {
		    DbConnectionFactory.closeSilently();
		}

	}

}
