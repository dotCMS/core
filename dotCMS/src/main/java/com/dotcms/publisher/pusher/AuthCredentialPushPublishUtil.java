package com.dotcms.publisher.pusher;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenAuthCredentialProcessorImpl;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.parameter.display.DropdownInput;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.IncorrectClaimException;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.ContainerRequest;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public enum AuthCredentialPushPublishUtil {
    INSTANCE;

    private final String BEARER = "Bearer ";

    public static String EXPIRED_TOKEN_ERROR_KEY = "__expired_token__";
    public static String INVALID_TOKEN_ERROR_KEY = "__invalid_token__";

    public Optional<String> getRequestToken(final PublishingEndPoint endpoint)  {
        try {
            final boolean useJWTToken = isJWTAvailable();
            final Optional<String> tokenOptional = useJWTToken ?
                    getJWTToken(endpoint) :
                    PushPublisher.retriveEndpointKeyDigest(endpoint);

            if (!tokenOptional.isPresent()) {
                return tokenOptional;
            }

            final String token = tokenOptional.get();

            return Optional.of(JsonWebTokenAuthCredentialProcessor.BEARER + token);
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    public static boolean isJWTAvailable() {
        return Config.getBooleanProperty("USE_JWT_TOKEN_IN_PUSH_PUBLISH", true);
    }

    public boolean processAuthHeader(final HttpServletRequest request) throws DotSecurityException{
        final boolean useJWTToken = isJWTAvailable();

        try {
            if (useJWTToken) {
                final User user = JsonWebTokenAuthCredentialProcessorImpl.getInstance().processAuthHeaderFromJWT(request);

                if (!APILocator.getUserAPI().isCMSAdmin(user)){
                    throw new DotSecurityException("Operation jus allow o admin user");
                }

                return true;
            } else {
                return isValidDotCMSToken(request);
            }
        }catch (DotDataException | IOException | DotSecurityException exception) {
            return false;
        }
    }

    private boolean isValidDotCMSToken(final HttpServletRequest request) throws DotDataException, IOException {
        final String remoteIP = request.getRemoteHost();
        final PublishingEndPoint publishingEndPoint =
                APILocator.getPublisherEndPointAPI().findEnabledSendingEndPointByAddress(remoteIP);

        Optional<String> key = PushPublisher.retriveEndpointKeyDigest(publishingEndPoint);
        if(!key.isPresent()) {
            return false;
        }

        final String token = getTokenFromRequest(request);

        return token.equals( key.get() );
    }

    private String getTokenFromRequest(final HttpServletRequest request) {
        final String authorizationHeader = request.getHeader(ContainerRequest.AUTHORIZATION);

        if (StringUtils.isNotEmpty(authorizationHeader) && authorizationHeader.trim()
                .startsWith(BEARER)) {

            return authorizationHeader.substring(BEARER.length());
        } else {
            throw new IllegalArgumentException("Bearer Authorization header expected");
        }
    }

    @NotNull
    private Optional<String> getJWTToken(final PublishingEndPoint endpoint) throws IOException {
        return PushPublisher.retriveEndpointKey(endpoint);
    }

}
