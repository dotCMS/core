package com.dotcms.job.system.event.delegate;

import static com.dotmarketing.util.DateUtil.daysToMillis;

import java.util.Date;

import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.job.system.event.AbstractJobDelegate;
import com.dotcms.job.system.event.DeleteOldSystemEventsJob;
import com.dotcms.job.system.event.delegate.bean.JobDelegateDataBean;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;

/**
 * This delegate class is registered to the {@link DeleteOldSystemEventsJob},
 * which is the Job that deletes old entries from the System Events table every
 * specified amount of days. The configuration property for this delegate is set
 * via the {@code dotmarketing-config.properties} file:
 * <ul>
 * <li>{@code systemevents.job.deleteevents.olderthan} (defaults to {@code 31}):
 * This property represents the number of days ― from the current date ― that
 * the application will keep track of events. Therefore, a value of <i>n</i>
 * indicates that System Events older than <i>[current date] - n</i> will be
 * deleted from the database.</li>
 * </ul>
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 18, 2016
 *
 */
@SuppressWarnings("serial")
public class DeleteOldSystemEventsDelegate extends AbstractJobDelegate {

	protected static final String DELETE_EVENTS_OLDER_THAN = "systemevents.job.deleteevents.olderthan";

	protected final SystemEventsAPI systemEventsAPI = APILocator.getSystemEventsAPI();

	@Override
	protected void executeDelegate(JobDelegateDataBean data) throws Exception {
		// Property value represents days, defaults to 31 days
		int olderThan = Config.getIntProperty(DELETE_EVENTS_OLDER_THAN, 31);
		long toDate = new Date().getTime() - daysToMillis(olderThan);
		this.systemEventsAPI.deleteEvents(toDate);
	}

}
