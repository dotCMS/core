package com.dotcms.api.aop;

import com.dotcms.repackage.org.apache.commons.lang.time.StopWatch;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;

import java.lang.reflect.Method;

/**
 * Method handler for the {@link com.dotcms.util.LogTime} annotation aspect
 * @author jsanca
 */
public class LogTimeMethodInterceptor implements MethodInterceptor<Object> {

    public static final LogTimeMethodInterceptor INSTANCE = new LogTimeMethodInterceptor();

    @Override
    public Object invoke(final DelegateMethodInvocation<Object> delegate) throws Throwable {

        Object methodReturn = null;
        final StopWatch stopWatch = new StopWatch();
        final Object target = delegate.getTarget();
        final Method method = delegate.getMethod();

        stopWatch.start();

        methodReturn = delegate.proceed();

        stopWatch.stop();

        Logger.debug(this, "Call for class: " +
                ((null != target)? target.getClass().getName(): "Null" ) +
                "#" + ((null != method)?method.getName():"Null") +
                ", duration:" +
                DateUtil.millisToSeconds(stopWatch.getTime()) + " seconds");

        return methodReturn;
    } // invoke.
} // E:O:F:LogTimeMethodInterceptor.
