package com.dotcms.aspects.aspectj;

import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.aspects.interceptors.MethodDecoratorMethodInterceptor;
import com.google.common.annotations.VisibleForTesting;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * This aspect handles the {@link com.dotcms.business.MethodDecorator} with AspectJ
 * @author jsanca
 */
@Aspect
public class MethodDecoratorAspect {

    private final MethodInterceptor<Object> interceptor;

    public MethodDecoratorAspect() {
        this( MethodDecoratorMethodInterceptor.INSTANCE);
    }

    @VisibleForTesting
    public MethodDecoratorAspect(final MethodInterceptor<Object> interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * Aspect implementation for the {@link com.dotcms.business.MethodDecorator} annotation
     *
     * @param point Joint point
     * @return The result of call
     * @throws Throwable If something goes wrong inside
     */
    @Around("execution(* *(..))"
            + " && @annotation(com.dotcms.business.MethodDecorator)")
    public Object invoke(final ProceedingJoinPoint point) throws Throwable {

        return
                this.interceptor.invoke
                        (new AspectJDelegateMethodInvocation<>(point));
    } // invoke.
} // E:O:F:MethodDecoratorAspect.