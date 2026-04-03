package com.dotcms.business;

import java.lang.annotation.*;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

/**
 * A method annotated with WrapInTransaction annotation will handle the local transactional block
 * if a transaction has been not started in the current thread, it will be started automatically otherwise will reuse the same.
 * In addition if a new transaction is started, the connection will be automatically closed at the end of the transaction.
 * <p>This annotation serves as both a ByteBuddy advice marker and a CDI interceptor binding.
 * ByteBuddy instruments non-CDI classes at load-time, while the CDI interceptor fires at the
 * proxy boundary for managed beans. A shared lifecycle guard prevents double-processing.</p>
 *
 * @author jsanca
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@InterceptorBinding
public @interface WrapInTransaction {

    /**
     * By default in false, set to true if you want to start a new transaction (externalize the transaction)
     * @return boolean
     */
    @Nonbinding
    boolean externalize() default false;
} // E:O:F:WrapInTransaction
