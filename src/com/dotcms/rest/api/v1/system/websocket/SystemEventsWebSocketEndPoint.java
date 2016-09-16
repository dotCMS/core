package com.dotcms.rest.api.v1.system.websocket;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
@ServerEndpoint(value = "/api/v1/system/events", encoders = { SystemEventEncoder.class }, configurator = DotCmsWebSocketConfigurator.class)
public class SystemEventsWebSocketEndPoint implements Serializable {

	public static final String ID = "id";
	private final Queue<Session> queue;
	private final UserAPI userAPI;
	private final RoleAPI roleAPI;

	public SystemEventsWebSocketEndPoint() {
		this(new ConcurrentLinkedQueue<Session>(),
				APILocator.getUserAPI(), APILocator.getRoleAPI());
	}

	@VisibleForTesting
	public SystemEventsWebSocketEndPoint(final Queue<Session> queue,
										 final UserAPI userAPI,
										 final RoleAPI roleAPI) {
		this.queue   = queue;
		this.userAPI = userAPI;
		this.roleAPI = roleAPI;
	}

	@OnOpen
	public void open(final Session session) {

		final Map<String, List<String>> paramMap =
				session.getRequestParameterMap();
		User user = null;
		boolean addToNormalSession = true;

		if(null != paramMap && paramMap.containsKey(ID)) {

			final List<String> idValues = paramMap.get(ID);

			if (null != idValues && idValues.size() > 0) {

				final String userId = idValues.get(0);

				try {

					if (UtilMethods.isSet(userId)) {

						user = this.userAPI.loadUserById(userId);
						if (null != user) {

							this.queue.add(new SessionWrapper(session, user));
							addToNormalSession = false; // not need to add the normal session, since the wrapper was added.
						}
					}
				} catch (Exception e) {

					if (Logger.isErrorEnabled(this.getClass())) {

						Logger.error(this.getClass(), e.getMessage(), e);
					}
				}
			}
		}

		if (addToNormalSession) {

			this.queue.add(session);
		}
	} // open.

	@OnError
	public void error(Session session, Throwable t) {
		queue.remove(session);
	}

	@OnClose
	public void closedConnection(Session session) {
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

			for (Session session : queue) {

				if (!session.isOpen()) {

					closedSessions.add(session);
				} else {

					if (this.apply (event, session)) {

						session.getAsyncRemote().sendObject(event);
					}
				}
			}

			this.queue.removeAll(closedSessions);
		} catch (Throwable e) {

			Logger.error(this, "An error occurred when sending a message through the " + this.getClass().getName(), e);
		}
	} // sendSystemEvent.

	private boolean apply(final SystemEvent event,
						  final Session session) throws DotDataException  {

		final Payload payload = event.getPayload();
		boolean apply = true; // by default consider it as Visibility.GLOBAL

		if (null != payload) {

			if (null != payload.getVisibility()) {

				if (session instanceof SessionWrapper) {

					if (null != SessionWrapper.class.cast(session).getUser()) {

							if (payload.getVisibility() == Visibility.USER) {
							// if the session user is the same of the event, them it apply to be sent
							apply = SessionWrapper.class.cast(session).getUser().getUserId()
										.equals(payload.getVisibilityId());
						} else if (payload.getVisibility() == Visibility.ROLE) {
							// if the session user match the event role, them it apply to be sent
							apply = this.checkRoles(SessionWrapper.class.cast(session).getUser(),
									payload.getVisibilityId());
						}
					}
				}
			}
		} else {

			apply = false; // if the payload is null, must not send to the session.
		}

		return apply;
	} // apply.

	private boolean checkRoles(final User user, final String roleId) throws DotDataException {

		return this.roleAPI.doesUserHaveRole(user, roleId);
	}

} // E:O:F:SystemEventsWebSocketEndPoint.
