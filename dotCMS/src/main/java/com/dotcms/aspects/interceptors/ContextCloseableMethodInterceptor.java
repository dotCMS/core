package com.dotcms.aspects.interceptors;

import com.dotcms.aspects.DelegateMethodInvocation;
import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.business.ContextCloseable;
import com.dotmarketing.db.DbConnectionFactory;

import static com.dotcms.util.AnnotationUtils.getMethodAnnotation;

/**
 * Method handler for the {@link com.dotcms.business.ContextCloseable} annotation aspect
 * @author jsanca
 */
public class ContextCloseableMethodInterceptor implements MethodInterceptor<Object> {

    public static final ContextCloseableMethodInterceptor INSTANCE = new ContextCloseableMethodInterceptor();

    public ContextCloseableMethodInterceptor() {

    }

    @Override
    public Object invoke(final DelegateMethodInvocation<Object> delegate) throws Throwable {


        final ContextCloseable contextCloseable =
                getMethodAnnotation(delegate.getMethod(), ContextCloseable.class);
        Object methodReturn = null;

        try {
            methodReturn = delegate.proceed();
        } finally {

            if (null != contextCloseable && contextCloseable.connection()
                    && !DbConnectionFactory.inTransaction()) {

                DbConnectionFactory.closeSilently();
            }
        }

        return methodReturn;
    } // invoke.
} // E:O:F:LogTimeMethodInterceptor.