package com.dotcms.api.provider;

import com.dotcms.api.AuthenticationContext;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.logging.Logger;


/**
 * Microprofile Provided that injects the authentication header into every api-request.
 */
@RequestScoped
public class DotCMSClientHeaders implements ClientHeadersFactory {

    @Inject
    Logger logger;

    @Inject
    AuthenticationContext authenticationContext;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> mm1,
            MultivaluedMap<String, String> mm2) {

        authenticationContext.getToken().ifPresentOrElse(token -> mm2.add("Authorization", "Bearer  " + new String(token)),
                () -> {
                    logger.error("Unable to get a valid token from the authentication context.");
                }
        );

        return mm2;
    }
}