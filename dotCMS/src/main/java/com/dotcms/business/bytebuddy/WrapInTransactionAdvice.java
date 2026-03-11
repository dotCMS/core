package com.dotcms.business.bytebuddy;

import com.dotcms.business.interceptor.WrapInTransactionHandler;
import com.dotcms.business.interceptor.WrapInTransactionHandler.TransactionState;
import net.bytebuddy.asm.Advice;

/**
 * ByteBuddy advice for {@code @WrapInTransaction}. Delegates to
 * {@link WrapInTransactionHandler} for the actual logic, keeping the implementation DRY
 * with the CDI interceptor.
 */
public class WrapInTransactionAdvice {

    @Advice.OnMethodEnter(inline = false)
    public static TransactionState enter(@Advice.Origin("#m") String methodName) throws Exception {
        return WrapInTransactionHandler.onEnter();
    }

    @Advice.OnMethodExit(inline = false, onThrowable = Throwable.class)
    public static void exit(@Advice.Enter TransactionState state,
                            @Advice.Thrown Throwable t) throws Throwable {
        try {
            if (state != null) {
                if (t != null) {
                    if (state.isLocalTransaction) {
                        WrapInTransactionHandler.onError(state, t);
                    }
                } else if (state.isLocalTransaction) {
                    WrapInTransactionHandler.onSuccess(state);
                }
            }
        } finally {
            WrapInTransactionHandler.onFinally(state);
        }

        if (t != null) {
            throw t;
        }
    }
}
