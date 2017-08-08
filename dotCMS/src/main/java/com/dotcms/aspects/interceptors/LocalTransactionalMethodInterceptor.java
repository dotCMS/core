package com.dotcms.aspects.interceptors;

import com.dotcms.aspects.DelegateMethodInvocation;
import com.dotcms.aspects.MethodInterceptor;
import com.dotcms.business.LocalTransactional;
import com.dotmarketing.db.LocalTransaction;

/**
 * Method handler for the {@link LocalTransactional} annotation aspect
 * @author jsanca
 */
public class LocalTransactionalMethodInterceptor implements MethodInterceptor<Object> {

    public static final LocalTransactionalMethodInterceptor INSTANCE = new LocalTransactionalMethodInterceptor();

    protected LocalTransactionalMethodInterceptor() {

    }

    @Override
    public Object invoke(final DelegateMethodInvocation<Object> delegate) throws Throwable {

        return LocalTransaction.wrapReturn(delegate::proceed);
    } // invoke.

} // E:O:F:LogTimeMethodInterceptor.