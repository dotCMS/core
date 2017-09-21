package com.dotcms.job.system.event;

import com.dotcms.job.system.event.delegate.SystemEventsJobDelegate;
import com.dotcms.job.system.event.delegate.bean.JobDelegateDataBean;
import com.dotcms.util.Delegate;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This Job is in charge of triggering the verification of new System Events
 * coming into the internal message queue. A list of {@link Delegate} classes
 * can be registered to this Job to let other services or pieces of the
 * application know about a specific event and react to it.
 * <p>
 * The configuration properties for this Job are set via the
 * {@code dotmarketing-config.properties} file:
 * <ul>
 * <li>{@code ENABLE_SYSTEM_EVENTS} (defaults to {@code true}): Set to
 * {@code false} to NOT execute this Job.</li>
 * <li>{@code SYSTEM_EVENTS_CRON_EXPRESSION} (defaults to {@code 0/5 * * * * ?}
 * ): Set the appropriate cron expression for the execution of this Job. By
 * default, this job checks for new System Events every 5 seconds.</li>
 * </ul>
 *
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
public class SystemEventsJob implements Runnable, Job { //extends DotJob {

	private static volatile AtomicLong lastCallback;
	private static List<Delegate<JobDelegateDataBean>> delegates;

	@Override
	//public void run(JobExecutionContext jobContext) throws JobExecutionException {
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		final List<Delegate<JobDelegateDataBean>> delegateList = this.getDelegates();
		if (delegateList != null && !delegateList.isEmpty()) {
			if (lastCallback != null && lastCallback.get() > 0) {
				for (Delegate<JobDelegateDataBean> delegate : delegateList) {
					final JobDelegateDataBean dataBean = new JobDelegateDataBean(jobContext, lastCallback.get());
					delegate.execute(dataBean);
				}
			}
			lastCallback = new AtomicLong(new Date().getTime());
		}
	}

	/**
	 * Returns the list of delegate classes. These classes will handle all the
	 * business logic that this Quartz Job is triggering.
	 *
	 * @return The list of {@link Delegate} classes.
	 */
	protected List<Delegate<JobDelegateDataBean>> getDelegates() {
		if (delegates == null) {
			delegates = new ArrayList<>();
			delegates.add(new SystemEventsJobDelegate());
		}
		return delegates;
	}

	@Override
	public void run() {

		try {
			this.execute(null);
		} catch (Exception e) {

			Logger.info(this, e.toString());
		} finally {
			DbConnectionFactory.closeSilently();
		}
	} // run.
}
