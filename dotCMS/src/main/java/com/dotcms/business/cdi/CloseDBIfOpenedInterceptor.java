package com.dotcms.business.cdi;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.interceptor.CloseDBIfOpenedHandler;

import java.io.Serializable;
import java.lang.reflect.Method;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * CDI interceptor for {@link CloseDBIfOpened}. Delegates to {@link CloseDBIfOpenedHandler}
 * for the actual logic, keeping the implementation DRY with the ByteBuddy advice.
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

        final boolean isNewConnection = CloseDBIfOpenedHandler.onEnter();
        try {
            return context.proceed();
        } finally {
            try {
                final Method method = context.getMethod();
                final CloseDBIfOpened annotation = method.getAnnotation(CloseDBIfOpened.class);
                final boolean connectionParam = annotation == null || annotation.connection();
                CloseDBIfOpenedHandler.onExit(isNewConnection, connectionParam);
            } finally {
                InterceptorGuard.release(CloseDBIfOpened.class);
            }
        }
    }
}
