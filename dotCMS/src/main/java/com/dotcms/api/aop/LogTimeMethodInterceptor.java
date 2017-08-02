package com.dotcms.api.aop;

import com.dotcms.api.aop.guice.MethodInterceptorConfig;
import com.dotcms.repackage.org.apache.commons.lang.time.StopWatch;
import com.dotcms.util.LogTime;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;

import java.lang.reflect.Method;

/**
 * Method handler for the {@link com.dotcms.util.LogTime} annotation aspect
 * @author jsanca
 */
@MethodInterceptorConfig(annotation = LogTime.class)
public class LogTimeMethodInterceptor implements MethodInterceptor<Object> {

    public static final LogTimeMethodInterceptor INSTANCE = new LogTimeMethodInterceptor();

    @Override
    public Object invoke(final DelegateMethodInvocation<Object> delegate) throws Throwable {

        Object methodReturn = null;

        if (Logger.isDebugEnabled(LogTimeMethodInterceptor.class)) {
            final StopWatch stopWatch = new StopWatch();
            final Object target = delegate.getTarget();
            final Method method = delegate.getMethod();

            stopWatch.start();

            methodReturn = delegate.proceed();

            stopWatch.stop();

            Logger.debug(this, "Call for class: " +
                    ((null != target) ? target.getClass().getName() : "Null") +
                    "#" + ((null != method) ? method.getName() : "Null") +
                    ", duration:" +
                    DateUtil.millisToSeconds(stopWatch.getTime()) + " seconds");
        } else {

            methodReturn = delegate.proceed();
        }

        return methodReturn;
    } // invoke.

} // E:O:F:LogTimeMethodInterceptor.