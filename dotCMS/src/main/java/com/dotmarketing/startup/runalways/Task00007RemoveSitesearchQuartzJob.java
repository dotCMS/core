package com.dotmarketing.startup.runalways;

import org.quartz.SchedulerException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

public class Task00007RemoveSitesearchQuartzJob implements StartupTask {
	
	private final String siteSearchTriggerCount = "SELECT COUNT(*) AS trigger_count FROM qrtz_excl_simple_triggers WHERE trigger_name='site-search-execute-once-trigger'";

	private final String deleteExclSimpleTrigger = "DELETE FROM qrtz_excl_simple_triggers WHERE trigger_name = 'site-search-execute-once-trigger'";
	
	private final String deleteExclTrigger = "DELETE FROM qrtz_excl_trigger WHERE trigger_name = 'site-search-execute-once-trigger'";
	
	private final String deleteJobDetails = "DELETE FROM qrtz_excl_job_details WHERE job_name = 'site-search-execute-once'";
	

	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		
		try {
			if (QuartzUtils.isJobSequentiallyScheduled("site-search-execute-once", "site-search-execute-once-group")) {
				QuartzUtils.removeJob("site-search-execute-once", "site-search-execute-once-group");	
			}
		} catch (SchedulerException e) {
			Logger.error(this, e.getMessage(),e);
		}
		DotConnect dc = new DotConnect();
		dc.setSQL(siteSearchTriggerCount);
		int trigger_count = dc.getInt("trigger_count");
		if(trigger_count > 0){
			dc.setSQL(deleteExclSimpleTrigger);
			dc.loadResult();
			dc.setSQL(deleteExclTrigger);
			dc.loadResult();
			dc.setSQL(deleteJobDetails);
			dc.loadResult();
		}
		
	}

	public boolean forceRun() {
		DotConnect dc = new DotConnect();
		dc.setSQL(siteSearchTriggerCount);
		int trigger_count = dc.getInt("trigger_count");
		return (trigger_count > 0);
	}

}
