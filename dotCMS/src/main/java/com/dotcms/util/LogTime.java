package com.dotcms.util;

import java.lang.annotation.*;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

/**
 * A method annotated with LogTime annotation will measure the execution time and log it
 * If the right appender is allowed
 * <p>This annotation serves as both a ByteBuddy advice marker and a CDI interceptor binding.
 * ByteBuddy instruments non-CDI classes at load-time, while the CDI interceptor fires at the
 * proxy boundary for managed beans. A shared lifecycle guard prevents double-processing.</p>
 * @author jsanca
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@InterceptorBinding
public @interface LogTime {

    @Nonbinding
    String loggingLevel() default "DEBUG";

} // E:O:F:LogTime
