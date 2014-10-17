/**
 * 
 */
package com.dotcms.cluster.common;

/**
 * @author Oscar Arrieta
 *
 */
public class ClusterServerActionThread extends Thread {
	
	private static ClusterServerActionThread instance;
	
	public void run() {
		
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
		if (instance == null) {
			instance = new ClusterServerActionThread();
			instance.start();
		}
	}

}
