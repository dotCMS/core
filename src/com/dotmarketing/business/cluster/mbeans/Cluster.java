package com.dotmarketing.business.cluster.mbeans;

import com.dotcms.enterprise.ClusterThreadProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.util.Config;

public class Cluster implements ClusterMBean {

	public void startCluster() {
		ClusterThreadProxy.startThread(Config.getIntProperty("DIST_INDEXATION_SLEEP",5000),Config.getIntProperty("DIST_INDEXATION_INIT_DELAY",1000));
		DistributedJournalAPI distJournal=APILocator.getDistributedJournalAPI();
		distJournal.setIndexationEnabled(true);
	}

	public void startCluster(int sleep, int delay) {
		ClusterThreadProxy.startThread(sleep,delay);
		DistributedJournalAPI distJournal=APILocator.getDistributedJournalAPI();
		distJournal.setIndexationEnabled(true);
	}

	public void stopCluster() {
		ClusterThreadProxy.stopThread();
		DistributedJournalAPI distJournal=APILocator.getDistributedJournalAPI();
		distJournal.setIndexationEnabled(false);

	}

}
