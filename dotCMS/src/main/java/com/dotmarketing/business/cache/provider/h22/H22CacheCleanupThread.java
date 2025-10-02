package com.dotmarketing.business.cache.provider.h22;

import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import java.io.File;

public class H22CacheCleanupThread extends Thread {
	final String dbRoot;
	final int db;
	final String databaseName;
	final long sleep;

	@Override
	public void run() {

		File dbFolder = new File(dbRoot + File.separator + db);
		File instanceFolder = new File(dbRoot + File.separator + db + File.separator + databaseName);


		try {
			Thread.sleep(sleep);
		} catch (InterruptedException e1) {
			Logger.warn(this.getClass(), "failed to cleanup:" + dbFolder);
		}

        FileUtil.deltree(instanceFolder);
	}

	public H22CacheCleanupThread(String dbRoot, int db, String databaseName, long sleep) {
		super();
		this.dbRoot = dbRoot;
		this.db = db;
		this.databaseName = databaseName;
		this.sleep = sleep;
	}

}
