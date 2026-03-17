package com.dotcms.business.bytebuddy;

import com.dotcms.business.interceptor.LogTimeHandler;
import com.dotcms.util.LogTime;
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang.time.StopWatch;

import java.lang.reflect.Method;

/**
 * ByteBuddy advice for {@link LogTime}. Delegates to {@link LogTimeHandler} for the actual
 * logging logic, keeping the implementation DRY with the CDI interceptor.
 *
 * @author spbolton
 */
public class LogTimeAdvice {

    @Advice.OnMethodEnter
    static TimerInfo enter(final @Advice.Origin Method method) {
        return new TimerInfo(method, method.getAnnotation(LogTime.class).loggingLevel());
    }

    @Advice.OnMethodExit
    static void exit(@Advice.Enter TimerInfo timerInfo) {
        if (timerInfo != null) {
            timerInfo.stop();
        }
    }

    public static class TimerInfo {
        private final StopWatch stopWatch;
        private final String loggingLevel;
        private final Class<?> clazz;
        private final String methodName;

        public TimerInfo(final Method method, final String loggingLevel) {
            this.clazz = method.getDeclaringClass();
            this.methodName = method.getName();
            this.stopWatch = new StopWatch();
            this.stopWatch.start();
            this.loggingLevel = loggingLevel;
        }

        public void stop() {
            stopWatch.stop();
            LogTimeHandler.logTime(clazz, methodName, stopWatch.getTime(), loggingLevel);
        }
    }
}
