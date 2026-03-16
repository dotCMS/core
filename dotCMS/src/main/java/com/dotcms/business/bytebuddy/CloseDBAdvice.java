package com.dotcms.business.bytebuddy;

import com.dotcms.business.interceptor.CloseDBHandler;
import net.bytebuddy.asm.Advice;

/**
 * ByteBuddy advice for {@code @CloseDB}. Delegates to {@link CloseDBHandler}
 * for the actual logic, keeping the implementation DRY with the CDI interceptor.
 */
public class CloseDBAdvice {

    @Advice.OnMethodExit(inline = false, onThrowable = Throwable.class)
    public static void exit(@Advice.Thrown Throwable t) throws Throwable {
        CloseDBHandler.onExit();
    }
}
