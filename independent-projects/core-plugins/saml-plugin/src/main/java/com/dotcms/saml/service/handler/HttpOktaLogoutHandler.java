package com.dotcms.saml.service.handler;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.MessageObserver;
import com.dotcms.saml.SamlName;
import com.dotcms.saml.service.internal.SamlCoreService;
import org.apache.velocity.app.VelocityEngine;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Implements the Logout handler by Redirect for Okta
 * Okta when does normal logout only does in the associated app, but if wants to get logout on all apps, needs to use a custom logout endpoint
 * @author jsanca
 */
public class HttpOktaLogoutHandler implements LogoutHandler {

    private final SamlCoreService samlCoreService;
    private final VelocityEngine  velocityEngine;
    private final MessageObserver messageObserver;

    public HttpOktaLogoutHandler(final SamlCoreService samlCoreService,
                                 final VelocityEngine velocityEngine,
                                 final MessageObserver messageObserver) {

        this.samlCoreService = samlCoreService;
        this.velocityEngine  = velocityEngine;
        this.messageObserver = messageObserver;
    }

    @Override
    public void handle(final HttpServletRequest  request,
                       final HttpServletResponse response,
                       final Object nameID,
                       final String sessionIndexValue,
                       final IdentityProviderConfiguration identityProviderConfiguration) {

        this.messageObserver.updateInfo(this.getClass().getName(), "Processing saml logout Okta for nameID: " + nameID);
        // this takes the logout callback if any defined, otherwise uses show-logout
        final String logoutCallback = identityProviderConfiguration.containsOptionalProperty(SamlName.DOT_SAML_LOGOUT_SERVICE_ENDPOINT_URL.getPropertyName())?
                identityProviderConfiguration.getOptionalProperty(SamlName.DOT_SAML_LOGOUT_SERVICE_ENDPOINT_URL.getPropertyName()).toString():
                "/dotAdmin/show-logout";

        // this is a hack to force okta to do logout from the idp
        final String logoutPath = identityProviderConfiguration.getOptionalProperty("logout.okta.url").toString();

        try {

            final String redirectUrl = logoutPath + "?fromURI=" + logoutCallback;
            this.messageObserver.updateInfo(this.getClass().getName(), "Logout redirect: " + redirectUrl);
            response.sendRedirect(redirectUrl);
        } catch (IOException e) {

            this.messageObserver.updateError(this.getClass().getName(), e.getMessage(), e);
        }
    }
}
