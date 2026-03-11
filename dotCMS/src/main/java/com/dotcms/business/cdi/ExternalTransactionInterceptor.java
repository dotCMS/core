package com.dotcms.business.cdi;

import com.dotcms.business.ExternalTransaction;
import com.dotcms.business.interceptor.ExternalTransactionHandler;
import com.dotcms.business.interceptor.ExternalTransactionHandler.ExternalTransactionState;

import java.io.Serializable;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * CDI interceptor for {@link ExternalTransaction}. Delegates to
 * {@link ExternalTransactionHandler} for the actual logic, keeping the implementation DRY
 * with the ByteBuddy advice.
 *
 * <p>Nesting is safe: external transactions always create a new connection and restore
 * the previous one on exit, regardless of nesting depth.</p>
 */
@Interceptor
@ExternalTransaction
@Priority(Interceptor.Priority.APPLICATION + 2)
public class ExternalTransactionInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {
        ExternalTransactionState state = null;
        try {
            state = ExternalTransactionHandler.onEnter();

            final Object result = context.proceed();

            ExternalTransactionHandler.onSuccess(state);
            return result;
        } catch (Throwable t) {
            ExternalTransactionHandler.onError(state);
            if (t instanceof Exception) {
                throw (Exception) t;
            }
            throw new RuntimeException(t);
        } finally {
            ExternalTransactionHandler.onFinally(state);
        }
    }
}
