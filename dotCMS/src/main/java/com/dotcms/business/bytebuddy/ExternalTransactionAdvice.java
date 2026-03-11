package com.dotcms.business.bytebuddy;

import com.dotcms.business.interceptor.ExternalTransactionHandler;
import com.dotcms.business.interceptor.ExternalTransactionHandler.ExternalTransactionState;
import net.bytebuddy.asm.Advice;

/**
 * ByteBuddy advice for {@code @ExternalTransaction}. Delegates to
 * {@link ExternalTransactionHandler} for the actual logic, keeping the implementation DRY
 * with the CDI interceptor.
 */
public class ExternalTransactionAdvice {

    @Advice.OnMethodEnter(inline = false)
    public static ExternalTransactionState enter(@Advice.Origin("#m") String methodName) throws Exception {
        return ExternalTransactionHandler.onEnter();
    }

    @Advice.OnMethodExit(inline = false, onThrowable = Throwable.class)
    public static void exit(@Advice.Enter final ExternalTransactionState state,
                            @Advice.Thrown final Throwable throwable) throws Throwable {
        try {
            if (state != null) {
                if (throwable != null) {
                    ExternalTransactionHandler.onError(state, throwable);
                } else {
                    ExternalTransactionHandler.onSuccess(state);
                }
            }
        } finally {
            ExternalTransactionHandler.onFinally(state);
        }

        if (throwable != null) {
            throw throwable;
        }
    }
}
