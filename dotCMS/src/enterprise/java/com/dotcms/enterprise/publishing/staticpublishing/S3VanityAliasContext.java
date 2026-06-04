package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.languagesmanager.model.Language;

import java.io.File;

/**
 * Operational context required to publish or remove vanity aliases.
 */
public final class S3VanityAliasContext {

    final S3VanityAliasLookup lookup;
    final String bucketName;
    final String bucketRegion;
    final String bucketPrefix;
    final Host host;
    final Language language;
    final File file;
    final AWSS3EndPointPublisher endpointPublisher;

    /**
     * Creates the operational context for vanity publish or unpublish.
     *
     * @param lookup logical key of the persisted mapping
     * @param bucketName S3 bucket name
     * @param bucketRegion S3 bucket region
     * @param bucketPrefix S3 bucket prefix
     * @param host page or static resource host
     * @param language page or static resource language
     * @param file physical file to publish or remove
     * @param endpointPublisher concrete S3 adapter
     */
    public S3VanityAliasContext(final S3VanityAliasLookup lookup, final String bucketName,
                                final String bucketRegion, final String bucketPrefix, final Host host,
                                final Language language, final File file,
                                final AWSS3EndPointPublisher endpointPublisher) {
        this.lookup = lookup;
        this.bucketName = bucketName;
        this.bucketRegion = bucketRegion;
        this.bucketPrefix = bucketPrefix;
        this.host = host;
        this.language = language;
        this.file = file;
        this.endpointPublisher = endpointPublisher;
    }
}
