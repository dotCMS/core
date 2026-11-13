package com.dotcms.rest.api.v1.system.websocket;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenAuthCredentialProcessorImpl;
import com.google.common.annotations.VisibleForTesting;
import org.glassfish.jersey.server.ContainerRequest;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;
import java.util.List;

/**
 * This {@link Configurator} is in charge of the single instantiation of the
 * ServerSocket End points. It delegates the {@link WebSocketContainerAPI} the
 * task to keep just one instance of the web sockets. Consequently, we can get
 * the endpoint instance in any other place of the application.
 *
 * @author jsanca
 * @version 3.7
 */
public class DotCmsWebSocketConfigurator extends Configurator {

	private final WebSocketContainerAPI webSocketContainerAPI;
	private final JsonWebTokenAuthCredentialProcessor authCredentialProcessor;

	public DotCmsWebSocketConfigurator() {

		this(APILocator.getWebSocketContainerAPI(),
				JsonWebTokenAuthCredentialProcessorImpl.getInstance());
	}

	/**
	 * Creates a configurator on the given collaborators, for tests.
	 *
	 * @param webSocketContainerAPI   keeps the single instance of each endpoint
	 * @param authCredentialProcessor resolves a user from a JWT {@code Authorization} header
	 */
	@VisibleForTesting
	protected DotCmsWebSocketConfigurator(final WebSocketContainerAPI webSocketContainerAPI,
										  final JsonWebTokenAuthCredentialProcessor authCredentialProcessor) {

		this.webSocketContainerAPI   = webSocketContainerAPI;
		this.authCredentialProcessor = authCredentialProcessor;
	}

	@Override
	public <T> T getEndpointInstance(final Class<T> endpointClass) throws InstantiationException {
		return this.webSocketContainerAPI.getEndpointInstance(endpointClass);
	}

	/**
	 * Puts the user and the session id of the handshake request on the endpoint's user
	 * properties. The user is the one the HTTP session resolves to through
	 * {@link PortalUtil#getUser(HttpSession)}, which checks the user's current status as every
	 * request does; a session that yields no user there yields no user here either. Without a
	 * session user, a JWT {@code Authorization} header is tried.
	 *
	 * @param serverEndpointConfig the endpoint configuration receiving the user properties
	 * @param request              the handshake request, with the HTTP session and headers
	 * @param response             the handshake response
	 */
	@Override
	public void modifyHandshake(final ServerEndpointConfig serverEndpointConfig,
								final HandshakeRequest request,
								final HandshakeResponse response) {

		super.modifyHandshake(serverEndpointConfig, request, response);

		String sessionId		   = null;
		User user      			   = null;
		String authorizationHeader = null;
		final List<String> headers = request.getHeaders().get(ContainerRequest.AUTHORIZATION);
		final Object session 	   = request.getHttpSession();
		HttpSession  httpSession   = null;

		if (UtilMethods.isSet(session) && session instanceof HttpSession) {

			try {

				httpSession = HttpSession.class.cast(session);
				sessionId   = httpSession.getId();
				user = PortalUtil.getUser(httpSession);
			} catch (Exception e) {

				if (Logger.isErrorEnabled(this.getClass())) {

					Logger.error(this.getClass(), e.getMessage(), e);
				}
			}
		}

		try {

			if (!UtilMethods.isSet(user) && ((null != headers) && (headers.size() > 0))) {

				authorizationHeader = headers.get(0);
				user = this.authCredentialProcessor.processAuthHeaderFromJWT
						(authorizationHeader, httpSession, "websocket");
			}

			if (UtilMethods.isSet(user)) {

				serverEndpointConfig.getUserProperties().put
						(SystemEventsWebSocketEndPoint.USER, user);
			}

			if (UtilMethods.isSet(sessionId)) {

				serverEndpointConfig.getUserProperties().put
						(SystemEventsWebSocketEndPoint.USER_SESSION_ID, sessionId);
			}
		} catch (Exception e) {

			if (Logger.isErrorEnabled(this.getClass())) {

				Logger.error(this.getClass(), e.getMessage(), e);
			}
		}
	} // modifyHandshake.

} // DotCmsWebSocketConfigurator.
