package com.dotcms.util;

import java.lang.annotation.*;

/**
 * A method annotated with LogTime annotation will measure the execution time and log it
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogTime {
} // E:O:F:LogTime
