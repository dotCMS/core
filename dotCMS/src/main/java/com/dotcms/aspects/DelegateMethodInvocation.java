package com.dotcms.aspects;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Delegate to proceed a method invocation on an AOP context for a {@link MethodInterceptor}
 * @param <T>
 */
public interface DelegateMethodInvocation<T> extends Serializable {

    /**
     * Gets the methods intercepted
     * @return Method
     */
    Method getMethod();

    /**
     * Gets the method arguments
     * @return Object array
     */
    Object[] getArguments();

    /**
     * Proceeds to the next interceptor in the chain
     * @return Object usually the method
     */
    Object proceed() throws Throwable;

    /**
     * Returns the instance that holds the method.
     * @return T
     */
    T getTarget();

} // E:O:F:DelegateMethodInvocation.