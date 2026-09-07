package com.dotcms.ai.client;

/**
 * Enumeration representing different strategies for proxying AI client requests.
 *
 * <p>
 * This enum provides strategies for handling AI client requests. Each strategy is
 * associated with an implementation of the {@link AIClientStrategy} interface.
 * </p>
 *
 * <p>
 * The strategies can be used to customize the behavior of AI client interactions,
 * allowing for flexible handling of requests and responses.
 * </p>
 *
 * @author vico
 */
public enum AIProxyStrategy {

    DEFAULT(new AIDefaultStrategy());

    private final AIClientStrategy strategy;

    AIProxyStrategy(final AIClientStrategy strategy) {
        this.strategy = strategy;
    }

    public AIClientStrategy getStrategy() {
        return strategy;
    }

}
