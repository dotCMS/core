package com.dotcms.publishing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.publisher.business.DotPublisherException;
import org.junit.Test;

/**
 * @author Jonathan Gamba 2019-01-08
 */
public class BundlerUtilTest {

    @Test
    public void sanitize_bundle_name_no_changes() throws Exception {

        final String[] testBundleNames = {"bundle.zip", "bundle.tar.gz", "bundle", "bunDLE.zip",
                "Bundle.tar.gz", "bundle.bundle.bundle", "bundle.bundle.bundle.bundle"};

        for (final String testBundleName : testBundleNames) {
            String sanitizedName = BundlerUtil.sanitizeBundleName(testBundleName);
            assertNotNull(sanitizedName);
            assertEquals(sanitizedName, testBundleName);
        }
    }

    @Test
    public void sanitize_bundle_name_with_changes() throws Exception {

        final String bundleName = "bundle.zip";
        final String[] testBundleNames = {
                "/some/path/" + bundleName,
                "/another/random/path/" + bundleName,
                "another/random/path/" + bundleName};

        for (final String testBundleName : testBundleNames) {
            String sanitizedName = BundlerUtil.sanitizeBundleName(testBundleName);
            assertNotNull(sanitizedName);
            assertEquals(sanitizedName, bundleName);
        }
    }

    @Test(expected = DotPublisherException.class)
    public void sanitize_bundle_name_null_name() throws Exception {
        BundlerUtil.sanitizeBundleName(null);
    }

    @Test(expected = DotPublisherException.class)
    public void sanitize_bundle_name_empty_name() throws Exception {
        BundlerUtil.sanitizeBundleName("");
    }

}