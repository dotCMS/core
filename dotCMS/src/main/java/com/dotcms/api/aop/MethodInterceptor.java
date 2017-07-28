package com.dotcms.api.aop;

import java.io.Serializable;

/**
 * The interceptor handles the invoke to a method on an AOP context.
 *
 * @param <T>
 */
public interface MethodInterceptor<T> extends Serializable {

    /**
     * This method will be called when the matcher catch the invoke to the target method.
     * @param delegate DelegateMethodInvocation
     * @return Object object to return as part of the method invoke.
     */
    Object invoke (final DelegateMethodInvocation<T> delegate) throws Throwable;

} // E:O:F:MethodInterceptor.
