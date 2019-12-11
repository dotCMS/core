package com.dotcms.publisher.bundle.business;

import java.util.Set;

public class BundleDeleteResult {

    private final Set<String> failedBundleSet;
    private final Set<String> deleteBundleSet;

    public BundleDeleteResult(final Set<String> failedBundleSet,
                              final Set<String> deleteBundleSet) {

        this.failedBundleSet = failedBundleSet;
        this.deleteBundleSet = deleteBundleSet;
    }

    public Set<String> getFailedBundleSet() {
        return failedBundleSet;
    }

    public Set<String> getDeleteBundleSet() {
        return deleteBundleSet;
    }
}
