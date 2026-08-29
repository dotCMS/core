package com.dotcms.job.system.event.delegate;

import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.job.system.event.AbstractJobDelegate;
import com.dotcms.job.system.event.SystemEventsCursorTracker;
import com.dotcms.job.system.event.SystemEventsJob;
import com.dotcms.job.system.event.delegate.bean.JobDelegateDataBean;
import com.dotcms.rest.api.v1.system.websocket.SystemEventsWebSocketEndPoint;
import com.dotcms.rest.api.v1.system.websocket.WebSocketContainerAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

import java.util.Collection;

/**
 * This delegate class is registered to the {@link SystemEventsJob}, which is
 * the Job that checks for incoming System Events every specified amount of
 * time. The data sent over from the Job indicates the last time that the
 * application checked for new System Events. Based on such a date, this
 * delegate can query the database for new incoming events <b>ONLY</b>.
 * <p>
 * Querying the most recent events that entered the message queue is useful for
 * displaying notifications to the users in order to get synchronous updates on
 * new System Events generated either by dotCMS or custom code.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 13, 2016
 *
 */
@SuppressWarnings("serial")
public class SystemEventsJobDelegate extends AbstractJobDelegate {

	private final SystemEventsAPI 		systemEventsAPI 	  = APILocator.getSystemEventsAPI();
	private final WebSocketContainerAPI webSocketContainerAPI = APILocator.getWebSocketContainerAPI();
	private static final String 		SERVER_ID		 	  = APILocator.getServerAPI().readServerId();

	@Override
	public void executeDelegate(final JobDelegateDataBean data) throws DotDataException {
		Collection<SystemEvent> newEvents = null;
		final long lastCallback = data.getLastCallback();

		try {

			Logger.debug(this, "Getting events, last callback: " + lastCallback);
			// getEventsSince already returns Collection<SystemEvent>; the previous unchecked cast to
			// List bought nothing and hid the real type.
			newEvents = this.systemEventsAPI.getEventsSince(lastCallback);
		} catch (Exception e) {

			// A failed read used to be invisible at debug level, so a node that had stopped consuming
			// looked identical to a quiet queue. Re-thrown so the Job leaves the cursor untouched and
			// retries this range rather than skipping it.
			Logger.warn(this, "Unable to read system events since [" + lastCallback + "]: "
					+ e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}

		if (null != newEvents && !newEvents.isEmpty()) {

			final SystemEventsWebSocketEndPoint webSocketEndPoint = this.webSocketContainerAPI
					.getEndpointInstance(SystemEventsWebSocketEndPoint.class);

			final SystemEventsCursorTracker cursorTracker = data.getCursorTracker();
			final long readAt = System.currentTimeMillis();

			for (final SystemEvent event : newEvents) {

				// The overlap window deliberately re-reads recent events so late commits are caught;
				// without this check it would also re-deliver everything inside the window.
				if (null != cursorTracker && cursorTracker.isAlreadyDelivered(event.getId())) {
					continue;
				}

				// the owner server does not need to send the message again!
				if (!SERVER_ID.equals(event.getServerId())) {

					if (this.isClusterWideEventWrapped(event)) {

						this.notifyLocalSystemEvent(event);
					} else {

						webSocketEndPoint.sendSystemEvent(event);
					}
				} else {

					// Kept at INFO deliberately: this line is the instrument the issue used to measure
					// the original 50-63% loss, and the spec's Step B verification still counts it.
					// Throttling or demoting it is a follow-up, to be done only once the reconciliation
					// counter has been validated in production - and it must update spec.md Step B at
					// the same time.
					Logger.info(this, "The event: " + event.getId() +
								", has been skipped on the server: " + SERVER_ID);

					if (null != cursorTracker) {
						cursorTracker.recordObservedOwnEvent();
					}
				}

				// The one remaining way an event can be lost after this fix is a transaction held open
				// longer than the overlap window. Warn while the margin is merely thin, rather than
				// after events start disappearing.
				if (null != cursorTracker
						&& cursorTracker.isCommitLagApproachingWindow(
								event.getCreationDate().getTime(), readAt)) {
					Logger.warn(this, "System event [" + event.getId() + "] was committed "
							+ (readAt - event.getCreationDate().getTime())
							+ "ms after its creation timestamp, approaching the overlap window. "
							+ "Raise SYSTEM_EVENTS_OVERLAP_WINDOW_SECONDS - events from transactions "
							+ "longer than that window are dropped.");
				}

				// Recorded whether delivered or skipped: a node's own events must not be re-examined
				// and re-logged on every poll for as long as they sit inside the window.
				if (null != cursorTracker) {
					cursorTracker.markDelivered(event.getId(), event.getCreationDate().getTime());
				}
			}
		}
	} // executeDelegate.

	private void notifyLocalSystemEvent(final SystemEvent event) {

		APILocator.getLocalSystemEventsAPI().asyncNotify(event.getPayload().getData());
	}

	private boolean isClusterWideEventWrapped(final SystemEvent event) {

		return event.getEventType() == SystemEventType.CLUSTER_WIDE_EVENT;
	}

}
