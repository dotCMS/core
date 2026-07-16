package com.dotcms.business.interceptor;

/**
 * Abstraction over license-level checks. Decouples the interceptor/advice layer from
 * {@code LicenseUtil} so that the annotations and handlers can eventually live in a
 * utility module.
 *
 * <p>The core module provides a real implementation; the default no-op always returns
 * {@code Integer.MAX_VALUE} (all features enabled).</p>
 */
public interface LicenseOps {

    /**
     * Returns the current license level as an integer. Higher values indicate more
     * permissive licenses (e.g. 100 = Community, 300 = Professional, 500 = Platform).
     */
    int getLicenseLevel();
}
