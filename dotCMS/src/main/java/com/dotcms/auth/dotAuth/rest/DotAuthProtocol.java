package com.dotcms.auth.dotAuth.rest;

/**
 * Authentication protocol handled by the dotAuth portlet. Used as the
 * discriminator on {@link DotAuthConfigView} and {@link DotAuthConfigForm}.
 */
public enum DotAuthProtocol {

    /** OAuth 2.0 / OIDC — secrets stored under the {@code dotAuth} app key. */
    OAUTH,

    /** SAML 2.0 — secrets stored under the {@code dotsaml-config} app key. */
    SAML
}
