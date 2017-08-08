package com.dotcms.aspects.aspectj;

import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.aspects.interceptors.LocalTransactionalMethodInterceptor;
import com.dotcms.business.LocalTransactional;
import com.google.common.annotations.VisibleForTesting;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * This aspect handles the @{@link LocalTransactional} with AspectJ
 * @author jsanca
 */
@Aspect
public class LocalTransactionalAspect {

    private final MethodInterceptor<Object> interceptor;

    public LocalTransactionalAspect() {
        this(LocalTransactionalMethodInterceptor.INSTANCE);
    }

    @VisibleForTesting
    public LocalTransactionalAspect(MethodInterceptor<Object> interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * Aspect implementation for the {@link LocalTransactional} annotation
     *
     * @param point Joint point
     * @return The result of call
     * @throws Throwable If something goes wrong inside
     */
    @Around("execution(* *(..))"
            + " && @annotation(com.dotcms.business.LocalTransactional)")
    public Object invoke(final ProceedingJoinPoint point) throws Throwable {

        return
                this.interceptor.invoke
                        (new AspectJDelegateMethodInvocation<Object>(point));
    } // invoke.
} // E:O:F:LogTimeAspect.