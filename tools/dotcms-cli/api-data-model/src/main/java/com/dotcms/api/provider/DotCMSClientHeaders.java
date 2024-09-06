package com.dotcms.api.provider;

import com.dotcms.api.AuthenticationContext;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.Optional;


/**
 * Microprofile Provided that injects the authentication header into every api-request.
 */
@RequestScoped
public class DotCMSClientHeaders extends BuildVersionHeaders {

    @Inject
    AuthenticationContext authenticationContext;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        final MultivaluedMap<String, String> versionedOutgoingHeaders = super.update(incomingHeaders, clientOutgoingHeaders);

        final Optional<char[]> contextToken;
        try {
            contextToken = authenticationContext.getToken();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get token from authentication context ",e);
        }

        contextToken.ifPresentOrElse(token -> versionedOutgoingHeaders.add("Authorization", "Bearer  " + new String(token)),
                () -> logger.error("Unable to get a valid token from the authentication context.")
        );

        return versionedOutgoingHeaders;
    }
}