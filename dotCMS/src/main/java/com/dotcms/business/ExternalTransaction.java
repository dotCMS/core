package com.dotcms.business;

import java.lang.annotation.*;
import javax.interceptor.InterceptorBinding;

/**
 * A method annotated with ExternalTransaction annotation will handle the local transactional block
 * This means if there is already a transaction in the current thread, it will start a new one, keeping the current one open.
 * As soon as the external transaction is completed, the current transaction will be set to the current thread and continue with the
 * original transaction.
 * <p>This annotation serves as both a ByteBuddy advice marker and a CDI interceptor binding.
 * ByteBuddy instruments non-CDI classes at load-time, while the CDI interceptor fires at the
 * proxy boundary for managed beans. A shared lifecycle guard prevents double-processing.</p>
 * @author jsanca
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@InterceptorBinding
public @interface ExternalTransaction {
}
