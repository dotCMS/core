/**
 *
 */
package com.dotmarketing.quartz.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

/**
 * This job will register the current date (db date) into db every configured period of time.
 * The idea here is to act as a heart beat of the server for clustering purposes
 *
 */
public class ServerHeartbeatJob implements StatefulJob {

	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		try {
			APILocator.getServerAPI().updateHeartbeat();

		} catch (DotDataException e) {
			Logger.error(getClass(), "Could not get ServerUptime", e);
		}
	}

}
