package com.dotcms.business.bytebuddy;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.DotInvalidLicenseException;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.util.EnterpriseFeature;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

/**
 * This Advice class handles the behavior of the @{@link EnterpriseFeature} Annotation.
 *
 * @author Jose Castro
 * @since Jan 23rd, 2024
 */
public class EnterpriseFeatureAdvice {

    /**
     * Checks that the specified Enterprise License level requirement is met. It allows for all
     * License levels that are equal or greater than the one set in the Annotation.
     *
     * @param method The method that has been annotated with @{@link EnterpriseFeature}
     */
    @Advice.OnMethodEnter
    static void enter(final @Advice.Origin Method method) {
        final LicenseLevel licenseLevel =
                method.getAnnotation(EnterpriseFeature.class).licenseLevel();
        final String errorMsg = method.getAnnotation(EnterpriseFeature.class).errorMsg();
        final int currenLicenseLevel = LicenseUtil.getLevel();
        if (currenLicenseLevel < licenseLevel.level) {
            throw new DotInvalidLicenseException(errorMsg);
        }
    }

}
