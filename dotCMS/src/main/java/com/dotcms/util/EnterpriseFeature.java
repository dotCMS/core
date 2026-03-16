package com.dotcms.util;

import com.dotcms.enterprise.license.LicenseLevel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

/**
 * This Annotation allows developers to check whether the current dotCMS instance is running with a
 * specific Enterprise License level or not. By default, this Annotation will check for a
 * Professional License -- level 300.
 * <p>This annotation serves as both a ByteBuddy advice marker and a CDI interceptor binding.
 * ByteBuddy instruments non-CDI classes at load-time, while the CDI interceptor fires at the
 * proxy boundary for managed beans. A shared lifecycle guard prevents double-processing.</p>
 *
 * @author Jose Castro
 * @since Jan 23rd, 2024
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@InterceptorBinding
public @interface EnterpriseFeature {

    /**
     * Sets the dotCMS License level to check for.
     *
     * @return The current License Level, or the Professional License level by default.
     */
    @Nonbinding
    LicenseLevel licenseLevel() default LicenseLevel.PROFESSIONAL;

    /**
     * Sets the specific error message that will be returned if the current dotCMS instance lower
     * than the one that has been specified
     *
     * @return The specified error message, or the default one.
     */
    @Nonbinding
    String errorMsg() default "This feature is only available in an Enterprise version of dotCMS";

}
