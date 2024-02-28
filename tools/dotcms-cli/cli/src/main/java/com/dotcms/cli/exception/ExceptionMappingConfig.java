package com.dotcms.cli.exception;

import io.smallrye.config.ConfigMapping;
import java.util.Map;

@ConfigMapping(prefix = "dotcms.status.conf")
public interface ExceptionMappingConfig {

    boolean override();

    Map<Integer, String> messages();

    String fallback();
}
