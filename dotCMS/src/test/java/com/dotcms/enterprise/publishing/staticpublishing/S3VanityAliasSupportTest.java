package com.dotcms.enterprise.publishing.staticpublishing;

import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies vanity path normalization for S3 materialization.
 */
public class S3VanityAliasSupportTest {

    @Test
    public void materializedVanityUsesLiteralVanityPathForEverySourceType() {
        final S3VanityAliasSupport support = new S3VanityAliasSupport();

        Assert.assertEquals("/home", support.materializeVanityPath("/home", S3VanityTargetType.PAGE).get());
        Assert.assertEquals("/home", support.materializeVanityPath("/home", S3VanityTargetType.PAGE_URL_MAP).get());
        Assert.assertEquals("/home", support.materializeVanityPath("/home", S3VanityTargetType.PAGE_INDEX).get());
        Assert.assertEquals("/home/",
                support.materializeVanityPath("/home/", S3VanityTargetType.PAGE_INDEX).get());
    }
}
