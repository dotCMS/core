package com.dotcms.api.aop.guice;

import com.dotcms.api.aop.DelegateMethodInvocation;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

/**
 * Implementation for Guice AOP
 * @author jsanca
 */
public class GuiceDelegateMethodInvocation<T> implements DelegateMethodInvocation<T> {

    private final MethodInvocation methodInvocation;

    public GuiceDelegateMethodInvocation(final MethodInvocation methodInvocation) {

        this.methodInvocation = methodInvocation;
    }

    @Override
    public Method getMethod() {
        return this.methodInvocation.getMethod();
    }

    @Override
    public Object[] getArguments() {
        return this.methodInvocation.getArguments();
    }

    @Override
    public Object proceed() throws Throwable {
        return this.methodInvocation.proceed();
    }

    @Override
    public T getTarget() {
        return (T) this.methodInvocation.getThis();
    }
} // E:O:F:GuiceDelegateMethodInvocation.
