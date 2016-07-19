package com.dotcms.job.system.event;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.web.websocket.JobDelegate;
import com.dotcms.web.websocket.delegate.SystemEventsJobDelegate;
import com.dotcms.web.websocket.delegate.bean.JobDelegateDataBean;
import com.dotmarketing.quartz.DotJob;
import com.dotmarketing.util.Logger;

/**
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
public class SystemEventsJob extends DotJob {

	private static volatile long lastCallback;
	private List<JobDelegate> delegates;

	@Override
	public void run(JobExecutionContext jobContext) throws JobExecutionException {
		Logger.info(this, "=======================================================================");
		Logger.info(this, "==== RUNNING THE SystemEventsJob at " + new Date() + " ====");
		Logger.info(this, "=======================================================================");
		
		List<JobDelegate> delegateList = this.getDelegates();
		if (delegateList != null && !delegateList.isEmpty()) {
			if (lastCallback > 0) {
				for (JobDelegate delegate : delegateList) {
					JobDelegateDataBean dataBean = new JobDelegateDataBean(jobContext, lastCallback);
					delegate.execute(dataBean);
				}
			}
			lastCallback = new Date().getTime();
		}
	}

	/**
	 * 
	 * @return
	 */
	private List<JobDelegate> getDelegates() {
		if (this.delegates == null) {
			this.delegates = new ArrayList<>();
			this.delegates.add(new SystemEventsJobDelegate());
		}
		return this.delegates;
	}

}
