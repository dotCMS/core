package com.dotcms.aspects.interceptors;

import com.dotcms.aspects.DelegateMethodInvocation;
import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.business.CloseDB;
import com.dotmarketing.db.DbConnectionFactory;

import static com.dotcms.util.AnnotationUtils.getMethodAnnotation;

/**
 * Method handler for the {@link CloseDB} annotation aspect
 * @author jsanca
 */
public class CloseDBMethodInterceptor implements MethodInterceptor<Object> {

    public static final CloseDBMethodInterceptor INSTANCE = new CloseDBMethodInterceptor();


    @Override
    public Object invoke(final DelegateMethodInvocation<Object> delegate) throws Throwable {


        final CloseDB closeDB =
                getMethodAnnotation(delegate.getMethod(), CloseDB.class);
        Object methodReturn = null;

        try {
            methodReturn = delegate.proceed();
        } finally {

            if (null != closeDB && closeDB.connection()
                    && !DbConnectionFactory.inTransaction()) {

                DbConnectionFactory.closeSilently();
            }
        }

        return methodReturn;
    } // invoke.
} // E:O:F:LogTimeMethodInterceptor.