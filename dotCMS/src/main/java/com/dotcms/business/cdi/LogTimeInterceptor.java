package com.dotcms.business.cdi;

import com.dotcms.util.LogTime;
import com.dotmarketing.util.Logger;
import org.apache.commons.lang.time.StopWatch;
import org.apache.logging.log4j.Level;

import java.io.Serializable;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * CDI interceptor for {@link LogTime}. Measures and logs the execution time of annotated methods.
 *
 * <p>This interceptor fires at the Weld proxy boundary for CDI-managed beans, complementing
 * the ByteBuddy advice that instruments non-CDI classes at load-time. The
 * {@link InterceptorGuard} prevents double-processing when both mechanisms are active.</p>
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
                final Class<?> clazz = method.getDeclaringClass();
                final String message = "Call for class: " + clazz.getName() + "#"
                        + method.getName() + ", duration:" + stopWatch.getTime() + " millis";

                if (Level.INFO.toString().equals(loggingLevel)) {
                    Logger.info(clazz, message);
                } else {
                    Logger.debug(clazz, message);
                }
            } finally {
                InterceptorGuard.release(LogTime.class);
            }
        }
    }
}
