package com.dotcms.api.provider;

import com.dotcms.api.AuthSecurityContext;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.logging.Logger;


/**
 * Microprofile Provided that injexcts the authentication header into every api-request.
 */
@RequestScoped
public class DotCMSClientHeaders implements ClientHeadersFactory {

    private static final Logger log = Logger.getLogger(DotCMSClientHeaders.class);

    @Inject
    AuthSecurityContext ctx;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> mm1,
            MultivaluedMap<String, String> mm2) {

        ctx.getToken().ifPresentOrElse(token -> mm2.add("Authorization", "Bearer  " + token),
                () -> {
                    log.error("Unable to get a valid token from the authentication context.");
                }
        );

        return mm2;
    }
}