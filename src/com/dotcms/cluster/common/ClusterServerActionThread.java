/**
 * 
 */
package com.dotcms.cluster.common;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.enterprise.cluster.action.business.ServerActionAPI;
import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;

/**
 * @author Oscar Arrieta
 *
 */
public class ClusterServerActionThread extends Thread {
	
	private boolean die = false;
	private boolean start = false;
	private int sleep = 2000;
	
	private final ServerAPI serverAPI = APILocator.getServerAPI();
	private final ServerActionAPI serverActionAPI = APILocator.getServerActionAPI();
	
	private static ClusterServerActionThread instance;
	
	public void run() {
		while (!die) {
			if (start) {
				Logger.info(ClusterServerActionThread.class, "ClusterServerActionThread Started with Sleep of "
						+ this.sleep + " millis.");
				start = false;
			}
			
			Connection connection = null;
			
			try {
				//Get my Server ID.
				String myServerID = serverAPI.readServerId();
				
				//Get a list of new ServerActionBean in my server.
				List<ServerActionBean> listServerActionBeans = serverActionAPI.getNewServerActionBeans(myServerID);
				
				if(!listServerActionBeans.isEmpty()){
					connection = DbConnectionFactory.getDataSource().getConnection();
					connection.setAutoCommit(false);
					
					//For each ServerActionBean we need to handle it.
					for (ServerActionBean serverActionBean : listServerActionBeans) {
						serverActionAPI.handleServerAction(serverActionBean);
					}
					connection.commit();
				}
				
			} catch (Exception e) {
				try {
					Logger.error(ClusterServerActionThread.class, 
							"Error trying handle ServerActionBean " + e.getMessage(), e);
					
					if(connection != null && !connection.isClosed()){
						connection.rollback();
					}
					
				} catch (SQLException sqlException) {
					Logger.error(ClusterServerActionThread.class, 
							"Error trying to Rollback DB connection " + sqlException.getMessage(), sqlException);
				}
				
			} finally {
				try {
					if(connection != null && !connection.isClosed()){
						connection.close();
					}
				} catch (SQLException sqlException) {
					Logger.error(ClusterServerActionThread.class, 
							"Error trying to close DB connection " + sqlException.getMessage(), sqlException);
				}
			}
			
			//Sleep for X millis. Configurable
			try {
				Thread.sleep(this.sleep);
			} catch (InterruptedException e) {
				Logger.error(ClusterServerActionThread.class, e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Tells the thread to start processing. Starts the thread
	 */
	public synchronized static void startThread(int sleep) {
		Logger.info(ClusterServerActionThread.class,
				"ClusterServerActionThread ordered to start processing");

		if (instance == null) {
			instance = new ClusterServerActionThread();
			instance.start();
		}
		instance.startProcessing(sleep);
	}
	
	private void startProcessing(int sleep) {
		this.sleep = sleep;
		this.start = true;
	}
	
	/**
	 * This instance is intended to already be started. It will try to restart
	 * the thread if instance is null.
	 */
	public static ClusterServerActionThread getInstance() {
		if (instance == null) {
			createThread();
		}
		return instance;
	}
	
	/**
	 * Creates and starts a thread that doesn't process anything yet
	 */
	public synchronized static void createThread() {
		if (instance == null || !instance.isAlive()) {
			instance = new ClusterServerActionThread();
			instance.start();
		}
	}

}
