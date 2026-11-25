package com.dotcms.saml.service.handler;

import com.dotcms.saml.IdentityProviderConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Encapsulates the Authentication Handler, could be POST or Redirect (default)
 * @author jsanca
 */
public interface LogoutHandler {

    /**
     * Handles the authentication method
     * @param request    HttpServletRequest
     * @param response  {@link HttpServletResponse}
     * @param nameID    {@link Object}
     * @param sessionIndexValue {@link String}
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     */
    void handle(final HttpServletRequest  request,
                final HttpServletResponse response,
                final Object nameID,
                final String sessionIndexValue,
                final IdentityProviderConfiguration identityProviderConfiguration);
}
