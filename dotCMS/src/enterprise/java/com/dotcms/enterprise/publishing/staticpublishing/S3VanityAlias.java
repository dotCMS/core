package com.dotcms.enterprise.publishing.staticpublishing;

/**
 * Represents a vanity alias that has been materialized on a static endpoint.
 */
public final class S3VanityAlias {

    final String endpointId;
    final String hostId;
    final long languageId;
    final String canonicalPath;
    final String vanityPath;
    final String vanityUrlId;
    final String bucketName;
    final String bucketRegion;
    final String bucketPrefix;

    /**
     * Creates an immutable vanity alias mapping.
     *
     * @param endpointId publishing endpoint identifier
     * @param hostId host identifier
     * @param languageId language identifier
     * @param canonicalPath canonical S3 key
     * @param vanityPath vanity S3 key
     * @param vanityUrlId source Vanity URL identifier
     * @param bucketName bucket where the alias has been materialized
     * @param bucketRegion bucket region used during publishing
     * @param bucketPrefix bucket prefix used for the vanity key
     */
    public S3VanityAlias(final String endpointId, final String hostId, final long languageId,
                         final String canonicalPath, final String vanityPath, final String vanityUrlId,
                         final String bucketName, final String bucketRegion, final String bucketPrefix) {
        this.endpointId = endpointId;
        this.hostId = hostId;
        this.languageId = languageId;
        this.canonicalPath = canonicalPath;
        this.vanityPath = vanityPath;
        this.vanityUrlId = vanityUrlId;
        this.bucketName = bucketName;
        this.bucketRegion = bucketRegion;
        this.bucketPrefix = bucketPrefix;
    }
}
