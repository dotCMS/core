package com.dotcms.enterprise.publishing.staticpublishing;

import java.io.File;

/**
 * Holds a live dotCMS resource materialized for restoring an obscured S3 key.
 */
public final class S3VanityRestoreResult {

    final S3VanityResolvedTarget target;
    final File file;

    /**
     * Creates a restore result for a resolved live resource.
     *
     * @param target resolved live resource
     * @param file file to publish on S3
     */
    public S3VanityRestoreResult(final S3VanityResolvedTarget target, final File file) {
        this.target = target;
        this.file = file;
    }
}
