package com.dotcms.auth.providers.jwt;

import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.rest.exception.SecurityException;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Optional;

/**
 * Process the Authentication Credentials
 * @author jsanca
 */
public interface JsonWebTokenAuthCredentialProcessor extends Serializable {


    String BEARER = "Bearer ";

    /**
     * Resolves the user of a JWT authorization header (it should start with the BEARER prefix) without
     * storing it anywhere: the session is not logged in by a token.
     * @param authorizationHeader {@link String}
     * @param httpSession {@link HttpSession} of the call, may be null; never written to
     * @param ipAddress the client address the token's network restriction is checked against
     * @return the token's {@link User}, or null
     */
    User processAuthHeaderFromJWT(final String authorizationHeader, final HttpSession httpSession, final String ipAddress);

    /**
     * Process the authentication credentials based on the jwt authorization header (it should starts with BEARER prefix)
     * this Authentication will only support {@link com.dotcms.auth.providers.jwt.beans.ApiToken},
     * if {@link com.dotcms.auth.providers.jwt.beans.UserToken} is sent on the header a 400, BAD REQUEST will be returned
     * @param request {@link HttpServletRequest}
     * @return User
     */
    User processAuthHeaderFromJWT(final HttpServletRequest request);

    /**
     * Process the authentication credentials based on the jwt authorization header (it should starts with BEARER prefix)
     * this Authentication will only support {@link com.dotcms.auth.providers.jwt.beans.ApiToken},
     *
     * @param request {@link HttpServletRequest}
     * @return JWToken the JWT token or {@link Optional#empty()} if the token is invalid
     *
     * @throws SecurityException if the Token is not a valid API Token or if the header not have the BEARER prefix
     */
    Optional<JWToken> processJWTAuthHeader(final HttpServletRequest request);
} // E:O:F:JsonWebTokenAuthCredentialProcessor.
