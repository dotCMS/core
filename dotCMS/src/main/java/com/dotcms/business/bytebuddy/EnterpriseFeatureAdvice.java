package com.dotcms.business.bytebuddy;

import com.dotcms.business.interceptor.EnterpriseFeatureHandler;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.util.EnterpriseFeature;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

/**
 * ByteBuddy advice for {@link EnterpriseFeature}. Delegates to
 * {@link EnterpriseFeatureHandler} for the actual logic, keeping the implementation DRY
 * with the CDI interceptor.
 *
 * @author Jose Castro
 * @since Jan 23rd, 2024
 */
public class EnterpriseFeatureAdvice {

    @Advice.OnMethodEnter
    static void enter(final @Advice.Origin Method method) {
        final EnterpriseFeature annotation = method.getAnnotation(EnterpriseFeature.class);
        final LicenseLevel licenseLevel = annotation.licenseLevel();
        final String errorMsg = annotation.errorMsg();
        EnterpriseFeatureHandler.checkLicense(licenseLevel.level, errorMsg);
    }
}
