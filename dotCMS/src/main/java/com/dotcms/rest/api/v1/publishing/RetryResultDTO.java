package com.dotcms.rest.api.v1.publishing;

/**
 * Simple DTO returned by PublishingRetryHelper on successful retry.
 * Contains the data needed to build the response view.
 *
 * @author hassandotcms
 * @since Feb 2026
 */
public class RetryResultDTO {

    private final String bundleId;
    private final boolean forcePush;
    private final String operation;
    private final String deliveryStrategy;
    private final int assetCount;

    public RetryResultDTO(
            final String bundleId,
            final boolean forcePush,
            final String operation,
            final String deliveryStrategy,
            final int assetCount) {
        this.bundleId = bundleId;
        this.forcePush = forcePush;
        this.operation = operation;
        this.deliveryStrategy = deliveryStrategy;
        this.assetCount = assetCount;
    }

    public String getBundleId() {
        return bundleId;
    }

    public boolean isForcePush() {
        return forcePush;
    }

    public String getOperation() {
        return operation;
    }

    public String getDeliveryStrategy() {
        return deliveryStrategy;
    }

    public int getAssetCount() {
        return assetCount;
    }
}
