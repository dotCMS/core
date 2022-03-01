package com.dotcms.business.bytebuddy;

import com.dotcms.util.LogTime;
import com.dotmarketing.util.Logger;
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang.time.StopWatch;
import org.apache.logging.log4j.Level;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public class LogTimeAdvice {

    @Advice.OnMethodEnter
    static TimerInfo enter(final @Advice.Origin Method method) {
        return new TimerInfo(method,method.getAnnotation(LogTime.class).loggingLevel());
    }

    @Advice.OnMethodExit
    static void exit(@Advice.Enter TimerInfo timerInfo) {
        if (timerInfo!=null)
        {
            timerInfo.stop();
        }
    }

    public static class TimerInfo {
        private StopWatch stopWatch;
        private String loggingLevel;
        private Supplier<String> messageSupplier;
        private Class<?> clazz;

        public TimerInfo(final Method method, final String loggingLevel) {
            clazz = method.getDeclaringClass();
            messageSupplier = () -> "Call for class: " +
                    clazz.getName() + "#" +
                    method.getName();

            stopWatch = new StopWatch();
            stopWatch.start();
            this.loggingLevel = loggingLevel;
        }

        public void stop() {
            stopWatch.stop();
            if (Level.INFO.toString().equals(loggingLevel)) {
                Logger.info(clazz, messageSupplier.get() +
                        ", duration:" +
                        stopWatch.getTime() + " millis");
            } else {
                Logger.debug(clazz, messageSupplier.get() +
                        ", duration:" +
                        stopWatch.getTime() + " millis");
            }
        }
    }
}
