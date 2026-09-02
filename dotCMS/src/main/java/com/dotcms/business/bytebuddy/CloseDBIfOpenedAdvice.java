package com.dotcms.business.bytebuddy;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.interceptor.CloseDBIfOpenedHandler;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

import static com.dotcms.util.AnnotationUtils.getMethodAnnotation;

/**
 * ByteBuddy advice for {@link CloseDBIfOpened}. Delegates to {@link CloseDBIfOpenedHandler}
 * for the actual logic, keeping the implementation DRY with the CDI interceptor.
 *
 * @author spbolton
 */
public class CloseDBIfOpenedAdvice {

    @Advice.OnMethodEnter(inline = false)
    public static boolean enter(final @Advice.Origin Method method) {
        return CloseDBIfOpenedHandler.onEnter();
    }

    @Advice.OnMethodExit(inline = false, onThrowable = Throwable.class)
    public static void exit(final @Advice.Origin Method method,
                            @Advice.Enter boolean isNewConnection) {
        final CloseDBIfOpened closeDB = getMethodAnnotation(method, CloseDBIfOpened.class);
        if (null != closeDB) {
            CloseDBIfOpenedHandler.onExit(isNewConnection, closeDB.connection());
        }
    }
}
