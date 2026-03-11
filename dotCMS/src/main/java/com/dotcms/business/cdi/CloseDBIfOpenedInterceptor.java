package com.dotcms.business.cdi;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;

import java.io.Serializable;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * CDI interceptor for {@link CloseDBIfOpened}. Closes the database connection if a new one
 * was opened during method execution.
 *
 * <p>This interceptor fires at the Weld proxy boundary for CDI-managed beans, complementing
 * the ByteBuddy advice that instruments non-CDI classes at load-time. The
 * {@link InterceptorGuard} prevents double-processing when both mechanisms are active.</p>
 */
@Interceptor
@CloseDBIfOpened
@Priority(Interceptor.Priority.APPLICATION)
public class CloseDBIfOpenedInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {
        if (!InterceptorGuard.acquire(CloseDBIfOpened.class)) {
            return context.proceed();
        }

        final boolean isNewConnection = !DbConnectionFactory.connectionExists();
        try {
            return context.proceed();
        } finally {
            try {
                final Method method = context.getMethod();
                final CloseDBIfOpened annotation = method.getAnnotation(CloseDBIfOpened.class);
                if (annotation != null && annotation.connection() && isNewConnection) {
                    DbConnectionFactory.closeSilently();
                }
            } finally {
                InterceptorGuard.release(CloseDBIfOpened.class);
            }
        }
    }
}
