package com.dotcms.aspects.aspectj;

import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.aspects.interceptors.CloseDBMethodInterceptor;
import com.dotcms.business.CloseDB;
import com.google.common.annotations.VisibleForTesting;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * This aspect handles the @{@link CloseDB} with AspectJ
 * @author jsanca
 */
@Aspect
public class CloseDBAspect {

    private final MethodInterceptor<Object> interceptor;

    public CloseDBAspect() {
        this(CloseDBMethodInterceptor.INSTANCE);
    }

    @VisibleForTesting
    public CloseDBAspect(final MethodInterceptor<Object> interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * Aspect implementation for the {@link CloseDB} annotation
     *
     * @param point Joint point
     * @return The result of call
     * @throws Throwable If something goes wrong inside
     */
    @Around("execution(* *(..))"
            + " && @annotation(com.dotcms.business.CloseDB)")
    public Object invoke(final ProceedingJoinPoint point) throws Throwable {

        return
                this.interceptor.invoke
                        (new AspectJDelegateMethodInvocation<Object>(point));
    } // invoke.
} // E:O:F:LogTimeAspect.