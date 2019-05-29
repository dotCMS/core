package com.dotcms.aspects.aspectj;

import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.aspects.interceptors.MethodHookMethodInterceptor;
import com.dotcms.business.MethodHook;
import com.google.common.annotations.VisibleForTesting;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * This aspect handles the {@link MethodHook} with AspectJ
 * @author jsanca
 */
@Aspect
public class MethodHookAspect {

    private final MethodInterceptor<Object> interceptor;

    public MethodHookAspect() {
        this( MethodHookMethodInterceptor.INSTANCE);
    }

    @VisibleForTesting
    public MethodHookAspect(final MethodInterceptor<Object> interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * Aspect implementation for the {@link MethodHook} annotation
     *
     * @param point Joint point
     * @return The result of call
     * @throws Throwable If something goes wrong inside
     */
    @Around("execution(* *(..))"
            + " && @annotation(com.dotcms.business.MethodHook)")
    public Object invoke(final ProceedingJoinPoint point) throws Throwable {

        return
                this.interceptor.invoke
                        (new AspectJDelegateMethodInvocation<>(point));
    } // invoke.
} // E:O:F:MethodHookAspect.