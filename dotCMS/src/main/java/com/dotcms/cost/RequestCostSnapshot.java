package com.dotcms.cost;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

/**
 * Immutable payload shipped to the external request-cost (a.k.a. request token) collection
 * endpoint on each scheduled tick. One snapshot = one point in the time series.
 */
@JsonAutoDetect(
        fieldVisibility = Visibility.PUBLIC_ONLY,
        getterVisibility = Visibility.NONE,
        isGetterVisibility = Visibility.NONE)
public final class RequestCostSnapshot {

    public final String clusterId;
    public final String serverId;
    public final String timestamp;
    public final int windowSeconds;
    public final long windowRequests;
    public final double windowTokens;
    public final double windowAvgTokensPerRequest;
    public final long lifetimeRequests;
    public final double lifetimeTokens;
    public final double lifetimeAvgTokensPerRequest;

    public RequestCostSnapshot(
            final String clusterId,
            final String serverId,
            final String timestamp,
            final int windowSeconds,
            final long windowRequests,
            final double windowTokens,
            final double windowAvgTokensPerRequest,
            final long lifetimeRequests,
            final double lifetimeTokens,
            final double lifetimeAvgTokensPerRequest) {
        this.clusterId = clusterId;
        this.serverId = serverId;
        this.timestamp = timestamp;
        this.windowSeconds = windowSeconds;
        this.windowRequests = windowRequests;
        this.windowTokens = windowTokens;
        this.windowAvgTokensPerRequest = windowAvgTokensPerRequest;
        this.lifetimeRequests = lifetimeRequests;
        this.lifetimeTokens = lifetimeTokens;
        this.lifetimeAvgTokensPerRequest = lifetimeAvgTokensPerRequest;
    }
}
