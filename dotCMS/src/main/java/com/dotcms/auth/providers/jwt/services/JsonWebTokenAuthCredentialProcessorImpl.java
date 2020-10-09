package com.dotcms.auth.providers.jwt.services;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.ContainerRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * Default implementation
 * @author jsanca
 */
public class JsonWebTokenAuthCredentialProcessorImpl implements JsonWebTokenAuthCredentialProcessor {

    private final JsonWebTokenUtils jsonWebTokenUtils;

    private static class SingletonHolder {
        private static final JsonWebTokenAuthCredentialProcessorImpl INSTANCE = new JsonWebTokenAuthCredentialProcessorImpl();
    }
    /**
     * Get the instance.
     * @return JsonWebTokenAuthCredentialProcessorImpl
     */
    public static JsonWebTokenAuthCredentialProcessorImpl getInstance() {

        return JsonWebTokenAuthCredentialProcessorImpl.SingletonHolder.INSTANCE;
    } // getInstance.

    private JsonWebTokenAuthCredentialProcessorImpl() {

        this(JsonWebTokenUtils.getInstance());
    }

    @VisibleForTesting
    protected JsonWebTokenAuthCredentialProcessorImpl(final JsonWebTokenUtils jsonWebTokenUtils) {
        this.jsonWebTokenUtils = jsonWebTokenUtils;
    }

    protected Optional<JWToken> internalProcessAuthHeaderFromJWT(final String authorizationHeader,
            final String ipAddress, final boolean rejectIfNotApiToken) {

        if (StringUtils.isNotEmpty(authorizationHeader) && authorizationHeader.trim()
                .startsWith(BEARER)) {

            final String jsonWebToken = authorizationHeader.substring(BEARER.length());

            if (!UtilMethods.isSet(jsonWebToken)) {
                // "Invalid syntax for username and password"
                throw new SecurityException("Invalid Json Web Token", Response.Status.BAD_REQUEST);
            }

            try {

                final Optional<JWToken> token = APILocator.getApiTokenAPI().fromJwt(jsonWebToken.trim(), ipAddress);

                if (rejectIfNotApiToken && token.isPresent() && !(token.get() instanceof ApiToken)) {

                    throw new SecurityException("The Api token sent on the request header must be api token", Response.Status.BAD_REQUEST);
                }

                return token;
            } catch(SecurityException se) {

                this.jsonWebTokenUtils.handleInvalidTokenExceptions(this.getClass(), se, null, null);
                throw se;
            } catch (Exception e) {

                this.jsonWebTokenUtils.handleInvalidTokenExceptions(this.getClass(), e, null, null);
            }

        }

        return Optional.empty();
    }

    @Override
    public User processAuthHeaderFromJWT(final String authorizationHeader,
            final HttpSession session, final String ipAddress) {

        final Optional<JWToken> jwToken = internalProcessAuthHeaderFromJWT(authorizationHeader, ipAddress, false);
        final User user = jwToken.isPresent() ? jwToken.get().getActiveUser().get() : null;

        if (user != null && null != session) {
            session.setAttribute(WebKeys.CMS_USER, user);
            session.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
        }

        return user;
    } // processAuthCredentialsFromJWT.

    @Override
    public User processAuthHeaderFromJWT(final HttpServletRequest request) {

        // Extract authentication credentials
        final String authentication = request.getHeader(ContainerRequest.AUTHORIZATION);

        final Optional<JWToken> jwToken = internalProcessAuthHeaderFromJWT(authentication, request.getRemoteAddr(), false);
        final User user = jwToken.isPresent() ? jwToken.get().getActiveUser().get() : null;

        if(user != null) {

            request.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
            request.setAttribute(com.liferay.portal.util.WebKeys.USER, user);
        }

        return user;
    } // processAuthCredentialsFromJWT.

    @Override
    public Optional<JWToken> processJWTAuthHeader(final HttpServletRequest request) {

        // Extract authentication credentials
        final String authentication = request.getHeader(ContainerRequest.AUTHORIZATION);

        final Optional<JWToken> jwToken = internalProcessAuthHeaderFromJWT(authentication, request.getRemoteAddr(), false);
        final User user = jwToken.isPresent()  && jwToken.get().getActiveUser().isPresent() ?
                jwToken.get().getActiveUser().get() : null;

        if(user != null) {

            request.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
            request.setAttribute(com.liferay.portal.util.WebKeys.USER, user);
        }

        return jwToken;
    } // processAuthCredentialsFromJWT.

} // E:O:F:JsonWebTokenAuthCredentialProcessorImpl.
