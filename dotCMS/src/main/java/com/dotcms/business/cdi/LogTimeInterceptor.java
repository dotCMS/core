package com.dotcms.business.cdi;

import com.dotcms.business.interceptor.LogTimeHandler;
import com.dotcms.util.LogTime;
import org.apache.commons.lang.time.StopWatch;

import java.io.Serializable;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * CDI interceptor for {@link LogTime}. Delegates to {@link LogTimeHandler} for the actual
 * logging logic, keeping the implementation DRY with the ByteBuddy advice.
 */
@Interceptor
@LogTime
@Priority(Interceptor.Priority.APPLICATION + 10)
public class LogTimeInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {
        if (!InterceptorGuard.acquire(LogTime.class)) {
            return context.proceed();
        }

        final StopWatch stopWatch = new StopWatch();
        try {
            stopWatch.start();
            return context.proceed();
        } finally {
            stopWatch.stop();
            try {
                final Method method = context.getMethod();
                final LogTime annotation = method.getAnnotation(LogTime.class);
                final String loggingLevel = (annotation != null)
                        ? annotation.loggingLevel() : "DEBUG";
                LogTimeHandler.logTime(method.getDeclaringClass(), method.getName(),
                        stopWatch.getTime(), loggingLevel);
            } finally {
                InterceptorGuard.release(LogTime.class);
            }
        }
    }
}
