package com.dotcms.business.interceptor;

/**
 * Shared handler for {@code @EnterpriseFeature} logic. Used by both the ByteBuddy advice
 * and the CDI interceptor to keep the implementation DRY.
 */
public final class EnterpriseFeatureHandler {

    private EnterpriseFeatureHandler() { }

    /**
     * Checks that the current license level meets the required level. Throws a
     * {@code RuntimeException} if the check fails.
     *
     * @param requiredLevel the minimum license level (e.g. 300 for Professional)
     * @param errorMsg      the error message for the exception
     * @throws RuntimeException wrapping a {@code DotInvalidLicenseException} when the
     *                          license is insufficient. The concrete exception type depends
     *                          on the core implementation; when running with no-op defaults,
     *                          a plain {@code RuntimeException} is thrown.
     */
    public static void checkLicense(final int requiredLevel, final String errorMsg) {
        final int currentLevel = InterceptorServiceProvider.getLicenseOps().getLicenseLevel();
        if (currentLevel < requiredLevel) {
            throw new InsufficientLicenseException(errorMsg);
        }
    }

    /**
     * Exception thrown when the license level is insufficient. This is a plain
     * {@code RuntimeException} subclass that lives in the interceptor package so there is
     * no dependency on the core {@code DotInvalidLicenseException}. The core implementation
     * may choose to throw the more specific exception type instead.
     */
    public static class InsufficientLicenseException extends RuntimeException {
        public InsufficientLicenseException(String message) {
            super(message);
        }
    }
}
