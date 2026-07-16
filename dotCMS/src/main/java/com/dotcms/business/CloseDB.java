package com.dotcms.business;

import java.lang.annotation.*;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

/**
 * A method annotated with CloseDB annotation will close resources in the current thread if needed, such as database connections...
 * <p>This annotation serves as both a ByteBuddy advice marker and a CDI interceptor binding.
 * ByteBuddy instruments non-CDI classes at load-time, while the CDI interceptor fires at the
 * proxy boundary for managed beans. A shared lifecycle guard prevents double-processing.</p>
 * @author jsanca
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@InterceptorBinding
public @interface CloseDB {

    /**
     * By default in true, set to false if you do not want to close the connection hold on the current thread
     * @return boolean
     */
    @Nonbinding
    boolean connection() default true;

} // E:O:F:CloseDB
