package com.dotcms.job.system.event;

import com.dotcms.api.system.event.SystemEventsCursor;
import com.dotcms.api.system.event.SystemEventsCursorAPI;
import com.dotcms.api.system.event.SystemEventsFactory;
import com.dotcms.job.system.event.delegate.SystemEventsJobDelegate;
import com.dotcms.job.system.event.delegate.bean.JobDelegateDataBean;
import com.dotcms.util.Delegate;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This Job is in charge of triggering the verification of new System Events
 * coming into the internal message queue. A list of {@link Delegate} classes
 * can be registered to this Job to let other services or pieces of the
 * application know about a specific event and react to it.
 * <p>
 * How far this node has consumed the queue is tracked by a <b>durable per-node cursor</b>
 * ({@code system_event_cursor}), read and written through {@link SystemEventsCursorAPI}. It replaces
 * the in-memory wall-clock high-water mark this Job used to keep in a {@code static} field, which
 * caused two independent classes of permanent event loss (issue #36827):
 * <ul>
 * <li>The mark was advanced to {@code new Date().getTime()} <i>after</i> the delegates had run, so it
 * covered wall-clock time the query had never read. Combined with {@code created} being stamped
 * before its transaction commits, any event committing in that gap could never satisfy
 * {@code created >= mark} again.</li>
 * <li>The mark lived in a {@code static} field, so a restart reset it to "now" and every event
 * committed while the node was down was skipped.</li>
 * </ul>
 * The cursor now advances only to the instant a completed read <i>started</i>, each read reaches one
 * overlap window further back, and a failed read leaves the cursor untouched so its range is retried.
 * <p>
 * The configuration properties for this Job are set via the
 * {@code dotmarketing-config.properties} file:
 * <ul>
 * <li>{@code ENABLE_SYSTEM_EVENTS} (defaults to {@code true}): Set to
 * {@code false} to NOT execute this Job.</li>
 * <li>{@code SYSTEM_EVENTS_CRON_EXPRESSION} (defaults to {@code 0/5 * * * * ?}
 * ): Set the appropriate cron expression for the execution of this Job. By
 * default, this job checks for new System Events every 5 seconds.</li>
 * <li>{@code SYSTEM_EVENTS_OVERLAP_WINDOW_SECONDS}, {@code SYSTEM_EVENTS_MAX_BACKLOG_MINUTES} — see
 * {@link SystemEventsConfig}.</li>
 * </ul>
 *
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
public class SystemEventsJob implements Runnable, Job {

	private static List<Delegate<JobDelegateDataBean>> delegates;
	private static final AtomicBoolean CONFIG_VALIDATED = new AtomicBoolean(false);

	/**
	 * Long-lived on purpose. The tracker carries the dedupe set that suppresses the repeat deliveries
	 * the overlap window necessarily produces, so a per-poll instance would defeat it entirely.
	 * The tracker is bound to live configuration, so changes to the overlap window and backlog take
	 * effect on the next poll; only the dedupe set is carried across polls.
	 */
	private static volatile SystemEventsCursorTracker cursorTracker;

	/** Folded into this Job rather than scheduled separately - it already holds the counts. */
	private static final SystemEventsReconciliation RECONCILIATION = new SystemEventsReconciliation();
	private static final AtomicLong LAST_RECONCILED_AT = new AtomicLong(0L);

	@Override
	public void execute(final JobExecutionContext jobContext) throws JobExecutionException {

		final List<Delegate<JobDelegateDataBean>> delegateList = this.getDelegates();
		if (delegateList == null || delegateList.isEmpty()) {
			return;
		}

		if (CONFIG_VALIDATED.compareAndSet(false, true)) {
			SystemEventsConfig.validateConfiguration();
		}

		final String serverId = APILocator.getServerAPI().readServerId();
		final SystemEventsCursorAPI cursorAPI = SystemEventsFactory.getInstance().getSystemEventsCursorAPI();
		final SystemEventsCursorTracker tracker = getCursorTracker();

		try {

			final Optional<SystemEventsCursor> storedCursor = cursorAPI.findByServerId(serverId);
			final Long cursorValue = storedCursor.map(SystemEventsCursor::getLastEventDate).orElse(null);

			if (cursorValue == null) {
				Logger.info(this, "No delivery cursor found for server [" + serverId
						+ "]; seeding at the current time. The retained backlog is deliberately not replayed.");
			}

			storedCursor.ifPresent(cursor -> warnIfPollerStalled(serverId, tracker, cursor));

			final SystemEventsPollWindow window = tracker.beginPoll(cursorValue, System.currentTimeMillis());

			if (window.isClamped()) {
				Logger.warn(this, "The system event cursor for server [" + serverId + "] was older than "
						+ SystemEventsConfig.getMaxBacklogMinutes() + " minutes and has been clamped. Events in "
						+ "the skipped span of " + window.getSkippedSpanMillis()
						+ "ms were NOT delivered to this node.");
			}

			for (final Delegate<JobDelegateDataBean> delegate : delegateList) {
				delegate.execute(new JobDelegateDataBean(jobContext, window.getReadFloor(), tracker));
			}

			// Only reached when every delegate completed. A failed read must leave the cursor alone so
			// the next poll retries the same range instead of skipping it.
			cursorAPI.save(serverId, tracker.completePoll(window));

			reconcileIfDue(serverId, tracker);
		} catch (final Exception e) {
			Logger.error(this, "Error processing system events for server [" + serverId
					+ "]; the delivery cursor was NOT advanced and this range will be retried: "
					+ e.getMessage(), e);
		}
	}

	/**
	 * Warns when the cursor has gone unwritten for several poll intervals, which means this poller is
	 * not running or every read is failing. Previously a stalled poller and a quiet queue looked
	 * identical - both produced no output at all.
	 */
	private void warnIfPollerStalled(final String serverId, final SystemEventsCursorTracker tracker,
			final SystemEventsCursor cursor) {

		if (null == cursor.getModDate()) {
			return;
		}

		final long pollIntervalMillis = SystemEventsConfig.getPollIntervalMillis();
		if (tracker.isCursorStale(cursor.getModDate().getTime(), System.currentTimeMillis(),
				pollIntervalMillis)) {
			Logger.warn(this, "The system event cursor for server [" + serverId + "] was last written "
					+ (System.currentTimeMillis() - cursor.getModDate().getTime())
					+ "ms ago, far longer than the " + pollIntervalMillis
					+ "ms poll interval. This node has not been consuming the queue.");
		}
	}

	/**
	 * Runs the authored-vs-observed reconciliation when its interval has elapsed. Folded into this Job
	 * rather than given its own schedule: this is where the observed count already lives, and one
	 * fewer Quartz job is one fewer thing to configure and to fail.
	 */
	private void reconcileIfDue(final String serverId, final SystemEventsCursorTracker tracker) {

		final long intervalMillis = SystemEventsConfig.getReconcileIntervalMillis();
		final long now = System.currentTimeMillis();
		final long last = LAST_RECONCILED_AT.get();

		if (last == 0L) {
			// First poll after start - begin the window here rather than reporting against a period
			// this node was not running for.
			LAST_RECONCILED_AT.set(now);
			tracker.resetObservedOwnEventCount();
			return;
		}

		if (now - last < intervalMillis) {
			return;
		}

		if (LAST_RECONCILED_AT.compareAndSet(last, now)) {
			try {
				RECONCILIATION.reconcile(serverId, now - last, tracker.resetObservedOwnEventCount());
			} catch (final Exception e) {
				// Reconciliation is a diagnostic; it must never break delivery.
				Logger.warn(this, "Unable to reconcile system event delivery for server [" + serverId
						+ "]: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Returns the poller's tracker, creating it on first use. Held across polls so its dedupe set
	 * survives from one poll to the next.
	 *
	 * @return The {@link SystemEventsCursorTracker} instance.
	 */
	private static SystemEventsCursorTracker getCursorTracker() {
		if (cursorTracker == null) {
			synchronized (SystemEventsJob.class) {
				if (cursorTracker == null) {
					// Bound to live config, not to the values at construction: the dedupe set has to
					// survive across polls, but the window and backlog are re-read each time so a
					// change takes effect on the next poll rather than at the next restart.
					cursorTracker = SystemEventsCursorTracker.fromConfig();
				}
			}
		}
		return cursorTracker;
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
		} catch (final Exception e) {

			Logger.error(this, e.getMessage(), e);
		} finally {
			DbConnectionFactory.closeSilently();
		}
	} // run.
}
