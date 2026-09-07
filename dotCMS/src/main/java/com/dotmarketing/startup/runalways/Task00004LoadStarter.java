package com.dotmarketing.startup.runalways;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DotCMSInitDb;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task00004LoadStarter implements StartupTask {

	@Override
	@WrapInTransaction
	public void executeUpgrade() throws DotDataException, DotRuntimeException {

		DotCMSInitDb.InitializeDb();
	}

	@Override
	public boolean forceRun() {

		final DotConnect db = new DotConnect();
		db.setSQL(DotCMSInitDb.INODE_EXISTS_SQL);

		return db.getInt("test") < 1;
	}

}
