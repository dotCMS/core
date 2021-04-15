package com.dotcms.rest.api.v1.system.websocket;

import com.dotcms.api.system.event.*;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import javax.ws.rs.ForbiddenException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.twelvemonkeys.lang.DateUtil;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This Websocket end-point allows other parts of the system (such as the User
 * Notification component) to register to this service and receive information
 * regarding new notifications or system events. Other application services can
 * get an instance of this end-point via the {@link WebSocketContainerAPI} and
 * send System Events so that other components can read and process them.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 11, 2016
 *
 */
@SuppressWarnings("serial")
// Just removing this annotation to avoid the web sockets
//@ServerEndpoint(value = SystemEventsWebSocketEndPoint.API_WS_V1_SYSTEM_EVENTS, encoders = { SystemEventEncoder.class }, configurator = DotCmsWebSocketConfigurator.class)
public class SystemEventsWebSocketEndPoint implements Serializable {

	public static final String ID 				= "userId";
	public static final String USER 			= "user";
	public static final String USER_SESSION_ID  = "userSessionId";
	public static final String API_WS_V1_SYSTEM_EVENTS = "/api/ws/v1/system/events";


	private final Queue<Session> queue;
	private final SystemEventProcessorFactory systemEventProcessorFactory;
    private final PayloadVerifierFactory payloadVerifierFactory;
    private final static ForbiddenCloseCode FORBIDDEN_CLOSE_CODE = new ForbiddenCloseCode();
    private final long millisPingTimeOut;

	/**
	 * Configuration for ping pong strategy
	 */
	public  static final String     DOTCMS_WEBSOCKET_MILLIS_PING_TIME_OUT = "dotcms.websocket.millis.ping.timeout";
	public  static final String     DOTCMS_WEBSOCKET_MILLIS_PINGPONG      = "dotcms.websocket.millis.pingpong";
	public  static final String     DOTCMS_WEBSOCKET_USEPINGPONG          = "dotcms.websocket.usepingpong";
	public  static final String     DOTCMS_WEBSOCKET_MAX_IDLE_TIMEOUT     = "dotcms.websocket.maxidletimeout";
	private static final ByteBuffer PING_RECEIVED                         = ByteBuffer.wrap("PING".getBytes());

	public SystemEventsWebSocketEndPoint() {

		this(new ConcurrentLinkedQueue<Session>(),
                SystemEventProcessorFactory.getInstance(),
                PayloadVerifierFactory.getInstance());
    }

	@VisibleForTesting
	public SystemEventsWebSocketEndPoint(final Queue<Session> queue,
                                         final SystemEventProcessorFactory systemEventProcessorFactory,
                                         final PayloadVerifierFactory payloadVerifierFactory) {

		this.queue       				 = queue;
        this.systemEventProcessorFactory = systemEventProcessorFactory;
        this.payloadVerifierFactory      = payloadVerifierFactory;

		final boolean usePingPong = Config.getBooleanProperty(DOTCMS_WEBSOCKET_USEPINGPONG, true);
		if (usePingPong) {

			final long millisForWaitPingPong = Config.getLongProperty(DOTCMS_WEBSOCKET_MILLIS_PINGPONG,
					DateUtil.MINUTE); // by default 1 min
			DotInitScheduler.getScheduledThreadPoolExecutor().
					scheduleWithFixedDelay(this::processPingPongQueue, 0, millisForWaitPingPong, TimeUnit.MILLISECONDS);
		}

		this.millisPingTimeOut =
				Config.getLongProperty(DOTCMS_WEBSOCKET_MILLIS_PING_TIME_OUT,
						-1);  // ten seconds by default.
	} // SystemEventsWebSocketEndPoint.

	private void processPingPongQueue () {

		Logger.debug(this,
				"Processing the session queue at: " + new Date());
		final ArrayList<Session> closedSessions = new ArrayList<>();
		for (final Session session : this.queue) {

			if (session.isOpen()) {
				this.doPing(session);
			} else {

				closedSessions.add(session);
			}
		}

		if (!closedSessions.isEmpty()) {
			this.queue.removeAll(closedSessions);
		}
	}

	@OnMessage
	public void onPong(final PongMessage pongMessage, final Session session) {
		// the browser will send the pong message automatically with the same data we sent on the ping.
		if (PING_RECEIVED.equals(pongMessage.getApplicationData())) {

			Logger.debug(this, "Pong message received from session: " + session);
		}
	} // onPong.

	private void doPing (final Session session) {

		// wait for N seconds
		try {

			// we wait for a N seconds and then send the ping message
			if (session.isOpen()) {

 				Logger.debug(this, "Doing ping to: " + session + " at " + new Date());
				final RemoteEndpoint.Async asyncHandle = session.getAsyncRemote();

				if (this.millisPingTimeOut > 0) {
					asyncHandle.setSendTimeout(this.millisPingTimeOut);
				}

				asyncHandle.sendPing(PING_RECEIVED);
			} else {

				Logger.debug(this, "Couldn't do the ping to: " + session + ", session is closed");
			}
		} catch (IOException e) {

			if (Logger.isErrorEnabled(this.getClass())) {

				Logger.error(this.getClass(), e.getMessage(), e);
			}

			if (ExceptionUtil.causedBy(e, TimeoutException.class, ExecutionException.class))  {

				this.queue.remove(session);
			}
		} catch (Exception e) {
			if (Logger.isErrorEnabled(this.getClass())) {

				Logger.error(this.getClass(), e.getMessage(), e);
			}
		}
	} // doPing.



