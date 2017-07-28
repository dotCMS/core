package com.dotcms.api.aop.aspectj;

import com.dotcms.api.aop.DelegateMethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * Implementation for Aspectj
 * @author jsanca
 */
public class AspectJDelegateMethodInvocation<T> implements DelegateMethodInvocation<T> {

    private final ProceedingJoinPoint joinPoint;


    public AspectJDelegateMethodInvocation(final ProceedingJoinPoint joinPoint) {

        this.joinPoint = joinPoint;
    }

    @Override
    public Method getMethod() {

        final Signature signature = this.joinPoint.getSignature();

        return (null != signature && signature instanceof MethodSignature)?
                MethodSignature.class.cast(signature).getMethod():null;
    }

    @Override
    public Object[] getArguments() {

        return this.joinPoint.getArgs();
    }

    @Override
    public Object proceed() throws Throwable {

        return this.joinPoint.proceed();
    }

    @Override
    public T getTarget() {

        return (T)joinPoint.getTarget();
    }
} // E:O:F:AspectJDelegateMethodInvocation.
