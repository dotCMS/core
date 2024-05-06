package com.dotcms.filters.interceptor.saml;

import com.dotcms.saml.IdentityProviderConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Implements a relay state strategy to generate the value
 * @author jsanca
 */
@FunctionalInterface
public interface RelayStateStrategy {

    String apply (final HttpServletRequest request,
                  final HttpServletResponse response,
                  final IdentityProviderConfiguration identityProviderConfiguration);
}
