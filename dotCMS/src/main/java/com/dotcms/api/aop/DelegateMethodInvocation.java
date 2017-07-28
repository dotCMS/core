package com.dotcms.api.aop;

import java.io.Serializable;
import java.lang.reflect.Method;

public interface DelegateInvocation<T> extends Serializable {

    /**
     * The
     * @return
     */
    Method getMethod();

    /**
     *
     * @return
     */
    Object[] getArguments();

    /**
     * Proceeds to the next interceptor in the chain
     * @return Object usually the method
     * @throws Throwable
     */
    Object proceed() throws Throwable;

    /**
     * Returns the instance that holds the method.
     * @return T
     */
    T getTarget();

} // E:O:F:DelegateInvocation.
