package com.dotcms.api.provider;

import com.dotcms.api.BuiltVersionService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.logging.Logger;

@RequestScoped
public class BuildVersionHeaders implements ClientHeadersFactory {

    @Inject
    Logger logger;

    @Inject
    BuiltVersionService builtVersionService;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        builtVersionService.version().ifPresentOrElse(version -> {
            clientOutgoingHeaders.add("X-DotCli-Build-Name", version.name());
            clientOutgoingHeaders.add("X-DotCli-Build-Version", version.version());
            clientOutgoingHeaders.add("X-DotCli-Build-Timestamp", String.valueOf(version.timestamp()));
            clientOutgoingHeaders.add("X-DotCli-Build-Revision", version.revision());
        }, () -> logger.error("Unable to get the version from the built version service."));

        return clientOutgoingHeaders;
    }
}
