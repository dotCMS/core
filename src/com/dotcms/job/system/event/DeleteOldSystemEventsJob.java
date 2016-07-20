package com.dotcms.job.system.event;

import java.util.ArrayList;
import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.job.JobDelegate;
import com.dotcms.job.system.event.delegate.DeleteOldSystemEventsDelegate;
import com.dotcms.job.system.event.delegate.bean.JobDelegateDataBean;
import com.dotmarketing.quartz.DotJob;

/**
 * This Job will be in charge of deleting old System Events from the database.
 * The main purpose is to keep the table from storing too many records that
 * might not be relevant at all after a given period of time.
 * <p>
 * The configuration properties for this Job are set via the
 * {@code dotmarketing-config.properties} file:
 * <ul>
 * <li>{@code ENABLE_DELETE_OLD_SYSTEM_EVENTS} (defaults to {@code true}): Set
 * to {@code false} to NOT execute this Job.</li>
 * <li>{@code DELETE_OLD_SYSTEM_EVENTS_CRON_EXPRESSION} (defaults to
 * {@code 0 0 0 1/3 * ? *} ): Set the appropriate cron expression for the
 * execution of this Job. By default, this job will try to delete old events
 * every 3 days, starting the first day of each month.</li>
 * </ul>
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 18, 2016
 *
 */
public class DeleteOldSystemEventsJob extends DotJob {

	private static List<JobDelegate<JobDelegateDataBean>> delegates;

	@Override
	public void run(JobExecutionContext jobContext) throws JobExecutionException {
		final List<JobDelegate<JobDelegateDataBean>> delegateList = this.getDelegates();
		if (delegateList != null && !delegateList.isEmpty()) {
			for (JobDelegate<JobDelegateDataBean> delegate : delegateList) {
				final JobDelegateDataBean dataBean = new JobDelegateDataBean(jobContext, 0L);
				delegate.execute(dataBean);
			}
		}
	}

	/**
	 * Returns the list of delegate classes. These classes will handle all the
	 * business logic that this Quartz Job is triggering.
	 * 
	 * @return The list of {@link JobDelegate} classes.
	 */
	protected List<JobDelegate<JobDelegateDataBean>> getDelegates() {
		if (delegates == null) {
			delegates = new ArrayList<>();
			delegates.add(new DeleteOldSystemEventsDelegate());
		}
		return delegates;
	}

}
