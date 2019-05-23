package com.dotcms.rest.api.v1.system.websocket;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenAuthCredentialProcessorImpl;
import com.dotcms.business.LazyUserAPIWrapper;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import org.glassfish.jersey.server.ContainerRequest;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
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
	private final UserAPI userAPI;

	public DotCmsWebSocketConfigurator() {

		this(APILocator.getWebSocketContainerAPI(),
				JsonWebTokenAuthCredentialProcessorImpl.getInstance(),
				new LazyUserAPIWrapper());
	}

	@VisibleForTesting
	protected DotCmsWebSocketConfigurator(final WebSocketContainerAPI webSocketContainerAPI,
			 							  final JsonWebTokenAuthCredentialProcessor authCredentialProcessor,
										  final UserAPI userAPI) {

		this.webSocketContainerAPI   = webSocketContainerAPI;
		this.authCredentialProcessor = authCredentialProcessor;
		this.userAPI				 = userAPI;
	}

	@Override
	public <T> T getEndpointInstance(final Class<T> endpointClass) throws InstantiationException {
		return this.webSocketContainerAPI.getEndpointInstance(endpointClass);
	}

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
				user = (User) PortalUtil.getUser(httpSession);

				if (!UtilMethods.isSet(user)) {

					user = this.getUserFromId(httpSession);
				}
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

	private User getUserFromId(final HttpSession httpSession) throws DotSecurityException, DotDataException {

		User user = null;
		final String userId = (String) httpSession.getAttribute
				(com.liferay.portal.util.WebKeys.USER_ID);

		if (UtilMethods.isSet(userId)) {

			user = this.userAPI.loadUserById(userId);
		}

		return user;
	} // getUserFromId.

} // DotCmsWebSocketConfigurator.
