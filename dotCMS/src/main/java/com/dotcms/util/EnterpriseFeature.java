package com.dotcms.util;

import com.dotcms.enterprise.license.LicenseLevel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This Annotation allows developers to check whether the current dotCMS instance is running with a
 * specific Enterprise License level or not. By default, this Annotation will check for a
 * Professional License -- level 300.
 *
 * @author Jose Castro
 * @since Jan 23rd, 2024
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EnterpriseFeature {

    /**
     * Sets the dotCMS License level to check for.
     *
     * @return The current License Level, or the Professional License level by default.
     */
    LicenseLevel licenseLevel() default LicenseLevel.PROFESSIONAL;

    /**
     * Sets the specific error message that will be returned if the current dotCMS instance lower
     * than the one that has been specified
     *
     * @return The specified error message, or the default one.
     */
    String errorMsg() default "This feature is only available in an Enterprise version of dotCMS";

}