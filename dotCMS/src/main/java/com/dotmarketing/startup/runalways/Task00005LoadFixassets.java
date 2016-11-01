package com.dotmarketing.startup.runalways;


import org.quartz.JobExecutionContext;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTasksExecutor;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class Task00005LoadFixassets implements StartupTask {

	
		public void executeUpgrade() throws DotDataException, DotRuntimeException {
			

	    	FixTasksExecutor fixtask=FixTasksExecutor.getInstance();
	        JobExecutionContext arg0=null;
	        fixtask.execute(arg0);
			
	}



	public boolean forceRun() {

		DotConnect dx = new DotConnect();
		Config.getStringProperty("RUN_FIX_INCONSISTENCIES_ON_STARTUP");
		dx.setSQL("select * from fixes_audit");
		String run=Config.getStringProperty("RUN_FIX_INCONSISTENCIES_ON_STARTUP");
		if (run.equals("true")) {
			try {
				dx.getResult();	
			} 
			catch (Exception e) {
				Logger.info(StartupTask.class,"This is a normal error if the startup task CreateFixesAuditTable has not been executed");
				return false;
			}
			return true;
		}

		else
			return false;
	}

}
