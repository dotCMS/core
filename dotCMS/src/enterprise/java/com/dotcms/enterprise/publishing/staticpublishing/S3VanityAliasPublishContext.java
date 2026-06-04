package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.languagesmanager.model.Language;

/**
 * Operational context required to publish a Vanity URL clone.
 */
public final class S3VanityAliasPublishContext {

    final String endpointId;
    final String bucketName;
    final String bucketRegion;
    final String bucketPrefix;
    final Host host;
    final Language language;
    final AWSS3EndPointPublisher endpointPublisher;

    /**
     * Creates the operational context for Vanity URL clone publishing.
     *
     * @param endpointId publishing endpoint identifier
     * @param bucketName S3 bucket name
     * @param bucketRegion S3 bucket region
     * @param bucketPrefix S3 bucket prefix
     * @param host Vanity URL site
     * @param language Vanity URL language
     * @param endpointPublisher concrete S3 adapter
     */
    public S3VanityAliasPublishContext(final String endpointId, final String bucketName,
                                       final String bucketRegion, final String bucketPrefix,
                                       final Host host, final Language language,
                                       final AWSS3EndPointPublisher endpointPublisher) {
        this.endpointId = endpointId;
        this.bucketName = bucketName;
        this.bucketRegion = bucketRegion;
        this.bucketPrefix = bucketPrefix;
        this.host = host;
        this.language = language;
        this.endpointPublisher = endpointPublisher;
    }
}
