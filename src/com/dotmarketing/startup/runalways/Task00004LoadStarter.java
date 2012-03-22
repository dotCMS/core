package com.dotmarketing.startup.runalways;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DotCMSInitDb;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task00004LoadStarter implements StartupTask {

	
		public void executeUpgrade() throws DotDataException, DotRuntimeException {
			
			
		DotCMSInitDb.InitializeDb();
	}

	public boolean forceRun() {

		DotConnect db = new DotConnect();
		db.setSQL("select count(*) as test from inode");

		int test = db.getInt("test");
		return (test < 1);
	}

}
