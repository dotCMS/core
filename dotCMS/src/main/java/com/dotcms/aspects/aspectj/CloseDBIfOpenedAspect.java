package com.dotcms.aspects.aspectj;

import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.aspects.interceptors.CloseDBIfOpenedMethodInterceptor;
import com.dotcms.business.CloseDBIfOpened;
import com.google.common.annotations.VisibleForTesting;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * This aspect handles the @{@link CloseDBIfOpened} with AspectJ
 * @author jsanca
 */
@Aspect
public class CloseDBIfOpenedAspect {

    private final MethodInterceptor<Object> interceptor;

    public CloseDBIfOpenedAspect() {
        this(CloseDBIfOpenedMethodInterceptor.INSTANCE);
    }

    @VisibleForTesting
    public CloseDBIfOpenedAspect(final MethodInterceptor<Object> interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * Aspect implementation for the {@link CloseDBIfOpened} annotation
     *
     * @param point Joint point
     * @return The result of call
     * @throws Throwable If something goes wrong inside
     */
    @Around("execution(* *(..))"
            + " && @annotation(com.dotcms.business.CloseDBIfOpened)")
    public Object invoke(final ProceedingJoinPoint point) throws Throwable {

        return
                this.interceptor.invoke
                        (new AspectJDelegateMethodInvocation<Object>(point));
    } // invoke.
} // E:O:F:LogTimeAspect.