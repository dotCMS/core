package com.dotcms.cli.common;

import io.smallrye.config.ConfigMapping;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ConfigMapping(prefix = "dotcms.client.status.conf")
public interface ExceptionMappingConfig {
    @ConfigProperty(name = "messages")
    Map<Integer, String> messages();


    @ConfigProperty(name = "defaultMessage")
    String defaultMessage();
    
}
