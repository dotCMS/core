package com.dotcms.saml.service.handler;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Encapsulates the Authentication Handler, could be POST or Redirect (default)
 * This is the class in charge of creating the authentication request, it could be GET, POST or POST_RAW
 * @author jsanca
 */
public interface AuthenticationHandler {

    /**
     * Handles the authentication method
     * @param request    HttpServletRequest
     * @param response  {@link HttpServletResponse}
     * @param idpConfig {@link IdentityProviderConfiguration}
     */
    void handle(final HttpServletRequest request, final HttpServletResponse response,
                final IdentityProviderConfiguration idpConfig, final String relayState);

}
