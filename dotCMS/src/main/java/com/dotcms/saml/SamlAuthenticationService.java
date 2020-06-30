package com.dotcms.saml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Writer;
import java.util.Map;

/**
 * Provides Open SAML Authentication Service; login, logout, authentication, etc.
 *
 * @author jsanca
 */
public interface SamlAuthenticationService {

    String NO_REPLY = "no-reply";
    String NO_REPLY_DOTCMS_COM = "@no-reply.dotcms.com";
    String AT_SYMBOL = "@";
    String AT_ = "_at_";
    String SAML_NAME_ID = "SAMLNameID";
    String SAML_SESSION_INDEX = "SAMLSessionIndex";


    void initService(Map<String, Object> context);

    /**
     * Determine if the request is a valid SAML request depending on the
     * siteName configuration
     *
     * @param request  {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return boolean
     */
     boolean isValidSamlRequest(final HttpServletRequest request, final HttpServletResponse response,
                                final IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Do the authentication with SAML
     *
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     */
    void authentication(final HttpServletRequest request, final HttpServletResponse response,
                        final IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Do the logout call for SAML
     *
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param nameID   {@link Object} represents the name id stored usually on the session.
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     */
    void logout(final HttpServletRequest request, final HttpServletResponse response,
                final Object nameID, final String sessionIndexValue,
                final IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Pre: the request parameter SAML_ART_PARAM_KEY must exists
     * Resolve the assertion by making a call to the idp.
     *
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param identityProviderConfiguration {@link IdentityProviderConfiguration}
     * @return Attributes
     */
    Attributes resolveAttributes(final HttpServletRequest request, final HttpServletResponse response,
                                 final IdentityProviderConfiguration identityProviderConfiguration);

    /**
     * Render the metadata as a XML
     * @param writer
     * @param identityProviderConfiguration
     */
    void renderMetadataXML(final Writer writer,
                           final IdentityProviderConfiguration identityProviderConfiguration);
}
