package com.dotmarketing.util;

import com.google.common.annotations.VisibleForTesting;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This services encapsulates the environment variables and allows
 * for a better testing to override, add environment variables.
 * @author jsanca
 */
public class EnvironmentVariablesService {

    private static class SingletonHolder {
        private static final EnvironmentVariablesService INSTANCE = new EnvironmentVariablesService();
    }
    /**
     * Get the instance.
     * @return EnvironmentVariablesService
     */
    public static EnvironmentVariablesService getInstance() {

        return EnvironmentVariablesService.SingletonHolder.INSTANCE;
    } // getInstance.

    private final Map<String, String> envMap = new ConcurrentHashMap<>();
    private EnvironmentVariablesService() {

        System.getenv().entrySet().stream()
                .forEach(e -> envMap.put(e.getKey(), e.getValue()));
    }

    @VisibleForTesting
    public EnvironmentVariablesService put (final String envKey, final String envValue) {

        // todo: add condition if profile is test, allows
        Optional.ofNullable(envValue).ifPresent(v -> this.envMap.put(envKey, envValue));
        return this;
    }

    /**
     * Returns the environment map
     * @return Map
     */
    public Map<String, String> getenv() {

        return this.envMap;
    }


}
