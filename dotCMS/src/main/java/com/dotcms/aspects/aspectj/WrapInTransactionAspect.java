package com.dotcms.aspects.aspectj;

import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.aspects.interceptors.WrapInTransactionMethodInterceptor;
import com.dotcms.business.WrapInTransaction;
import com.google.common.annotations.VisibleForTesting;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * This aspect handles the @{@link WrapInTransaction} with AspectJ
 * @author jsanca
 */
@Aspect
public class WrapInTransactionAspect {

    private final MethodInterceptor<Object> interceptor;

    public WrapInTransactionAspect() {
        this(WrapInTransactionMethodInterceptor.INSTANCE);
    }

    @VisibleForTesting
    public WrapInTransactionAspect(final MethodInterceptor<Object> interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * Aspect implementation for the {@link WrapInTransaction} annotation
     *
     * @param point Joint point
     * @return The result of call
     * @throws Throwable If something goes wrong inside
     */
    @Around("execution(* *(..))"
            + " && @annotation(com.dotcms.business.WrapInTransaction)")
    public Object invoke(final ProceedingJoinPoint point) throws Throwable {

        return
                this.interceptor.invoke
                        (new AspectJDelegateMethodInvocation<Object>(point));
    } // invoke.
} // E:O:F:LogTimeAspect.