	@OnOpen
	public void open(final Session session) {

		User   user 		 = null;
		String userSessionId = null;
		boolean isLoggedIn   = false;

		if (session.getUserProperties().containsKey(USER)) {

			try {

				final long maxIdleTimeout =
					Config.getLongProperty(DOTCMS_WEBSOCKET_MAX_IDLE_TIMEOUT, -1);

				if (maxIdleTimeout > 0) {
					session.setMaxIdleTimeout(maxIdleTimeout);
				}

				userSessionId = (String)session.getUserProperties().get(USER_SESSION_ID);
				user 		  = (User) session.getUserProperties().get(USER);
				this.queue.add(new SessionWrapper(session, user, userSessionId));
				isLoggedIn = true;
				Logger.debug(this, "New session open: " + session +
										", with user: " + user.getEmailAddress());
			} catch (Exception e) {

				if (Logger.isErrorEnabled(this.getClass())) {

					Logger.error(this.getClass(), e.getMessage(), e);
				}
			}
		}

		if (!isLoggedIn) {

			try {

				final ForbiddenException forbiddenException = new ForbiddenException("A web socket connection requires a previous web session created");
				if (session.isOpen()) {

					session.getAsyncRemote().sendObject(forbiddenException);
					session.close(new CloseReason(FORBIDDEN_CLOSE_CODE ,
							"A web socket connection requires a previous web session created"));
				}
				throw forbiddenException;
			} catch (IOException e) {
				if (Logger.isErrorEnabled(this.getClass())) {

					Logger.error(this.getClass(), e.getMessage(), e);
				}
				throw new IllegalStateException(e);
			}
		}
	} // open.

	@OnError
	public void error(final Session session, final Throwable t) {
		Logger.debug(this, "Error on the session: " + session + ", error: " + t);
		queue.remove(session);
	}

	@OnClose
	public void closedConnection(Session session) {

		Logger.debug(this, "Closing the session: " + session);
		queue.remove(session);
	}

	/**
	 * Sends the specified {@link SystemEvent} object to all the clients
	 * (front-end or back-end services) that are registered to this Websocket
	 * end-point.
	 * 
	 * @param event
	 *            - A new System Event that has been generated.
	 */
	public void sendSystemEvent(final SystemEvent event) {

		final ArrayList<Session> closedSessions = new ArrayList<>();

		try {

			for (Session session : this.queue) {

				if (!session.isOpen()) {

					closedSessions.add(session);
				} else {

					if (this.apply (event, session)) {

						session.getAsyncRemote().sendObject
								(this.processEvent(session, event));
					} else {

						Logger.debug(this, "The event: " + event
								+ ", has been filtered for the session: " + session.getId());
					}
				}
			}

			if (!closedSessions.isEmpty()) {
				this.queue.removeAll(closedSessions);
			}
		} catch (Throwable e) {

			Logger.error(this, "An error occurred when sending a message through the " + this.getClass().getName(), e);
		}
	} // sendSystemEvent.

	private SystemEvent processEvent(final Session session,
									 final SystemEvent event) {

		final SystemEventProcessor processor =
				this.systemEventProcessorFactory.createProcessor(event.getEventType());

		return null != processor? processor.process(event,
													(null != session && session instanceof SessionWrapper)?
															SessionWrapper.class.cast(session).getUser():null)
				 			      : event;
	} // processEvent.

    /**
     * Verifies if the current user has the "visibility" rights to use this given payload
     *
     * @param session Session wrapper needed in order to obtain the current user information
     * @param payload Payload to validate
     * @return true the current user has "visibility" rights on this payload
     */
    private boolean validPayload(final SessionWrapper session,
                                 final Payload payload) {

        //Get the verifier associated to this Payload
        final PayloadVerifier verifier = this.payloadVerifierFactory.getVerifier(payload);

        //Check if we have the "visibility" rights to use this payload
        return (null != verifier) ? verifier.verified(payload, session) : true;
    }

	private boolean apply(final SystemEvent event,
						  final Session session) throws DotDataException  {

		final Payload payload = event.getPayload();
		boolean apply = true; // by default consider it as Visibility.GLOBAL

		if (null != payload) {

			if (null != payload.getVisibility()) {

				if (session instanceof SessionWrapper) {

					if (null != SessionWrapper.class.cast(session).getUser()) {
                        apply = this.validPayload((SessionWrapper) session, payload);
                    }
                }
            }
        } else {

			apply = false; // if the payload is null, must not send to the session.
		}

		return apply;
	} // apply.



} // E:O:F:SystemEventsWebSocketEndPoint.
