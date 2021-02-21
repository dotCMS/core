package com.dotcms.config;

import java.util.HashMap;
import java.util.Map;

public class EnvConfigurationProvider extends AbstractOrderedConfigurationProvider {

    private final static String ENV_PREFIX="DOT_";


    @Override
    public Map<String, Object> getConfig() {

        final Map<String, Object> props = new HashMap<>();
        System.getenv().entrySet().stream().filter(e -> e.getKey().startsWith(ENV_PREFIX))
                .forEach(e -> props.put(e.getKey(), e.getValue()));

        return props;
    }
}
