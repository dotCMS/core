package com.dotcms.business.interceptor;

import com.dotcms.enterprise.LicenseUtil;

/**
 * Core implementation of {@link LicenseOps} that delegates to {@link LicenseUtil}.
 * This class stays in the core module when the SPI interfaces and handlers are extracted
 * to a utility module.
 */
public final class CoreLicenseOps implements LicenseOps {

    public static final CoreLicenseOps INSTANCE = new CoreLicenseOps();

    private CoreLicenseOps() { }

    @Override
    public int getLicenseLevel() {
        return LicenseUtil.getLevel();
    }
}
