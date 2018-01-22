package com.dotcms.aspects.interceptors;

import com.dotcms.aspects.DelegateMethodInvocation;
import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.LogTimeUtil;

import java.lang.reflect.Method;

/**
 * Method handler for the {@link com.dotcms.util.LogTime} annotation aspect
 * @author jsanca
 */
public class LogTimeMethodInterceptor implements MethodInterceptor<Object> {

    public static final LogTimeMethodInterceptor INSTANCE = new LogTimeMethodInterceptor();
    private final transient LogTimeUtil logTimeUtil;

    public LogTimeMethodInterceptor() {
        this (LogTimeUtil.INSTANCE);
    }

    @VisibleForTesting
    protected LogTimeMethodInterceptor(final LogTimeUtil logTimeUtil) {
        this.logTimeUtil = logTimeUtil;
    }


    @Override
    public Object invoke(final DelegateMethodInvocation<Object> delegate) throws Throwable {

        final Object target  = delegate.getTarget();
        final Method method  = delegate.getMethod();

        return  this.logTimeUtil.logTime(
                delegate::proceed,
                () -> "Call for class: " +
                            ((null != target)? target.getClass().getName(): "Null" ) + "#" +
                            ((null != method)?method.getName():"Null")) ;
    } // invoke.
} // E:O:F:LogTimeMethodInterceptor.