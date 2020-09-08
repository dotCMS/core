package com.dotcms.auth.providers.jwt;

import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;

/**
 * Process the Authentication Credentials
 * @author jsanca
 */
public interface JsonWebTokenAuthCredentialProcessor extends Serializable {


    String BEARER = "Bearer ";

    /**
     * Process the authentication credentials based on the jwt authorization header (it should starts with BEARER prefix)
     * @param authorizationHeader {@link String}
     * @param httpSession {@link HttpSession}
     * @return User
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
} // E:O:F:JsonWebTokenAuthCredentialProcessor.
