package com.dotcms.business.interceptor;

import com.dotcms.enterprise.license.DotInvalidLicenseException;

/**
 * Shared handler for {@code @EnterpriseFeature} logic. Used by both the ByteBuddy advice
 * and the CDI interceptor to keep the implementation DRY.
 */
public final class EnterpriseFeatureHandler {

    private EnterpriseFeatureHandler() { }

    /**
     * Checks that the current license level meets the required level.
     *
     * @param requiredLevel the minimum license level (e.g. 300 for Professional)
     * @param errorMsg      the error message for the exception
     * @throws DotInvalidLicenseException when the license is insufficient
     */
    public static void checkLicense(final int requiredLevel, final String errorMsg) {
        final int currentLevel = InterceptorServiceProvider.getLicenseOps().getLicenseLevel();
        if (currentLevel < requiredLevel) {
            throw new DotInvalidLicenseException(errorMsg);
        }
    }
}
