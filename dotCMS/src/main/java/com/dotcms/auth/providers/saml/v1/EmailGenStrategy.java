package com.dotcms.auth.providers.saml.v1;

/**
 * Generates an unique email based on an existing one
 * @author jsanca
 */
@FunctionalInterface
public interface EmailGenStrategy {

    String apply (final String email);
}
