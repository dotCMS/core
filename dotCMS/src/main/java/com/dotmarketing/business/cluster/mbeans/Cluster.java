package com.dotmarketing.business.cluster.mbeans;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;

public class Cluster implements ClusterMBean {

	public void startCluster() {
		DistributedJournalAPI distJournal=APILocator.getDistributedJournalAPI();
		distJournal.setIndexationEnabled(true);
	}

	public void startCluster(int sleep, int delay) {
		DistributedJournalAPI distJournal=APILocator.getDistributedJournalAPI();
		distJournal.setIndexationEnabled(true);
	}

	public void stopCluster() {
		DistributedJournalAPI distJournal=APILocator.getDistributedJournalAPI();
		distJournal.setIndexationEnabled(false);

	}

}
