package com.dotcms.rest;

import com.dotcms.publisher.pusher.AuthCredentialPushPublishUtil;
import com.dotcms.repackage.org.apache.http.HttpStatus;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * Provide util method for {@link IntegrityResource} and {@link BundleResource} classes
 */
public class PushPublishResourceUtil {

    public static Optional<Response> getFailResponse(
            final HttpServletRequest request,
            final AuthCredentialPushPublishUtil.PushPublishAuthenticationToken pushPublishAuthenticationToken) {

        final ResourceResponse responseResource = new ResourceResponse(CollectionsUtils.map("type", "plain"));
        final String localAddress = RestEndPointIPUtil.getFullLocalIp(request);
        final String remoteIP = RestEndPointIPUtil.resolveRemoteIp(request);

        if (pushPublishAuthenticationToken.isTokenInvalid()) {

            final String message = String.format(
                    "Receiver at %s:> Authentication Token is invalid for ip: %s",
                    localAddress,
                    remoteIP);

            Logger.error(IntegrityResource.class, String.format("Receiver at %s> :%s", localAddress, message));
            return Optional.of(
                    responseResource.responseAuthenticateError(
                            message,
                            "invalid_token",
                            AuthCredentialPushPublishUtil.INVALID_TOKEN_ERROR_KEY
                    )
            );
        } else if (pushPublishAuthenticationToken.isTokenExpired()) {
            final String message = String.format(
                    "Receiver at %s:> Authentication Token is expired for ip: %s",
                    localAddress,
                    remoteIP);

            Logger.error(IntegrityResource.class, String.format("Receiver at %s> :%s", localAddress, message));

            return Optional.of(
                    responseResource.responseAuthenticateError(
                            message,
                            "invalid_token",
                            AuthCredentialPushPublishUtil.EXPIRED_TOKEN_ERROR_KEY
                    )
            );
        }

        if (pushPublishAuthenticationToken.isJWTTokenWay()) {

            final Optional<User> optionalUser = pushPublishAuthenticationToken.getToken().getActiveUser();

            if (optionalUser.isPresent() && optionalUser.get().isAdmin()) {
                return Optional.empty();
            } else {
                final String message = String.format(
                        "Receiver at %s:> JWT Token is grant just for Admin user for ip: %s",
                        localAddress,
                        remoteIP);

                Logger.error(IntegrityResource.class, String.format("Receiver at %s> :%s", localAddress, message));

                return  Optional.of(
                        responseResource.responseAuthenticateError(
                                message,
                                "invalid_token",
                            AuthCredentialPushPublishUtil.INVALID_TOKEN_ERROR_KEY
                        )
                );
            }
        } else {
            return Optional.empty();
        }
    }
}
