package com.dotmarketing.business.cluster.mbeans;

public interface ClusterMBean {
	public abstract void startCluster();
	public abstract void startCluster(int sleep,int delay);
	public abstract void stopCluster();

}
