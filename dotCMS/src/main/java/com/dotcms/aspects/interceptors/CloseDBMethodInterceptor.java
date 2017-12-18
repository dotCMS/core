package com.dotcms.aspects.interceptors;

import static com.dotcms.util.AnnotationUtils.getMethodAnnotation;

import com.dotcms.aspects.DelegateMethodInvocation;
import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.business.CloseDB;


import com.dotmarketing.db.DbConnectionFactory;

/**
 * Method handler for the {@link CloseDBIfOpened} annotation aspect
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

                DbConnectionFactory.closeSilently();
        }

        return methodReturn;
    } // invoke.
} // E:O:F:LogTimeMethodInterceptor.