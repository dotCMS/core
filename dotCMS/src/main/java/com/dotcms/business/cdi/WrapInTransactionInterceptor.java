package com.dotcms.business.cdi;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.business.interceptor.WrapInTransactionHandler;
import com.dotcms.business.interceptor.WrapInTransactionHandler.TransactionState;

import java.io.Serializable;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * CDI interceptor for {@link WrapInTransaction}. Delegates to
 * {@link WrapInTransactionHandler} for the actual logic, keeping the implementation DRY
 * with the ByteBuddy advice.
 */
@Interceptor
@WrapInTransaction
@Priority(Interceptor.Priority.APPLICATION + 1)
public class WrapInTransactionInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {
        if (!InterceptorGuard.acquire(WrapInTransaction.class)) {
            return context.proceed();
        }

        TransactionState state = null;
        try {
            state = WrapInTransactionHandler.onEnter();

            final Object result = context.proceed();

            WrapInTransactionHandler.onSuccess(state);
            return result;
        } catch (Throwable t) {
            WrapInTransactionHandler.onError(state);
            if (t instanceof Exception) {
                throw (Exception) t;
            }
            throw new RuntimeException(t);
        } finally {
            WrapInTransactionHandler.onFinally(state);
            InterceptorGuard.release(WrapInTransaction.class);
        }
    }
}
