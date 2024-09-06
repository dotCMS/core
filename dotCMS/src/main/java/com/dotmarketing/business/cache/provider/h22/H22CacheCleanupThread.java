package com.dotmarketing.business.cache.provider.h22;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TrashUtils;

public class H22CacheCleanupThread extends Thread {
	final String dbRoot;
	final int db;
	final String databaseName;
	final long sleep;

	@Override
	public void run() {

		File dbFolder = new File(dbRoot + File.separator + db);
		File instanceFolder = new File(dbRoot + File.separator + db + File.separator + databaseName);
		long lastMod = instanceFolder.lastModified();

		try {
			Thread.sleep(sleep);
		} catch (InterruptedException e1) {
			Logger.warn(this.getClass(), "failed to cleanup:" + dbFolder);
		}

		File trashFolder = new File(dbRoot).getParentFile();
		trashFolder = new File(trashFolder.getAbsolutePath() + File.separator + "trash" + File.separator + "h22" + File.separator
				+ System.currentTimeMillis());
		trashFolder.mkdirs();

		File[] files = dbFolder.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);

		for (File folder : files) {
			if (folder.lastModified() <= lastMod) {
				try {
					if(dbFolder.getCanonicalPath().contains("/tmp/")){
						new TrashUtils("/tmp/trash").moveFileToTrash(folder, "h22");
					}
					else{
						new TrashUtils().moveFileToTrash(folder, "h22");
					}
				} catch (IOException e) {
					//Prevent Flaky tests from failing
					Logger.warn(this.getClass(), "unable to delete folder:" + folder, e);
				}
			}
		}
	}

	public H22CacheCleanupThread(String dbRoot, int db, String databaseName, long sleep) {
		super();
		this.dbRoot = dbRoot;
		this.db = db;
		this.databaseName = databaseName;
		this.sleep = sleep;
	}

}
