package com.dotcms.enterprise.publishing.staticpublishing;

/**
 * Minimal context required to remove vanity aliases already materialized on S3.
 */
public final class S3VanityAliasCleanupContext {

    final String endpointId;
    final AWSS3EndPointPublisher endpointPublisher;

    /**
     * Creates the cleanup context for a static endpoint.
     *
     * @param endpointId static endpoint identifier
     * @param endpointPublisher concrete S3 adapter
     */
    public S3VanityAliasCleanupContext(final String endpointId,
                                       final AWSS3EndPointPublisher endpointPublisher) {
        this.endpointId = endpointId;
        this.endpointPublisher = endpointPublisher;
    }
}
