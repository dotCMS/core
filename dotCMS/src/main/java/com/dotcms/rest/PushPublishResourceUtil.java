package com.dotcms.rest;

import com.dotcms.publisher.pusher.AuthCredentialPushPublishUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Provide util method for {@link IntegrityResource} and {@link BundleResource} classes
 */
public class PushPublishResourceUtil {

    public static Optional<Response> getFailResponse(
            final HttpServletRequest request,
            final AuthCredentialPushPublishUtil.PushPublishAuthenticationToken pushPublishAuthenticationToken) {

        final ResourceResponse responseResource = new ResourceResponse(new HashMap<>(Map.of("type", "plain")));
        final String localAddress = RestEndPointIPUtil.getFullLocalIp(request);
        final String remoteIP = RestEndPointIPUtil.resolveRemoteIp(request);
        String message = null;
        String errorKey = null;

        if (pushPublishAuthenticationToken.isTokenInvalid()) {
            message = String.format(
                    "Receiver at %s:> Authentication Token is invalid for ip: %s",
                    localAddress,
                    remoteIP);
            errorKey = AuthCredentialPushPublishUtil.INVALID_TOKEN_ERROR_KEY;
        } else if (pushPublishAuthenticationToken.isTokenExpired()) {
            message = String.format(
                    "Receiver at %s:> Authentication Token is expired for ip: %s",
                    localAddress,
                    remoteIP);
            errorKey = AuthCredentialPushPublishUtil.EXPIRED_TOKEN_ERROR_KEY;
        } else if (pushPublishAuthenticationToken.isJWTTokenWay()) {
            final Optional<User> optionalUser = pushPublishAuthenticationToken.getToken().getActiveUser();
            if (optionalUser.isEmpty() || !optionalUser.get().isAdmin()) {
                message = String.format(
                        "Receiver at %s:> JWT Token is grant just for Admin user for ip: %s",
                        localAddress,
                        remoteIP);
                errorKey = AuthCredentialPushPublishUtil.INVALID_TOKEN_ERROR_KEY;
            }
        }

        if (message == null) {
            return Optional.empty();
        }

        Logger.error(IntegrityResource.class, message);
        return Optional.of(responseResource.responseAuthenticateError(message, "invalid_token", errorKey));
    }
}
