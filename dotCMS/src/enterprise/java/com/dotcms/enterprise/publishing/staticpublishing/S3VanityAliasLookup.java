package com.dotcms.enterprise.publishing.staticpublishing;

/**
 * Uniquely identifies the vanity mapping associated with a static resource.
 */
public final class S3VanityAliasLookup {

    final String endpointId;
    final String hostId;
    final long languageId;
    final String canonicalPath;

    /**
     * Creates an immutable lookup key for the vanity mapping.
     *
     * @param endpointId publishing endpoint identifier
     * @param hostId host identifier
     * @param languageId language identifier
     * @param canonicalPath canonical resource path
     */
    public S3VanityAliasLookup(final String endpointId, final String hostId,
                               final long languageId, final String canonicalPath) {
        this.endpointId = endpointId;
        this.hostId = hostId;
        this.languageId = languageId;
        this.canonicalPath = canonicalPath;
    }
}
