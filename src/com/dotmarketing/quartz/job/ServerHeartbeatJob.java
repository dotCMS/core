/**
 *
 */
package com.dotmarketing.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.cluster.common.ClusterServerActionThread;
import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;

/**
 * This job will register the current date (db date) into db every configured period of time.
 * The idea here is to act as a heart beat of the server for clustering purposes
 *
 */
public class ServerHeartbeatJob implements Job {

	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		try {
			APILocator.getServerAPI().updateHeartbeat();
			LicenseUtil.updateLicenseHeartbeat();

		} catch (DotDataException e) {
			Logger.error(getClass(), "Could not get ServerUptime", e);
		}
		
		finally {
		    try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.warn(this, e.getMessage(), e);
            }
		    finally {
		        DbConnectionFactory.closeConnection();
		    }
		}
		
		ClusterServerActionThread.createThread();
	}

}
