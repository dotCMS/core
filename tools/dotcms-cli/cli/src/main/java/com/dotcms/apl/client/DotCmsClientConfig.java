package com.dotcms.apl.client;

import io.smallrye.config.ConfigMapping;
import java.net.URI;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;


@ConfigMapping(prefix = "dotcms.client")
public interface DotCmsClientConfig {
    @ConfigProperty(name = "servers")
    Map<String,URI> servers();
}
