package com.dotcms.publisher.pusher;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenAuthCredentialProcessorImpl;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.IncorrectClaimException;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.ContainerRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

/**
 * Singleton class to provide util method to Push Publish authentication, it support two authentication methods:
 * JWToken and Auth key set in the {@link com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint},
 * if the config property 'USE_JWT_TOKEN_IN_PUSH_PUBLISH' is set to true (default value)
 * it is going to use JWT token, but if the config property is set to false or the JWT token fails then auth key is going to use
 * */
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

    /**
     * Proceess the request to Authenticate a Push publish request
     *
     * @param request
     * @return If the token is invalid {@link PushPublishAuthenticationToken#INVALID_TOKEN} or {@link PushPublishAuthenticationToken#EXPIRE_TOKEN}
     * otherwise return a different {@link PushPublishAuthenticationToken} instance
     */
    public PushPublishAuthenticationToken processAuthHeader(final HttpServletRequest request) {
        final boolean useJWTToken = isJWTAvailable();

        try{
            if (useJWTToken) {
                final PushPublishAuthenticationToken pushPublishAuthenticationToken = getFromJWTToken(request);

                if (pushPublishAuthenticationToken == PushPublishAuthenticationToken.INVALID_TOKEN) {
                    return getFromEndPointAuthKey(request);
                } else {
                    return pushPublishAuthenticationToken;
                }
            } else {
                return getFromEndPointAuthKey(request);
            }
        } catch (DotDataException | IOException e) {
            return PushPublishAuthenticationToken.INVALID_TOKEN;
        }

    }

    private PushPublishAuthenticationToken getFromEndPointAuthKey(HttpServletRequest request) throws DotDataException, IOException {
        final Optional<PublishingEndPoint> publishingEndPointOptional = getPublishingEndPointDotCMSToken(request);
        return publishingEndPointOptional.isPresent() ?
                new PushPublishAuthenticationToken(publishingEndPointOptional.get()) :
                PushPublishAuthenticationToken.INVALID_TOKEN;
    }

    private PushPublishAuthenticationToken getFromJWTToken(HttpServletRequest request) {
        try {
            final Optional<JWToken> jwTokenOptional =
                    JsonWebTokenAuthCredentialProcessorImpl.getInstance().processJWTAuthHeader(request);

            if (!jwTokenOptional.isPresent()){
                return PushPublishAuthenticationToken.INVALID_TOKEN;
            }

            final Optional<User> optionalUser = jwTokenOptional.get().getActiveUser();
            if (!optionalUser.isPresent()){
                return PushPublishAuthenticationToken.INVALID_TOKEN;
            }

            return new PushPublishAuthenticationToken(jwTokenOptional.get());
        } catch(IncorrectClaimException e){
            final String claimName = e.getClaimName();

            return Claims.EXPIRATION.equals(claimName) ? PushPublishAuthenticationToken.EXPIRE_TOKEN :
                    PushPublishAuthenticationToken.INVALID_TOKEN;
        }
    }

    private Optional<PublishingEndPoint> getPublishingEndPointDotCMSToken(final HttpServletRequest request)
            throws DotDataException, IOException {
        final String remoteIP = request.getRemoteHost();
        final PublishingEndPoint publishingEndPoint =
                APILocator.getPublisherEndPointAPI().findEnabledSendingEndPointByAddress(remoteIP);

        Optional<String> key = PushPublisher.retriveEndpointKeyDigest(publishingEndPoint);
        if(!key.isPresent()) {
            return Optional.empty();
        }

        final String token = getTokenFromRequest(request);

        return token.equals( key.get() ) ? Optional.of(publishingEndPoint) : Optional.empty();
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

    private Optional<String> getJWTToken(final PublishingEndPoint endpoint) throws IOException {
        return PushPublisher.retriveEndpointKey(endpoint);
    }

    public static class PushPublishAuthenticationToken {

        public static final PushPublishAuthenticationToken EXPIRE_TOKEN = new PushPublishAuthenticationToken(true, false);
        public static final PushPublishAuthenticationToken INVALID_TOKEN = new PushPublishAuthenticationToken(false, true);
        private final boolean tokenExpired;
        private final boolean tokenInvalid;

        private boolean usingJWTTokenWay = false;
        private JWToken token;
        private PublishingEndPoint publishingEndPoint;

        private PushPublishAuthenticationToken(boolean tokenExprired, boolean tokenInvalid) {
            this(null, null, tokenExprired, tokenInvalid);
        }

        public PushPublishAuthenticationToken(final JWToken token) {
            this(token, null, false, false);
        }

        public PushPublishAuthenticationToken(final PublishingEndPoint publishingEndPoint) {
            this(null, publishingEndPoint, false, false);
        }

        private PushPublishAuthenticationToken(final JWToken token, final PublishingEndPoint publishingEndPoint,
                boolean tokenExpired, boolean tokenInvalid) {
            this.token = token;
            this.publishingEndPoint = publishingEndPoint;
            this.tokenExpired = tokenExpired;
            this.tokenInvalid = tokenInvalid;
        }

        public JWToken getToken() {
            return token;
        }

        public PublishingEndPoint getPublishingEndPoint() {
            return publishingEndPoint;
        }

        public boolean isJWTTokenWay(){
            return this.token != null;
        }

        public boolean isTokenExpired() {
            return tokenExpired;
        }

        public boolean isTokenInvalid() {
            return tokenInvalid;
        }

    }

}
