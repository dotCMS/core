package com.dotcms.config;

public abstract class AbstractOrderedConfigurationProvider implements ConfigurationProvider {

    private int order = 0;


    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public ConfigurationProvider suggestOrder(final int suggestedOrder) {

        this.order = suggestedOrder;
        return this;
    }


}
