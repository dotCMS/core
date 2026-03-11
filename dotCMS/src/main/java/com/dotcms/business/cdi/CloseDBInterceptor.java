package com.dotcms.business.cdi;

import com.dotcms.business.CloseDB;
import com.dotmarketing.db.DbConnectionFactory;

import java.io.Serializable;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * CDI interceptor for {@link CloseDB}. Always closes and commits the database connection
 * after method execution, regardless of whether the connection existed before the call.
 *
 * <p>This interceptor fires at the Weld proxy boundary for CDI-managed beans, complementing
 * the ByteBuddy advice that instruments non-CDI classes at load-time. The
 * {@link InterceptorGuard} prevents double-processing when both mechanisms are active.</p>
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
                DbConnectionFactory.closeAndCommit();
            } finally {
                try {
                    DbConnectionFactory.closeSilently();
                } finally {
                    InterceptorGuard.release(CloseDB.class);
                }
            }
        }
    }
}
