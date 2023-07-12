package com.dotmarketing.quartz.job;

import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.util.Logger;

public class ShutdownHookThread extends Thread {


	public void run() {
        Logger.info(this, "Running dotCMS shutdown cleanup sequence.");
        try {
        	ReindexThread.stopThread();
        	APILocator.getServerAPI().removeServerFromClusterTable(APILocator.getServerAPI().readServerId());
        	LicenseManager.getInstance().freeLicenseOnRepo();
            //Sleeping a second to wait for any index write not flushed yet
            Thread.sleep(1000);
        } catch (Exception e) {
            Logger.error(this, "A error ocurred trying to close the lucene writer, maybe be the lucene index would be corrupted at the next startup.");
        }
	}


}
