package com.dotcms.config;


import java.util.Map;

/**
 * This class is in charge of recovery a map  of configuration
 * @author jsanca
 */
public interface ConfigurationProvider extends Comparable<ConfigurationProvider> {

    default String getName() {
        return this.getClass().getName();
    }
    Map<String, Object> getConfig();

    default int getOrder() {
        return -1;
    }

    default ConfigurationProvider suggestOrder (final int suggestedOrder) {

        return this;
    }

    @Override
    default int compareTo(final ConfigurationProvider provider) {

        return this.getOrder() - provider.getOrder();
    }
}
