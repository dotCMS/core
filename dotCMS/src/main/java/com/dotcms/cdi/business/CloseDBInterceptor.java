package com.dotcms.cdi.business;

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
        try {
            return context.proceed();
        } finally {
            CloseDBHandler.onExit();
        }
    }
}
