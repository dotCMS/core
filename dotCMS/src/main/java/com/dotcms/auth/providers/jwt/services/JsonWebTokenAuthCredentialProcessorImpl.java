package com.dotcms.auth.providers.jwt.services;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.ContainerRequest;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Default implementation
 * @author jsanca
 */
public class JsonWebTokenAuthCredentialProcessorImpl implements JsonWebTokenAuthCredentialProcessor {

    public static final String BEARER = "Bearer ";
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


    @Override
    public User processAuthCredentialsFromJWT(final String authorizationHeader,
                                              final HttpSession httpSession) {

        return this.doProcessAuthenticationCredentialsFromJsonWebToken(authorizationHeader, null, httpSession);
    } // processAuthCredentialsFromJWT.

    private User doProcessAuthenticationCredentialsFromJsonWebToken(final String authorizationHeader,
                                                                    final HttpServletRequest request,
                                                                    final HttpSession httpSession) {

        final String jsonWebToken;
        User user = null;

        if (StringUtils.isNotEmpty(authorizationHeader) && authorizationHeader.trim().startsWith(BEARER)) {

            jsonWebToken = authorizationHeader.substring(BEARER.length());

            if(!UtilMethods.isSet(jsonWebToken)) {
                // "Invalid syntax for username and password"
                throw new SecurityException("Invalid Json Web Token", Response.Status.BAD_REQUEST);
            }

            try {
                user = this.jsonWebTokenUtils.getUser(jsonWebToken.trim());
            } catch (Exception e) {
                this.jsonWebTokenUtils.handleInvalidTokenExceptions(this.getClass(), e, null, null);
            }

            if(!UtilMethods.isSet(user)) {
                // "Invalid syntax for username and password"
                throw new SecurityException("Invalid Json Web Token", Response.Status.BAD_REQUEST);
            }

            if (null != httpSession && null != request) {

                final HttpSession newSession = APILocator.getLoginServiceAPI().preventSessionFixation(request);
                if (null != newSession) {
                    newSession.setAttribute(WebKeys.CMS_USER, user);
                    newSession.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
                }
            }
        }

        return user;
    } // doProcessAuthenticationCredentialsFromJsonWebToken.

    @Override
    public User processAuthCredentialsFromJWT(final HttpServletRequest request) {

        // Extract authentication credentials
        final String authentication = request.getHeader(ContainerRequest.AUTHORIZATION);
        final HttpSession session   = request.getSession();
        return this.doProcessAuthenticationCredentialsFromJsonWebToken(authentication, request, session);
    } // doProcessAuthenticationCredentialsFromJsonWebToken.

} // E:O:F:JsonWebTokenAuthCredentialProcessorImpl.
