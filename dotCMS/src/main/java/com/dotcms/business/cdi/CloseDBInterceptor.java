package com.dotcms.business.cdi;

import com.dotcms.business.CloseDB;
import com.dotcms.business.interceptor.CloseDBHandler;

import java.io.Serializable;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * CDI interceptor for {@link CloseDB}. Delegates to {@link CloseDBHandler}
 * for the actual logic, keeping the implementation DRY with the ByteBuddy advice.
 */
@Interceptor
@CloseDB
@Priority(Interceptor.Priority.APPLICATION)
public class CloseDBInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {
        if (!InterceptorGuard.acquire(CloseDB.class)) {
            return context.proceed();
        }

        try {
            return context.proceed();
        } finally {
            try {
                CloseDBHandler.onExit();
            } finally {
                InterceptorGuard.release(CloseDB.class);
            }
        }
    }
}
