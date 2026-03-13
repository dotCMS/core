package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.util.UtilMethods;

/**
 * Immutable value object returned by the nested-host URL resolution step.
 *
 * <p>When a request arrives for {@code example.com/en/nestedHost2/some/page},
 * the resolver maps it to:
 * <ul>
 *   <li>{@code resolvedHostId} = UUID of {@code nestedHost2}</li>
 *   <li>{@code remainingUri}   = {@code /some/page}</li>
 * </ul>
 * When no nested host is matched the {@code resolvedHostId} is {@code null}
 * and {@code remainingUri} equals the original request URI.</p>
 */
public final class HostResolutionResult {

    /** UUID of the most-specific matched nested host, or {@code null} when no nested host matched. */
    private final String resolvedHostId;

    /** The URI that should be used for content resolution after stripping any nested-host prefix. */
    private final String remainingUri;

    private HostResolutionResult(final String resolvedHostId, final String remainingUri) {
        this.resolvedHostId = resolvedHostId;
        this.remainingUri   = remainingUri;
    }

    /**
     * Creates a result indicating that the request was matched to a nested host.
     *
     * @param nestedHostId UUID of the nested host
     * @param remaining    URI after the nested-host path prefix was stripped
     * @return result with {@link #isNested()} == {@code true}
     */
    public static HostResolutionResult nested(final String nestedHostId, final String remaining) {
        if (!UtilMethods.isSet(nestedHostId)) {
            throw new IllegalArgumentException("nestedHostId must not be blank");
        }
        final String safeRemaining = UtilMethods.isSet(remaining) ? remaining : "/";
        return new HostResolutionResult(nestedHostId, safeRemaining);
    }

    /**
     * Creates a result indicating that the request belongs to the top-level host (no nested match).
     *
     * @param originalUri the original, unmodified request URI
     * @return result with {@link #isNested()} == {@code false}
     */
    public static HostResolutionResult topLevel(final String originalUri) {
        return new HostResolutionResult(null, originalUri);
    }

    /**
     * @return {@code true} when a nested host was matched
     */
    public boolean isNested() {
        return resolvedHostId != null;
    }

    /**
     * @return UUID of the resolved nested host, or {@code null} when none was matched
     */
    public String getResolvedHostId() {
        return resolvedHostId;
    }

    /**
     * @return the URI to use for content resolution; equals the original URI when
     *         {@link #isNested()} is {@code false}
     */
    public String getRemainingUri() {
        return remainingUri;
    }

    @Override
    public String toString() {
        return "HostResolutionResult{resolvedHostId='" + resolvedHostId
                + "', remainingUri='" + remainingUri + "'}";
    }
}
