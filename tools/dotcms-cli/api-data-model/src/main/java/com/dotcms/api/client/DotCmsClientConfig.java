package com.dotcms.api.client;

import io.smallrye.config.ConfigMapping;
import java.net.URI;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;


/**
 * Bean mean to collect rest client configurations
 */
@ConfigMapping(prefix = "dotcms.client")
public interface DotCmsClientConfig {
    @ConfigProperty(name = "servers")
    Map<String,URI> servers();
}
