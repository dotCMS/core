package com.dotcms.rest;

import com.dotcms.publisher.pusher.AuthCredentialPushPublishUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.util.Logger;

import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * Provide util method for {@link IntegrityResource} and {@link BundleResource} classes
 */
public class PushPublishResourceUtil {

    public static Optional<Response> getFailResponse(
            final String remoteIP,
            final AuthCredentialPushPublishUtil.PushPublishAuthenticationToken pushPublishAuthenticationToken) {

        final ResourceResponse responseResource = new ResourceResponse(CollectionsUtils.map("type", "plain"));

        if (pushPublishAuthenticationToken.isTokenInvalid()) {
            Logger.error(PushPublishResourceUtil.class, "Invalid token from " + remoteIP + " not permission");
            return Optional.of(
                    responseResource.responseAuthenticateError("invalid_token",
                            AuthCredentialPushPublishUtil.INVALID_TOKEN_ERROR_KEY)
            );
        } else if (pushPublishAuthenticationToken.isTokenExpired()) {
            return Optional.of(
                    responseResource.responseAuthenticateError("invalid_token",
                            AuthCredentialPushPublishUtil.EXPIRED_TOKEN_ERROR_KEY)
            );
        }

        if (pushPublishAuthenticationToken.isJWTTokenWay()) {
            return pushPublishAuthenticationToken.getToken().getActiveUser().isPresent() ?
                    Optional.of(responseResource.responseAuthenticateError("invalid_token",
                            AuthCredentialPushPublishUtil.INVALID_TOKEN_ERROR_KEY )) :
                    Optional.empty();
        } else {
            return Optional.empty();
        }
    }

}
