package com.dotmarketing.quartz.job;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * This job will clean up the DB tables(cluster_table, cluster_server_uptime) after a server reach
 * REMOVE_INACTIVE_CLUSTER_SERVER_PERIOD.
 *
 * @author Oswaldo Gallango.
 */
public class DeleteInactiveClusterServersJob implements StatefulJob {
	private final String DEFAULT_TIME="2W";

	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
			int amount;
			String unit;
			String amountUnit = Config.getStringProperty("REMOVE_INACTIVE_CLUSTER_SERVER_PERIOD",DEFAULT_TIME);
			if(UtilMethods.isSet(amountUnit)){
				try {
					amount= Integer.parseInt(amountUnit.substring(0,amountUnit.length()-1));
					unit= amountUnit.substring(amountUnit.length()-1).toUpperCase();
					if(!validUnit(unit)){
						Logger.error(DeleteInactiveClusterServersJob.class, "The REMOVE_INACTIVE_CLUSTER_SERVER_PERIOD variable is not set properly. Default value will be used.");
						amount=2;
						unit="W";
					}
				} catch(Exception e) {
					Logger.error(DeleteInactiveClusterServersJob.class, "The REMOVE_INACTIVE_CLUSTER_SERVER_PERIOD variable is not set properly. Error: "+e.getMessage()+". Default value will be used.", e);
					amount=2;
					unit="W";
				}
			} else {
				Logger.error(DeleteInactiveClusterServersJob.class, "The REMOVE_INACTIVE_CLUSTER_SERVER_PERIOD variable is not set. Default value will be used.");
				amount=2;
				unit="W";
			}

            long maxAmountOfTime;

			if(unit.equals("M")){
				maxAmountOfTime = amount * 1000 * 60;
			}else if(unit.equals("H")){
				maxAmountOfTime = amount * 1000 * 60 * 60;
			}else if(unit.equals("D")){
				maxAmountOfTime = amount * 1000 * 60 * 60 * 24;
			}else{
				//weeks to millis seconds
				maxAmountOfTime = amount * 1000 * 60 * 60 * 24 * 7;
			}
			
			List<Server> inactiveServers = APILocator.getServerAPI().getInactiveServers();
			Date currentDate = new Date();
			for(Server server : inactiveServers){
				Date lastBeat = server.getLastHeartBeat();
				if(UtilMethods.isSet(lastBeat)){
					long timeOff = currentDate.getTime() - lastBeat.getTime();
					if(timeOff >= maxAmountOfTime){
						APILocator.getServerAPI().removeServerFromClusterTable(server.getServerId());
					}
				}
			}
		} catch (DotDataException e) {
			Logger.error(DeleteInactiveClusterServersJob.class, "Could not remove inactive cluster servers", e);
		} catch (IOException e) {
			Logger.error(DeleteInactiveClusterServersJob.class, "Could not remove inactive cluster servers", e);
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

	}
	/**
	 * Validate if the Unit pass is valid. the valid Values are: M (for Minutes),H (for Hours), 
     * D (for Days) or W (for Weeks)
	 * @param unit Uppercase Initial of time unit
	 * @return boolean
	 */
	private boolean validUnit(String unit){
		if(unit.equals("M") || unit.equals("H") || unit.equals("D") || unit.equals("W")){
			return true;
		}else{
			return false;
		}
	}
}
