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
    /**
     * Tokens consumed by work that ran outside any HTTP request — site-search reindexing,
     * scheduled publishing, remote/push publishing, content indexing, embedding generation.
     * <p>
     * Reported <strong>separately</strong> from {@code windowTokens} / {@code lifetimeTokens},
     * which remain request-only. Total cluster consumption is the sum of the two. Keeping them
     * apart means every field has one meaning: {@code windowTokens} still divides by
     * {@code windowRequests} to give {@code windowAvgTokensPerRequest}, which it would not if
     * background work were folded in.
     * <p>
     * Before these fields existed this cost was not merely unattributed, it was discarded —
     * {@code incrementCost} returned early when no request was on the thread, so reindexing and
     * scheduled publishing reached the collector as zero.
     */
    public final double windowJobTokens;
    public final double lifetimeJobTokens;

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
            final double lifetimeAvgTokensPerRequest,
            final double windowJobTokens,
            final double lifetimeJobTokens) {
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
        this.windowJobTokens = windowJobTokens;
        this.lifetimeJobTokens = lifetimeJobTokens;
    }
}
